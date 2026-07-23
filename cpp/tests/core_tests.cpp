#include "jondra/keyboard.hpp"
#include "jondra/machine.hpp"
#include "jondra/memory.hpp"

#include <cstdlib>
#include <filesystem>
#include <iostream>

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
#endif
}

} // namespace

int main() {
    test_keyboard_matrix();
    test_memory_mapping();
    test_rom_and_cpu();
    test_machine_ports_and_video();
    test_embedded_roms();
    std::cout << "All jondra core tests passed\n";
}
