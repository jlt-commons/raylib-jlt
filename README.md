# raylib-jlt: raylib examples in Jolt

[![CI](https://github.com/jlt-commons/raylib-jlt/actions/workflows/ci.yml/badge.svg)](https://github.com/jlt-commons/raylib-jlt/actions/workflows/ci.yml)
[![Site](https://github.com/jlt-commons/raylib-jlt/actions/workflows/site.yml/badge.svg)](https://github.com/jlt-commons/raylib-jlt/actions/workflows/site.yml)

A community suite of [raylib](https://github.com/raysan5/raylib) examples written in
**jolt** (native Clojure, no JVM). These call a **real external C library**: raylib is
the upstream C game library (raysan5/raylib), and jolt binds it directly over its C ABI
with `jolt.ffi`: no wrapper library, no codegen, just the shared `libraylib` loaded at
runtime and called through the FFI.

All the FFI bindings and an ergonomic keyword-argument drawing API live in
`net.b12n.raylib`, a library of 19 focused modules aggregated as
`net.b12n.raylib.all`; each example is a small namespace on top of it.

**Documentation site: <https://jlt-commons.github.io/raylib-jlt/>** carries the guide, the full
example catalog, and every demo GIF at full size.

## Examples

<table>
<tr><th>Preview</th><th>Example</th><th>What it demonstrates</th></tr>
<tr>
<td><img src="docs/demos/tetris.gif" width="240"></td>
<td><code>bb tetris</code></td>
<td>the block-stacking puzzle (move/rotate/drop)</td>
</tr>
<tr>
<td><img src="docs/demos/game-of-life.gif" width="240"></td>
<td><code>bb game-of-life</code></td>
<td>Conway's Game of Life (SPACE reseeds)</td>
</tr>
<tr>
<td><img src="docs/demos/waving-cubes.gif" width="240"></td>
<td><code>bb waving-cubes</code></td>
<td>an NxN grid of cubes rippling in 3D</td>
</tr>
<tr>
<td><img src="docs/demos/kaleidoscope.gif" width="240"></td>
<td><code>bb kaleidoscope</code></td>
<td>strokes mirrored with 6-fold symmetry</td>
</tr>
</table>

**[Browse the full gallery →](docs/demos/README.md)**: every example, recorded via `bb record`.
`bb record --only <example-name>` matches an exact id or an id prefix
(e.g. `--only camera-3d` also selects `camera-3d-first-person`); combine a
short prefix with `--force` carefully.

Every GIF in that gallery is committed, so you never need to record anything to
browse them. Regenerating them is a maintainer task: `bb record` drives the suite
through an internal capture tool (`screen-grab`) that is not publicly released, and
the task tells you so if the tool is absent.

## Requirements

### jolt

```sh
jolt --version               # requires jolt 0.8.0 or newer
```

`deps.edn` declares `:jolt/min-version "0.8.0"`, and jolt v0.8.0 was released on
2026-09-01, so a current jolt satisfies it and nothing extra is needed. The reason
for the floor is `jolt.ffi/write`: as of 0.8.0 it takes the value *before* the
offset, `(write p type value offset)`, which is `babashka.ffi`'s order, where it
used to be `(write p type offset value)`. Both spellings are integers, so nothing
raises and nothing warns. An older jolt running this code writes every camera and
uniform field to the wrong address, then draws something subtly wrong instead of
failing.

**The floor is a forward guard, not protection against that particular break.**
jolt honours `:jolt/min-version` only from the release that introduced the key, and
that commit is the direct child of the one that moved `ffi/write`. The two sets
don't overlap: any jolt new enough to read the key already has the new argument
order, and any jolt with the old order is too old to know the key and ignores it,
exactly as it ignores every key it doesn't recognise. So on jolt v0.7.29 or below
nothing refuses anything, the suite still compiles, and the writes land in the wrong
place. Upgrading is the only thing that fixes that; the declaration earns its place
against the *next* break.

One case still needs the escape hatch. A jolt built from `main` between the
`ffi/write` change and the v0.8.0 tag reports a version like
`v0.7.29-25-gd4e92a43`, whose numeric prefix reads as `0.7.29` and therefore sorts
below the floor even though it carries the new behaviour. A build tagged v0.8.0 or
later parses as `[0 8 0]` and passes normally. For the in-between case:

```sh
JOLT_SKIP_MIN_VERSION=1 bb check
```

Reach for it only when you know your build is past the `ffi/write` change. On jolt
v0.7.29 or below it is a no-op, because no such release reads the key at all, so it
is never a substitute for checking what you are actually running. Installing v0.8.0
is the better answer in both cases.

An older floor still appears in the guide pages, and it is still accurate as history:
**jolt 0.7.23** is what added `[:by-value [:struct ...]]`, which `DrawCircleGradient`
is bound with, and below that the binding fails at compile with a type error. So
0.7.23 is where these bindings became expressible, while 0.8.0 is what the suite
needs today.

One thing to know if you edit the library's modules
(`lib/src/net/b12n/raylib/`): since **jolt 0.4.0** unresolved symbols are a
compile error rather than being resolved late, so within each module a
definition must appear before its first use. Every example requires
`net.b12n.raylib.all`, which aggregates all 19 modules, so a single
misordered symbol in any one of them stops the whole suite from loading:

```
$ bb check:lib
error[analyze/unresolved-symbol]: Unable to resolve symbol: rgba in this context
  --> ./src/net/b12n/raylib/color.clj:11:16
```

This is why `color.clj`'s `rgba` (and the named palette built from it) is
defined before anything that calls it, and why `models.clj` requires `color`
for `shade-color` / `cube!` / `sphere!` rather than redefining it. Compilation
stops at the first unresolved symbol, so fix them one at a time. `bb
check:lib` compiles the library headlessly and `jolt -M:check` (or `bb
check`) does the same for every example namespace; both are the quickest
confirmation that the suite still loads.

The launcher is `jolt`. It was called `joltc` before jolt 0.5.0, and current
releases install only `jolt`, so if a `joltc` shim is still on your PATH from an older
install, every `jolt -M:<alias>` below works under that name too. The `bb` tasks
resolve whichever one you have.

### libraylib

The system `libraylib` shared library must be installed, **version 6.0 or
newer**. `bb lib:check` verifies this and refuses an older one: 6.0 changed
`DrawCircleGradient` to take its centre as a by-value `Vector2` where 5.5 took
two ints, and because the symbol name did not change, a 5.5 library links
without complaint and draws in the wrong place. With babashka you can
check and install it for your platform (Linux, macOS Intel, macOS Apple Silicon):

```sh
bb lib:check                 # is libraylib installed for this OS/arch? (read-only)
bb lib:install               # install it via brew / pacman / apt / dnf / zypper / apk
bb lib:install --dry-run     # …or just print the command it would run
```

Or install it yourself:

```sh
brew install raylib          # macOS (Homebrew picks the right prefix per CPU)
sudo pacman -S raylib        # Arch Linux
# or your distro's raylib package (libraylib-dev, raylib-devel, …)
```

`deps.edn` points jolt at it via a `:jolt/native` entry (Homebrew's
`/opt/homebrew/lib/libraylib.dylib` first, then the bare name on the loader path;
`libraylib.so.6` / `libraylib.so` on Linux).

**Using a source build of raylib** (e.g. from `~/dev/raylib`): build the shared
library (`cmake --build build`), then point the dynamic loader at it instead of
installing system-wide:

```sh
export LD_LIBRARY_PATH=$HOME/dev/raylib/build/raylib    # Linux
export DYLD_LIBRARY_PATH=$HOME/dev/raylib/build/raylib  # macOS
bb lib:check                                            # confirms it's now found
```

## Running

### With Babashka (friendliest)

With [babashka](https://babashka.org) installed, `bb.edn` gives every example a task:

```sh
bb info                 # grouped cheat-sheet of every example
bb examples             # flat list with descriptions
bb bouncing-ball        # run one example (opens a window)
bb run following-eyes   # …or run one by argument
bb run-all [secs]       # demo reel / smoke test: every example, N seconds each (default 15)
bb check                # headless compile-check of every example (no window)
bb test                 # unit suite: ffi/write argument order
bb lib:check            # is the native libraylib installed for this OS/arch?
bb lib:install          # install libraylib via the platform package manager
bb tasks                # raw babashka task list
```

A few more exist for the docs. `bb record` needs an external capture tool and says
so if it is absent. The `site:*` tasks need a checkout of
[jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine) and print the
clone command if they cannot find one; CI needs neither, because it checks the
engine out itself.

```sh
bb record       # re-record the demo GIFs (needs an internal capture tool)
bb site:serve   # build the docs site and preview it at localhost:3000
bb site:build   # build it into _site/ without serving
```

The `bb` names are friendly aliases; each maps to a `jolt` alias below.

#### Quality: lint / format / checks

```sh
bb lint                          # clj-kondo over src (report only)
bb lint:strict                   # bb lint, but exit non-zero if any findings
bb lsp:format                    # reformat all Clojure files (clojure-lsp)
bb lsp:format-check              # check formatting (dry run)
bb lsp:clean-ns                  # clean + organize ns forms (clojure-lsp)
bb lsp:clean-ns-check            # check ns forms (dry run)
bb lsp:diagnostics               # clojure-lsp diagnostics
bb lsp:check                     # all LSP checks (format + clean-ns + diagnostics, dry run)
bb lsp:fix                       # auto-fix: format + clean-ns (mutating)
bb check:positional-args         # find fns with 3+ positional args (report only)
bb check:positional-args:strict  # bb check:positional-args, but exit non-zero if any found
bb check:kwarg-calls             # find flat :k v calls to a kwargs fn (report only)
bb check:kwarg-calls:strict      # bb check:kwarg-calls, but exit non-zero if any found
bb fix:kwarg-calls               # rewrite flat :k v call sites into an explicit {} map
bb check:demos                   # galleries, ledger and stated counts agree with the registry
```

`bb check`, `bb test` and `bb lint`/`bb lsp:format-check` are the gates worth
running before a commit: `check` compiles every example namespace, `test` round-trips
`ffi/write` to prove the value lands where the offset says, `lint` runs clj-kondo
(static analysis), `lsp:format-check` runs clojure-lsp (formatting). `check` alone
cannot catch an `ffi/write` argument flip, because both arguments are integers and
the swapped call compiles cleanly, which is what `test` is there for. **Formatting is owned
by clojure-lsp, not cljfmt**; the two disagree on some compact literal tables
(e.g. `digital_clock.clj`'s seven-segment map), so only one formatter runs against
`src`; clojure-lsp was chosen so `lsp:clean-ns` (no cljfmt equivalent) and
`lsp:format` share one tool. `bb lsp:fix` applies both format and clean-ns fixes
in place.

`lint` needs [clj-kondo](https://github.com/clj-kondo/clj-kondo); it uses the
native binary when installed and falls back to the
[clojure CLI](https://clojure.org/guides/install_clojure) route otherwise. The
`lsp:*` tasks need [clojure-lsp](https://clojure-lsp.io) on `PATH`. Nothing in the
suite needs a JVM at *runtime*; these are dev-time tools only.

clj-kondo can't see through `jolt.ffi/defcfn`, which defines one var per bound C
symbol; untaught, it reports ~500 false positives and is useless as a gate. The
hook in [`.clj-kondo/hooks/jolt_ffi.clj`](.clj-kondo/hooks/jolt_ffi.clj) rewrites
each `defcfn` into an equivalent `defn`, so the bindings resolve *and* call sites
get arity- and return-type-checked, catching `(rl/init-window 1 2)` at lint time
instead of as a native crash.

#### Git hooks

```sh
bb hooks:install       # install FAST pre-commit hook (lint + format + clean-ns, ~2s)
bb hooks:install:full  # install FULL pre-commit hook (+ bb check, slower)
bb hooks:uninstall     # remove the git pre-commit hook
```

The pre-commit hook is local-only (`.git/hooks/pre-commit` is never committed);
each clone that wants it runs `bb hooks:install` once. Skip it for a single commit
with `git commit --no-verify`.

#### Dev

```sh
bb nrepl [port]  # start a jolt nREPL server for interactive dev (default 7888)
```

Start an example from a connected editor with `rl/run!`, never a bare `(-main)`:

```clojure
(comment
  (rl/run! -main))
```

raylib opens its window through GLFW, and macOS only lets AppKit initialize on the
process main thread. An nREPL eval runs on a worker thread, so calling `-main`
directly traps the whole jolt process, taking the editor connection with it.
`rl/run!` hops onto the main thread, and runs inline (changing nothing) when there
is no REPL. See
[`docs/guide/repl-driven-development.md`](docs/guide/repl-driven-development.md).

### With jolt directly

Each example is also a `jolt` alias:

```sh
jolt -M:run        # basic window (default)      jolt -M:mouse     # circle follows cursor
jolt -M:input      # move a circle, arrow keys   jolt -M:wheel     # move a box, mouse wheel
jolt -M:bounce     # bouncing ball, SPACE pauses  jolt -M:shapes    # shape primitives + triangle
jolt -M:colors     # named-color palette grid     jolt -M:gradient  # vertical color gradient
jolt -M:text       # font sizes + MeasureText     jolt -M:logo      # the raylib logo
jolt -M:stars      # twinkling starfield          jolt -M:eyes      # eyes follow the mouse
jolt -M:camera2d   # 2D camera (see note below)
```

Headless compile-check of the whole suite (no window needed):

```sh
jolt -M:check      # requires every example namespace; prints "compiled OK"
```

### Environment variables (used by every example)

- `RAYLIB_APP_AUTO_QUIT_MS=<n>`: close the window after `n` milliseconds, so an
  example is smoke-testable with no person at the keyboard.
- `RAYLIB_APP_SHOT=<name>`: dump one frame as a PNG (headless visual proof). raylib
  writes the file's **basename into the current working directory** (it ignores
  directory components), and the batched text is flushed first so it appears.

## The examples

| Alias | raylib source | Shows |
|---|---|---|
| `run` | core/core_basic_window | window, clear, text |
| `input` | core/core_input_keys | `IsKeyDown`, move a circle |
| `bounce` | shapes/shapes_bouncing_ball | animation, `IsKeyPressed` pause, `DrawFPS` |
| `colors` | (showcase) | the whole named-color palette as swatches |
| `mouse` | core/core_input_mouse | `GetMouseX/Y`, `IsMouseButtonDown` |
| `wheel` | core/core_input_mouse_wheel | `GetMouseWheelMove` (a float return) |
| `shapes` | shapes/shapes_basic_shapes | rect/circle/ellipse/line + an rlgl triangle |
| `gradient` | shapes | `DrawRectangleGradientV` (two by-value Colors) |
| `text` | text | font sizes/colors + `MeasureText` centering |
| `logo` | (raylib logo) | rectangles + text, positioned with `MeasureText` |
| `stars` | (showcase) | `GetRandomValue` + bulk draw, per-star twinkle |
| `eyes` | shapes/shapes_following_eyes | pupils track the mouse (scalar trig) |
| `camera2d` | core/core_2d_camera | 2D camera, struct-by-value (see note) |
| `delta-time` | core/core_delta_time | per-frame vs `GetFrameTime` movement |
| `scissor-test` | core/core_scissor_test | `BeginScissorMode` clips a grid |
| `mouse-trail` | shapes/shapes_mouse_trail | fading cursor trail (alpha) |
| `recursive-tree` | shapes | a binary fractal tree (lines + trig) |
| `math-sine-cosine` | shapes/shapes_math_sine_cosine | unit-circle sine/cosine viz |
| `bullet-hell` | shapes/shapes_bullet_hell | a rotating bullet spiral |
| `triangle-strip` | shapes/shapes_triangle_strip | rainbow strip via rlgl immediate mode |
| `writing-anim` | text/text_writing_anim | a self-typing message |
| `format-text` | text/text_format_text | `format` padded score + MM:SS timer |
| `basic-screen-manager` | core/core_basic_screen_manager | LOGO/TITLE/GAMEPLAY/ENDING state flow |
| `random-values` | core/core_random_values | a new random value every 2s |
| `collision-area` | shapes/shapes_collision_area | AABB collision (computed in Clojure) |
| `dashed-line` | shapes/shapes_dashed_line | a dashed line follows the mouse |
| `double-pendulum` | shapes | a chaotic double pendulum + trail |
| `kaleidoscope` | shapes | strokes mirrored with 6-fold symmetry |
| `hilbert-curve` | shapes | a rainbow Hilbert space-filling curve |
| `math-angle-rotation` | shapes/shapes_math_angle_rotation | fixed spokes + a spinning line |
| `words-alignment` | text/text_words_alignment | align a word with `MeasureText` |
| `camera-3d` | core/core_3d_camera | orbiting 3D camera, `Camera3D` by value + rlgl cube |
| `waving-cubes` | models/models_waving_cubes | 196 cubes rippling in 3D (shared `rl/cube!`) |
| `camera-3d-first-person` | core/core_3d_camera_first_person | WASD + mouse-look walk through columns |
| `tesseract-view` | (4D) | a rotating 4D hypercube projected 4D→3D→2D |
| `wireframe-shapes` | models | pyramid/octahedron/torus/helix as rlgl 3D lines |
| `rlgl-solar-system` | models/models_rlgl_solar_system | Sun/Earth/Moon via the rlgl matrix stack |
| `box-collisions` | models/models_box_collisions | player cube vs. boxes, 3D AABB highlight |
| `rotating-cube` | models | one cube spinning via the rlgl matrix stack |
| `spinning-cubes` | models | a row of cubes each spinning with a phase offset |
| `orthographic-projection` | core/core_3d_camera | perspective vs orthographic (`:projection` toggle) |
| `point-cloud` | (showcase) | ~1500 points as tiny rlgl cubes, rotating |
| `bouncing-spheres` | models | spheres bouncing in a 3D box (`rl/sphere!`) |
| `ball-physics` | shapes | 2D balls under gravity + restitution |
| `lines-bezier` | shapes/shapes_lines_bezier | a cubic Bézier sampled in Clojure, follows the mouse |
| `input-box` | text/text_input_box | a text field via `GetCharPressed` (blinking cursor) |
| `asteroids` | (game) | the classic vector shooter: rotate/thrust/fire, splitting asteroids |
| `tetris` | (game) | 10×20 well, 7 tetrominoes, rotation, line-clearing, levels |
| `pong` | (game) | two-paddle classic, you (W/S) vs a ball-tracking CPU |
| `vampire-survivors` | (game) | auto-firing survivors-like: chasing waves, XP gems, leveling |
| `snake` | (game) | the classic snake: arrow keys, grow, don't crash |
| `breakout` | (game) | paddle (mouse) + ball + brick grid, clear to win |
| `space-invaders` | (game) | marching alien grid, shoot up (arrows + SPACE) |
| `flappy-bird` | (game) | flap through scrolling pipe gaps (SPACE) |
| `game-2048` | (game) | **2048**: 4×4 tile-merge puzzle (arrow keys) |
| `minesweeper` | (game) | reveal/flag grid, flood-fill (mouse L/R), new `mouse-pressed?` |
| `game-of-life` | (generative) | Conway's Game of Life on a toroidal grid (SPACE reseeds) |
| `boids` | (generative) | Reynolds flocking: separation / alignment / cohesion |
| `fireworks` | (generative) | rockets + gravity-fading particle bursts (alpha channel) |
| `fourier-epicycles` | (generative) | a chain of rotating circles traces a square wave |
| `spirograph` | (generative) | animated hypotrochoid roulette curves |
| `l-system` | (generative) | an L-system fractal plant (rewrite + turtle graphics) |
| `flow-field` | (generative) | particles steered by a sine-layered flow field |
| `color-wheel` | shapes/shapes_rlgl_color_wheel | an HSV hue ring as an rlgl triangle fan (per-vertex color) |
| `pie-chart` | shapes/shapes_pie_chart | labelled slices via `rl/sector!` + a legend |
| `splines` | shapes/shapes_splines_drawing | Catmull-Rom / Bézier / B-spline through animated points (SPACE cycles) |
| `vector-angle` | shapes/shapes_vector_angle | the signed angle between two vectors: arc + degrees (`atan2`) |
| `easings` | shapes/shapes_easings_* | a 3×4 grid of balls, each on a different easing curve |
| `penrose-tiling` | shapes/shapes_penrose_tile | a P3 Penrose rhombus tiling by golden-ratio deflation |
| `analog-clock` | shapes/shapes_clock_of_clocks | a live analog clock: `ring!` bezel, `line-ex!` ticks/hands, **libc** local time |
| `digital-clock` | shapes/shapes_digital_clock | a seven-segment `HH:MM:SS` display (libc local time) |
| `ring-drawing` | shapes/shapes_ring_drawing | an animated annulus via `rl/ring!` + a stroked outline |
| `rounded-rectangle` | shapes/shapes_rounded_rectangle_drawing | rounded rects from `sector!` corners + rects |
| `rectangle-scaling` | shapes/shapes_rectangle_scaling | drag the corner handle to resize a rectangle |
| `lines-drawing` | shapes/shapes_lines_drawing | a rotating fan of thick lines via `rl/line-ex!` |
| `helitorus` | (showcase) | a helix wound around a torus, swept into a tube: hand-rolled projection, painter's-algorithm depth, ~10k vertices/frame from primitive arrays |
| `doom` | (showcase) | a textured raycaster: one ray per screen column, procedural atlas via `rl/texture-from-fn`, sprites depth-tested against a per-column z-buffer |
| `pacman` | (game) | pac-man with the classic ghost personalities (Blinky/Pinky/Inky/Clyde), buffered turns, `rl/sector!` for the mouth |

### Verification status

Every example has been run in a real window (clean launch → render → exit; most
inspected via `RAYLIB_APP_SHOT`). That includes `camera2d`, which confirmed the
**`Camera2D` struct-by-value spike works**: passing a 24-byte struct by value to a
C function via a pointer renders correctly on AArch64 (Apple silicon). See the
Camera2D note below for the x86-64 caveat.

## How the FFI works

### `Color` passed by value (every draw call)

raylib's `Color` is a 4-byte struct `{u8 r,g,b,a}` passed **by value**. On the
AArch64 and x86-64 ABIs a 4-byte all-integer struct travels in a single
general-purpose register, exactly like a `uint32`, so `net.b12n.raylib.color/rgba` packs RGBA
little-endian into an int and each `Color` parameter is bound as `:uint`:

```clojure
(defn rgba [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))
(ffi/defcfn clear-background "ClearBackground" [:uint] :void)
```

### Keyword-argument drawing API

raylib's C functions are positional; `net.b12n.raylib.kwargs` wraps the multi-argument draw
calls so examples read self-descriptively:

```clojure
(rl/text! "hi" :x 10 :y 20 :size 20 :color rl/RED)     ; DrawText
(rl/rect! :x 60 :y 80 :width 120 :height 90 :color rl/BLUE)
(rl/circle! :x 400 :y 225 :radius 50 :color rl/MAROON)
```

### rlgl immediate mode (for shapes with by-value `Vector2` args)

A few raylib calls (e.g. `DrawTriangle`) take `Vector2` by value, which does **not**
reduce to the `Color` trick (a 2-float struct goes in floating-point registers). The
`shapes` example draws its triangle with rlgl's scalar immediate mode instead:
`rlBegin` / `rlColor4ub` / `rlVertex2f` / `rlEnd`.

### `Camera2D` struct by value (the `camera2d` example)

raylib's `BeginMode2D(Camera2D)` takes a 24-byte struct by value. On the AArch64
(Apple) ABI a composite larger than 16 bytes is passed **indirectly**: the caller
allocates a copy and passes a pointer, so `net.b12n.raylib.camera/with-camera-2d` builds the
struct in native memory (`ffi/alloc` + six `ffi/write :float`s) and binds
`BeginMode2D` as `[:pointer]`.

**This is AArch64-specific.** On the x86-64 SysV ABI those 24 bytes are passed on
the stack, which a `[:pointer]` binding does not do, so this example is not portable
as written. The portable alternative is to apply the same transform with rlgl's
scalar matrix ops (`rlPushMatrix` / `rlTranslatef` / `rlRotatef` / `rlScalef`,
flushing the batch before `rlPopMatrix`), which is exactly what `BeginMode2D` does
internally. If `camera2d` crashes with an "invalid memory reference", the struct
passing is the cause; switch to the rlgl-matrix approach.

### 3D: `Camera3D` by value + rlgl geometry (the `camera-3d` example)

The same pointer approach scales to 3D: `BeginMode3D(Camera3D)` takes a 44-byte
struct (three `Vector3` + `fovy` + `projection`), which is `>16` bytes so it goes
by pointer too; `net.b12n.raylib.camera/with-camera-3d` builds it (`ffi/alloc` 44 +
`ffi/write` ten floats + an `:int`). But 3D shape helpers (`DrawCube`,
`DrawSphere`, `DrawLine3D`) take a `Vector3` **by value**, a 12-byte float struct
passed in FP registers, which the pointer trick does **not** cover. So `camera-3d`
draws its cube with rlgl immediate mode (`rlVertex3f`, scalar) inside `BeginMode3D`;
`DrawGrid` is scalar and used directly. That combination (`Camera3D` by pointer +
rlgl for geometry) is the path for any 3D example here until jolt gains native
by-value struct support for `Vector3`-taking functions.

## Documentation

Everything below is also published at **<https://jlt-commons.github.io/raylib-jlt/>**, which is
the nicer way to read it; the guide pages are cross-linked and the demo gallery renders
inline.

- [`docs/guide/`](docs/guide/index.md): patterns & pitfalls, each with source
  citations:
  - [`structs-by-value.md`](docs/guide/structs-by-value.md): `[:by-value [:struct ...]]` in both directions, and where the jolt floor comes from
  - [`color-by-value.md`](docs/guide/color-by-value.md): why `Color` crosses the FFI as a packed `:uint`
  - [`struct-by-value-pointer-trick.md`](docs/guide/struct-by-value-pointer-trick.md): `Camera2D`/`Camera3D` by pointer on AArch64 (+ the x86-64 caveat)
  - [`rlgl-immediate-mode.md`](docs/guide/rlgl-immediate-mode.md): the fallback for by-value `Vector2`/`Vector3` geometry, + the matrix stack
  - [`textures-via-rlgl.md`](docs/guide/textures-via-rlgl.md): why `LoadTexture` has no binding, and reaching textures and framebuffers from underneath it
  - [`kwarg-drawing-api.md`](docs/guide/kwarg-drawing-api.md): the positional-binds / keyword-wrappers two-layer design
  - [`headless-smoke-testing.md`](docs/guide/headless-smoke-testing.md): `RAYLIB_APP_AUTO_QUIT_MS` + `RAYLIB_APP_SHOT` proof without a person
  - [`example-catalog.md`](docs/guide/example-catalog.md): a tour of all 175, and the five-touchpoint recipe for adding one

- Two galleries show the same 171 recordings for different purposes.
  [`docs/demos/README.md`](docs/demos/README.md) is the flat gallery generated by
  `bb record` from `scripts/demo_manifest.edn` (don't hand-edit it), while
  [`docs/guide/demos.md`](docs/guide/demos.md) is the full-size companion to the
  example catalog's thumbnails.

  Every example now has one, so the count matches the suite and there is no gap
  left to explain. Nineteen of the GIFs are a single frame, which is the honest
  result for an example that draws something fixed and only moves once you drive
  it. Two of those will not improve whatever happens to the capture tool:
  `drop-files` waits on an operating-system drop event, and `highdpi-demo` fights
  the tool because FLAG_WINDOW_HIGHDPI and the capture path double the scale
  independently. `directory-files` records, but it shows the tree at rest, since
  nothing synthetic navigates it.

- Publishing is automatic. `.github/workflows/site.yml` builds the site on every
  pull request and deploys it from `main`, so a merged docs change is live without
  anyone running anything. The generator is
  [jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine), pinned to
  a tag; this repo owns its content, its `docs/site.edn`, and its homepage template
  in `docs/templates/`.

- To preview locally, clone the engine next to this repo and run `bb site:serve`.
  It serves at the root while the published site lives under `/raylib-jlt/`, so
  absolute links will 404 locally and work once deployed. The workflow checks for
  exactly that, and CI is the authority on it.

The suite has a sibling project, `b12n-tsj`, which binds tree-sitter from Jolt with
the same `jolt.ffi` mechanism. It is not public yet, but the contrast shapes the FFI
guides here: tree-sitter passes *and returns* a 32-byte struct by value, so it needs
a full C shim: raylib's by-value structs stay within what the pointer trick and rlgl
can cover, so this repo needs no shim at all.

## Layout

```
raylib-jlt/
├── bb.edn                   ; babashka tasks + the example registry (bb info / run-all)
├── deps.edn                 ; net.b12n/raylib dep (:local/root "lib") + one alias per example
├── docs/guide/              ; the pattern guides listed under Documentation above
├── lib/                     ; the net.b12n.raylib library: 19 FFI-binding modules
│   └── src/net/b12n/raylib/
│       ├── all.clj          ; GENERATED aggregator (bb gen:all); never hand-edited
│       ├── check.clj        ; headless compile-check of the library itself
│       ├── color.clj  core.clj  kwargs.clj  native.clj  …
│       └── …
├── scripts/                 ; check_positional_args.clj (bb check:positional-args)
│                             ; kwarg_calls_to_maps.clj (bb check|fix:kwarg-calls)
│                             ; check_demos.clj (bb check:demos)
└── src/net/b12n/raylib_jlt/
    ├── check.clj            ; headless compile-check of every example
    ├── core.clj             ; basic window (the default, jolt -M:run)
    ├── input.clj  bounce.clj  colors.clj   mouse.clj  wheel.clj
    ├── shapes.clj gradient.clj text.clj    logo.clj   stars.clj
    ├── eyes.clj   camera2d.clj
    └── …
```

Adding an example touches four places: write `src/net/b12n/raylib_jlt/<name>.clj` against
the `net.b12n.raylib.all` API, add a `:<name>` alias to `deps.edn`, add the
namespace to `net.b12n.raylib-jlt.check` so the headless compile-check covers it, and add
a registry row to `bb.edn` so it appears in `bb info` / `bb examples` / `bb run-all`.
The [example catalog](docs/guide/example-catalog.md) walks through all four under
"Adding an example: the four touchpoints".

## Contributing

New examples are welcome; the suite is deliberately mechanical to grow, and the
four touchpoints above are the whole recipe. See [`CONTRIBUTING.md`](CONTRIBUTING.md)
for setup, the four pre-PR gates (`bb check`, `bb test`, `bb lint:strict`,
`bb lsp:format-check`), and what to know before touching the shared binding layer.

## License

Released under the [Eclipse Public License 2.0](LICENSE), matching the rest of
jlt-commons and jolt itself. SPDX identifier: `EPL-2.0`. Third-party attribution
lives in [`NOTICE`](NOTICE).

It was zlib until 2026-09-05, chosen so the licence matched raylib, since many
examples here are Clojure ports of raylib's own example programs. The org's own
convention won instead, since one exception across the organisation is harder to
explain than that symmetry was worth.

Changing the outbound licence relicenses nothing that arrived under another one,
because those files are not ours to relicense. The ported examples remain derived
from raylib's zlib-licensed originals (raylib is Copyright (c) 2013-2026 Ramon
Santamaria, @raysan5). zlib is permissive and imposes nothing EPL 2.0 conflicts
with, so the combination distributes cleanly provided raylib's notice travels with
it, which is what `NOTICE` is for. zlib's requirement that altered sources be
plainly marked survives the change: the example table above names the upstream
source of every port, and it has to keep doing so.

This project does not vendor or redistribute raylib; it loads the system-installed
`libraylib` at runtime over its C ABI.
