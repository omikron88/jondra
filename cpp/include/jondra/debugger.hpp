#pragma once

#include <array>
#include <cstdint>
#include <string>

namespace jondra {

class Machine;

struct DecodedInstruction {
    std::uint16_t address = 0;
    std::uint8_t length = 1;
    std::array<std::uint8_t, 8> bytes{};
    std::string text;
    bool step_over_candidate = false;
};

[[nodiscard]] DecodedInstruction decode_instruction(
    const Machine& machine, std::uint16_t address);

} // namespace jondra
