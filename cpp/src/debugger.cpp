#include "jondra/debugger.hpp"

#include "jondra/machine.hpp"

#include "z80.h"

#include <algorithm>
#include <cstdio>

namespace {

class Disassembler final : public z80::z80_disasm<Disassembler> {
public:
    void decode(const std::array<std::uint8_t, 8>& bytes) {
        bytes_ = bytes;
        index_ = 0;
        output_[0] = '\0';

        // DD and FD only select an index register. Decode again to consume
        // the actual instruction following the prefix.
        do {
            z80::z80_disasm<Disassembler>::on_disassemble();
        } while(output_[0] == '\0' && index_ < bytes_.size());
    }

    z80::fast_u8 on_read_next_byte() {
        if(index_ >= bytes_.size())
            return 0;
        return bytes_[index_++];
    }

    void on_emit(const char* text) {
        std::snprintf(output_, sizeof(output_), "%s", text);
    }

    [[nodiscard]] const char* output() const noexcept { return output_; }
    [[nodiscard]] std::size_t consumed() const noexcept { return index_; }

private:
    std::array<std::uint8_t, 8> bytes_{};
    std::size_t index_ = 0;
    char output_[64]{};
};

bool is_step_over_candidate(
    const std::array<std::uint8_t, 8>& bytes, std::size_t length) {
    std::size_t index = 0;
    while(index + 1 < length &&
          (bytes[index] == 0xddu || bytes[index] == 0xfdu))
        ++index;
    const auto opcode = bytes[index];
    return opcode == 0xcdu || (opcode & 0xc7u) == 0xc4u ||
           (opcode & 0xc7u) == 0xc7u;
}

} // namespace

namespace jondra {

DecodedInstruction decode_instruction(
    const Machine& machine, std::uint16_t address) {
    DecodedInstruction result;
    result.address = address;
    for(std::size_t index = 0; index < result.bytes.size(); ++index) {
        result.bytes[index] = machine.memory().read(
            static_cast<std::uint16_t>(address + index));
    }

    Disassembler disassembler;
    disassembler.decode(result.bytes);
    result.length = static_cast<std::uint8_t>(
        std::clamp<std::size_t>(disassembler.consumed(), 1, result.bytes.size()));
    result.text = disassembler.output();
    if(result.text.empty())
        result.text = "db";
    result.step_over_candidate =
        is_step_over_candidate(result.bytes, result.length);
    return result;
}

} // namespace jondra
