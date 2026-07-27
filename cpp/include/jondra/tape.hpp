#pragma once

#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <string>
#include <string_view>
#include <vector>

namespace jondra {

enum class TapeMode {
    Empty,
    Playback,
    Recording
};

class Tape {
public:
    static constexpr std::uint64_t cpu_rate = 2'000'000;
    static constexpr std::uint32_t recording_rate = 22'050;

    Tape() = default;
    ~Tape();

    Tape(const Tape&) = delete;
    Tape& operator=(const Tape&) = delete;

    void open_playback(const std::filesystem::path& path);
    void open_recording(const std::filesystem::path& path);
    void close();
    void rewind();
    void set_motor(bool running);
    void advance(unsigned ticks, bool output_high);

    [[nodiscard]] TapeMode mode() const noexcept { return mode_; }
    [[nodiscard]] bool motor_running() const noexcept { return motor_running_; }
    [[nodiscard]] bool input_high() const noexcept { return input_high_; }
    [[nodiscard]] bool finished() const noexcept { return finished_; }
    [[nodiscard]] std::size_t position() const noexcept { return position_; }
    [[nodiscard]] std::size_t length() const noexcept {
        return mode_ == TapeMode::Playback ? playback_samples_.size()
                                           : recording_samples_.size();
    }
    [[nodiscard]] std::uint32_t sample_rate() const noexcept {
        return sample_rate_;
    }
    [[nodiscard]] std::string_view filename() const noexcept {
        return filename_;
    }
    [[nodiscard]] std::string_view error() const noexcept {
        return error_;
    }

private:
    void load_wav(const std::filesystem::path& path);
    void load_csw(const std::filesystem::path& path);
    void load_tap(const std::filesystem::path& path);
    void flush_recording();

    TapeMode mode_ = TapeMode::Empty;
    std::filesystem::path recording_path_;
    std::vector<bool> playback_samples_;
    std::vector<bool> recording_samples_;
    std::uint32_t sample_rate_ = 0;
    std::uint64_t phase_ = 0;
    std::size_t position_ = 0;
    bool motor_running_ = false;
    bool input_high_ = true;
    bool finished_ = false;
    bool recording_dirty_ = false;
    std::string filename_;
    std::string error_;
};

} // namespace jondra
