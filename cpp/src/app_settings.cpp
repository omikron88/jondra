#include "jondra/app_settings.hpp"

#include <charconv>
#include <fstream>
#include <iomanip>
#include <optional>
#include <sstream>
#include <stdexcept>
#include <string>
#include <string_view>
#include <system_error>

namespace {

std::optional<bool> parse_bool(std::string_view value) {
    if(value == "1" || value == "true")
        return true;
    if(value == "0" || value == "false")
        return false;
    return std::nullopt;
}

std::optional<int> parse_int(std::string_view value) {
    int result = 0;
    const auto parsed =
        std::from_chars(value.data(), value.data() + value.size(), result);
    if(parsed.ec != std::errc{} ||
       parsed.ptr != value.data() + value.size())
        return std::nullopt;
    return result;
}

std::optional<jondra::RomType> parse_rom(std::string_view value) {
    if(value == "basic") return jondra::RomType::Basic;
    if(value == "tesla") return jondra::RomType::Tesla;
    if(value == "vili") return jondra::RomType::Vili;
    if(value == "plus") return jondra::RomType::Plus;
    return std::nullopt;
}

std::string_view rom_name(jondra::RomType type) {
    switch(type) {
    case jondra::RomType::Basic: return "basic";
    case jondra::RomType::Tesla: return "tesla";
    case jondra::RomType::Vili: return "vili";
    case jondra::RomType::Plus: return "plus";
    }
    return "basic";
}

std::optional<std::string> parse_quoted(std::string_view value) {
    std::istringstream stream{std::string(value)};
    std::string result;
    if(!(stream >> std::quoted(result)))
        return std::nullopt;
    stream >> std::ws;
    if(!stream.eof())
        return std::nullopt;
    return result;
}

void write_string(std::ostream& stream, std::string_view key,
                  const std::string& value) {
    stream << key << '=' << std::quoted(value) << '\n';
}

} // namespace

namespace jondra {

AppSettings load_app_settings(const std::filesystem::path& path) {
    AppSettings settings;
    std::ifstream stream(path);
    if(!stream) {
        if(std::filesystem::exists(path))
            throw std::runtime_error("Cannot read settings file");
        return settings;
    }

    std::string line;
    while(std::getline(stream, line)) {
        if(line.empty() || line.front() == '#' || line.front() == ';')
            continue;
        const auto separator = line.find('=');
        if(separator == std::string::npos)
            continue;
        const std::string_view key(line.data(), separator);
        const std::string_view value(
            line.data() + separator + 1, line.size() - separator - 1);

        if(key == "rom") {
            if(const auto parsed = parse_rom(value))
                settings.rom_type = *parsed;
        } else if(key == "fullscreen") {
            if(const auto parsed = parse_bool(value))
                settings.fullscreen = *parsed;
        } else if(key == "scanlines") {
            if(const auto parsed = parse_bool(value))
                settings.scanlines = *parsed;
        } else if(key == "builtin_sound") {
            if(const auto parsed = parse_bool(value))
                settings.builtin_sound = *parsed;
        } else if(key == "melodik") {
            if(const auto parsed = parse_bool(value))
                settings.melodik = *parsed;
        } else if(key == "window_width") {
            if(const auto parsed = parse_int(value);
               parsed && *parsed >= 640 && *parsed <= 8192)
                settings.window_width = *parsed;
        } else if(key == "window_height") {
            if(const auto parsed = parse_int(value);
               parsed && *parsed >= 480 && *parsed <= 8192)
                settings.window_height = *parsed;
        } else if(key == "binary_load_path") {
            if(const auto parsed = parse_quoted(value))
                settings.binary_load_path = *parsed;
        } else if(key == "binary_save_path") {
            if(const auto parsed = parse_quoted(value))
                settings.binary_save_path = *parsed;
        } else if(key == "snapshot_load_path") {
            if(const auto parsed = parse_quoted(value))
                settings.snapshot_load_path = *parsed;
        } else if(key == "snapshot_save_path") {
            if(const auto parsed = parse_quoted(value))
                settings.snapshot_save_path = *parsed;
        } else if(key == "tape_load_path") {
            if(const auto parsed = parse_quoted(value))
                settings.tape_load_path = *parsed;
        } else if(key == "tape_save_path") {
            if(const auto parsed = parse_quoted(value))
                settings.tape_save_path = *parsed;
        }
    }
    if(stream.bad())
        throw std::runtime_error("Cannot read settings file");
    return settings;
}

void save_app_settings(const std::filesystem::path& path,
                       const AppSettings& settings) {
    if(!path.parent_path().empty())
        std::filesystem::create_directories(path.parent_path());
    auto temporary = path;
    temporary += ".tmp";

    std::ofstream stream(temporary, std::ios::trunc);
    if(!stream)
        throw std::runtime_error("Cannot create settings file");
    stream << "# JOndra C++ settings\n"
           << "rom=" << rom_name(settings.rom_type) << '\n'
           << "fullscreen=" << settings.fullscreen << '\n'
           << "scanlines=" << settings.scanlines << '\n'
           << "builtin_sound=" << settings.builtin_sound << '\n'
           << "melodik=" << settings.melodik << '\n'
           << "window_width=" << settings.window_width << '\n'
           << "window_height=" << settings.window_height << '\n';
    write_string(stream, "binary_load_path", settings.binary_load_path);
    write_string(stream, "binary_save_path", settings.binary_save_path);
    write_string(stream, "snapshot_load_path", settings.snapshot_load_path);
    write_string(stream, "snapshot_save_path", settings.snapshot_save_path);
    write_string(stream, "tape_load_path", settings.tape_load_path);
    write_string(stream, "tape_save_path", settings.tape_save_path);
    stream.close();
    if(!stream)
        throw std::runtime_error("Cannot write settings file");

    std::error_code error;
    std::filesystem::rename(temporary, path, error);
    if(error) {
        std::error_code ignored;
        std::filesystem::remove(path, ignored);
        error.clear();
        std::filesystem::rename(temporary, path, error);
    }
    if(error)
        throw std::runtime_error("Cannot replace settings file: " +
                                 error.message());
}

} // namespace jondra
