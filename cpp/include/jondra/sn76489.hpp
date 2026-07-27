#pragma once

#include <array>
#include <cstdint>

namespace jondra {

class Sn76489 {
public:
    static constexpr std::uint32_t clock_rate = 2'000'000;
    static constexpr std::uint32_t sample_rate = 44'100;

    Sn76489();

    void reset();
    void write(std::uint8_t value);
    [[nodiscard]] std::int16_t next_sample();

private:
    static constexpr std::array<std::int16_t, 16> volumes{{
        8192, 6507, 5168, 4105, 3261, 2590, 2057, 1634,
        1298, 1031, 819, 651, 517, 411, 326, 0,
    }};

    std::array<std::uint16_t, 3> tone_period_{{1, 1, 1}};
    std::array<std::uint8_t, 4> volume_{{15, 15, 15, 15}};
    std::array<std::uint64_t, 3> tone_phase_{};
    std::array<bool, 3> tone_high_{{true, true, true}};
    std::uint64_t noise_phase_ = 0;
    std::uint16_t noise_shift_ = 0x8000;
    std::uint8_t noise_control_ = 0;
    std::uint8_t latched_register_ = 0;
};

} // namespace jondra
