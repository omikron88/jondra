#include "jondra/snapshot.hpp"

#include "jondra/machine.hpp"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <fstream>
#include <span>
#include <stdexcept>
#include <string>
#include <vector>

namespace {

using ByteVector = std::vector<std::uint8_t>;

constexpr std::array<std::uint8_t, 3> signature{'O', 'S', 'N'};
constexpr std::uint8_t current_version = 2;
constexpr std::size_t v1_size = 65'574;
constexpr std::size_t v2_size = 65'575;

struct Snapshot {
    std::uint8_t version = current_version;
    std::uint8_t i = 0;
    std::uint16_t alt_hl = 0;
    std::uint16_t alt_de = 0;
    std::uint16_t alt_bc = 0;
    std::uint16_t alt_af = 0;
    std::uint16_t hl = 0;
    std::uint16_t de = 0;
    std::uint16_t bc = 0;
    std::uint16_t iy = 0;
    std::uint16_t ix = 0;
    bool iff1 = false;
    bool iff2 = false;
    bool pending_ei = false;
    bool halted = false;
    std::uint8_t r = 0;
    std::uint16_t af = 0;
    std::uint16_t sp = 0;
    std::uint16_t pc = 0;
    std::uint8_t interrupt_mode = 0;
    std::uint16_t wz = 0;
    std::uint8_t port_a0 = 0;
    std::uint8_t port_a1 = 0;
    std::uint8_t port_a3 = 0;
    jondra::RomType rom_type = jondra::RomType::Basic;
    std::uint8_t resolution = 255;
    std::array<std::uint8_t, jondra::Memory::address_space_size> ram{};
};

ByteVector read_file(const std::filesystem::path& path) {
    std::ifstream stream(path, std::ios::binary);
    if(!stream)
        throw std::runtime_error("Cannot open snapshot: " + path.string());
    stream.seekg(0, std::ios::end);
    const auto end = stream.tellg();
    if(end < 0)
        throw std::runtime_error("Cannot determine snapshot size");
    stream.seekg(0, std::ios::beg);

    ByteVector data(static_cast<std::size_t>(end));
    if(!data.empty() &&
       !stream.read(reinterpret_cast<char*>(data.data()),
                    static_cast<std::streamsize>(data.size())))
        throw std::runtime_error("Cannot read snapshot");
    return data;
}

std::uint8_t read_u8(const ByteVector& data, std::size_t& position) {
    if(position == data.size())
        throw std::runtime_error("Truncated snapshot");
    return data[position++];
}

std::uint16_t read_u16(const ByteVector& data, std::size_t& position) {
    const auto low = read_u8(data, position);
    const auto high = read_u8(data, position);
    return static_cast<std::uint16_t>(
        low | (static_cast<unsigned>(high) << 8u));
}

void write_u8(std::ostream& stream, std::uint8_t value) {
    stream.put(static_cast<char>(value));
}

void write_u16(std::ostream& stream, std::uint16_t value) {
    write_u8(stream, static_cast<std::uint8_t>(value));
    write_u8(stream, static_cast<std::uint8_t>(value >> 8u));
}

jondra::RomType decode_rom(std::uint8_t value) {
    switch(value) {
    case 0: return jondra::RomType::Basic;
    case 1: return jondra::RomType::Tesla;
    case 2: return jondra::RomType::Vili;
    case 3: return jondra::RomType::Plus;
    case 100:
        throw std::runtime_error(
            "Snapshots with a custom ROM are not supported yet");
    default:
        throw std::runtime_error("Snapshot contains an unknown ROM type");
    }
}

std::uint8_t encode_rom(jondra::RomType type) {
    return static_cast<std::uint8_t>(type);
}

Snapshot parse_snapshot(const ByteVector& data) {
    if(data.size() < 4 ||
       !std::equal(signature.begin(), signature.end(), data.begin()))
        throw std::runtime_error("Not a JOndra OSN snapshot");

    Snapshot result;
    std::size_t position = 3;
    result.version = read_u8(data, position);
    if(result.version != 1 && result.version != 2)
        throw std::runtime_error("Unsupported snapshot version");
    const auto expected_size =
        result.version == 1 ? v1_size : v2_size;
    if(data.size() != expected_size)
        throw std::runtime_error("Unexpected snapshot size");

    result.i = read_u8(data, position);
    result.alt_hl = read_u16(data, position);
    result.alt_de = read_u16(data, position);
    result.alt_bc = read_u16(data, position);
    result.alt_af = read_u16(data, position);
    result.hl = read_u16(data, position);
    result.de = read_u16(data, position);
    result.bc = read_u16(data, position);
    result.iy = read_u16(data, position);
    result.ix = read_u16(data, position);

    const auto flags = read_u8(data, position);
    result.iff1 = (flags & 0x01u) != 0;
    result.iff2 = (flags & 0x02u) != 0;
    result.pending_ei = (flags & 0x04u) != 0;
    result.halted = (flags & 0x20u) != 0;
    result.r = read_u8(data, position);
    result.af = read_u16(data, position);
    result.sp = read_u16(data, position);
    result.pc = read_u16(data, position);
    result.interrupt_mode = read_u8(data, position);
    if(result.interrupt_mode > 2)
        throw std::runtime_error("Snapshot contains an invalid interrupt mode");
    result.wz = read_u16(data, position);
    result.port_a0 = read_u8(data, position);
    result.port_a1 = read_u8(data, position);
    result.port_a3 = read_u8(data, position);
    result.rom_type = decode_rom(read_u8(data, position));
    if(result.version == 2)
        result.resolution = read_u8(data, position);

    std::copy_n(data.begin() + static_cast<std::ptrdiff_t>(position),
                result.ram.size(), result.ram.begin());
    return result;
}

} // namespace

namespace jondra {

void save_snapshot_file(const std::filesystem::path& path,
                        const Machine& machine) {
    std::ofstream stream(path, std::ios::binary | std::ios::trunc);
    if(!stream)
        throw std::runtime_error("Cannot create snapshot: " + path.string());

    for(const auto byte : signature)
        write_u8(stream, byte);
    write_u8(stream, current_version);
    write_u8(stream, static_cast<std::uint8_t>(machine.get_i()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_alt_hl()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_alt_de()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_alt_bc()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_alt_af()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_hl()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_de()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_bc()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_iy()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_ix()));

    std::uint8_t flags = 0;
    if(machine.get_iff1()) flags |= 0x01u;
    if(machine.get_iff2()) flags |= 0x02u;
    if(machine.is_int_disabled()) flags |= 0x04u;
    if(machine.is_halted()) flags |= 0x20u;
    write_u8(stream, flags);
    write_u8(stream, static_cast<std::uint8_t>(machine.get_r()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_af()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_sp()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_pc()));
    write_u8(stream, static_cast<std::uint8_t>(machine.get_int_mode()));
    write_u16(stream, static_cast<std::uint16_t>(machine.get_wz()));
    write_u8(stream, machine.port_a0());
    write_u8(stream, machine.port_a1());
    write_u8(stream, machine.port_a3());
    write_u8(stream, encode_rom(machine.rom_type()));
    write_u8(stream, machine.resolution());

    const auto ram = machine.memory().ram();
    stream.write(reinterpret_cast<const char*>(ram.data()),
                 static_cast<std::streamsize>(ram.size()));
    if(!stream)
        throw std::runtime_error("Cannot write snapshot");
}

RomType load_snapshot_file(const std::filesystem::path& path,
                           Machine& machine,
                           const std::filesystem::path& rom_directory) {
    const auto snapshot = parse_snapshot(read_file(path));
    machine.load_rom(snapshot.rom_type, rom_directory);
    machine.memory().restore_ram(snapshot.ram);

    machine.set_i(snapshot.i);
    machine.set_alt_hl(snapshot.alt_hl);
    machine.set_alt_de(snapshot.alt_de);
    machine.set_alt_bc(snapshot.alt_bc);
    machine.set_alt_af(snapshot.alt_af);
    machine.set_hl(snapshot.hl);
    machine.set_de(snapshot.de);
    machine.set_bc(snapshot.bc);
    machine.set_iy(snapshot.iy);
    machine.set_ix(snapshot.ix);
    machine.set_iff1(snapshot.iff1);
    machine.set_iff2(snapshot.iff2);
    machine.set_is_int_disabled(snapshot.pending_ei);
    machine.set_is_halted(snapshot.halted);
    machine.set_r(snapshot.r);
    machine.set_af(snapshot.af);
    machine.set_sp(snapshot.sp);
    machine.set_pc(snapshot.pc);
    machine.set_int_mode(snapshot.interrupt_mode);
    machine.set_wz(snapshot.wz);
    machine.restore_snapshot_peripherals(
        snapshot.port_a0, snapshot.port_a1, snapshot.port_a3,
        snapshot.resolution);
    return snapshot.rom_type;
}

} // namespace jondra
