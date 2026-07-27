#pragma once

#include <array>
#include <cstdint>
#include <optional>

namespace jondra {

enum class Key {
    Q, T, W, E, R,
    A, G, S, D, F,
    Symbols, V, Z, X, C,
    Space, Shift, Numbers, Czech,
    Enter, H, L, K, J,
    P, Y, O, I, U,
    Control, B, Up, M, N,
    Right, Down, Left,
    JoyFire, JoyDown, JoyUp, JoyLeft, JoyRight
};

class Keyboard {
public:
    static constexpr std::size_t matrix_size = 2'048;

    Keyboard();

    void reset();
    void set(Key key, bool pressed);
    [[nodiscard]] std::uint8_t read(std::uint16_t address) const;
    void set_tape_input(bool high);
    void set_melodik_present(bool present) noexcept {
        melodik_present_ = present;
    }

private:
    struct Position {
        std::uint8_t row;
        std::uint8_t bit;
    };

    static std::optional<Position> position(Key key);

    std::array<std::uint8_t, matrix_size> matrix_{};
    bool melodik_present_ = true;
};

} // namespace jondra
