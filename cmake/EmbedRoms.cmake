function(jondra_generate_embedded_roms output_file)
    get_filename_component(output_directory "${output_file}" DIRECTORY)
    file(MAKE_DIRECTORY "${output_directory}")

    set(rom_entries
        "basic_a|Ondra_BASICEXP_V5_a.rom"
        "basic_b|Ondra_BASICEXP_V5_b.rom"
        "tesla_a|Ondra_TESLA_V5_a.rom"
        "tesla_b|Ondra_TESLA_V5_b.rom"
        "vili_a|Ondra_ViLi_v27_a.rom"
        "vili_b|Ondra_ViLi_v27_b.rom"
        "plus_a|Ondra_PLUS_a.rom"
        "plus_b|Ondra_PLUS_b.rom"
    )

    file(WRITE "${output_file}"
        "#include \"jondra/embedded_roms.hpp\"\n\n"
        "#include <cstdint>\n\n"
        "namespace jondra {\nnamespace {\n"
    )

    foreach(entry IN LISTS rom_entries)
        string(REPLACE "|" ";" fields "${entry}")
        list(GET fields 0 symbol)
        list(GET fields 1 filename)
        file(READ "${CMAKE_CURRENT_SOURCE_DIR}/src/roms/${filename}" rom_hex HEX)
        string(REGEX REPLACE "([0-9a-f][0-9a-f])" "0x\\1," rom_bytes "${rom_hex}")
        file(APPEND "${output_file}"
            "constexpr std::uint8_t ${symbol}[] = {${rom_bytes}};\n"
        )
    endforeach()

    file(APPEND "${output_file}" "} // namespace\n\n"
        "std::span<const std::uint8_t> embedded_rom(std::string_view name) {\n"
    )
    foreach(entry IN LISTS rom_entries)
        string(REPLACE "|" ";" fields "${entry}")
        list(GET fields 0 symbol)
        list(GET fields 1 filename)
        file(APPEND "${output_file}"
            "    if(name == \"${filename}\") return {${symbol}, sizeof ${symbol}};\n"
        )
    endforeach()
    file(APPEND "${output_file}"
        "    return {};\n}\n\n} // namespace jondra\n"
    )
endfunction()
