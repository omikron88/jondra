#include "jondra/keyboard.hpp"

#include <algorithm>

namespace jondra {

Keyboard::Keyboard() {
    reset();
}

void Keyboard::reset() {
    matrix_.fill(0xff);
}

std::optional<Keyboard::Position> Keyboard::position(Key key) {
    switch(key) {
    case Key::Q: return Position{0, 4};
    case Key::T: return Position{0, 3};
    case Key::W: return Position{0, 2};
    case Key::E: return Position{0, 1};
    case Key::R: return Position{0, 0};
    case Key::A: return Position{1, 4};
    case Key::G: return Position{1, 3};
    case Key::S: return Position{1, 2};
    case Key::D: return Position{1, 1};
    case Key::F: return Position{1, 0};
    case Key::Symbols: return Position{2, 4};
    case Key::V: return Position{2, 3};
    case Key::Z: return Position{2, 2};
    case Key::X: return Position{2, 1};
    case Key::C: return Position{2, 0};
    case Key::Space: return Position{3, 0};
    case Key::Shift: return Position{4, 4};
    case Key::Czech: return Position{4, 2};
    case Key::Numbers: return Position{4, 1};
    case Key::Enter: return Position{5, 4};
    case Key::H: return Position{5, 3};
    case Key::L: return Position{5, 2};
    case Key::K: return Position{5, 1};
    case Key::J: return Position{5, 0};
    case Key::P: return Position{6, 4};
    case Key::Y: return Position{6, 3};
    case Key::O: return Position{6, 2};
    case Key::I: return Position{6, 1};
    case Key::U: return Position{6, 0};
    case Key::Control: return Position{7, 4};
    case Key::B: return Position{7, 3};
    case Key::Up: return Position{7, 2};
    case Key::M: return Position{7, 1};
    case Key::N: return Position{7, 0};
    case Key::Right: return Position{8, 4};
    case Key::Down: return Position{8, 2};
    case Key::Left: return Position{8, 1};
    case Key::JoyFire: return Position{9, 4};
    case Key::JoyDown: return Position{9, 3};
    case Key::JoyUp: return Position{9, 2};
    case Key::JoyLeft: return Position{9, 1};
    case Key::JoyRight: return Position{9, 0};
    }
    return std::nullopt;
}

void Keyboard::set(Key key, bool pressed) {
    const auto pos = position(key);
    if(!pos)
        return;

    const auto mask = static_cast<std::uint8_t>(1u << pos->bit);
    if(pressed)
        matrix_[pos->row] &= static_cast<std::uint8_t>(~mask);
    else
        matrix_[pos->row] |= mask;
}

std::uint8_t Keyboard::read(std::uint16_t address) const {
    auto value = matrix_[address & 0xffu];
    if(melodik_present_ && (address & 0xffu) == 0x0fu)
        value &= 0xdfu;
    return value;
}

void Keyboard::set_tape_input(bool high) {
    for(auto& value : matrix_) {
        if(high)
            value |= 0x80u;
        else
            value &= 0x7fu;
    }
}

} // namespace jondra
