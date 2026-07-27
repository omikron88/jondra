#pragma once

#include <cstdint>
#include <span>

namespace jondra {

[[nodiscard]] std::span<const std::uint8_t>
embedded_sound_sample(unsigned tone) noexcept;

} // namespace jondra
