#include "jondra/machine.hpp"
#include "jondra/ui.hpp"

#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>

#include "imgui.h"
#include "imgui_impl_sdl3.h"
#include "imgui_impl_sdlrenderer3.h"

#include <array>
#include <chrono>
#include <cstdint>
#include <exception>
#include <filesystem>
#include <iostream>
#include <optional>
#include <string_view>
#include <thread>

namespace {

using jondra::Key;

std::optional<Key> translate_key(SDL_Scancode key) {
    switch(key) {
    case SDL_SCANCODE_Q: return Key::Q;
    case SDL_SCANCODE_T: return Key::T;
    case SDL_SCANCODE_W: return Key::W;
    case SDL_SCANCODE_E: return Key::E;
    case SDL_SCANCODE_R: return Key::R;
    case SDL_SCANCODE_A: return Key::A;
    case SDL_SCANCODE_G: return Key::G;
    case SDL_SCANCODE_S: return Key::S;
    case SDL_SCANCODE_D: return Key::D;
    case SDL_SCANCODE_F: return Key::F;
    case SDL_SCANCODE_LALT:
    case SDL_SCANCODE_RALT: return Key::Symbols;
    case SDL_SCANCODE_V: return Key::V;
    case SDL_SCANCODE_Z: return Key::Z;
    case SDL_SCANCODE_X: return Key::X;
    case SDL_SCANCODE_C: return Key::C;
    case SDL_SCANCODE_SPACE: return Key::Space;
    case SDL_SCANCODE_LSHIFT:
    case SDL_SCANCODE_RSHIFT: return Key::Shift;
    case SDL_SCANCODE_TAB: return Key::Numbers;
    case SDL_SCANCODE_EQUALS: return Key::Czech;
    case SDL_SCANCODE_RETURN:
    case SDL_SCANCODE_KP_ENTER: return Key::Enter;
    case SDL_SCANCODE_H: return Key::H;
    case SDL_SCANCODE_L: return Key::L;
    case SDL_SCANCODE_K: return Key::K;
    case SDL_SCANCODE_J: return Key::J;
    case SDL_SCANCODE_P: return Key::P;
    case SDL_SCANCODE_Y: return Key::Y;
    case SDL_SCANCODE_O: return Key::O;
    case SDL_SCANCODE_I: return Key::I;
    case SDL_SCANCODE_U: return Key::U;
    case SDL_SCANCODE_LCTRL:
    case SDL_SCANCODE_RCTRL: return Key::Control;
    case SDL_SCANCODE_B: return Key::B;
    case SDL_SCANCODE_UP: return Key::Up;
    case SDL_SCANCODE_M: return Key::M;
    case SDL_SCANCODE_N: return Key::N;
    case SDL_SCANCODE_RIGHT: return Key::Right;
    case SDL_SCANCODE_DOWN: return Key::Down;
    case SDL_SCANCODE_LEFT: return Key::Left;
    case SDL_SCANCODE_KP_0: return Key::JoyFire;
    case SDL_SCANCODE_KP_2: return Key::JoyDown;
    case SDL_SCANCODE_KP_8: return Key::JoyUp;
    case SDL_SCANCODE_KP_4: return Key::JoyLeft;
    case SDL_SCANCODE_KP_6: return Key::JoyRight;
    default: return std::nullopt;
    }
}
jondra::RomType parse_rom(std::string_view value) {
    if(value == "basic") return jondra::RomType::Basic;
    if(value == "tesla") return jondra::RomType::Tesla;
    if(value == "vili") return jondra::RomType::Vili;
    if(value == "plus") return jondra::RomType::Plus;
    throw std::runtime_error("Unknown ROM type: " + std::string(value));
}

} // namespace

int main(int argc, char** argv) {
    try {
        std::filesystem::path rom_directory = JONDRA_DEFAULT_ROM_DIR;
        auto rom_type = jondra::RomType::Basic;

        for(int i = 1; i < argc; ++i) {
            const std::string_view argument(argv[i]);
            if(argument == "--rom-dir" && i + 1 < argc)
                rom_directory = argv[++i];
            else if(argument == "--rom" && i + 1 < argc)
                rom_type = parse_rom(argv[++i]);
            else if(argument == "--help") {
                std::cout << "Usage: jondra [--rom basic|tesla|vili|plus] [--rom-dir PATH]\n"
                             "F5 pause, F11 NMI, F12 reset, Escape quit\n";
                return 0;
            } else {
                throw std::runtime_error("Unknown argument: " + std::string(argument));
            }
        }

        if(!SDL_Init(SDL_INIT_VIDEO))
            throw std::runtime_error(SDL_GetError());

        SDL_Window* window = nullptr;
        SDL_Renderer* renderer = nullptr;
        if(!SDL_CreateWindowAndRenderer(
                "JOndra C++", 1120, 820,
                SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIGH_PIXEL_DENSITY,
                &window, &renderer))
            throw std::runtime_error(SDL_GetError());

        SDL_Texture* texture = SDL_CreateTexture(
            renderer, SDL_PIXELFORMAT_ARGB8888, SDL_TEXTUREACCESS_STREAMING,
            jondra::Machine::screen_width, jondra::Machine::screen_height);
        if(!texture)
            throw std::runtime_error(SDL_GetError());

        SDL_SetTextureScaleMode(texture, SDL_SCALEMODE_NEAREST);

        IMGUI_CHECKVERSION();
        ImGui::CreateContext();
        ImGuiIO& io = ImGui::GetIO();
        io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
        ImGui::StyleColorsDark();
        if(!ImGui_ImplSDL3_InitForSDLRenderer(window, renderer))
            throw std::runtime_error("Could not initialize ImGui SDL3 backend");
        if(!ImGui_ImplSDLRenderer3_Init(renderer))
            throw std::runtime_error("Could not initialize ImGui renderer backend");

        jondra::Machine machine;
        machine.load_rom(rom_type, rom_directory);
        machine.reset();
        jondra::UiState ui_state;
        ui_state.rom_type = rom_type;

        std::array<std::uint32_t,
                   jondra::Machine::screen_width * jondra::Machine::screen_height> pixels{};
        bool running = true;
        bool paused = false;
        auto deadline = std::chrono::steady_clock::now();

        while(running) {
            SDL_Event event{};
            while(SDL_PollEvent(&event)) {
                ImGui_ImplSDL3_ProcessEvent(&event);
                if(event.type == SDL_EVENT_QUIT)
                    running = false;
                else if(event.type == SDL_EVENT_KEY_DOWN ||
                        event.type == SDL_EVENT_KEY_UP) {
                    const bool pressed = event.type == SDL_EVENT_KEY_DOWN;
                    const bool captured = ImGui::GetIO().WantCaptureKeyboard;
                    if(pressed && !event.key.repeat && !captured) {
                        if(event.key.scancode == SDL_SCANCODE_ESCAPE)
                            running = false;
                        else if(event.key.scancode == SDL_SCANCODE_F5)
                            paused = !paused;
                        else if(event.key.scancode == SDL_SCANCODE_F11)
                            machine.nmi();
                        else if(event.key.scancode == SDL_SCANCODE_F12)
                            machine.reset();
                    }
                    if(!pressed || !captured) {
                        if(const auto key = translate_key(event.key.scancode))
                            machine.key(*key, pressed);
                    }
                }
            }

            running &= !ui_state.quit_requested;
            if(!paused)
                machine.run_frame();

            const auto packed = machine.framebuffer();
            for(std::size_t byte = 0; byte < packed.size(); ++byte) {
                for(unsigned bit = 0; bit < 8; ++bit) {
                    const bool set = (packed[byte] & (0x80u >> bit)) != 0;
                    pixels[byte * 8 + bit] = set ? 0xffe8e8e8u : 0xff111716u;
                }
            }

            SDL_UpdateTexture(texture, nullptr, pixels.data(),
                              jondra::Machine::screen_width * sizeof(std::uint32_t));

            ImGui_ImplSDLRenderer3_NewFrame();
            ImGui_ImplSDL3_NewFrame();
            ImGui::NewFrame();
            jondra::draw_ui(window, texture, machine, rom_directory, paused,
                            ui_state);
            ImGui::Render();

            SDL_SetRenderScale(renderer, io.DisplayFramebufferScale.x,
                              io.DisplayFramebufferScale.y);
            SDL_SetRenderDrawColor(renderer, 18, 20, 22, 255);
            SDL_RenderClear(renderer);
            ImGui_ImplSDLRenderer3_RenderDrawData(ImGui::GetDrawData(), renderer);
            SDL_RenderPresent(renderer);

            deadline += std::chrono::milliseconds(20);
            std::this_thread::sleep_until(deadline);
            const auto now = std::chrono::steady_clock::now();
            if(deadline + std::chrono::milliseconds(100) < now)
                deadline = now;
        }

        ImGui_ImplSDLRenderer3_Shutdown();
        ImGui_ImplSDL3_Shutdown();
        ImGui::DestroyContext();
        SDL_DestroyTexture(texture);
        SDL_DestroyRenderer(renderer);
        SDL_DestroyWindow(window);
        SDL_Quit();
        return 0;
    } catch(const std::exception& error) {
        std::cerr << "jondra: " << error.what() << '\n';
        SDL_Quit();
        return 1;
    }
}
