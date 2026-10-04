# raylib-jlt: raylib bindings for Jolt

[![CI](https://github.com/jlt-commons/raylib-jlt/actions/workflows/ci.yml/badge.svg)](https://github.com/jlt-commons/raylib-jlt/actions/workflows/ci.yml)
[![Site](https://github.com/jlt-commons/raylib-jlt/actions/workflows/site.yml/badge.svg)](https://github.com/jlt-commons/raylib-jlt/actions/workflows/site.yml)

`net.b12n.raylib` binds [raylib](https://github.com/raysan5/raylib) for
**jolt** (native Clojure, no JVM). It calls the real C library: jolt loads the
shared `libraylib` at runtime and calls it over its C ABI with `jolt.ffi`, with
no wrapper library, no codegen and no C shim. The bindings and a keyword-argument
drawing API live in 19 focused modules under `lib/`, aggregated as
`net.b12n.raylib.all`.

**Looking for the examples?** The 187 example programs that used to live here
moved to [raylib-jolt-demo](https://github.com/jlt-commons/raylib-jolt-demo),
one runnable project each, with a
[gallery](https://jlt-commons.github.io/raylib-jolt-demo/).

**Documentation site: <https://jlt-commons.github.io/raylib-jlt/>** carries the
guide.

## Using the library

Depend on `lib/` by git sha. `:deps/root` points jolt at the library inside
the repo, and the library's own `deps.edn` brings its `:jolt/native` lookup for
`libraylib` with it:

```clojure
{:deps {net.b12n/raylib {:git/url   "https://github.com/jlt-commons/raylib-jlt.git"
                         :git/sha   "<full commit sha>"
                         :deps/root "lib"}}}
```

Then:

```clojure
(ns my.game
  (:require [net.b12n.raylib.all :as rl]))

(defn -main [& _]
  (rl/window! {:width 800 :height 450 :title "hello"})
  (rl/set-target-fps 60)
  (while (not (rl/window-should-close?))
    (rl/begin-drawing)
    (rl/clear-background rl/RAYWHITE)
    (rl/text! "hello from jolt" {:x 190 :y 200 :size 20 :color rl/DARKGRAY})
    (rl/end-drawing))
  (rl/close-window))
```

[`docs/guide/the-library.md`](docs/guide/the-library.md) lists the modules,
and [`lib/README.md`](lib/README.md) covers the library on its own.

## Requirements

### jolt

```sh
jolt --version               # requires jolt 0.8.0 or newer
```

`lib/deps.edn` declares `:jolt/min-version "0.8.0"`, and jolt v0.8.0 was released on
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
nothing refuses anything, the library still compiles, and the writes land in the wrong
place. Upgrading is the only thing that fixes that; the declaration earns its place
against the *next* break.

One case still needs the escape hatch. A jolt built from `main` between the
`ffi/write` change and the v0.8.0 tag reports a version like
`v0.7.29-25-gd4e92a43`, whose numeric prefix reads as `0.7.29` and therefore sorts
below the floor even though it carries the new behaviour. A build tagged v0.8.0 or
later parses as `[0 8 0]` and passes normally. For the in-between case:

```sh
JOLT_SKIP_MIN_VERSION=1 bb check:lib
```

Reach for it only when you know your build is past the `ffi/write` change. On jolt
v0.7.29 or below it is a no-op, because no such release reads the key at all, so it
is never a substitute for checking what you are actually running. Installing v0.8.0
is the better answer in both cases.

An older floor still appears in the guide pages, and it is still accurate as history:
**jolt 0.7.23** is what added `[:by-value [:struct ...]]`, which `DrawCircleGradient`
is bound with, and below that the binding fails at compile with a type error. So
0.7.23 is where these bindings became expressible, while 0.8.0 is what the library
needs today.

One thing to know if you edit the library's modules
(`lib/src/net/b12n/raylib/`): since **jolt 0.4.0** unresolved symbols are a
compile error rather than being resolved late, so within each module a
definition must appear before its first use. Most programs require
`net.b12n.raylib.all`, which aggregates all 19 modules, so a single
misordered symbol in any one of them stops the whole library from loading:

```
$ bb check:lib
error[analyze/unresolved-symbol]: Unable to resolve symbol: rgba in this context
  --> ./src/net/b12n/raylib/color.clj:11:16
```

This is why `color.clj`'s `rgba` (and the named palette built from it) is
defined before anything that calls it, and why `models.clj` requires `color`
for `shade-color` / `cube!` / `sphere!` rather than redefining it. Compilation
stops at the first unresolved symbol, so fix them one at a time. `bb
check:lib` loads every library namespace headlessly, which is the quickest
confirmation that the library still loads.

The launcher is `jolt`. It was called `joltc` before jolt 0.5.0, and current
releases install only `jolt`, so if a `joltc` shim is still on your PATH from an older
install, every `jolt` command below works under that name too. The `bb` tasks
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

`lib/deps.edn` points jolt at it via a `:jolt/native` entry (Homebrew's
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

## Working on the library

With [babashka](https://babashka.org) installed, `bb info` prints a cheat-sheet.
jolt reads `bb.edn` too, so `jolt <task>` works for each of these as well.

```sh
bb test                          # unit suite: ffi/write argument order (runs in lib/)
bb check:lib                     # load every library namespace, no window
bb lib:check                     # is libraylib installed for this OS/arch?
```

### Quality: lint / format / checks

```sh
bb lint                          # clj-kondo over lib/src and lib/test (report only)
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
bb gen:all                       # regenerate lib/src/net/b12n/raylib/all.clj
bb check:aggregator              # all.clj matches what gen:all would write
```

`bb check:lib`, `bb test` and `bb lint`/`bb lsp:format-check` are the gates worth
running before a commit: `check:lib` loads every library namespace, `test`
round-trips `ffi/write` to prove the value lands where the offset says, `lint`
runs clj-kondo (static analysis), `lsp:format-check` runs clojure-lsp
(formatting). `check:lib` alone cannot catch an `ffi/write` argument flip,
because both arguments are integers and the swapped call compiles cleanly,
which is what `test` is there for. **Formatting is owned by clojure-lsp, not
cljfmt.** The two disagree on some compact literal tables, so only one
formatter runs against the source, and clojure-lsp was chosen so
`lsp:clean-ns` (no cljfmt equivalent) and `lsp:format` share one tool.
`bb lsp:fix` applies both format and clean-ns fixes in place.

`lint` needs [clj-kondo](https://github.com/clj-kondo/clj-kondo); it uses the
native binary when installed and falls back to the
[clojure CLI](https://clojure.org/guides/install_clojure) route otherwise. The
`lsp:*` tasks need [clojure-lsp](https://clojure-lsp.io) on `PATH`. Nothing in the
library needs a JVM at *runtime*; these are dev-time tools only.

clj-kondo can't see through `jolt.ffi/defcfn`, which defines one var per bound C
symbol; untaught, it reports ~500 false positives and is useless as a gate. The
hook in [`.clj-kondo/hooks/jolt_ffi.clj`](.clj-kondo/hooks/jolt_ffi.clj) rewrites
each `defcfn` into an equivalent `defn`, so the bindings resolve *and* call sites
get arity- and return-type-checked, catching `(rl/init-window 1 2)` at lint time
instead of as a native crash.

### Git hooks

```sh
bb hooks:install       # install FAST pre-commit hook (lint + format + clean-ns, ~2s)
bb hooks:install:full  # install FULL pre-commit hook (+ check:lib and test, slower)
bb hooks:uninstall     # remove the git pre-commit hook
```

The pre-commit hook is local-only (`.git/hooks/pre-commit` is never committed);
each clone that wants it runs `bb hooks:install` once. Skip it for a single commit
with `git commit --no-verify`.

### Dev

```sh
bb nrepl [port]  # start a jolt nREPL server for interactive dev (default 7888)
```

Start a program from a connected editor with `rl/run!`, never a bare `(-main)`:

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
calls so programs read self-descriptively:

```clojure
(rl/text! "hi" :x 10 :y 20 :size 20 :color rl/RED)     ; DrawText
(rl/rect! :x 60 :y 80 :width 120 :height 90 :color rl/BLUE)
(rl/circle! :x 400 :y 225 :radius 50 :color rl/MAROON)
```

### rlgl immediate mode (for shapes with by-value `Vector2` args)

A few raylib calls (e.g. `DrawTriangle`) take `Vector2` by value, which does **not**
reduce to the `Color` trick (a 2-float struct goes in floating-point registers). The
[`shapes`](https://github.com/jlt-commons/raylib-jolt-demo/tree/main/shapes) demo draws its triangle with rlgl's scalar immediate mode instead:
`rlBegin` / `rlColor4ub` / `rlVertex2f` / `rlEnd`.

### `Camera2D` struct by value (the [`camera2d`](https://github.com/jlt-commons/raylib-jolt-demo/tree/main/camera2d) demo)

raylib's `BeginMode2D(Camera2D)` takes a 24-byte struct by value. On the AArch64
(Apple) ABI a composite larger than 16 bytes is passed **indirectly**: the caller
allocates a copy and passes a pointer, so `net.b12n.raylib.camera/with-camera-2d` builds the
struct in native memory (`ffi/alloc` + six `ffi/write :float`s) and binds
`BeginMode2D` as `[:pointer]`.

**This is AArch64-specific.** On the x86-64 SysV ABI those 24 bytes are passed on
the stack, which a `[:pointer]` binding does not do, so this binding is not portable
as written. The portable alternative is to apply the same transform with rlgl's
scalar matrix ops (`rlPushMatrix` / `rlTranslatef` / `rlRotatef` / `rlScalef`,
flushing the batch before `rlPopMatrix`), which is exactly what `BeginMode2D` does
internally. If `camera2d` crashes with an "invalid memory reference", the struct
passing is the cause; switch to the rlgl-matrix approach.

### 3D: `Camera3D` by value + rlgl geometry (the [`camera-3d`](https://github.com/jlt-commons/raylib-jolt-demo/tree/main/camera-3d) demo)

The same pointer approach scales to 3D: `BeginMode3D(Camera3D)` takes a 44-byte
struct (three `Vector3` + `fovy` + `projection`), which is `>16` bytes so it goes
by pointer too; `net.b12n.raylib.camera/with-camera-3d` builds it (`ffi/alloc` 44 +
`ffi/write` ten floats + an `:int`). But 3D shape helpers (`DrawCube`,
`DrawSphere`, `DrawLine3D`) take a `Vector3` **by value**, a 12-byte float struct
passed in FP registers, which the pointer trick does **not** cover. So `camera-3d`
draws its cube with rlgl immediate mode (`rlVertex3f`, scalar) inside `BeginMode3D`;
`DrawGrid` is scalar and used directly. That combination (`Camera3D` by pointer +
rlgl for geometry) is the path for any 3D program until jolt gains native
by-value struct support for `Vector3`-taking functions.

## Documentation

Everything below is also published at **<https://jlt-commons.github.io/raylib-jlt/>**,
which is the nicer way to read it.

- [`docs/guide/`](docs/guide/index.md): patterns & pitfalls, each with source
  citations:
  - [`the-library.md`](docs/guide/the-library.md): the 19 modules and how to depend on them
  - [`structs-by-value.md`](docs/guide/structs-by-value.md): `[:by-value [:struct ...]]` in both directions, and where the jolt floor comes from
  - [`color-by-value.md`](docs/guide/color-by-value.md): why `Color` crosses the FFI as a packed `:uint`
  - [`struct-by-value-pointer-trick.md`](docs/guide/struct-by-value-pointer-trick.md): `Camera2D`/`Camera3D` by pointer on AArch64 (+ the x86-64 caveat)
  - [`rlgl-immediate-mode.md`](docs/guide/rlgl-immediate-mode.md): the fallback for by-value `Vector2`/`Vector3` geometry, + the matrix stack
  - [`textures-via-rlgl.md`](docs/guide/textures-via-rlgl.md): why `LoadTexture` has no binding, and reaching textures and framebuffers from underneath it
  - [`kwarg-drawing-api.md`](docs/guide/kwarg-drawing-api.md): the positional-binds / keyword-wrappers two-layer design
  - [`headless-smoke-testing.md`](docs/guide/headless-smoke-testing.md): proving a window rendered with nobody watching
  - [`repl-driven-development.md`](docs/guide/repl-driven-development.md): why `(-main)` kills your editor connection

- Publishing is automatic. `.github/workflows/site.yml` builds the site on every
  pull request and deploys it from `main`. The generator is
  [jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine), pinned to
  a tag; this repo owns its content, its `docs/site.edn`, and its homepage template
  in `docs/templates/`.

- To preview locally, clone the engine next to this repo and run `bb site:serve`.
  It serves at the root while the published site lives under `/raylib-jlt/`, so
  absolute links will 404 locally and work once deployed.

## Layout

```
raylib-jlt/
├── bb.edn                   ; babashka tasks (bb info)
├── deps.edn                 ; puts lib/ on the classpath for the repo's tooling
├── docs/guide/              ; the pattern guides listed under Documentation above
├── lib/                     ; the net.b12n.raylib library: 19 FFI-binding modules
│   ├── deps.edn             ; :jolt/native for libraylib, :check and :test aliases
│   ├── src/net/b12n/raylib/
│   │   ├── all.clj          ; GENERATED aggregator (bb gen:all); never hand-edited
│   │   ├── check.clj        ; headless load-check of the library
│   │   └── color.clj  core.clj  kwargs.clj  native.clj  …
│   └── test/                ; the unit suite (bb test)
└── scripts/                 ; gen_aggregator.clj (bb gen:all)
                              ; check_positional_args.clj (bb check:positional-args)
                              ; kwarg_calls_to_maps.clj (bb check|fix:kwarg-calls)
```

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for setup, the pre-PR gates
(`bb check:lib`, `bb test`, `bb lint:strict`, `bb lsp:format-check`), and what to
know before touching the shared binding layer. New examples belong in
[raylib-jolt-demo](https://github.com/jlt-commons/raylib-jolt-demo).

## License

Released under the [Eclipse Public License 2.0](LICENSE), matching the rest of
jlt-commons and jolt itself. SPDX identifier: `EPL-2.0`. Third-party attribution
lives in [`NOTICE`](NOTICE).

It was zlib until 2026-09-05, chosen so the licence matched raylib, since many
of the examples that used to live here are Clojure ports of raylib's own example
programs. The org's own convention won instead, since one exception across the
organisation is harder to explain than that symmetry was worth.

Those examples, including the three babashka/ffi ports (`helitorus`, `doom` and
`pacman`, MIT), now live in
[raylib-jolt-demo](https://github.com/jlt-commons/raylib-jolt-demo), whose
`NOTICE` carries their terms and whose pages mark each port's upstream source.
This repo's `NOTICE` keeps the same notices, because its history still holds
those files.

This project does not vendor or redistribute raylib; it loads the system-installed
`libraylib` at runtime over its C ABI.
