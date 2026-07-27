#include "jondra/tape.hpp"

#include <algorithm>
#include <array>
#include <cctype>
#include <cstddef>
#include <cstdint>
#include <deque>
#include <fstream>
#include <iterator>
#include <limits>
#include <span>
#include <stdexcept>
#include <string>
#include <vector>

namespace {

using Bytes = std::vector<std::uint8_t>;

constexpr std::string_view csw_signature = "Compressed Square Wave";

Bytes read_file(const std::filesystem::path& path) {
    std::ifstream stream(path, std::ios::binary);
    if(!stream)
        throw std::runtime_error("Cannot open tape: " + path.string());
    return {
        std::istreambuf_iterator<char>{stream},
        std::istreambuf_iterator<char>{}
    };
}

std::uint16_t read_u16(const Bytes& data, std::size_t position) {
    if(position > data.size() || data.size() - position < 2)
        throw std::runtime_error("Truncated tape file");
    return static_cast<std::uint16_t>(
        data[position] | (static_cast<unsigned>(data[position + 1]) << 8u));
}

std::uint32_t read_u32(const Bytes& data, std::size_t position) {
    if(position > data.size() || data.size() - position < 4)
        throw std::runtime_error("Truncated tape file");
    return static_cast<std::uint32_t>(data[position]) |
           (static_cast<std::uint32_t>(data[position + 1]) << 8u) |
           (static_cast<std::uint32_t>(data[position + 2]) << 16u) |
           (static_cast<std::uint32_t>(data[position + 3]) << 24u);
}

void write_u32(std::ostream& stream, std::uint32_t value) {
    for(unsigned shift = 0; shift < 32; shift += 8)
        stream.put(static_cast<char>(value >> shift));
}

std::int32_t decode_pcm_sample(const std::uint8_t* source,
                               unsigned bits_per_sample) {
    if(bits_per_sample == 8)
        return static_cast<std::int32_t>(source[0]) - 128;

    const auto bytes = bits_per_sample / 8u;
    std::uint32_t value = 0;
    for(unsigned i = 0; i < bytes; ++i)
        value |= static_cast<std::uint32_t>(source[i]) << (i * 8u);
    const auto sign_bit = std::uint32_t{1} << (bits_per_sample - 1u);
    if((value & sign_bit) != 0) {
        const auto mask =
            bits_per_sample == 32 ? std::numeric_limits<std::uint32_t>::max()
                                  : (std::uint32_t{1} << bits_per_sample) - 1u;
        value |= ~mask;
    }
    return static_cast<std::int32_t>(value);
}

void append_logical_byte(std::vector<bool>& bits, std::uint8_t value) {
    bits.push_back((value & 1u) == 0);
    for(unsigned bit = 0; bit < 8; ++bit)
        bits.push_back((value & (1u << bit)) != 0);
}

void align_and_pause(std::vector<bool>& bits, std::size_t block_start) {
    // TapFile.java sets the bit position to zero without advancing to the
    // next byte, then advances the byte position by two. Reproduce that
    // slightly unusual truncation exactly; rounding up first adds one
    // spurious byte and makes the following pilot undetectable by the ROM.
    const auto relative_size = bits.size() - block_start;
    const auto target =
        block_start + (relative_size / 8u + 2u) * 8u;
    bits.resize(target, false);
}

void append_pilot(std::vector<bool>& bits) {
    bits.insert(bits.end(), 200u * 8u + 7u, true);
    bits.push_back(false);
}

void append_tap_block(std::vector<bool>& bits,
                      const std::span<const std::uint8_t> header,
                      const std::span<const std::uint8_t> body) {
    const auto block_start = bits.size();
    bits.insert(bits.end(), 100u * 8u, false);
    append_pilot(bits);
    for(const auto value : header)
        append_logical_byte(bits, value);
    append_logical_byte(bits, 0);
    align_and_pause(bits, block_start);
    append_pilot(bits);
    for(const auto value : body)
        append_logical_byte(bits, value);
    append_logical_byte(bits, 0);
    align_and_pause(bits, block_start);
    // The Java reader treats bit zero at the final write position as part of
    // the buffer before it marks the block consumed.
    bits.push_back(false);
}

std::vector<bool> logical_to_tap_wave(const std::vector<bool>& bits) {
    std::vector<bool> result;
    result.reserve(bits.size() * 140u);
    for(const auto bit : bits) {
        result.insert(result.end(), 70, bit);
        result.insert(result.end(), 70, !bit);
    }
    return result;
}

} // namespace

namespace jondra {

Tape::~Tape() {
    try {
        flush_recording();
    } catch(...) {
    }
}

void Tape::open_playback(const std::filesystem::path& path) {
    flush_recording();
    playback_samples_.clear();
    recording_samples_.clear();
    recording_path_.clear();
    mode_ = TapeMode::Empty;
    filename_.clear();
    sample_rate_ = 0;
    error_.clear();
    motor_running_ = false;
    input_high_ = true;
    phase_ = 0;
    position_ = 0;
    finished_ = false;

    auto extension = path.extension().string();
    std::transform(extension.begin(), extension.end(), extension.begin(),
                   [](unsigned char value) {
                       return static_cast<char>(std::tolower(value));
                   });
    if(extension == ".wav")
        load_wav(path);
    else if(extension == ".csw")
        load_csw(path);
    else if(extension == ".tap")
        load_tap(path);
    else
        throw std::runtime_error("Supported tape formats are WAV, CSW and TAP");

    if(playback_samples_.empty())
        throw std::runtime_error("Tape contains no samples");
    mode_ = TapeMode::Playback;
    filename_ = path.filename().string();
    input_high_ = playback_samples_.front();
}

void Tape::open_recording(const std::filesystem::path& path) {
    flush_recording();
    playback_samples_.clear();
    recording_samples_.clear();
    recording_path_ = path;
    if(recording_path_.extension().empty())
        recording_path_.replace_extension(".csw");
    error_.clear();
    mode_ = TapeMode::Recording;
    filename_ = recording_path_.filename().string();
    sample_rate_ = recording_rate;
    motor_running_ = false;
    input_high_ = true;
    phase_ = 0;
    position_ = 0;
    finished_ = false;
    recording_dirty_ = true;
    flush_recording();
}

void Tape::close() {
    flush_recording();
    mode_ = TapeMode::Empty;
    recording_path_.clear();
    playback_samples_.clear();
    recording_samples_.clear();
    filename_.clear();
    sample_rate_ = 0;
    phase_ = 0;
    position_ = 0;
    motor_running_ = false;
    input_high_ = true;
    finished_ = false;
}

void Tape::rewind() {
    phase_ = 0;
    position_ = 0;
    finished_ = false;
    if(mode_ == TapeMode::Playback && !playback_samples_.empty())
        input_high_ = playback_samples_.front();
}

void Tape::set_motor(bool running) {
    if(motor_running_ == running)
        return;
    motor_running_ = running && mode_ != TapeMode::Empty && !finished_;
    if(!motor_running_ && mode_ == TapeMode::Recording) {
        try {
            flush_recording();
        } catch(const std::exception& exception) {
            error_ = exception.what();
        }
    }
}

void Tape::advance(unsigned ticks, bool output_high) {
    if(!motor_running_ || sample_rate_ == 0)
        return;

    phase_ += static_cast<std::uint64_t>(ticks) * sample_rate_;
    while(phase_ >= cpu_rate) {
        phase_ -= cpu_rate;
        if(mode_ == TapeMode::Playback) {
            if(++position_ >= playback_samples_.size()) {
                position_ = playback_samples_.size();
                finished_ = true;
                motor_running_ = false;
                input_high_ = true;
                break;
            }
            input_high_ = playback_samples_[position_];
        } else if(mode_ == TapeMode::Recording) {
            recording_samples_.push_back(output_high);
            position_ = recording_samples_.size();
            recording_dirty_ = true;
        }
    }
}

void Tape::load_wav(const std::filesystem::path& path) {
    const auto data = read_file(path);
    if(data.size() < 12 ||
       std::string_view(reinterpret_cast<const char*>(data.data()), 4) !=
           "RIFF" ||
       std::string_view(reinterpret_cast<const char*>(data.data() + 8), 4) !=
           "WAVE")
        throw std::runtime_error("Invalid WAV tape header");

    std::uint16_t format = 0;
    std::uint16_t channels = 0;
    std::uint16_t block_align = 0;
    std::uint16_t bits = 0;
    std::uint32_t rate = 0;
    std::span<const std::uint8_t> samples;
    for(std::size_t position = 12; position + 8 <= data.size();) {
        const auto id = std::string_view(
            reinterpret_cast<const char*>(data.data() + position), 4);
        const auto size = read_u32(data, position + 4);
        position += 8;
        if(size > data.size() - position)
            throw std::runtime_error("Truncated WAV chunk");
        if(id == "fmt " && size >= 16) {
            format = read_u16(data, position);
            channels = read_u16(data, position + 2);
            rate = read_u32(data, position + 4);
            block_align = read_u16(data, position + 12);
            bits = read_u16(data, position + 14);
        } else if(id == "data") {
            samples = std::span(data).subspan(position, size);
        }
        position += size + (size & 1u);
    }
    if(format != 1 || channels == 0 || rate == 0 ||
       (bits != 8 && bits != 16 && bits != 24 && bits != 32) ||
       block_align != channels * (bits / 8u) || samples.empty())
        throw std::runtime_error("Unsupported WAV tape format");

    const auto frames = samples.size() / block_align;
    playback_samples_.reserve(frames);
    std::deque<std::pair<std::size_t, std::int64_t>> minima;
    std::deque<std::pair<std::size_t, std::int64_t>> maxima;
    for(std::size_t frame = 0; frame < frames; ++frame) {
        std::int64_t sample = 0;
        for(unsigned channel = 0; channel < channels; ++channel) {
            const auto offset =
                frame * block_align + channel * (bits / 8u);
            sample += decode_pcm_sample(samples.data() + offset, bits);
        }
        sample /= channels;

        while(!minima.empty() && minima.back().second >= sample)
            minima.pop_back();
        while(!maxima.empty() && maxima.back().second <= sample)
            maxima.pop_back();
        minima.emplace_back(frame, sample);
        maxima.emplace_back(frame, sample);
        constexpr std::size_t window = 256;
        while(!minima.empty() && minima.front().first + window <= frame)
            minima.pop_front();
        while(!maxima.empty() && maxima.front().first + window <= frame)
            maxima.pop_front();
        const auto threshold =
            (minima.front().second + maxima.front().second) / 2;
        playback_samples_.push_back(sample > threshold);
    }
    sample_rate_ = rate;
}

void Tape::load_csw(const std::filesystem::path& path) {
    const auto data = read_file(path);
    if(data.size() < 52 ||
       std::string_view(reinterpret_cast<const char*>(data.data()),
                        csw_signature.size()) != csw_signature ||
       data[22] != 0x1a || data[23] < 2 || data[33] != 1)
        throw std::runtime_error("Unsupported CSW tape");

    sample_rate_ = read_u32(data, 25);
    const auto extension_size = data[35];
    std::size_t position = 52u + extension_size;
    if(sample_rate_ == 0 || position > data.size())
        throw std::runtime_error("Invalid CSW tape header");

    bool level = false;
    while(position < data.size()) {
        std::uint32_t length = data[position++];
        if(length == 0) {
            length = read_u32(data, position);
            position += 4;
        }
        if(length == 0)
            throw std::runtime_error("Invalid zero-length CSW pulse");
        level = !level;
        playback_samples_.insert(playback_samples_.end(), length, level);
    }
}

void Tape::load_tap(const std::filesystem::path& path) {
    const auto data = read_file(path);
    std::vector<bool> logical_bits;
    std::size_t position = 0;
    while(position < data.size()) {
        if(data.size() - position < 25)
            throw std::runtime_error("Truncated TAP header");
        const auto header =
            std::span(data).subspan(position, 25);
        if(header[0] != 'H' || header[16] != 'D')
            throw std::runtime_error("Invalid Ondra TAP header");
        position += 25;
        if(data.size() - position >= 2 &&
           data[position] == 0x0a && data[position + 1] == 0x0d)
            position += 2;

        const auto size = static_cast<std::size_t>(header[21]) |
                          (static_cast<std::size_t>(header[22]) << 8u);
        if(data.size() - position < size + 2 || data[position] != 'D')
            throw std::runtime_error("Invalid Ondra TAP data block");
        const auto body = std::span(data).subspan(position, size + 2);
        std::uint8_t checksum = 0;
        for(std::size_t i = 0; i < size; ++i)
            checksum = static_cast<std::uint8_t>(
                checksum + body[1 + i]);
        if(body[size + 1] != checksum)
            throw std::runtime_error("Invalid Ondra TAP checksum");
        position += size + 2;
        if(data.size() - position >= 2 &&
           data[position] == 0x0a && data[position + 1] == 0x0d)
            position += 2;
        append_tap_block(logical_bits, header, body);
    }
    playback_samples_ = logical_to_tap_wave(logical_bits);
    sample_rate_ = tap_sample_rate;
}

void Tape::flush_recording() {
    if(mode_ != TapeMode::Recording || !recording_dirty_ ||
       recording_path_.empty())
        return;

    std::ofstream stream(recording_path_,
                         std::ios::binary | std::ios::trunc);
    if(!stream)
        throw std::runtime_error(
            "Cannot create CSW tape: " + recording_path_.string());
    stream.write(csw_signature.data(),
                 static_cast<std::streamsize>(csw_signature.size()));
    stream.put(static_cast<char>(0x1a));
    stream.put(2);
    stream.put(0);
    write_u32(stream, sample_rate_);

    std::uint32_t pulses = 0;
    if(!recording_samples_.empty()) {
        pulses = 1;
        for(std::size_t i = 1; i < recording_samples_.size(); ++i)
            pulses += recording_samples_[i] != recording_samples_[i - 1];
    }
    write_u32(stream, pulses);
    stream.put(1);
    stream.put(0);
    stream.put(0);
    constexpr std::array<char, 16> application{
        'J','O','n','d','r','a',' ','C','+','+','\0','\0','\0','\0','\0','\0'
    };
    stream.write(application.data(), application.size());

    for(std::size_t begin = 0; begin < recording_samples_.size();) {
        std::size_t end = begin + 1;
        while(end < recording_samples_.size() &&
              recording_samples_[end] == recording_samples_[begin] &&
              end - begin < std::numeric_limits<std::uint32_t>::max())
            ++end;
        const auto length = static_cast<std::uint32_t>(end - begin);
        if(length <= 0xffu)
            stream.put(static_cast<char>(length));
        else {
            stream.put(0);
            write_u32(stream, length);
        }
        begin = end;
    }
    if(!stream)
        throw std::runtime_error("Cannot write CSW tape");
    recording_dirty_ = false;
    error_.clear();
}

} // namespace jondra
