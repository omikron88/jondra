#include "jondra/binary_file.hpp"
#include "jondra/embedded_roms.hpp"
#include "jondra/keyboard.hpp"
#include "jondra/machine.hpp"
#include "jondra/memory.hpp"
#include "jondra/snapshot.hpp"
#include "jondra/tape.hpp"

#include <algorithm>
#include <array>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <vector>

namespace {

void check(bool condition, const char* expression, int line) {
    if(condition)
        return;
    std::cerr << "CHECK failed at line " << line << ": " << expression << '\n';
    std::exit(1);
}

#define CHECK(expression) check((expression), #expression, __LINE__)

void test_keyboard_matrix() {
    jondra::Keyboard keyboard;
    CHECK(keyboard.read(0) == 0xff);
    keyboard.set(jondra::Key::Q, true);
    CHECK(keyboard.read(0) == 0xef);
    keyboard.set(jondra::Key::Q, false);
    CHECK(keyboard.read(0) == 0xff);
    keyboard.set(jondra::Key::JoyRight, true);
    CHECK(keyboard.read(9) == 0xfe);
}

void test_memory_mapping() {
    jondra::Keyboard keyboard;
    jondra::Memory memory(keyboard);
    memory.write_ram(0, 0x42);
    memory.map_rom(false);
    CHECK(memory.read(0) == 0x42);

    memory.map_io(true);
    keyboard.set(jondra::Key::A, true);
    CHECK(memory.read(0xe001) == 0xef);
    memory.write_ram(0xe001, 0x5a);
    memory.write(0xe001, 0);
    CHECK(memory.read_ram(0xe001) == 0x5a);
}

void test_rom_and_cpu() {
    jondra::Machine machine;
    for(const auto type : {jondra::RomType::Basic, jondra::RomType::Tesla,
                           jondra::RomType::Vili, jondra::RomType::Plus})
        machine.load_rom(type, JONDRA_DEFAULT_ROM_DIR);

    machine.load_rom(jondra::RomType::Basic, JONDRA_DEFAULT_ROM_DIR);
    CHECK(machine.memory().read(0) != machine.memory().read_ram(0));
    const auto before = machine.ticks();
    machine.run_frame();
    CHECK(machine.ticks() >= before + jondra::Machine::default_frame_ticks);
    CHECK(machine.get_pc() != 0);
}

void test_machine_ports_and_video() {
    jondra::Machine machine;
    CHECK(machine.memory().rom_mapped());
    CHECK(!machine.memory().io_mapped());

    machine.on_output(0x0000, 0x06);
    CHECK(!machine.memory().rom_mapped());
    CHECK(machine.memory().io_mapped());
    CHECK(!machine.dma_enabled());

    machine.on_output(0x0000, 0x01);
    CHECK(machine.memory().rom_mapped());
    CHECK(!machine.memory().io_mapped());
    CHECK(machine.dma_enabled());

    machine.on_write(0xffff, 0xff);
    bool lit_pixel_byte = false;
    for(const auto value : machine.framebuffer())
        lit_pixel_byte |= value != 0;
    CHECK(lit_pixel_byte);
}

void test_embedded_roms() {
#ifdef JONDRA_HAS_EMBEDDED_ROMS
    jondra::Keyboard keyboard;
    jondra::Memory memory(keyboard);
    memory.load_rom(jondra::RomType::Basic, "directory-that-does-not-exist");
    CHECK(memory.read(0) != memory.read_ram(0));

    const auto rom_path =
        std::filesystem::path(JONDRA_DEFAULT_ROM_DIR) / "Ondra_PLUS_a.rom";
    std::ifstream stream(rom_path, std::ios::binary);
    const std::vector<std::uint8_t> file{
        std::istreambuf_iterator<char>{stream},
        std::istreambuf_iterator<char>{}};
    const auto embedded = jondra::embedded_rom("Ondra_PLUS_a.rom");
    CHECK(file.size() == embedded.size());
    CHECK(std::equal(file.begin(), file.end(), embedded.begin()));
#endif
}

void test_binary_files() {
    const auto path =
        std::filesystem::temp_directory_path() / "jondra-core-binary-test.bin";
    jondra::Machine machine;

    {
        const std::array<unsigned char, 4> data{0x21, 0x34, 0x12, 0xc9};
        std::ofstream stream(path, std::ios::binary | std::ios::trunc);
        stream.write(reinterpret_cast<const char*>(data.data()), data.size());
    }
    jondra::BinaryLoadOptions raw;
    raw.load_address = 0x4000;
    raw.run_after_load = true;
    raw.run_address = 0x4000;
    const auto raw_result = jondra::load_binary_file(path, machine, raw);
    CHECK(raw_result.bytes_loaded == 4);
    CHECK(raw_result.run_address == 0x4000);
    CHECK(machine.get_pc() == 0x4000);
    CHECK(machine.memory().read(0x4002) == 0x12);

    {
        const std::array<unsigned char, 10> data{
            1, 0x00, 0x10, 0x02, 0x00, 0xaa, 0x55, 2, 0x00, 0x10};
        std::ofstream stream(path, std::ios::binary | std::ios::trunc);
        stream.write(reinterpret_cast<const char*>(data.data()), data.size());
    }
    jondra::BinaryLoadOptions header;
    header.has_header = true;
    header.all_ram = true;
    const auto header_result = jondra::load_binary_file(path, machine, header);
    CHECK(header_result.bytes_loaded == 2);
    CHECK(header_result.run_address == 0x1000);
    CHECK(machine.memory().read_ram(0x1000) == 0xaa);
    CHECK(machine.memory().read_ram(0x1001) == 0x55);
    CHECK(machine.memory().rom_mapped());

    {
        const std::array<unsigned char, 9> truncated{
            1, 0x00, 0x40, 0x01, 0x00, 0x11, 1, 0x00, 0x50};
        std::ofstream output(path, std::ios::binary | std::ios::trunc);
        output.write(reinterpret_cast<const char*>(truncated.data()),
                     truncated.size());
    }
    const auto unchanged = machine.memory().read_ram(0x4000);
    bool rejected = false;
    try {
        jondra::load_binary_file(path, machine, header);
    } catch(const std::exception&) {
        rejected = true;
    }
    CHECK(rejected);
    CHECK(machine.memory().read_ram(0x4000) == unchanged);

    machine.memory().map_rom(false);
    const auto saved = jondra::save_binary_file(path, machine, 0x1000, 0x1001);
    CHECK(saved == 2);
    std::ifstream stream(path, std::ios::binary);
    CHECK(stream.get() == 0xaa);
    CHECK(stream.get() == 0x55);

    std::error_code error;
    std::filesystem::remove(path, error);
}

void test_snapshots() {
    const auto path =
        std::filesystem::temp_directory_path() / "jondra-core-snapshot-test.osn";
    jondra::Machine machine;
    machine.load_rom(jondra::RomType::Vili, JONDRA_DEFAULT_ROM_DIR);
    machine.set_i(0x12);
    machine.set_alt_hl(0x2345);
    machine.set_alt_de(0x3456);
    machine.set_alt_bc(0x4567);
    machine.set_alt_af(0x5678);
    machine.set_hl(0x6789);
    machine.set_de(0x789a);
    machine.set_bc(0x89ab);
    machine.set_iy(0x9abc);
    machine.set_ix(0xabcd);
    machine.set_iff1(true);
    machine.set_iff2(false);
    machine.set_is_int_disabled(true);
    machine.set_is_halted(true);
    machine.set_r(0xbc);
    machine.set_af(0xcdef);
    machine.set_sp(0xdef0);
    machine.set_pc(0xef01);
    machine.set_int_mode(2);
    machine.set_wz(0xf012);
    machine.write_memory(0x0000, 0xa5, true);
    machine.write_memory(0xd800, 0x5a, true);
    machine.restore_snapshot_peripherals(0x11, 0x22, 0x07, 0xfe);

    jondra::save_snapshot_file(path, machine);
    CHECK(std::filesystem::file_size(path) == 65'575);
    {
        std::ifstream stream(path, std::ios::binary);
        CHECK(stream.get() == 'O');
        CHECK(stream.get() == 'S');
        CHECK(stream.get() == 'N');
        CHECK(stream.get() == 2);
    }

    jondra::Machine restored;
    const auto rom =
        jondra::load_snapshot_file(path, restored, JONDRA_DEFAULT_ROM_DIR);
    CHECK(rom == jondra::RomType::Vili);
    CHECK(restored.rom_type() == jondra::RomType::Vili);
    CHECK(restored.get_i() == 0x12);
    CHECK(restored.get_alt_hl() == 0x2345);
    CHECK(restored.get_alt_de() == 0x3456);
    CHECK(restored.get_alt_bc() == 0x4567);
    CHECK(restored.get_alt_af() == 0x5678);
    CHECK(restored.get_hl() == 0x6789);
    CHECK(restored.get_de() == 0x789a);
    CHECK(restored.get_bc() == 0x89ab);
    CHECK(restored.get_iy() == 0x9abc);
    CHECK(restored.get_ix() == 0xabcd);
    CHECK(restored.get_iff1());
    CHECK(!restored.get_iff2());
    CHECK(restored.is_int_disabled());
    CHECK(restored.is_halted());
    CHECK(restored.get_r() == 0xbc);
    CHECK(restored.get_af() == 0xcdef);
    CHECK(restored.get_sp() == 0xdef0);
    CHECK(restored.get_pc() == 0xef01);
    CHECK(restored.get_int_mode() == 2);
    CHECK(restored.get_wz() == 0xf012);
    CHECK(restored.port_a0() == 0x11);
    CHECK(restored.port_a1() == 0x22);
    CHECK(restored.port_a3() == 0x07);
    CHECK(restored.resolution() == 0xfe);
    CHECK(!restored.memory().rom_mapped());
    CHECK(restored.memory().io_mapped());
    CHECK(restored.dma_enabled());
    CHECK(restored.memory().read_ram(0x0000) == 0xa5);
    CHECK(restored.memory().read_ram(0xd800) == 0x5a);

    {
        std::ofstream stream(path, std::ios::binary | std::ios::trunc);
        stream.write("OSN", 3);
    }
    restored.set_pc(0x1357);
    bool rejected = false;
    try {
        jondra::load_snapshot_file(path, restored, JONDRA_DEFAULT_ROM_DIR);
    } catch(const std::exception&) {
        rejected = true;
    }
    CHECK(rejected);
    CHECK(restored.get_pc() == 0x1357);

    std::error_code error;
    std::filesystem::remove(path, error);
}

void write_test_wav(const std::filesystem::path& path) {
    const std::array<std::uint8_t, 4> samples{0, 255, 0, 255};
    std::ofstream stream(path, std::ios::binary | std::ios::trunc);
    const auto write_u16 = [&stream](std::uint16_t value) {
        stream.put(static_cast<char>(value));
        stream.put(static_cast<char>(value >> 8u));
    };
    const auto write_u32 = [&stream](std::uint32_t value) {
        for(unsigned shift = 0; shift < 32; shift += 8)
            stream.put(static_cast<char>(value >> shift));
    };
    stream.write("RIFF", 4);
    write_u32(36u + samples.size());
    stream.write("WAVEfmt ", 8);
    write_u32(16);
    write_u16(1);
    write_u16(1);
    write_u32(1'000);
    write_u32(1'000);
    write_u16(1);
    write_u16(8);
    stream.write("data", 4);
    write_u32(samples.size());
    stream.write(reinterpret_cast<const char*>(samples.data()),
                 samples.size());
}

void test_tape() {
    const auto temporary = std::filesystem::temp_directory_path();
    const auto wav_path = temporary / "jondra-core-tape-test.wav";
    const auto csw_path = temporary / "jondra-core-tape-test.csw";
    write_test_wav(wav_path);

    jondra::Tape tape;
    tape.open_playback(wav_path);
    CHECK(tape.mode() == jondra::TapeMode::Playback);
    CHECK(tape.sample_rate() == 1'000);
    CHECK(tape.length() == 4);
    CHECK(!tape.input_high());
    tape.set_motor(true);
    tape.advance(2'000, false);
    CHECK(tape.position() == 1);
    CHECK(tape.input_high());
    tape.pause();
    CHECK(tape.transport() == jondra::TapeTransport::Paused);
    CHECK(!tape.motor_running());
    tape.advance(2'000, false);
    CHECK(tape.position() == 1);
    tape.set_motor(true);
    CHECK(!tape.motor_running());
    tape.play();
    CHECK(tape.transport() == jondra::TapeTransport::Playing);
    CHECK(tape.motor_running());
    tape.stop();
    CHECK(tape.transport() == jondra::TapeTransport::Stopped);
    CHECK(!tape.motor_running());
    tape.play();
    CHECK(tape.motor_running());
    tape.advance(6'000, false);
    CHECK(tape.finished());
    CHECK(!tape.motor_running());
    tape.rewind();
    CHECK(!tape.finished());
    CHECK(tape.motor_running());

    jondra::Machine machine;
    machine.tape().open_playback(wav_path);
    machine.on_output(0x000e, 0x10);
    CHECK(machine.tape().motor_running());
    machine.on_output(0x0003, 0x04);
    machine.on_tick(2'000);
    CHECK((machine.memory().read(0xe000) & 0x80u) != 0);
    machine.on_output(0x000e, 0x00);
    CHECK(!machine.tape().motor_running());

    tape.open_recording(csw_path);
    tape.set_motor(true);
    tape.advance(91, false);
    tape.advance(91, false);
    tape.advance(91, true);
    tape.advance(91, true);
    tape.set_motor(false);
    CHECK(std::filesystem::file_size(csw_path) > 52);

    jondra::Tape recorded;
    recorded.open_playback(csw_path);
    CHECK(recorded.mode() == jondra::TapeMode::Playback);
    CHECK(recorded.sample_rate() == jondra::Tape::recording_rate);
    CHECK(recorded.length() == 4);

    const auto repository =
        std::filesystem::path(JONDRA_DEFAULT_ROM_DIR).parent_path().parent_path();
    const auto tap_path =
        repository / "ONDRA - PLUS" / "Ondra mezi balvany" /
        "Ondra_mezi_balvany_ViLi.tap";
    jondra::Tape binary_tape;
    binary_tape.open_playback(tap_path);
    CHECK(binary_tape.sample_rate() == jondra::Tape::tap_sample_rate);
    const auto short_pulse_ns =
        70'000'000'000ull / binary_tape.sample_rate();
    const auto long_pulse_ns =
        140'000'000'000ull / binary_tape.sample_rate();
    CHECK(short_pulse_ns >= 219'000 && short_pulse_ns <= 221'000);
    CHECK(long_pulse_ns >= 439'000 && long_pulse_ns <= 441'000);
    // This exact length also guards the Java-compatible byte truncation and
    // per-block alignment used by TapFile.tapbuffer.
    CHECK(binary_tape.length() == 32'946'200);

    std::error_code error;
    std::filesystem::remove(wav_path, error);
    std::filesystem::remove(csw_path, error);
}

} // namespace

int main() {
    test_keyboard_matrix();
    test_memory_mapping();
    test_rom_and_cpu();
    test_machine_ports_and_video();
    test_embedded_roms();
    test_binary_files();
    test_snapshots();
    test_tape();
    std::cout << "All jondra core tests passed\n";
}
