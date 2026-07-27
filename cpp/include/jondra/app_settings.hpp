#pragma once

#include "jondra/memory.hpp"

#include <filesystem>
#include <string>

namespace jondra {

struct AppSettings {
    RomType rom_type = RomType::Basic;
    bool fullscreen = false;
    bool scanlines = false;
    bool builtin_sound = true;
    bool melodik = true;
    int window_width = 1120;
    int window_height = 820;
    std::string binary_load_path;
    std::string binary_save_path;
    std::string snapshot_load_path;
    std::string snapshot_save_path = "snapshot.osn";
    std::string tape_load_path;
    std::string tape_save_path = "recording.csw";
};

[[nodiscard]] AppSettings
load_app_settings(const std::filesystem::path& path);
void save_app_settings(const std::filesystem::path& path,
                       const AppSettings& settings);

} // namespace jondra
