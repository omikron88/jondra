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
- Dear ImGui menu bar, toolbar, status bar, settings and interactive debugger;
- native file dialogs for loading and saving raw or header-based memory blocks;
- Java-compatible OSN v1/v2 snapshot loading and OSN v2 snapshot saving;
- cycle-timed tape playback from WAV, CSW and Ondra TAP files, plus CSW
  recording;
- SDL3 audio output, the seven built-in Ondra tones and the optional
  SN76489-based Melodik module;
- active-low green and yellow front-panel LED indicators from port A0;
- 20 ms emulated frame loop, maskable interrupt, NMI, pause and reset;
- headless core tests.

Timeline rewind and memory-access watchpoints still use the Java implementation
and will be migrated in later milestones.

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

## Tape

Use **File > Open tape for load** for WAV, CSW or Ondra TAP images. Use
**Open tape for recording** to create a CSW recording. Playback and recording
start and stop automatically when the emulated software switches the cassette
motor through port A0. The status bar shows `ready`, `play`, `record` or `end`.
The File menu also provides rewind and eject actions.

## Audio

The built-in speaker reproduces the seven original fixed tones selected by
bits 5-7 of port A0. The optional Melodik module emulates its SN76489 tone and
noise generator at 2 MHz. Both sources are mixed into a 44.1 kHz mono SDL3
audio stream. They can be enabled independently in **Tools > Settings**.

## Settings

The selected ROM, fullscreen and scanline options, both audio switches, the
window size and the most recently used file-dialog paths are restored on the
next start. They are written to `jondra.ini` below the platform-specific user
directory returned by SDL. No configuration file is created beside the
executable.

Emulator controls:

- `F5`: pause/resume
- `F7`: debugger Step Into (while paused)
- `F8`: debugger Step Over (while paused)
- `F11`: NMI
- `F12`: reset
- `Escape`: quit

## Debugger

Open **Tools > Debugger** and pause emulation to edit all main and alternate Z80
registers, interrupt flip-flops and interrupt mode. The debugger disassembles
memory from PC (or a manually selected address), highlights the next
instruction, and supports persistent execution breakpoints. Breakpoints stop
before the marked instruction. **Step Over** runs through `CALL` and `RST`
instructions to their return address; all other instructions execute as one
step. The named F-register flags use the original debugger's `M/P`, `Z/NZ`,
`AC/NA`, `PE/PO`, `N1/N0` and `C/NC` notation; click a flag to toggle it.

## Dependency licensing

The fetched Z80 core is copyright Ivan Kosarev and distributed under the MIT
license. Dear ImGui is copyright Omar Cornut and contributors and distributed
under the MIT license. See the upstream `LICENSE.txt` files in the CMake
dependency source tree. The combined JOndra application remains licensed under
GPL-2.0.
