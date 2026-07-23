#pragma once

#include <cstdint>
#include <span>
#include <string_view>

namespace jondra {

[[nodiscard]] std::span<const std::uint8_t> embedded_rom(std::string_view name);

} // namespace jondra
