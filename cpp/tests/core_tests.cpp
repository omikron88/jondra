#include "jondra/binary_file.hpp"
#include "jondra/embedded_roms.hpp"
#include "jondra/keyboard.hpp"
#include "jondra/machine.hpp"
#include "jondra/memory.hpp"

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
    const std::vector<std::uint8_t> file(
        std::istreambuf_iterator<char>(stream),
        std::istreambuf_iterator<char>());
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

} // namespace

int main() {
    test_keyboard_matrix();
    test_memory_mapping();
    test_rom_and_cpu();
    test_machine_ports_and_video();
    test_embedded_roms();
    test_binary_files();
    std::cout << "All jondra core tests passed\n";
}
