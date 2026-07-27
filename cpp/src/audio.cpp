#include "jondra/audio.hpp"

#include "jondra/embedded_sound.hpp"

#include <algorithm>
#include <cstdint>
#include <limits>

namespace jondra {

AudioGenerator::AudioGenerator() {
    reset();
}

void AudioGenerator::reset() {
    melodik_.reset();
    frame_.fill(0);
    frame_start_ticks_ = 0;
    rendered_samples_ = 0;
    builtin_position_ = 0;
    builtin_tone_ = 0;
    frame_active_ = false;
}

void AudioGenerator::begin_frame(std::uint64_t ticks) {
    frame_.fill(0);
    frame_start_ticks_ = ticks;
    rendered_samples_ = 0;
    frame_active_ = true;
}

void AudioGenerator::end_frame() {
    if(!frame_active_)
        return;
    render_to(samples_per_frame);
    frame_active_ = false;
}

void AudioGenerator::select_builtin(std::uint8_t tone,
                                    std::uint64_t ticks) {
    if(frame_active_)
        render_until(ticks);
    tone &= 7u;
    if(tone != builtin_tone_) {
        builtin_tone_ = tone;
        builtin_position_ = 0;
    }
}

void AudioGenerator::write_melodik(std::uint8_t value,
                                   std::uint64_t ticks) {
    if(frame_active_)
        render_until(ticks);
    if(melodik_enabled_)
        melodik_.write(value);
}

void AudioGenerator::render_until(std::uint64_t ticks) {
    const auto relative =
        ticks > frame_start_ticks_ ? ticks - frame_start_ticks_ : 0;
    const auto bounded = std::min(relative, frame_ticks);
    const auto target = static_cast<std::size_t>(
        bounded * samples_per_frame / frame_ticks);
    render_to(target);
}

void AudioGenerator::render_to(std::size_t target) {
    target = std::min(target, frame_.size());
    while(rendered_samples_ < target) {
        const auto builtin = next_builtin_sample();
        const auto melodik = melodik_.next_sample();
        std::int32_t mixed = 0;
        if(builtin_enabled_)
            mixed += builtin;
        if(melodik_enabled_)
            mixed += melodik;
        frame_[rendered_samples_++] = static_cast<std::int16_t>(
            std::clamp(mixed,
                       static_cast<std::int32_t>(
                           std::numeric_limits<std::int16_t>::min()),
                       static_cast<std::int32_t>(
                           std::numeric_limits<std::int16_t>::max())));
    }
}

std::int16_t AudioGenerator::next_builtin_sample() {
    if(builtin_tone_ == 0)
        return 0;
    const auto data = embedded_sound_sample(builtin_tone_);
    const auto sample_count = data.size() / 2u;
    if(sample_count == 0)
        return 0;

    builtin_position_ %= sample_count;
    const auto offset = builtin_position_++ * 2u;
    const auto value = static_cast<std::uint16_t>(data[offset]) |
                       (static_cast<std::uint16_t>(data[offset + 1]) << 8u);
    return static_cast<std::int16_t>(value);
}

} // namespace jondra
