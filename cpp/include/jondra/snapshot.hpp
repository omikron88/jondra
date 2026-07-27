#pragma once

#include "jondra/memory.hpp"

#include <filesystem>

namespace jondra {

class Machine;

// Reads and writes the Java JOndra OSN v2 snapshot format.
void save_snapshot_file(const std::filesystem::path& path,
                        const Machine& machine);

RomType load_snapshot_file(const std::filesystem::path& path,
                           Machine& machine,
                           const std::filesystem::path& rom_directory);

} // namespace jondra
