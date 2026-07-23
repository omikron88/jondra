#include "jondra/memory.hpp"

#include "jondra/embedded_roms.hpp"

#include <algorithm>
#include <fstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace jondra {

Memory::Memory(Keyboard& keyboard)
    : keyboard_(keyboard) {
    reset();
}

void Memory::reset(bool dirty) {
    if(dirty) {
        std::uint8_t value = 0;
        for(std::size_t i = 0; i < ram_.size(); ++i) {
            ram_[i] = value;
            if((i & 127u) == 127u)
                value ^= 0xffu;
        }
    }
    rom_mapped_ = true;
    io_mapped_ = false;
}

void Memory::load_fragment(const std::filesystem::path& path,
                           std::span<std::uint8_t> destination,
                           bool repeat_to_fill) {
    std::vector<std::uint8_t> external_source;
    std::span<const std::uint8_t> source;
    if(std::filesystem::exists(path)) {
        std::ifstream stream(path, std::ios::binary);
        if(!stream)
            throw std::runtime_error("Cannot open ROM: " + path.string());
        external_source.assign(
            std::istreambuf_iterator<char>(stream),
            std::istreambuf_iterator<char>());
        source = external_source;
    } else {
        source = embedded_rom(path.filename().string());
    }

    if(source.empty())
        throw std::runtime_error("Cannot open ROM: " + path.string());
    if((!repeat_to_fill && source.size() != destination.size()) ||
       (repeat_to_fill && destination.size() % source.size() != 0)) {
        throw std::runtime_error("Unexpected ROM size: " + path.string());
    }

    for(std::size_t i = 0; i < destination.size(); ++i)
        destination[i] = source[i % source.size()];
}

void Memory::load_rom(RomType type, const std::filesystem::path& directory) {
    std::string_view first;
    std::string_view second;
    std::size_t half_size = 8'192;
    bool repeat = false;

    switch(type) {
    case RomType::Basic:
        first = "Ondra_BASICEXP_V5_a.rom";
        second = "Ondra_BASICEXP_V5_b.rom";
        break;
    case RomType::Tesla:
        first = "Ondra_TESLA_V5_a.rom";
        second = "Ondra_TESLA_V5_b.rom";
        half_size = 8'192;
        repeat = true;
        break;
    case RomType::Vili:
        first = "Ondra_ViLi_v27_a.rom";
        second = "Ondra_ViLi_v27_b.rom";
        half_size = 8'192;
        repeat = true;
        break;
    case RomType::Plus:
        first = "Ondra_PLUS_a.rom";
        second = "Ondra_PLUS_b.rom";
        break;
    }

    load_fragment(directory / first, std::span(rom_).first(half_size), repeat);
    load_fragment(directory / second, std::span(rom_).subspan(half_size, half_size), repeat);
}

std::uint8_t Memory::read(std::uint16_t address) const {
    if(rom_mapped_ && address < rom_.size())
        return rom_[address];
    if(io_mapped_ && address >= 0xe000u)
        return keyboard_.read(address);
    return ram_[address];
}

std::uint8_t Memory::read_ram(std::uint16_t address) const {
    return ram_[address];
}

void Memory::write(std::uint16_t address, std::uint8_t value) {
    if((rom_mapped_ && address < rom_.size()) || (io_mapped_ && address >= 0xe000u))
        return;
    ram_[address] = value;
}

void Memory::write_ram(std::uint16_t address, std::uint8_t value) {
    ram_[address] = value;
}

} // namespace jondra
