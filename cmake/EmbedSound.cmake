function(jondra_generate_embedded_sound output_file)
    get_filename_component(output_directory "${output_file}" DIRECTORY)
    file(MAKE_DIRECTORY "${output_directory}")

    file(WRITE "${output_file}"
        "#include \"jondra/embedded_sound.hpp\"\n\n"
        "#include <cstdint>\n\n"
        "namespace jondra {\nnamespace {\n"
    )

    foreach(tone RANGE 1 7)
        set(sample_path "${CMAKE_CURRENT_SOURCE_DIR}/src/sound/${tone}.sample")
        set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS
            "${sample_path}"
        )
        file(READ "${sample_path}" sample_hex HEX)
        string(REGEX REPLACE "([0-9a-f][0-9a-f])" "0x\\1,"
            sample_bytes "${sample_hex}")
        file(APPEND "${output_file}"
            "constexpr std::uint8_t tone_${tone}[] = {${sample_bytes}};\n"
        )
    endforeach()

    file(APPEND "${output_file}"
        "} // namespace\n\n"
        "std::span<const std::uint8_t>\n"
        "embedded_sound_sample(unsigned tone) noexcept {\n"
        "    switch(tone) {\n"
    )
    foreach(tone RANGE 1 7)
        file(APPEND "${output_file}"
            "    case ${tone}: return {tone_${tone}, sizeof tone_${tone}};\n"
        )
    endforeach()
    file(APPEND "${output_file}"
        "    default: return {};\n"
        "    }\n"
        "}\n\n} // namespace jondra\n"
    )
endfunction()
