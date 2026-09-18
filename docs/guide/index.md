# raylib-jlt Guide

User-facing documentation for `raylib-jlt`: a suite of **[raylib](https://github.com/raysan5/raylib)
examples written in [jolt](https://github.com/jolt-lang)** (native Clojure on Chez
Scheme, no JVM) over `jolt.ffi`. Each page below covers one FFI pattern or drawing
convention, with citations to the source files that implement it.

## Why this exists

The examples show you *what* the suite draws; these pages explain *why* the binding
layer is shaped the way it is. Almost every non-obvious decision in
`net.b12n.raylib-jlt.raylib` traces back to one question (how a given C struct
crosses the FFI boundary), and the answer differs per struct. Read these when you
want to bind a C library from Jolt yourself, or when an example does something that
looks needlessly indirect and you want the ABI reason behind it.

## What raylib-jlt is

A community suite of 161 raylib examples: the classic core/shapes/text demos, a
handful of games (asteroids, tetris, pong, vampire-survivors), and a 3D set
(orbiting cameras, waving cubes, an rlgl solar system), each a small Clojure
namespace on top of one shared binding layer, `net.b12n.raylib-jlt.raylib`.

It is the **graphics sibling** of `b12n-tsj` (tree-sitter from Jolt, not yet
public). Both bind a real external C library directly over its C ABI with
`jolt.ffi`, Chez `foreign-procedure` under the hood. They differ on how hard the
library leans on **by-value structs**, and that difference is the whole story of the
FFI pages here:

> **raylib hits the *mild* version of struct-by-value: its hot path is `Color`,
> a 4-byte struct that reduces to a `uint32`, and its only large structs
> (`Camera2D`/`Camera3D`) are by-value *arguments* it can fake behind a pointer on
> AArch64. tree-sitter hits the *severe* version: a 32-byte `TSNode` passed AND
> returned by value, so it needs a full C shim. raylib needs none.**

Four ABI facts drive every distinctive decision in this repo:

1. **`Color` packs into a `:uint`**: a 4-byte all-integer struct travels in one
   general-purpose register, so every draw call passes color as an int, no struct
   marshaling. ([`color-by-value.md`](color-by-value.md))
2. **`Camera2D`/`Camera3D` go by pointer**: a >16-byte composite is passed
   indirectly on AArch64, so a `[:pointer]` binding + a hand-built native struct
   works (with an x86-64 caveat). ([`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md))
3. **`Vector2`/`Vector3` geometry uses rlgl**: small float structs go in FP
   registers, which the pointer trick does *not* cover, so shapes and 3D cubes are
   drawn with rlgl's scalar immediate mode instead.
   ([`rlgl-immediate-mode.md`](rlgl-immediate-mode.md))
4. **Structs by value, in both directions**: jolt 0.7.23's
   `[:by-value [:struct ...]]` passes and returns C structs directly, which is
   what makes `LoadShader` and the sixteen `shaders` examples possible.
   ([`structs-by-value.md`](structs-by-value.md))

   Facts 2 and 3 predate it and describe what the suite still mostly does;
   before 0.7.23 a returned struct could not be expressed at all, and
   `LoadTexture`'s 20-byte `Texture2D` coming back through AArch64's `x8`
   indirect-result register is why textures and framebuffers are still reached
   through rlgl's scalar layer.
   ([`textures-via-rlgl.md`](textures-via-rlgl.md))

Nothing about `jolt.ffi` is raylib-specific: it binds any C ABI symbol. The
`analog-clock` / `digital-clock` examples call plain **libc** `time()`/`localtime()`
(via `rl/local-time`) for real wall-clock time, the repo's one non-raylib FFI, reading
`struct tm`'s `tm_hour`/`tm_min`/`tm_sec` ints straight out of native memory.

## Capability pages

### The FFI core (the reason this repo is interesting)

- ✅ [`structs-by-value.md`](structs-by-value.md): `[:by-value [:struct ...]]` in
  argument and return position, the leading-destination-pointer return
  convention, why the descriptor must be a literal, `ffi/layout` /
  `with-layout` / `read-field` / `write-field`, and why a uniform-type enum is
  read from the header actually being linked. Source: `raylib.clj` (the shader
  bindings, `set-uniform-texture!`).
- ✅ [`color-by-value.md`](color-by-value.md): why raylib's `Color` crosses the
  FFI boundary as a packed `:uint` and not a struct, the little-endian `rgba`
  packing, and the two-by-value-Colors-in-one-call case (`DrawRectangleGradientV`).
  Source: `src/net/b12n/raylib_jlt/raylib.clj` (`rgba`, `clear-background`, the palette).
- ✅ [`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md): how a
  24-byte `Camera2D` / 44-byte `Camera3D` is passed by value on AArch64 by
  allocating the struct in native memory and binding `BeginMode2D`/`BeginMode3D` as
  `[:pointer]`. **The x86-64 non-portability caveat is here.** Source:
  `raylib.clj` (`with-camera-2d`, `with-camera-3d`).
- ✅ [`rlgl-immediate-mode.md`](rlgl-immediate-mode.md): the fallback for by-value
  `Vector2`/`Vector3` args the pointer trick can't fake: rlgl scalar immediate mode
  (`rlBegin`/`rlVertex2f`/`rlVertex3f`/`rlColor4ub`) for the 2D triangle and the 3D
  `cube!`, plus the rlgl matrix stack for nested transforms (the solar-system demo).
  Source: `raylib.clj` (`cube!`, `quad-3f`, the `rl-*` binds).
- ✅ [`textures-via-rlgl.md`](textures-via-rlgl.md): why every raylib `Load*`
  function is unbindable (a >16-byte struct returned through `x8`), and how
  `rlLoadTexture` / `rlLoadFramebuffer` reach the same GPU objects with nothing
  but ints. Includes the HiDPI restore that `EndTextureMode` does across two
  functions and which is easy to get subtly wrong. Source: `raylib.clj`
  (`texture-from-fn`, `texture!`, `render-texture`, `with-render-texture`).

### The drawing API

- ✅ [`kwarg-drawing-api.md`](kwarg-drawing-api.md): the two-layer design: raw
  positional `ffi/defcfn` binds at the boundary (mirroring C), ergonomic
  keyword-argument wrappers (`text!`/`rect!`/`circle!`/…) on top, and the
  ">3 arguments → keyword args" style convention. Source: `raylib.clj`.

### Working from an editor

- ✅ [`repl-driven-development.md`](repl-driven-development.md): why evaluating
  `(-main)` over nREPL kills the whole process on macOS (AppKit only initializes
  on the main thread, an nREPL eval does not run there), the `rl/run!` call that
  fixes it, and what does and does not work against a running window. Source:
  `raylib.clj` (`run!`).

### Verifying without a display

- ✅ [`headless-smoke-testing.md`](headless-smoke-testing.md): how a windowed
  example proves itself with no person at the keyboard: `RAYLIB_APP_AUTO_QUIT_MS`
  (auto-close), `RAYLIB_APP_SHOT` (dump one PNG), and the batched-geometry flush
  that makes the screenshot non-empty. Source: `raylib.clj` (`auto-quit-deadline`,
  `keep-running?`, `maybe-screenshot!`).

### Orientation

- ✅ [`example-catalog.md`](example-catalog.md): a tour of all 161 examples grouped
  games / core / shapes / text / 3d / generative / textures / shaders / audio, what each demonstrates, and the
  five-touchpoint recipe for adding one (source ns + `deps.edn` alias +
  `check.clj` require + `examples_registry.clj` row + `bb.edn` task). Read this
  for the map; the FFI pages for the mechanics.

## See also

- [raylib](https://github.com/raysan5/raylib): the upstream C library, and the
  source of most examples here. Its own `examples/` tree is the reference these
  ports are named after.
- [jolt](https://github.com/jolt-lang): the native Clojure implementation whose
  `jolt.ffi` does all the binding work described on these pages.
- `b12n-tsj` (not yet public): the Jolt sibling that binds a by-value-*returning*
  C API (tree-sitter) and therefore needs a full C shim this repo avoids.
  [`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md) is the
  delta between "fake it with a pointer" (raylib arguments) and "you can't, write a
  shim" (tree-sitter returns).
