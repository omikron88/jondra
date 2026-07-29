#include "jondra/ui.hpp"

#include "jondra/binary_file.hpp"
#include "jondra/debugger.hpp"
#include "jondra/machine.hpp"
#include "jondra/snapshot.hpp"

#include <SDL3/SDL.h>

#include "imgui.h"

#include <algorithm>
#include <array>
#include <cstdio>
#include <cstdint>
#include <exception>
#include <filesystem>
#include <memory>
#include <mutex>
#include <optional>
#include <string_view>
#include <vector>

namespace {

using jondra::Machine;
using jondra::RomType;
using jondra::UiState;

enum class FileDialogKind {
    BinaryLoad,
    BinarySave,
    SnapshotLoad,
    SnapshotSave,
    TapeLoad,
    TapeSave
};

struct FileDialogContext {
    FileDialogKind kind;
    std::string default_location;
};

struct FileDialogResult {
    FileDialogKind kind;
    std::optional<std::string> path;
    std::string error;
};

std::mutex file_dialog_mutex;
std::vector<FileDialogResult> file_dialog_results;

constexpr std::array<std::pair<RomType, std::string_view>, 4> rom_names{{
    {RomType::Basic, "BASIC EXP V5"},
    {RomType::Tesla, "Tesla V5"},
    {RomType::Vili, "ViLi v27"},
    {RomType::Plus, "Ondra Plus"},
}};

void SDLCALL file_dialog_callback(void* userdata,
                                  const char* const* file_list, int) {
    const auto context =
        std::unique_ptr<FileDialogContext>(
            static_cast<FileDialogContext*>(userdata));
    FileDialogResult result{context->kind, std::nullopt, {}};
    if(file_list == nullptr)
        result.error = SDL_GetError();
    else if(*file_list != nullptr)
        result.path = *file_list;

    const std::scoped_lock lock(file_dialog_mutex);
    file_dialog_results.push_back(std::move(result));
}

std::filesystem::path path_from_utf8(std::string_view text) {
    const auto* begin = reinterpret_cast<const char8_t*>(text.data());
    return std::filesystem::path(
        std::u8string(begin, begin + text.size()));
}

bool is_load_dialog(FileDialogKind kind) {
    return kind == FileDialogKind::BinaryLoad ||
           kind == FileDialogKind::SnapshotLoad ||
           kind == FileDialogKind::TapeLoad;
}

void finish_snapshot_dialog(bool& paused, UiState& state) {
    if(state.snapshot_dialog_was_running)
        paused = false;
    state.snapshot_dialog_was_running = false;
}

void finish_tape_dialog(bool& paused, UiState& state) {
    if(state.tape_dialog_was_running)
        paused = false;
    state.tape_dialog_was_running = false;
}

void process_file_dialog_results(
    Machine& machine, const std::filesystem::path& rom_directory,
    bool& paused, UiState& state) {
    std::vector<FileDialogResult> results;
    {
        const std::scoped_lock lock(file_dialog_mutex);
        results.swap(file_dialog_results);
    }
    for(auto& result : results) {
        state.native_file_dialog_open = false;
        if(!result.error.empty()) {
            state.status = "File dialog failed: " + result.error;
        } else if(result.path) {
            if(result.kind == FileDialogKind::BinaryLoad) {
                state.binary_load_path = std::move(*result.path);
            } else if(result.kind == FileDialogKind::BinarySave) {
                state.binary_save_path = std::move(*result.path);
            } else if(result.kind == FileDialogKind::SnapshotLoad ||
                      result.kind == FileDialogKind::SnapshotSave) {
                try {
                    auto path = path_from_utf8(*result.path);
                    if(result.kind == FileDialogKind::SnapshotLoad) {
                        state.snapshot_load_path = std::move(*result.path);
                        state.rom_type = jondra::load_snapshot_file(
                            path, machine, rom_directory);
                        state.status = "Snapshot loaded";
                    } else {
                        if(path.extension().empty())
                            path.replace_extension(".osn");
                        state.snapshot_save_path = path.string();
                        jondra::save_snapshot_file(path, machine);
                        state.status = "Snapshot saved";
                    }
                } catch(const std::exception& error) {
                    state.status =
                        std::string("Snapshot failed: ") + error.what();
                }
            } else {
                try {
                    const auto path = path_from_utf8(*result.path);
                    if(result.kind == FileDialogKind::TapeLoad) {
                        state.tape_load_path = std::move(*result.path);
                        machine.tape().open_playback(path);
                        state.status = "Tape loaded: " +
                            std::string(machine.tape().filename());
                    } else {
                        state.tape_save_path = std::move(*result.path);
                        machine.tape().open_recording(path);
                        state.status = "Tape recording prepared: " +
                            std::string(machine.tape().filename());
                    }
                } catch(const std::exception& error) {
                    state.status =
                        std::string("Tape failed: ") + error.what();
                }
            }
        }
        if(result.kind == FileDialogKind::SnapshotLoad ||
           result.kind == FileDialogKind::SnapshotSave)
            finish_snapshot_dialog(paused, state);
        if(result.kind == FileDialogKind::TapeLoad ||
           result.kind == FileDialogKind::TapeSave)
            finish_tape_dialog(paused, state);
    }
}

void show_native_file_dialog(SDL_Window* window, UiState& state,
                             FileDialogKind kind) {
    if(state.native_file_dialog_open)
        return;

    static constexpr SDL_DialogFileFilter binary_filters[]{
        {"Binary files", "bin;rom"},
        {"All files", "*"},
    };
    static constexpr SDL_DialogFileFilter snapshot_filters[]{
        {"JOndra snapshots", "osn"},
        {"All files", "*"},
    };
    static constexpr SDL_DialogFileFilter tape_load_filters[]{
        {"Ondra tapes", "wav;csw;tap"},
        {"All files", "*"},
    };
    static constexpr SDL_DialogFileFilter tape_save_filters[]{
        {"Compressed Square Wave", "csw"},
        {"All files", "*"},
    };
    const bool snapshot = kind == FileDialogKind::SnapshotLoad ||
                          kind == FileDialogKind::SnapshotSave;
    const bool tape_load = kind == FileDialogKind::TapeLoad;
    const bool tape_save = kind == FileDialogKind::TapeSave;
    const auto* filters = snapshot ? snapshot_filters
                        : tape_load ? tape_load_filters
                        : tape_save ? tape_save_filters
                                    : binary_filters;
    const auto filter_count =
        snapshot ? static_cast<int>(std::size(snapshot_filters))
      : tape_load ? static_cast<int>(std::size(tape_load_filters))
      : tape_save ? static_cast<int>(std::size(tape_save_filters))
                  : static_cast<int>(std::size(binary_filters));
    const auto& current_path = [&]() -> const std::string& {
        switch(kind) {
        case FileDialogKind::BinaryLoad: return state.binary_load_path;
        case FileDialogKind::BinarySave: return state.binary_save_path;
        case FileDialogKind::SnapshotLoad: return state.snapshot_load_path;
        case FileDialogKind::SnapshotSave: return state.snapshot_save_path;
        case FileDialogKind::TapeLoad: return state.tape_load_path;
        case FileDialogKind::TapeSave: return state.tape_save_path;
        }
        return state.binary_load_path;
    }();
    auto context = std::make_unique<FileDialogContext>();
    context->kind = kind;
    context->default_location = current_path;
    auto* callback_context = context.release();
    state.native_file_dialog_open = true;

    if(is_load_dialog(kind)) {
        SDL_ShowOpenFileDialog(file_dialog_callback, callback_context, window,
                               filters, filter_count,
                               callback_context->default_location.empty()
                                   ? nullptr
                                   : callback_context->default_location.c_str(),
                               false);
    } else {
        SDL_ShowSaveFileDialog(file_dialog_callback, callback_context, window,
                               filters, filter_count,
                               callback_context->default_location.empty()
                                   ? nullptr
                                   : callback_context->default_location.c_str());
    }
}

std::string_view rom_name(RomType type) {
    for(const auto& [candidate, name] : rom_names) {
        if(candidate == type)
            return name;
    }
    return "Unknown";
}

void reset_machine(Machine& machine, bool& paused, UiState& state) {
    machine.reset();
    paused = false;
    state.status = "Machine reset";
}

void open_binary_window(bool load, bool& paused, UiState& state) {
    if(!state.show_binary_load && !state.show_binary_save)
        state.binary_dialog_was_running = !paused;
    paused = true;
    state.show_binary_load = load;
    state.show_binary_save = !load;
    state.status = load ? "Select a binary file to load"
                        : "Select a memory range to save";
}

void finish_binary_window(bool& open, bool& paused, UiState& state) {
    open = false;
    if(state.binary_dialog_was_running)
        paused = false;
    state.binary_dialog_was_running = false;
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

void open_snapshot_dialog(SDL_Window* window, bool load, bool& paused,
                          UiState& state) {
    if(state.native_file_dialog_open)
        return;
    state.snapshot_dialog_was_running = !paused;
    paused = true;
    state.status = load ? "Select a snapshot to load"
                        : "Select where to save the snapshot";
    show_native_file_dialog(
        window, state,
        load ? FileDialogKind::SnapshotLoad : FileDialogKind::SnapshotSave);
}

void open_tape_dialog(SDL_Window* window, bool load, bool& paused,
                      UiState& state) {
    if(state.native_file_dialog_open)
        return;
    state.tape_dialog_was_running = !paused;
    paused = true;
    state.status = load ? "Select a tape to load"
                        : "Select where to record the tape";
    show_native_file_dialog(
        window, state,
        load ? FileDialogKind::TapeLoad : FileDialogKind::TapeSave);
}

void toggle_tape_playback(Machine& machine, UiState& state) {
    auto& tape = machine.tape();
    if(tape.transport() == jondra::TapeTransport::Playing) {
        tape.pause();
        state.status = "Tape paused";
    } else {
        tape.play();
        state.status = tape.mode() == jondra::TapeMode::Recording
                     ? "Tape recording enabled"
                     : "Tape playback enabled";
    }
}

void stop_tape(Machine& machine, UiState& state) {
    machine.tape().stop();
    state.status = "Tape stopped";
}

void rewind_tape(Machine& machine, UiState& state) {
    machine.tape().rewind();
    state.status = "Tape rewound";
}

void draw_file_menu(SDL_Window* window, Machine& machine, bool& paused,
                    UiState& state) {
    if(!ImGui::BeginMenu("File"))
        return;

    if(ImGui::MenuItem("Open tape for load..."))
        open_tape_dialog(window, true, paused, state);
    if(ImGui::MenuItem("Open tape for recording..."))
        open_tape_dialog(window, false, paused, state);
    ImGui::BeginDisabled(machine.tape().mode() == jondra::TapeMode::Empty);
    const bool tape_playing =
        machine.tape().transport() == jondra::TapeTransport::Playing;
    if(ImGui::MenuItem(tape_playing ? "Pause tape" : "Play tape"))
        toggle_tape_playback(machine, state);
    if(ImGui::MenuItem("Stop tape"))
        stop_tape(machine, state);
    ImGui::BeginDisabled(
        machine.tape().mode() != jondra::TapeMode::Playback);
    if(ImGui::MenuItem("Rewind tape")) {
        rewind_tape(machine, state);
    }
    ImGui::EndDisabled();
    if(ImGui::MenuItem("Eject tape")) {
        machine.tape().close();
        state.status = "Tape ejected";
    }
    ImGui::EndDisabled();
    ImGui::Separator();
    if(ImGui::MenuItem("Open snapshot..."))
        open_snapshot_dialog(window, true, paused, state);
    if(ImGui::MenuItem("Save snapshot..."))
        open_snapshot_dialog(window, false, paused, state);
    ImGui::BeginDisabled();
    ImGui::MenuItem("Save screenshot...");
    ImGui::EndDisabled();
    ImGui::Separator();
    if(ImGui::MenuItem("Load memory block..."))
        open_binary_window(true, paused, state);
    if(ImGui::MenuItem("Save memory block..."))
        open_binary_window(false, paused, state);
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

    draw_file_menu(window, machine, paused, state);
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

void draw_toolbar(SDL_Window* window, Machine& machine, bool& paused,
                  UiState& state) {
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
    if(ImGui::Button("Load tape"))
        open_tape_dialog(window, true, paused, state);
    ImGui::SameLine();
    if(ImGui::Button("Record tape"))
        open_tape_dialog(window, false, paused, state);
    ImGui::SameLine();
    if(ImGui::Button("Open snapshot"))
        open_snapshot_dialog(window, true, paused, state);
    ImGui::SameLine();
    if(ImGui::Button("Save snapshot"))
        open_snapshot_dialog(window, false, paused, state);
    ImGui::SameLine();
    if(ImGui::Button("Load binary"))
        open_binary_window(true, paused, state);
    ImGui::SameLine();
    if(ImGui::Button("Save binary"))
        open_binary_window(false, paused, state);
    ImGui::SameLine();
    if(ImGui::Button("Debugger"))
        state.show_debugger = !state.show_debugger;
    ImGui::SameLine();
    if(ImGui::Button("Settings"))
        state.show_settings = true;
}

std::string tape_time(std::size_t samples, std::uint32_t sample_rate) {
    if(sample_rate == 0)
        return "--:--";
    const auto seconds = samples / sample_rate;
    const auto minutes = seconds / 60u;
    char text[32]{};
    std::snprintf(text, sizeof(text), "%02llu:%02llu",
                  static_cast<unsigned long long>(minutes),
                  static_cast<unsigned long long>(seconds % 60u));
    return text;
}

void draw_tape_transport(Machine& machine, UiState& state) {
    auto& tape = machine.tape();
    const bool empty = tape.mode() == jondra::TapeMode::Empty;
    const bool playing =
        tape.transport() == jondra::TapeTransport::Playing;

    ImGui::TextUnformatted("Tape");
    ImGui::SameLine();
    ImGui::BeginDisabled(empty);
    if(ImGui::Button(playing ? "Pause##Tape" : "Play##Tape"))
        toggle_tape_playback(machine, state);
    ImGui::SameLine();
    if(ImGui::Button("Stop##Tape"))
        stop_tape(machine, state);
    ImGui::SameLine();
    ImGui::BeginDisabled(tape.mode() != jondra::TapeMode::Playback);
    if(ImGui::Button("Rewind##Tape"))
        rewind_tape(machine, state);
    ImGui::EndDisabled();
    ImGui::EndDisabled();

    ImGui::SameLine();
    if(tape.mode() == jondra::TapeMode::Playback) {
        const float progress = tape.length() == 0
            ? 0.0f
            : static_cast<float>(
                  static_cast<double>(tape.position()) /
                  static_cast<double>(tape.length()));
        const auto overlay =
            tape_time(tape.position(), tape.sample_rate()) + " / " +
            tape_time(tape.length(), tape.sample_rate());
        ImGui::ProgressBar(progress, {220.0f, 0.0f}, overlay.c_str());
    } else if(tape.mode() == jondra::TapeMode::Recording) {
        ImGui::Text("REC %s",
                    tape_time(tape.position(), tape.sample_rate()).c_str());
    } else {
        ImGui::TextDisabled("(no tape)");
    }

    if(!empty) {
        ImGui::SameLine();
        ImGui::TextDisabled("%.*s",
                            static_cast<int>(tape.filename().size()),
                            tape.filename().data());
    }
}

bool hex_address_input(const char* label, std::uint16_t& value) {
    return ImGui::InputScalar(label, ImGuiDataType_U16, &value, nullptr,
                              nullptr, "%04X",
                              ImGuiInputTextFlags_CharsHexadecimal);
}

void draw_binary_load(SDL_Window* window, Machine& machine, bool& paused,
                      UiState& state) {
    if(!state.show_binary_load)
        return;

    bool open = true;
    ImGui::SetNextWindowSize({520.0f, 0.0f}, ImGuiCond_FirstUseEver);
    if(ImGui::Begin("Load memory block", &open,
                    ImGuiWindowFlags_AlwaysAutoResize)) {
        ImGui::TextUnformatted("File");
        ImGui::TextWrapped("%s", state.binary_load_path.empty()
                                    ? "(no file selected)"
                                    : state.binary_load_path.c_str());
        if(ImGui::Button("Browse..."))
            show_native_file_dialog(window, state, FileDialogKind::BinaryLoad);

        ImGui::Checkbox("File contains block headers",
                        &state.binary_has_header);
        ImGui::Checkbox("Write directly to all RAM", &state.binary_all_ram);

        ImGui::BeginDisabled(state.binary_has_header);
        hex_address_input("Load address", state.binary_load_address);
        ImGui::Checkbox("Run after loading", &state.binary_run_after_load);
        ImGui::BeginDisabled(!state.binary_run_after_load);
        hex_address_input("Run address", state.binary_run_address);
        ImGui::EndDisabled();
        ImGui::EndDisabled();

        ImGui::Separator();
        ImGui::BeginDisabled(state.binary_load_path.empty() ||
                             state.native_file_dialog_open);
        if(ImGui::Button("Load")) {
            try {
                jondra::BinaryLoadOptions options;
                options.load_address = state.binary_load_address;
                options.run_address = state.binary_run_address;
                options.run_after_load = state.binary_run_after_load;
                options.all_ram = state.binary_all_ram;
                options.has_header = state.binary_has_header;
                const auto result = jondra::load_binary_file(
                    path_from_utf8(state.binary_load_path), machine, options);
                state.status =
                    "Loaded " + std::to_string(result.bytes_loaded) + " bytes";
                if(result.run_address)
                    state.status += " and set PC";
                finish_binary_window(state.show_binary_load, paused, state);
            } catch(const std::exception& error) {
                state.status = std::string("Load failed: ") + error.what();
            }
        }
        ImGui::EndDisabled();
        ImGui::SameLine();
        if(ImGui::Button("Cancel"))
            finish_binary_window(state.show_binary_load, paused, state);
    }
    ImGui::End();

    if(!open && state.show_binary_load)
        finish_binary_window(state.show_binary_load, paused, state);
}

void draw_binary_save(SDL_Window* window, const Machine& machine, bool& paused,
                      UiState& state) {
    if(!state.show_binary_save)
        return;

    bool open = true;
    ImGui::SetNextWindowSize({520.0f, 0.0f}, ImGuiCond_FirstUseEver);
    if(ImGui::Begin("Save memory block", &open,
                    ImGuiWindowFlags_AlwaysAutoResize)) {
        ImGui::TextUnformatted("File");
        ImGui::TextWrapped("%s", state.binary_save_path.empty()
                                    ? "(no file selected)"
                                    : state.binary_save_path.c_str());
        if(ImGui::Button("Browse..."))
            show_native_file_dialog(window, state, FileDialogKind::BinarySave);

        hex_address_input("First address", state.binary_save_first);
        hex_address_input("Last address", state.binary_save_last);
        const bool valid_range =
            state.binary_save_first <= state.binary_save_last;
        if(!valid_range)
            ImGui::TextColored({1.0f, 0.45f, 0.35f, 1.0f},
                               "The first address must not exceed the last.");

        ImGui::Separator();
        ImGui::BeginDisabled(state.binary_save_path.empty() || !valid_range ||
                             state.native_file_dialog_open);
        if(ImGui::Button("Save")) {
            try {
                const auto count = jondra::save_binary_file(
                    path_from_utf8(state.binary_save_path), machine,
                    state.binary_save_first, state.binary_save_last);
                state.status = "Saved " + std::to_string(count) + " bytes";
                finish_binary_window(state.show_binary_save, paused, state);
            } catch(const std::exception& error) {
                state.status = std::string("Save failed: ") + error.what();
            }
        }
        ImGui::EndDisabled();
        ImGui::SameLine();
        if(ImGui::Button("Cancel"))
            finish_binary_window(state.show_binary_save, paused, state);
    }
    ImGui::End();

    if(!open && state.show_binary_save)
        finish_binary_window(state.show_binary_save, paused, state);
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

void draw_panel_led(const char* label, bool on, ImU32 color,
                    const char* port_bit) {
    const float height = ImGui::GetFrameHeight();
    const ImVec2 top_left = ImGui::GetCursorScreenPos();
    const ImVec2 center{top_left.x + 7.0f, top_left.y + height * 0.5f};
    auto* draw_list = ImGui::GetWindowDrawList();

    if(on) {
        const ImU32 glow =
            (color & ~IM_COL32_A_MASK) | IM_COL32(0, 0, 0, 55);
        draw_list->AddCircleFilled(center, 7.0f, glow);
    }
    draw_list->AddCircleFilled(center, 6.0f, IM_COL32(12, 15, 13, 255));
    draw_list->AddCircleFilled(
        center, 4.5f, on ? color : IM_COL32(43, 48, 44, 255));
    if(on) {
        draw_list->AddCircleFilled(
            {center.x - 1.5f, center.y - 1.5f}, 1.3f,
            IM_COL32(255, 255, 255, 170));
    }

    ImGui::Dummy({14.0f, height});
    if(ImGui::IsItemHovered())
        ImGui::SetTooltip("%s: %s (%s, active low)",
                          label, on ? "on" : "off", port_bit);
    ImGui::SameLine(0.0f, 3.0f);
    ImGui::TextUnformatted(label);
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
    ImGui::TextDisabled("|");
    ImGui::SameLine();
    draw_panel_led("LED1", machine.green_led_on(),
                   IM_COL32(35, 225, 85, 255), "A0 bit 0");
    ImGui::SameLine();
    draw_panel_led("LED2", machine.yellow_led_on(),
                   IM_COL32(245, 205, 35, 255), "A0 bit 1");
    ImGui::SameLine();
    const auto& tape = machine.tape();
    const char* tape_state = "empty";
    if(tape.mode() == jondra::TapeMode::Playback)
        tape_state = tape.finished() ? "end" :
                     tape.transport() == jondra::TapeTransport::Paused
                         ? "paused" :
                     tape.transport() == jondra::TapeTransport::Stopped
                         ? "stopped" :
                     tape.motor_running() ? "play" : "ready";
    else if(tape.mode() == jondra::TapeMode::Recording)
        tape_state =
            tape.transport() == jondra::TapeTransport::Paused ? "paused" :
            tape.transport() == jondra::TapeTransport::Stopped ? "stopped" :
            tape.motor_running() ? "record" : "record ready";
    const auto status =
        tape.error().empty() ? std::string_view(state.status) : tape.error();
    ImGui::Text("| ROM: %.*s | DMA: %s | TAPE: %s | T-states: %llu | %s",
                static_cast<int>(rom_name(state.rom_type).size()),
                rom_name(state.rom_type).data(),
                machine.dma_enabled() ? "on" : "off",
                tape_state,
                static_cast<unsigned long long>(machine.ticks()),
                status.data());
}

void draw_main_window(SDL_Window* window, SDL_Texture* texture,
                      Machine& machine, bool& paused, UiState& state) {
    const ImGuiViewport* viewport = ImGui::GetMainViewport();
    const float menu_height = ImGui::GetFrameHeight();
    ImGui::SetNextWindowPos({viewport->Pos.x, viewport->Pos.y + menu_height});
    ImGui::SetNextWindowSize(
        {viewport->Size.x, std::max(1.0f, viewport->Size.y - menu_height)});

    constexpr ImGuiWindowFlags flags =
        ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove |
        ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoBringToFrontOnFocus;
    ImGui::Begin("##JOndraMain", nullptr, flags);
    draw_toolbar(window, machine, paused, state);
    draw_tape_transport(machine, state);
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
        bool sound = machine.audio().builtin_enabled();
        if(ImGui::Checkbox("Built-in sound", &sound)) {
            machine.set_builtin_sound_enabled(sound);
            state.status = sound ? "Built-in sound enabled"
                                 : "Built-in sound disabled";
        }
        bool melodik = machine.audio().melodik_enabled();
        if(ImGui::Checkbox("Melodik (SN76489)", &melodik)) {
            machine.set_melodik_enabled(melodik);
            state.status = melodik ? "Melodik enabled" : "Melodik disabled";
        }
    }
    ImGui::End();
}

template<typename Getter, typename Setter>
void draw_register(Machine& machine, const char* label,
                   Getter getter, Setter setter) {
    auto value = static_cast<std::uint16_t>((machine.*getter)());
    ImGui::SetNextItemWidth(82.0f);
    if(ImGui::InputScalar(label, ImGuiDataType_U16, &value, nullptr, nullptr,
                          "%04X",
                          ImGuiInputTextFlags_CharsHexadecimal |
                          ImGuiInputTextFlags_EnterReturnsTrue))
        (machine.*setter)(value);
}

void draw_cpu_flags(Machine& machine) {
    struct Flag {
        std::uint8_t mask;
        const char* set_label;
        const char* clear_label;
        const char* description;
    };
    static constexpr std::array flags{
        Flag{0x80u, "M", "P", "sign"},
        Flag{0x40u, "Z", "NZ", "zero"},
        Flag{0x10u, "AC", "NA", "half carry"},
        Flag{0x04u, "PE", "PO", "parity / overflow"},
        Flag{0x02u, "N1", "N0", "add / subtract"},
        Flag{0x01u, "C", "NC", "carry"},
    };

    ImGui::TextDisabled("F:");
    ImGui::SameLine(0.0f, 5.0f);
    for(std::size_t index = 0; index < flags.size(); ++index) {
        const auto& flag = flags[index];
        const bool set = (machine.get_f() & flag.mask) != 0;
        if(index != 0)
            ImGui::SameLine(0.0f, 7.0f);
        ImGui::PushID(static_cast<int>(flag.mask));
        ImGui::TextColored(
            set ? ImVec4{0.95f, 0.95f, 0.95f, 1.0f}
                : ImVec4{0.42f, 0.42f, 0.42f, 1.0f},
            "%s", set ? flag.set_label : flag.clear_label);
        if(ImGui::IsItemClicked())
            machine.set_f(machine.get_f() ^ flag.mask);
        if(ImGui::IsItemHovered())
            ImGui::SetTooltip("%s flag: %s (click to toggle)",
                              flag.description, set ? "set" : "clear");
        ImGui::PopID();
    }
}

void draw_debugger(Machine& machine, bool& paused, UiState& state) {
    if(!state.show_debugger)
        return;

    ImGui::SetNextWindowSize({760.0f, 620.0f}, ImGuiCond_FirstUseEver);
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
        ImGui::BeginDisabled(!paused);
        if(ImGui::Button("Step into (F7)")) {
            machine.step_instruction();
            state.debugger_follow_pc = true;
            state.status = "Executed one instruction";
        }
        ImGui::SameLine();
        if(ImGui::Button("Step over (F8)")) {
            if(machine.step_over()) {
                paused = false;
                state.status = "Running to return address";
            } else {
                state.status = "Executed one instruction";
            }
            state.debugger_follow_pc = true;
        }
        ImGui::EndDisabled();

        ImGui::SeparatorText("CPU");
        if(ImGui::BeginTable("##Registers", 4,
                             ImGuiTableFlags_SizingStretchSame)) {
            ImGui::TableNextColumn();
            draw_register(machine, "AF", &Machine::get_af, &Machine::set_af);
            draw_cpu_flags(machine);
            draw_register(machine, "BC", &Machine::get_bc, &Machine::set_bc);
            draw_register(machine, "DE", &Machine::get_de, &Machine::set_de);
            draw_register(machine, "HL", &Machine::get_hl, &Machine::set_hl);
            ImGui::TableNextColumn();
            draw_register(machine, "AF'", &Machine::get_alt_af,
                          &Machine::set_alt_af);
            draw_register(machine, "BC'", &Machine::get_alt_bc,
                          &Machine::set_alt_bc);
            draw_register(machine, "DE'", &Machine::get_alt_de,
                          &Machine::set_alt_de);
            draw_register(machine, "HL'", &Machine::get_alt_hl,
                          &Machine::set_alt_hl);
            ImGui::TableNextColumn();
            draw_register(machine, "IX", &Machine::get_ix, &Machine::set_ix);
            draw_register(machine, "IY", &Machine::get_iy, &Machine::set_iy);
            draw_register(machine, "SP", &Machine::get_sp, &Machine::set_sp);
            draw_register(machine, "PC", &Machine::get_pc, &Machine::set_pc);
            ImGui::TableNextColumn();
            draw_register(machine, "IR", &Machine::get_ir, &Machine::set_ir);
            bool iff1 = machine.get_iff1();
            if(ImGui::Checkbox("IFF1", &iff1))
                machine.set_iff1(iff1);
            bool iff2 = machine.get_iff2();
            if(ImGui::Checkbox("IFF2", &iff2))
                machine.set_iff2(iff2);
            auto interrupt_mode =
                static_cast<std::uint8_t>(machine.get_int_mode());
            ImGui::SetNextItemWidth(55.0f);
            if(ImGui::InputScalar("IM", ImGuiDataType_U8, &interrupt_mode,
                                  nullptr, nullptr, "%u"))
                machine.set_int_mode(std::min<unsigned>(interrupt_mode, 2));
            ImGui::EndTable();
        }
        ImGui::Text("T-states: %llu    ROM: %s    I/O: %s",
                    static_cast<unsigned long long>(machine.ticks()),
                    machine.memory().rom_mapped() ? "mapped" : "RAM",
                    machine.memory().io_mapped() ? "mapped" : "off");

        ImGui::SeparatorText("Disassembly");
        ImGui::Checkbox("Follow PC", &state.debugger_follow_pc);
        ImGui::SameLine();
        ImGui::BeginDisabled(state.debugger_follow_pc);
        ImGui::SetNextItemWidth(88.0f);
        ImGui::InputScalar("Address", ImGuiDataType_U16,
                           &state.debugger_address, nullptr, nullptr, "%04X",
                           ImGuiInputTextFlags_CharsHexadecimal);
        ImGui::EndDisabled();

        auto address = state.debugger_follow_pc
            ? static_cast<std::uint16_t>(machine.get_pc())
            : state.debugger_address;
        if(ImGui::BeginTable(
               "##Disassembly", 4,
               ImGuiTableFlags_Borders | ImGuiTableFlags_RowBg |
               ImGuiTableFlags_ScrollY, {0.0f, 230.0f})) {
            ImGui::TableSetupColumn("BP", ImGuiTableColumnFlags_WidthFixed,
                                    34.0f);
            ImGui::TableSetupColumn("Address",
                                    ImGuiTableColumnFlags_WidthFixed, 70.0f);
            ImGui::TableSetupColumn("Bytes",
                                    ImGuiTableColumnFlags_WidthFixed, 125.0f);
            ImGui::TableSetupColumn("Instruction");
            ImGui::TableHeadersRow();
            for(unsigned row = 0; row < 18; ++row) {
                const auto instruction = jondra::decode_instruction(
                    machine, address);
                const bool current =
                    address == static_cast<std::uint16_t>(machine.get_pc());
                ImGui::TableNextRow();
                if(current) {
                    ImGui::TableSetBgColor(
                        ImGuiTableBgTarget_RowBg0,
                        ImGui::GetColorU32(ImGuiCol_HeaderActive));
                }
                ImGui::TableSetColumnIndex(0);
                ImGui::PushID(static_cast<int>(address));
                if(ImGui::SmallButton(
                       machine.has_breakpoint(address) ? "x" : "+")) {
                    if(machine.has_breakpoint(address))
                        machine.remove_breakpoint(address);
                    else
                        machine.add_breakpoint(address);
                }
                ImGui::PopID();
                ImGui::TableSetColumnIndex(1);
                ImGui::Text("%04X%s", address, current ? " >" : "");
                ImGui::TableSetColumnIndex(2);
                std::string bytes;
                char byte[4]{};
                for(unsigned index = 0; index < instruction.length; ++index) {
                    std::snprintf(byte, sizeof(byte), "%02X",
                                  instruction.bytes[index]);
                    if(!bytes.empty())
                        bytes += ' ';
                    bytes += byte;
                }
                ImGui::TextUnformatted(bytes.c_str());
                ImGui::TableSetColumnIndex(3);
                ImGui::TextUnformatted(instruction.text.c_str());
                address = static_cast<std::uint16_t>(
                    address + instruction.length);
            }
            ImGui::EndTable();
        }

        ImGui::SeparatorText("Execution breakpoints");
        ImGui::SetNextItemWidth(88.0f);
        ImGui::InputScalar("##BreakpointAddress", ImGuiDataType_U16,
                           &state.debugger_breakpoint_address,
                           nullptr, nullptr, "%04X",
                           ImGuiInputTextFlags_CharsHexadecimal);
        ImGui::SameLine();
        if(ImGui::Button("Add")) {
            machine.add_breakpoint(state.debugger_breakpoint_address);
            state.status = "Execution breakpoint added";
        }
        ImGui::SameLine();
        ImGui::BeginDisabled(machine.breakpoints().empty());
        if(ImGui::Button("Clear all"))
            machine.clear_breakpoints();
        ImGui::EndDisabled();

        for(const auto breakpoint : machine.breakpoints()) {
            ImGui::PushID(static_cast<int>(breakpoint));
            ImGui::Text("$%04X", breakpoint);
            ImGui::SameLine();
            if(ImGui::SmallButton("Remove")) {
                machine.remove_breakpoint(breakpoint);
                ImGui::PopID();
                break;
            }
            ImGui::PopID();
            ImGui::SameLine();
        }
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
    process_file_dialog_results(machine, rom_directory, paused, state);
    draw_menu_bar(window, machine, paused, state);
    draw_main_window(window, screen_texture, machine, paused, state);
    draw_binary_load(window, machine, paused, state);
    draw_binary_save(window, machine, paused, state);
    draw_settings(window, machine, rom_directory, paused, state);
    draw_debugger(machine, paused, state);
    draw_about(state);
}

} // namespace jondra
