#pragma once

#include "jondra/memory.hpp"

#include <filesystem>
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
    bool fullscreen = false;
    bool scanlines = false;
    bool quit_requested = false;
    std::string status = "Ready";
};

void draw_ui(SDL_Window* window, SDL_Texture* screen_texture, Machine& machine,
             const std::filesystem::path& rom_directory, bool& paused,
             UiState& state);

} // namespace jondra
