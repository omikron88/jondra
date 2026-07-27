#include "jondra/sn76489.hpp"

#include <algorithm>
#include <cstddef>
#include <cstdint>

namespace jondra {

Sn76489::Sn76489() {
    reset();
}

void Sn76489::reset() {
    tone_period_.fill(1);
    volume_.fill(15);
    tone_phase_.fill(0);
    tone_high_.fill(true);
    noise_phase_ = 0;
    noise_shift_ = 0x8000;
    noise_control_ = 0;
    latched_register_ = 0;
}

void Sn76489::write(std::uint8_t value) {
    if((value & 0x80u) != 0) {
        latched_register_ = static_cast<std::uint8_t>((value >> 4u) & 7u);
        if((latched_register_ & 1u) != 0) {
            volume_[latched_register_ >> 1u] =
                static_cast<std::uint8_t>(value & 0x0fu);
        } else if(latched_register_ == 6) {
            noise_control_ = static_cast<std::uint8_t>(value & 7u);
            noise_shift_ = 0x8000;
            noise_phase_ = 0;
        } else {
            const auto channel = latched_register_ >> 1u;
            tone_period_[channel] = static_cast<std::uint16_t>(
                (tone_period_[channel] & 0x3f0u) | (value & 0x0fu));
            if(tone_period_[channel] == 0)
                tone_period_[channel] = 1;
        }
        return;
    }

    if(latched_register_ == 0 || latched_register_ == 2 ||
       latched_register_ == 4) {
        const auto channel = latched_register_ >> 1u;
        tone_period_[channel] = static_cast<std::uint16_t>(
            (tone_period_[channel] & 0x0fu) |
            ((static_cast<std::uint16_t>(value) & 0x3fu) << 4u));
        if(tone_period_[channel] == 0)
            tone_period_[channel] = 1;
    } else if(latched_register_ == 6) {
        noise_control_ = static_cast<std::uint8_t>(value & 7u);
        noise_shift_ = 0x8000;
        noise_phase_ = 0;
    } else {
        volume_[latched_register_ >> 1u] =
            static_cast<std::uint8_t>(value & 0x0fu);
    }
}

std::int16_t Sn76489::next_sample() {
    std::int32_t output = 0;
    unsigned tone_two_rising_edges = 0;
    for(std::size_t channel = 0; channel < tone_period_.size(); ++channel) {
        tone_phase_[channel] += clock_rate;
        const auto half_period =
            static_cast<std::uint64_t>(16u * tone_period_[channel]) *
            sample_rate;
        while(tone_phase_[channel] >= half_period) {
            tone_phase_[channel] -= half_period;
            tone_high_[channel] = !tone_high_[channel];
            if(channel == 2 && tone_high_[channel])
                ++tone_two_rising_edges;
        }
        output += tone_high_[channel] ? volumes[volume_[channel]]
                                     : -volumes[volume_[channel]];
    }

    unsigned noise_shifts = 0;
    if((noise_control_ & 3u) == 3u) {
        noise_shifts = tone_two_rising_edges;
    } else {
        static constexpr std::array<std::uint16_t, 3> divisors{512, 1024, 2048};
        noise_phase_ += clock_rate;
        const auto period =
            static_cast<std::uint64_t>(divisors[noise_control_ & 3u]) *
            sample_rate;
        noise_shifts = static_cast<unsigned>(noise_phase_ / period);
        noise_phase_ %= period;
    }
    while(noise_shifts-- > 0) {
        const auto feedback = (noise_control_ & 4u) != 0
            ? static_cast<std::uint16_t>(
                  ((noise_shift_ >> 0u) ^ (noise_shift_ >> 3u)) & 1u)
            : static_cast<std::uint16_t>(noise_shift_ & 1u);
        noise_shift_ =
            static_cast<std::uint16_t>((noise_shift_ >> 1u) |
                                       (feedback << 15u));
    }
    output += (noise_shift_ & 1u) != 0 ? volumes[volume_[3]]
                                       : -volumes[volume_[3]];

    return static_cast<std::int16_t>(
        std::clamp(output, std::int32_t{-32768}, std::int32_t{32767}));
}

} // namespace jondra
