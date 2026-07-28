#pragma once

#include "jondra/audio.hpp"
#include "jondra/keyboard.hpp"
#include "jondra/memory.hpp"
#include "jondra/tape.hpp"

#include <array>
#include <cstdint>
#include <filesystem>
#include <optional>
#include <set>
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
    bool run_frame();
    void step_instruction();
    [[nodiscard]] bool step_over();
    void add_breakpoint(std::uint16_t address);
    void remove_breakpoint(std::uint16_t address);
    void clear_breakpoints();
    [[nodiscard]] bool has_breakpoint(std::uint16_t address) const;
    [[nodiscard]] const std::set<std::uint16_t>& breakpoints() const noexcept {
        return breakpoints_;
    }
    [[nodiscard]] bool breakpoint_hit() const noexcept {
        return breakpoint_hit_;
    }
    [[nodiscard]] std::uint16_t breakpoint_address() const noexcept {
        return breakpoint_address_;
    }

    void key(Key key, bool pressed) { keyboard_.set(key, pressed); }
    void nmi();
    void write_memory(std::uint16_t address, std::uint8_t value,
                      bool physical_ram = false);

    [[nodiscard]] std::span<const std::uint8_t> framebuffer() const noexcept {
        return framebuffer_;
    }
    [[nodiscard]] Memory& memory() noexcept { return memory_; }
    [[nodiscard]] const Memory& memory() const noexcept { return memory_; }
    [[nodiscard]] AudioGenerator& audio() noexcept { return audio_; }
    [[nodiscard]] const AudioGenerator& audio() const noexcept {
        return audio_;
    }
    [[nodiscard]] Tape& tape() noexcept { return tape_; }
    [[nodiscard]] const Tape& tape() const noexcept { return tape_; }
    [[nodiscard]] std::uint64_t ticks() const noexcept { return ticks_; }
    [[nodiscard]] bool dma_enabled() const noexcept { return dma_enabled_; }
    [[nodiscard]] RomType rom_type() const noexcept { return rom_type_; }
    [[nodiscard]] std::uint8_t port_a0() const noexcept { return port_a0_; }
    [[nodiscard]] bool green_led_on() const noexcept {
        return (port_a0_ & 0x01u) == 0;
    }
    [[nodiscard]] bool yellow_led_on() const noexcept {
        return (port_a0_ & 0x02u) == 0;
    }
    [[nodiscard]] std::uint8_t port_a1() const noexcept { return port_a1_; }
    [[nodiscard]] std::uint8_t port_a3() const noexcept { return port_a3_; }
    [[nodiscard]] std::uint8_t resolution() const noexcept {
        return resolution_;
    }
    void set_builtin_sound_enabled(bool enabled) noexcept {
        audio_.set_builtin_enabled(enabled);
    }
    void set_melodik_enabled(bool enabled) noexcept {
        audio_.set_melodik_enabled(enabled);
        keyboard_.set_melodik_present(enabled);
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
    [[nodiscard]] bool on_is_breakpoint_addr(
        z80::fast_u16 address) const;

private:
    void rebuild_display_map();
    void refresh_display();
    void update_vram(std::uint16_t address);
    void set_dma(bool enabled);
    void update_resolution(std::uint8_t port);

    Keyboard keyboard_;
    Memory memory_;
    AudioGenerator audio_;
    Tape tape_;
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
    std::set<std::uint16_t> breakpoints_;
    std::optional<std::uint16_t> temporary_breakpoint_;
    std::uint16_t breakpoint_address_ = 0;
    bool breakpoint_hit_ = false;
};

} // namespace jondra
