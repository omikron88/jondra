#pragma once

#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <optional>

namespace jondra {

class Machine;

struct BinaryLoadOptions {
    std::uint16_t load_address = 0x4000;
    std::uint16_t run_address = 0x4000;
    bool run_after_load = false;
    bool all_ram = false;
    bool has_header = false;
};

struct BinaryLoadResult {
    std::size_t bytes_loaded = 0;
    std::optional<std::uint16_t> run_address;
};

BinaryLoadResult load_binary_file(const std::filesystem::path& path,
                                  Machine& machine,
                                  const BinaryLoadOptions& options);

std::size_t save_binary_file(const std::filesystem::path& path,
                             const Machine& machine,
                             std::uint16_t first_address,
                             std::uint16_t last_address);

} // namespace jondra
