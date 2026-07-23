cmake_minimum_required(VERSION 3.24)

project(jondra_cpp VERSION 0.1.0 LANGUAGES CXX)

include(FetchContent)
include(CTest)

option(JONDRA_BUILD_APP "Build the SDL3 application" ON)
option(JONDRA_FETCH_SDL "Download SDL3 when it is not installed" ON)
option(JONDRA_STATIC "Statically link third-party runtime libraries" OFF)
option(JONDRA_EMBED_ROMS "Embed the bundled ROM images in the executable" ${JONDRA_STATIC})

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

FetchContent_Declare(
    z80
    GIT_REPOSITORY https://github.com/kosarev/z80.git
    GIT_TAG edfe9fb4b82a64c0b042215fd9df340c1d5bfdc9
    GIT_SHALLOW TRUE
    SOURCE_SUBDIR cmake/not-used
)
FetchContent_MakeAvailable(z80)

set(JONDRA_CORE_SOURCES
    cpp/src/keyboard.cpp
    cpp/src/machine.cpp
    cpp/src/memory.cpp
)

if(JONDRA_EMBED_ROMS)
    include(cmake/EmbedRoms.cmake)
    set(JONDRA_GENERATED_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
    set(JONDRA_EMBEDDED_ROMS_SOURCE "${JONDRA_GENERATED_DIR}/embedded_roms.cpp")
    jondra_generate_embedded_roms("${JONDRA_EMBEDDED_ROMS_SOURCE}")
    list(APPEND JONDRA_CORE_SOURCES "${JONDRA_EMBEDDED_ROMS_SOURCE}")
else()
    list(APPEND JONDRA_CORE_SOURCES cpp/src/embedded_roms_stub.cpp)
endif()

add_library(jondra_core ${JONDRA_CORE_SOURCES})
target_include_directories(jondra_core
    PUBLIC cpp/include
)
target_include_directories(jondra_core SYSTEM PUBLIC "${z80_SOURCE_DIR}")
target_compile_definitions(jondra_core
    PUBLIC JONDRA_DEFAULT_ROM_DIR="${CMAKE_CURRENT_SOURCE_DIR}/src/roms"
)
if(JONDRA_EMBED_ROMS)
    target_compile_definitions(jondra_core PUBLIC JONDRA_HAS_EMBEDDED_ROMS=1)
endif()
if(MSVC)
    target_compile_options(jondra_core PRIVATE /W4 /permissive-)
else()
    target_compile_options(jondra_core PRIVATE -Wall -Wextra -Wpedantic)
endif()

if(JONDRA_BUILD_APP)
    if(JONDRA_STATIC)
        find_package(SDL3 CONFIG QUIET COMPONENTS SDL3-static)
    else()
        find_package(SDL3 CONFIG QUIET)
    endif()
    if(NOT SDL3_FOUND AND JONDRA_FETCH_SDL)
        if(JONDRA_STATIC)
            set(SDL_SHARED OFF CACHE BOOL "" FORCE)
            set(SDL_STATIC ON CACHE BOOL "" FORCE)
        endif()
        FetchContent_Declare(
            SDL3
            GIT_REPOSITORY https://github.com/libsdl-org/SDL.git
            GIT_TAG release-3.4.12
            GIT_SHALLOW TRUE
        )
        set(SDL_TEST_LIBRARY OFF CACHE BOOL "" FORCE)
        set(SDL_TESTS OFF CACHE BOOL "" FORCE)
        FetchContent_MakeAvailable(SDL3)
    elseif(NOT SDL3_FOUND)
        message(FATAL_ERROR "SDL3 was not found; install it or configure with -DJONDRA_FETCH_SDL=ON")
    endif()

    add_executable(jondra cpp/src/main.cpp)
    if(JONDRA_STATIC)
        if(NOT TARGET SDL3::SDL3-static)
            message(FATAL_ERROR "The selected SDL3 package does not provide SDL3::SDL3-static")
        endif()
        target_link_libraries(jondra PRIVATE jondra_core SDL3::SDL3-static)
        if(MINGW)
            target_link_options(jondra PRIVATE -static -static-libgcc -static-libstdc++)
        endif()
    else()
        target_link_libraries(jondra PRIVATE jondra_core SDL3::SDL3)
    endif()
endif()

if(BUILD_TESTING)
    add_executable(jondra_core_tests cpp/tests/core_tests.cpp)
    target_link_libraries(jondra_core_tests PRIVATE jondra_core)
    add_test(NAME jondra_core_tests COMMAND jondra_core_tests)
endif()
