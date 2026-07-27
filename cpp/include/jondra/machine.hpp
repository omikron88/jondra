#pragma once

#include "jondra/keyboard.hpp"
#include "jondra/memory.hpp"

#include <array>
#include <cstdint>
#include <filesystem>
#include <span>

#include "z80.h"

namespace jondra {

class Machine final : public z80::z80_cpu<Machine> {
public:
    using Base = z80::z80_cpu<Machine>;
    using Events = typename Base::events_mask;

    static constexpr unsigned screen_width = 320;
    static constexpr unsigned screen_height = 256;
    static constexpr std::uint64_t default_frame_ticks = 312u * 128u;

    Machine();

    void reset(bool dirty = true);
    void load_rom(RomType type, const std::filesystem::path& directory);
    void run_frame();

    void key(Key key, bool pressed) { keyboard_.set(key, pressed); }
    void nmi();
    void write_memory(std::uint16_t address, std::uint8_t value,
                      bool physical_ram = false);

    [[nodiscard]] std::span<const std::uint8_t> framebuffer() const noexcept {
        return framebuffer_;
    }
    [[nodiscard]] Memory& memory() noexcept { return memory_; }
    [[nodiscard]] const Memory& memory() const noexcept { return memory_; }
    [[nodiscard]] std::uint64_t ticks() const noexcept { return ticks_; }
    [[nodiscard]] bool dma_enabled() const noexcept { return dma_enabled_; }
    [[nodiscard]] RomType rom_type() const noexcept { return rom_type_; }
    [[nodiscard]] std::uint8_t port_a0() const noexcept { return port_a0_; }
    [[nodiscard]] std::uint8_t port_a1() const noexcept { return port_a1_; }
    [[nodiscard]] std::uint8_t port_a3() const noexcept { return port_a3_; }
    [[nodiscard]] std::uint8_t resolution() const noexcept {
        return resolution_;
    }

    void restore_snapshot_peripherals(std::uint8_t port_a0,
                                      std::uint8_t port_a1,
                                      std::uint8_t port_a3,
                                      std::uint8_t resolution);

    z80::fast_u8 on_read(z80::fast_u16 address);
    void on_write(z80::fast_u16 address, z80::fast_u8 value);
    z80::fast_u8 on_input(z80::fast_u16 port);
    void on_output(z80::fast_u16 port, z80::fast_u8 value);
    void on_tick(unsigned count);

private:
    void rebuild_display_map();
    void refresh_display();
    void update_vram(std::uint16_t address);
    void set_dma(bool enabled);
    void update_resolution(std::uint8_t port);

    Keyboard keyboard_;
    Memory memory_;
    std::array<std::int16_t, 0x2800> display_map_{};
    std::array<std::uint8_t, screen_width * screen_height / 8> framebuffer_{};

    std::uint64_t ticks_ = 0;
    std::uint64_t frame_ticks_ = default_frame_ticks;
    RomType rom_type_ = RomType::Basic;
    std::uint8_t port_a0_ = 0;
    std::uint8_t port_a1_ = 0;
    std::uint8_t port_a3_ = 0;
    std::uint8_t resolution_ = 255;
    bool dma_enabled_ = true;
    bool dma_timing_on_ = false;
};

} // namespace jondra
