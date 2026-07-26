#include "jondra/binary_file.hpp"

#include "jondra/machine.hpp"

#include <fstream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace {

using ByteVector = std::vector<std::uint8_t>;

struct DataBlock {
    std::uint16_t address;
    ByteVector data;
};

ByteVector read_file(const std::filesystem::path& path) {
    std::ifstream stream(path, std::ios::binary);
    if(!stream)
        throw std::runtime_error("Cannot open binary file: " + path.string());

    stream.seekg(0, std::ios::end);
    const auto end = stream.tellg();
    if(end < 0)
        throw std::runtime_error("Cannot determine binary file size");
    stream.seekg(0, std::ios::beg);

    ByteVector data(static_cast<std::size_t>(end));
    if(!data.empty() &&
       !stream.read(reinterpret_cast<char*>(data.data()),
                    static_cast<std::streamsize>(data.size()))) {
        throw std::runtime_error("Cannot read binary file");
    }
    return data;
}

std::uint16_t read_u16(const ByteVector& data, std::size_t& position) {
    if(data.size() - position < 2)
        throw std::runtime_error("Truncated binary file header");
    const auto value = static_cast<std::uint16_t>(
        data[position] | (static_cast<unsigned>(data[position + 1]) << 8u));
    position += 2;
    return value;
}

void validate_block(std::uint16_t address, std::size_t length) {
    constexpr auto address_space = jondra::Memory::address_space_size;
    if(length > address_space - static_cast<std::size_t>(address))
        throw std::runtime_error("Binary block exceeds the 64 KiB address space");
}

void write_blocks(jondra::Machine& machine,
                  const std::vector<DataBlock>& blocks, bool all_ram) {
    for(const auto& block : blocks) {
        auto address = static_cast<std::size_t>(block.address);
        for(const auto value : block.data)
            machine.write_memory(static_cast<std::uint16_t>(address++), value,
                                 all_ram);
    }
}

} // namespace

namespace jondra {

BinaryLoadResult load_binary_file(const std::filesystem::path& path,
                                  Machine& machine,
                                  const BinaryLoadOptions& options) {
    const auto file = read_file(path);
    std::vector<DataBlock> blocks;
    BinaryLoadResult result;

    if(!options.has_header) {
        validate_block(options.load_address, file.size());
        blocks.push_back({options.load_address, file});
        if(options.run_after_load)
            result.run_address = options.run_address;
    } else {
        std::size_t position = 0;
        while(position < file.size()) {
            const auto type = file[position++];
            if(type == 1) {
                const auto address = read_u16(file, position);
                const auto length = read_u16(file, position);
                validate_block(address, length);
                if(file.size() - position < length)
                    throw std::runtime_error("Truncated binary data block");

                DataBlock block{address, {}};
                block.data.insert(block.data.end(),
                                  file.begin() + static_cast<std::ptrdiff_t>(position),
                                  file.begin() + static_cast<std::ptrdiff_t>(
                                                     position + length));
                position += length;
                blocks.push_back(std::move(block));
            } else if(type == 2) {
                result.run_address = read_u16(file, position);
                break;
            } else {
                throw std::runtime_error("Unknown binary header block type");
            }
        }
    }

    for(const auto& block : blocks)
        result.bytes_loaded += block.data.size();
    write_blocks(machine, blocks, options.all_ram);
    if(result.run_address)
        machine.set_pc(*result.run_address);
    return result;
}

std::size_t save_binary_file(const std::filesystem::path& path,
                             const Machine& machine,
                             std::uint16_t first_address,
                             std::uint16_t last_address) {
    if(first_address > last_address)
        throw std::runtime_error("The first address must not exceed the last");

    std::ofstream stream(path, std::ios::binary | std::ios::trunc);
    if(!stream)
        throw std::runtime_error("Cannot create binary file: " + path.string());

    const auto count = static_cast<std::size_t>(last_address) - first_address + 1;
    for(std::size_t offset = 0; offset < count; ++offset) {
        const auto value = machine.memory().read(
            static_cast<std::uint16_t>(first_address + offset));
        stream.put(static_cast<char>(value));
    }
    if(!stream)
        throw std::runtime_error("Cannot write binary file");
    return count;
}

} // namespace jondra
