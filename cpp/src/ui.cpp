#include "jondra/ui.hpp"

#include "jondra/machine.hpp"

#include <SDL3/SDL.h>

#include "imgui.h"

#include <algorithm>
#include <array>
#include <cstdint>
#include <exception>
#include <string_view>

namespace {

using jondra::Machine;
using jondra::RomType;
using jondra::UiState;

constexpr std::array<std::pair<RomType, std::string_view>, 4> rom_names{{
    {RomType::Basic, "BASIC EXP V5"},
    {RomType::Tesla, "Tesla V5"},
    {RomType::Vili, "ViLi v27"},
    {RomType::Plus, "Ondra Plus"},
}};

std::string_view rom_name(RomType type) {
    for(const auto& [candidate, name] : rom_names) {
        if(candidate == type)
            return name;
    }
    return "Unknown";
}

void unavailable_button(const char* label) {
    ImGui::BeginDisabled();
    ImGui::Button(label);
    ImGui::EndDisabled();
    if(ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenDisabled))
        ImGui::SetTooltip("This feature has not been ported yet.");
}

void reset_machine(Machine& machine, bool& paused, UiState& state) {
    machine.reset();
    paused = false;
    state.status = "Machine reset";
}

void toggle_fullscreen(SDL_Window* window, UiState& state) {
    const bool requested = !state.fullscreen;
    if(SDL_SetWindowFullscreen(window, requested)) {
        state.fullscreen = requested;
        state.status = requested ? "Fullscreen enabled" : "Fullscreen disabled";
    } else {
        state.status = std::string("Fullscreen failed: ") + SDL_GetError();
    }
}

void draw_file_menu(UiState& state) {
    if(!ImGui::BeginMenu("File"))
        return;

    ImGui::BeginDisabled();
    ImGui::MenuItem("Open tape...");
    ImGui::MenuItem("Save tape...");
    ImGui::Separator();
    ImGui::MenuItem("Open snapshot...");
    ImGui::MenuItem("Save snapshot...");
    ImGui::Separator();
    ImGui::MenuItem("Load memory block...");
    ImGui::MenuItem("Save memory block...");
    ImGui::MenuItem("Save screenshot...");
    ImGui::EndDisabled();
    ImGui::Separator();
    if(ImGui::MenuItem("Exit", "Esc"))
        state.quit_requested = true;
    ImGui::EndMenu();
}

void draw_control_menu(SDL_Window* window, Machine& machine, bool& paused,
                       UiState& state) {
    if(!ImGui::BeginMenu("Control"))
        return;

    if(ImGui::MenuItem("Reset", "F12"))
        reset_machine(machine, paused, state);
    if(ImGui::MenuItem(paused ? "Resume" : "Pause", "F5")) {
        paused = !paused;
        state.status = paused ? "Emulation paused" : "Emulation running";
    }
    if(ImGui::MenuItem("NMI", "F11")) {
        machine.nmi();
        state.status = "NMI requested";
    }
    ImGui::Separator();
    if(ImGui::MenuItem("Fullscreen", nullptr, state.fullscreen))
        toggle_fullscreen(window, state);
    ImGui::EndMenu();
}

void draw_tools_menu(UiState& state) {
    if(!ImGui::BeginMenu("Tools"))
        return;

    ImGui::MenuItem("Debugger", nullptr, &state.show_debugger);
    ImGui::MenuItem("Settings", nullptr, &state.show_settings);
    ImGui::BeginDisabled();
    ImGui::MenuItem("On-screen keyboard");
    ImGui::EndDisabled();
    ImGui::EndMenu();
}

void draw_menu_bar(SDL_Window* window, Machine& machine, bool& paused,
                   UiState& state) {
    if(!ImGui::BeginMainMenuBar())
        return;

    draw_file_menu(state);
    draw_control_menu(window, machine, paused, state);
    draw_tools_menu(state);
    if(ImGui::BeginMenu("Help")) {
        if(ImGui::MenuItem("About JOndra"))
            state.show_about = true;
        ImGui::EndMenu();
    }
    if(ImGui::BeginMenu("View")) {
        if(ImGui::MenuItem("Fullscreen", nullptr, state.fullscreen))
            toggle_fullscreen(window, state);
        ImGui::MenuItem("Scanlines", nullptr, &state.scanlines);
        ImGui::EndMenu();
    }

    ImGui::EndMainMenuBar();
}

void draw_toolbar(Machine& machine, bool& paused, UiState& state) {
    if(ImGui::Button("Reset"))
        reset_machine(machine, paused, state);
    ImGui::SameLine();
    if(ImGui::Button(paused ? "Resume" : "Pause")) {
        paused = !paused;
        state.status = paused ? "Emulation paused" : "Emulation running";
    }
    ImGui::SameLine();
    if(ImGui::Button("NMI")) {
        machine.nmi();
        state.status = "NMI requested";
    }

    ImGui::SameLine();
    ImGui::TextDisabled("|");
    ImGui::SameLine();
    unavailable_button("Open tape");
    ImGui::SameLine();
    unavailable_button("Snapshot");
    ImGui::SameLine();
    if(ImGui::Button("Debugger"))
        state.show_debugger = !state.show_debugger;
    ImGui::SameLine();
    if(ImGui::Button("Settings"))
        state.show_settings = true;
}

void draw_screen(SDL_Texture* texture, const UiState& state) {
    const ImVec2 available = ImGui::GetContentRegionAvail();
    constexpr float aspect =
        static_cast<float>(Machine::screen_width) / Machine::screen_height;

    ImVec2 size{available.x, available.x / aspect};
    if(size.y > available.y)
        size = {available.y * aspect, available.y};
    size.x = std::max(size.x, 1.0f);
    size.y = std::max(size.y, 1.0f);

    const ImVec2 cursor = ImGui::GetCursorPos();
    ImGui::SetCursorPosX(cursor.x + std::max(0.0f, (available.x - size.x) * 0.5f));
    ImGui::SetCursorPosY(cursor.y + std::max(0.0f, (available.y - size.y) * 0.5f));

    const ImVec2 top_left = ImGui::GetCursorScreenPos();
    ImGui::Image(reinterpret_cast<ImTextureID>(texture), size);

    if(!state.scanlines)
        return;

    auto* draw_list = ImGui::GetWindowDrawList();
    const float row_height = size.y / Machine::screen_height;
    for(unsigned row = 1; row < Machine::screen_height; row += 2) {
        const float y = top_left.y + static_cast<float>(row) * row_height;
        draw_list->AddRectFilled(
            {top_left.x, y}, {top_left.x + size.x, y + row_height},
            IM_COL32(0, 0, 0, 70));
    }
}

void draw_status_bar(const Machine& machine, bool paused, const UiState& state) {
    const ImVec2 cursor = ImGui::GetCursorScreenPos();
    const ImVec2 led{cursor.x + 7.0f, cursor.y + 9.0f};
    ImGui::GetWindowDrawList()->AddCircleFilled(
        led, 5.0f, paused ? IM_COL32(230, 180, 30, 255)
                          : IM_COL32(40, 210, 90, 255));
    ImGui::Dummy({16.0f, 1.0f});
    ImGui::SameLine();
    ImGui::TextUnformatted(paused ? "PAUSED" : "RUNNING");
    ImGui::SameLine();
    ImGui::Text("| ROM: %.*s | DMA: %s | T-states: %llu | %s",
                static_cast<int>(rom_name(state.rom_type).size()),
                rom_name(state.rom_type).data(),
                machine.dma_enabled() ? "on" : "off",
                static_cast<unsigned long long>(machine.ticks()),
                state.status.c_str());
}

void draw_main_window(SDL_Texture* texture, Machine& machine, bool& paused,
                      UiState& state) {
    const ImGuiViewport* viewport = ImGui::GetMainViewport();
    const float menu_height = ImGui::GetFrameHeight();
    ImGui::SetNextWindowPos({viewport->Pos.x, viewport->Pos.y + menu_height});
    ImGui::SetNextWindowSize(
        {viewport->Size.x, std::max(1.0f, viewport->Size.y - menu_height)});

    constexpr ImGuiWindowFlags flags =
        ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove |
        ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoBringToFrontOnFocus;
    ImGui::Begin("##JOndraMain", nullptr, flags);
    draw_toolbar(machine, paused, state);
    ImGui::Separator();

    const float status_height = ImGui::GetFrameHeightWithSpacing();
    ImGui::BeginChild("##EmulatorScreen", {0.0f, -status_height},
                      ImGuiChildFlags_Borders);
    draw_screen(texture, state);
    ImGui::EndChild();
    draw_status_bar(machine, paused, state);
    ImGui::End();
}

void draw_settings(SDL_Window* window, Machine& machine,
                   const std::filesystem::path& rom_directory, bool& paused,
                   UiState& state) {
    if(!state.show_settings)
        return;

    ImGui::SetNextWindowSize({420.0f, 0.0f}, ImGuiCond_FirstUseEver);
    if(ImGui::Begin("Settings", &state.show_settings,
                    ImGuiWindowFlags_AlwaysAutoResize)) {
        ImGui::TextUnformatted("System ROM");
        RomType selected = state.rom_type;
        for(const auto& [type, name] : rom_names) {
            const bool active = selected == type;
            if(ImGui::RadioButton(name.data(), active))
                selected = type;
        }
        if(selected != state.rom_type) {
            try {
                machine.load_rom(selected, rom_directory);
                state.rom_type = selected;
                reset_machine(machine, paused, state);
                state.status = "ROM changed to " + std::string(rom_name(selected));
            } catch(const std::exception& error) {
                state.status = std::string("Could not load ROM: ") + error.what();
            }
        }

        ImGui::Separator();
        if(ImGui::Checkbox("Fullscreen", &state.fullscreen)) {
            if(!SDL_SetWindowFullscreen(window, state.fullscreen)) {
                state.fullscreen = !state.fullscreen;
                state.status = std::string("Fullscreen failed: ") + SDL_GetError();
            }
        }
        ImGui::Checkbox("Scanlines", &state.scanlines);

        ImGui::Separator();
        ImGui::BeginDisabled();
        bool sound = false;
        bool melodik = false;
        ImGui::Checkbox("Sound", &sound);
        ImGui::Checkbox("Melodik", &melodik);
        ImGui::EndDisabled();
        if(ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenDisabled))
            ImGui::SetTooltip("Audio has not been ported yet.");
    }
    ImGui::End();
}

void draw_debugger(Machine& machine, bool& paused, UiState& state) {
    if(!state.show_debugger)
        return;

    ImGui::SetNextWindowSize({600.0f, 460.0f}, ImGuiCond_FirstUseEver);
    if(ImGui::Begin("Debugger", &state.show_debugger)) {
        if(ImGui::Button(paused ? "Run" : "Pause")) {
            paused = !paused;
            state.status = paused ? "Debugger paused emulation"
                                  : "Debugger resumed emulation";
        }
        ImGui::SameLine();
        if(ImGui::Button("Reset"))
            reset_machine(machine, paused, state);
        ImGui::SameLine();
        ImGui::BeginDisabled();
        ImGui::Button("Step");
        ImGui::SameLine();
        ImGui::Button("Step into");
        ImGui::EndDisabled();

        ImGui::SeparatorText("CPU");
        ImGui::Text("PC: %04X", static_cast<unsigned>(machine.get_pc()));
        ImGui::Text("T-states: %llu",
                    static_cast<unsigned long long>(machine.ticks()));
        ImGui::Text("ROM mapped: %s",
                    machine.memory().rom_mapped() ? "yes" : "no");
        ImGui::Text("I/O mapped: %s",
                    machine.memory().io_mapped() ? "yes" : "no");

        ImGui::SeparatorText("Memory at PC");
        const auto pc = static_cast<std::uint16_t>(machine.get_pc());
        if(ImGui::BeginTable("##Memory", 9,
                             ImGuiTableFlags_Borders | ImGuiTableFlags_RowBg)) {
            ImGui::TableSetupColumn("Address");
            for(int column = 0; column < 8; ++column) {
                const std::string label =
                    " +" + std::to_string(column);
                ImGui::TableSetupColumn(label.c_str());
            }
            ImGui::TableHeadersRow();
            for(unsigned row = 0; row < 8; ++row) {
                const auto address =
                    static_cast<std::uint16_t>(pc + row * 8u);
                ImGui::TableNextRow();
                ImGui::TableSetColumnIndex(0);
                ImGui::Text("%04X", address);
                for(unsigned column = 0; column < 8; ++column) {
                    ImGui::TableSetColumnIndex(static_cast<int>(column + 1));
                    ImGui::Text("%02X", machine.memory().read(
                        static_cast<std::uint16_t>(address + column)));
                }
            }
            ImGui::EndTable();
        }

        ImGui::Spacing();
        ImGui::TextDisabled(
            "Register editing, disassembly, breakpoints and timeline "
            "will be connected in the next UI milestone.");
    }
    ImGui::End();
}

void draw_about(UiState& state) {
    if(!state.show_about)
        return;

    ImGui::SetNextWindowSize({390.0f, 0.0f}, ImGuiCond_FirstUseEver);
    if(ImGui::Begin("About JOndra", &state.show_about,
                    ImGuiWindowFlags_AlwaysAutoResize)) {
        ImGui::TextUnformatted("JOndra C++/SDL3");
        ImGui::Separator();
        ImGui::TextWrapped(
            "An in-progress C++20 port of the Ondra computer emulator. "
            "The user interface is powered by Dear ImGui.");
        ImGui::TextUnformatted("License: GPL-2.0");
    }
    ImGui::End();
}

} // namespace

namespace jondra {

void draw_ui(SDL_Window* window, SDL_Texture* screen_texture, Machine& machine,
             const std::filesystem::path& rom_directory, bool& paused,
             UiState& state) {
    draw_menu_bar(window, machine, paused, state);
    draw_main_window(screen_texture, machine, paused, state);
    draw_settings(window, machine, rom_directory, paused, state);
    draw_debugger(machine, paused, state);
    draw_about(state);
}

} // namespace jondra
