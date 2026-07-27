#include "jondra/machine.hpp"

#include <algorithm>

namespace jondra {

Machine::Machine()
    : memory_(keyboard_) {
    reset();
}

void Machine::reset(bool dirty) {
    Base::on_reset();
    keyboard_.reset();
    memory_.reset(dirty);
    port_a0_ = port_a1_ = port_a3_ = 0;
    ticks_ = 0;
    frame_ticks_ = default_frame_ticks;
    resolution_ = 255;
    dma_enabled_ = true;
    dma_timing_on_ = false;
    rebuild_display_map();
    refresh_display();
}

void Machine::load_rom(RomType type, const std::filesystem::path& directory) {
    memory_.load_rom(type, directory);
    rom_type_ = type;
}

void Machine::run_frame() {
    const auto target = ticks_ + frame_ticks_;
    on_handle_active_int();
    while(ticks_ < target)
        on_step();
}

void Machine::nmi() {
    initiate_nmi();
}

void Machine::write_memory(std::uint16_t address, std::uint8_t value,
                           bool physical_ram) {
    if(physical_ram)
        memory_.write_ram(address, value);
    else
        memory_.write(address, value);
    if(address >= 0xd800u)
        update_vram(address);
}

z80::fast_u8 Machine::on_read(z80::fast_u16 address) {
    return memory_.read(static_cast<std::uint16_t>(address));
}

void Machine::on_write(z80::fast_u16 address, z80::fast_u8 value) {
    const auto addr = static_cast<std::uint16_t>(address);
    write_memory(addr, static_cast<std::uint8_t>(value));
}

z80::fast_u8 Machine::on_input(z80::fast_u16 port) {
    if((port_a3_ & 0x30u) == 0)
        update_resolution(static_cast<std::uint8_t>(port));
    return 0xffu;
}

void Machine::on_output(z80::fast_u16 port, z80::fast_u8 value) {
    const auto p = static_cast<std::uint8_t>(port);
    const auto v = static_cast<std::uint8_t>(value);

    if((p & 0x08u) == 0) {
        port_a3_ = v;
        memory_.map_rom((v & 0x02u) == 0);
        memory_.map_io((v & 0x04u) != 0);
        set_dma((v & 0x01u) != 0);
    }
    if((p & 0x01u) == 0)
        port_a0_ = v;
    if((p & 0x02u) == 0)
        port_a1_ = v;
}

void Machine::on_tick(unsigned count) {
    ticks_ += count;
}

void Machine::restore_snapshot_peripherals(std::uint8_t port_a0,
                                           std::uint8_t port_a1,
                                           std::uint8_t port_a3,
                                           std::uint8_t resolution) {
    port_a0_ = port_a0;
    port_a1_ = port_a1;
    port_a3_ = port_a3;
    resolution_ = resolution;
    ticks_ = 0;

    memory_.map_rom((port_a3_ & 0x02u) == 0);
    memory_.map_io((port_a3_ & 0x04u) != 0);
    dma_enabled_ = (port_a3_ & 0x01u) != 0;
    dma_timing_on_ = dma_enabled_;
    const auto correction =
        static_cast<std::uint64_t>(255u - resolution_) * 128u;
    frame_ticks_ =
        dma_timing_on_ ? (57u * 128u + correction) : default_frame_ticks;
    rebuild_display_map();
    refresh_display();
}

void Machine::rebuild_display_map() {
    display_map_.fill(-1);
    framebuffer_.fill(0);
    const auto skew = 255u - resolution_;
    std::int16_t output = 0;

    for(unsigned y = 255u - skew; y != 0; --y) {
        for(unsigned x = 0xff00u; x != 0xd700u; x -= 0x0100u) {
            const auto video_address =
                static_cast<std::uint16_t>((y >> 1u) | ((y & 1u) << 7u) | x);
            display_map_[video_address - 0xd800u] = output++;
        }
    }
}

void Machine::refresh_display() {
    if(!dma_enabled_) {
        framebuffer_.fill(0);
        return;
    }
    for(std::uint32_t address = 0xd800u; address <= 0xffffu; ++address)
        update_vram(static_cast<std::uint16_t>(address));
}

void Machine::update_vram(std::uint16_t address) {
    if(!dma_enabled_ || address < 0xd800u)
        return;
    const auto output = display_map_[address - 0xd800u];
    if(output >= 0)
        framebuffer_[static_cast<std::size_t>(output)] = memory_.read_ram(address);
}

void Machine::set_dma(bool enabled) {
    dma_timing_on_ = enabled;
    const auto correction = static_cast<std::uint64_t>(255u - resolution_) * 128u;
    frame_ticks_ = enabled ? (57u * 128u + correction) : default_frame_ticks;
    if(dma_enabled_ == enabled)
        return;
    dma_enabled_ = enabled;
    refresh_display();
}

void Machine::update_resolution(std::uint8_t port) {
    const auto carry = (port & 0x80u) != 0 ? 1u : 0u;
    resolution_ = static_cast<std::uint8_t>((port << 1u) | carry);
    const auto correction = static_cast<std::uint64_t>(255u - resolution_) * 128u;
    frame_ticks_ = dma_timing_on_ ? (57u * 128u + correction) : default_frame_ticks;
    rebuild_display_map();
    refresh_display();
}

} // namespace jondra
