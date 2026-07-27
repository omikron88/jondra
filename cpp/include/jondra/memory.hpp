#pragma once

#include "jondra/keyboard.hpp"

#include <array>
#include <cstdint>
#include <filesystem>
#include <span>
#include <string_view>

namespace jondra {

enum class RomType {
    Basic,
    Tesla,
    Vili,
    Plus
};

class Memory {
public:
    static constexpr std::size_t address_space_size = 65'536;
    static constexpr std::size_t rom_size = 16'384;

    explicit Memory(Keyboard& keyboard);

    void reset(bool dirty = true);
    void load_rom(RomType type, const std::filesystem::path& directory);

    [[nodiscard]] std::uint8_t read(std::uint16_t address) const;
    [[nodiscard]] std::uint8_t read_ram(std::uint16_t address) const;
    [[nodiscard]] std::span<const std::uint8_t> ram() const noexcept {
        return ram_;
    }
    void write(std::uint16_t address, std::uint8_t value);
    void write_ram(std::uint16_t address, std::uint8_t value);
    void restore_ram(std::span<const std::uint8_t> data);

    void map_rom(bool enabled) noexcept { rom_mapped_ = enabled; }
    void map_io(bool enabled) noexcept { io_mapped_ = enabled; }
    [[nodiscard]] bool rom_mapped() const noexcept { return rom_mapped_; }
    [[nodiscard]] bool io_mapped() const noexcept { return io_mapped_; }

private:
    void load_fragment(const std::filesystem::path& path,
                       std::span<std::uint8_t> destination,
                       bool repeat_to_fill);

    Keyboard& keyboard_;
    std::array<std::uint8_t, address_space_size> ram_{};
    std::array<std::uint8_t, rom_size> rom_{};
    bool rom_mapped_ = true;
    bool io_mapped_ = false;
};

} // namespace jondra
