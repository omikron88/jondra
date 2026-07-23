# JOndra C++/SDL3 port

This directory contains the in-progress C++20 port of JOndra. The original Java
implementation remains available as a behaviour reference while features are
migrated.

## Implemented in the first milestone

- cycle-accurate Z80 execution using the MIT-licensed
  [kosarev/z80](https://github.com/kosarev/z80) core;
- Ondra ROM/RAM and memory-mapped keyboard switching;
- BASIC, Tesla, ViLi and Ondra Plus ROM layouts;
- DMA video address mapping and dynamic vertical resolution;
- SDL3 window, nearest-neighbour rendering and keyboard input;
- 20 ms emulated frame loop, maskable interrupt, NMI, pause and reset;
- headless core tests.

Audio, tape formats, snapshots, rewind and the graphical debugger still use the
Java implementation and will be migrated in later milestones.

## Build

Requirements are a C++20 compiler, CMake 3.24 or newer, Git and SDL3. By
default CMake downloads pinned revisions of the Z80 core and SDL3 when needed.

```sh
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --parallel
ctest --test-dir build --output-on-failure
./build/jondra
```

Use an installed SDL3 instead:

```sh
cmake -S . -B build -DJONDRA_FETCH_SDL=OFF
```

### Self-contained Windows executable with MSYS2

The static configuration links SDL3 and the MinGW runtime into the executable
and embeds all bundled ROM variants:

```sh
cmake -S . -B build-static -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DJONDRA_FETCH_SDL=OFF \
  -DJONDRA_STATIC=ON
cmake --build build-static --parallel
ctest --test-dir build-static --output-on-failure
```

Run `ldd build-static/jondra.exe` to verify that no SDL3 or MinGW runtime DLLs
remain. Windows system DLLs are expected. `--rom-dir` remains available for
custom or replacement ROM images.

The default ROM is BASIC. Other bundled variants can be selected with
`--rom tesla`, `--rom vili`, or `--rom plus`.

## Keyboard

The original mappings are retained: Shift is Shift, Symbols is Alt, Control is
Control, Numbers is Tab, and the Czech key is `=`. Arrow keys map directly.
Numpad 0/2/8/4/6 map to joystick fire/down/up/left/right.

Emulator controls:

- `F5`: pause/resume
- `F11`: NMI
- `F12`: reset
- `Escape`: quit

## Dependency licensing

The fetched Z80 core is copyright Ivan Kosarev and distributed under the MIT
license. See its upstream `LICENSE` file in the CMake dependency source tree.
The combined JOndra application remains licensed under GPL-2.0.
