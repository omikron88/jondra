#include "jondra/embedded_roms.hpp"

namespace jondra {

std::span<const std::uint8_t> embedded_rom(std::string_view) {
    return {};
}

} // namespace jondra
