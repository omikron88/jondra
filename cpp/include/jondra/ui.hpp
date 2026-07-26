#pragma once

#include "jondra/memory.hpp"

#include <filesystem>
#include <cstdint>
#include <string>

struct SDL_Texture;
struct SDL_Window;

namespace jondra {

class Machine;

struct UiState {
    RomType rom_type = RomType::Basic;
    bool show_settings = false;
    bool show_debugger = false;
    bool show_about = false;
    bool show_binary_load = false;
    bool show_binary_save = false;
    bool fullscreen = false;
    bool scanlines = false;
    bool quit_requested = false;
    bool binary_dialog_was_running = false;
    bool native_file_dialog_open = false;
    bool binary_has_header = false;
    bool binary_run_after_load = false;
    bool binary_all_ram = false;
    std::uint16_t binary_load_address = 0x4000;
    std::uint16_t binary_run_address = 0x4000;
    std::uint16_t binary_save_first = 0x4000;
    std::uint16_t binary_save_last = 0xffff;
    std::string binary_load_path;
    std::string binary_save_path;
    std::string status = "Ready";
};

void draw_ui(SDL_Window* window, SDL_Texture* screen_texture, Machine& machine,
             const std::filesystem::path& rom_directory, bool& paused,
             UiState& state);

} // namespace jondra
