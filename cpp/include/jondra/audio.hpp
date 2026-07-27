#pragma once

#include "jondra/sn76489.hpp"

#include <array>
#include <cstddef>
#include <cstdint>
#include <span>

namespace jondra {

class AudioGenerator {
public:
    static constexpr std::uint32_t sample_rate = 44'100;
    static constexpr std::size_t samples_per_frame = sample_rate / 50;
    static constexpr std::uint64_t frame_ticks = 312u * 128u;

    AudioGenerator();

    void reset();
    void begin_frame(std::uint64_t ticks);
    void end_frame();
    void select_builtin(std::uint8_t tone, std::uint64_t ticks);
    void write_melodik(std::uint8_t value, std::uint64_t ticks);

    void set_builtin_enabled(bool enabled) noexcept {
        builtin_enabled_ = enabled;
    }
    void set_melodik_enabled(bool enabled) noexcept {
        melodik_enabled_ = enabled;
    }
    [[nodiscard]] bool builtin_enabled() const noexcept {
        return builtin_enabled_;
    }
    [[nodiscard]] bool melodik_enabled() const noexcept {
        return melodik_enabled_;
    }
    [[nodiscard]] std::span<const std::int16_t> frame() const noexcept {
        return frame_;
    }

private:
    void render_until(std::uint64_t ticks);
    void render_to(std::size_t target);
    [[nodiscard]] std::int16_t next_builtin_sample();

    Sn76489 melodik_;
    std::array<std::int16_t, samples_per_frame> frame_{};
    std::uint64_t frame_start_ticks_ = 0;
    std::size_t rendered_samples_ = 0;
    std::size_t builtin_position_ = 0;
    std::uint8_t builtin_tone_ = 0;
    bool frame_active_ = false;
    bool builtin_enabled_ = true;
    bool melodik_enabled_ = true;
};

} // namespace jondra
