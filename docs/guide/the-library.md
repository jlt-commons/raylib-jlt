# The library: `net.b12n.raylib`

Every raylib FFI binding, the keyword-argument drawing API, and the named color
palette live in `net.b12n.raylib`, its own jolt project under `lib/`. This repo
is the library's first consumer, not its only intended one: `lib/` has its own
`deps.edn`, its own headless compile-check, and nothing in it depends on the
182 examples that sit beside it. This page is for anyone who wants to bind
against raylib from jolt without also taking the example suite.

## What it binds

`net.b12n.raylib` is a direct `jolt.ffi` binding to `libraylib`, the same
`jolt.ffi/defcfn` style the rest of this guide describes: no codegen, no C
shim, the shared library dlopened at runtime and called over its C ABI. It
covers `rcore` (window, input, time), `rshapes`, `rtext`, a `rlgl`-backed
subset of `rtextures` and `rmodels` (see
[`textures-via-rlgl.md`](textures-via-rlgl.md) and
[`rlgl-immediate-mode.md`](rlgl-immediate-mode.md) for why those two go
through `rlgl` rather than raylib's own loaders), `raudio`, and the shader
uniform family. It does not yet bind `Font`, `Model`, `Mesh`, `Sound`, `Wave`
or `Music` as their own types; those are a later porting batch's business.

## The 19 modules

Split out of what used to be one 2506-line file, each module is now small
enough to read in one sitting. `net.b12n.raylib.all` (below) re-exports all of
them, so a caller normally only ever requires that one namespace; the table
is for when you need to know which module actually owns a binding, or where
to add a new one.

| Module | Group | Binds |
|---|---|---|
| `audio` | media | raudio: `AudioStream`, play/update/pan, `SetAudioStreamCallback` |
| `camera` | 3D | `Camera2D`/`Camera3D`, `with-camera-2d`/`with-camera-3d`, `world-to-screen` |
| `color` | drawing | the packed `:uint` `Color` and the named palette |
| `core` | window & platform | window lifecycle, monitors, clipboard, placement, `run!` |
| `files` | window & platform | `FilePathList` directory and dropped-file listings |
| `images` | media | CPU-side `Image`: generators, processing, geometry/convolution |
| `input` | window & platform | keyboard/mouse/gamepad/touch predicates and their constants |
| `kwargs` | drawing | the keyword-argument drawing API over color/core/rlgl/shapes/text |
| `log` | window & platform | the trace log: `LOG-*` constants and `on-trace-log!` |
| `models` | 3D | 3D geometry drawn inside `with-camera-3d`: `cube!`, `sphere!`, the by-value `Draw*` calls |
| `native` | native | shared FFI plumbing: `Vector2`/`Vector3`/`Texture2D` layouts, `staged`, `vec2->ptr!` |
| `rays` | 3D | `GetScreenToWorldRay`, `GetRayCollisionBox`, `DrawRay`, `IsCursorHidden` |
| `rlgl` | drawing | scalar immediate mode: `rlBegin`/`rlVertex2f`, backface culling, `flush-batch` |
| `shaders` | media | shader compile/link and the `SetShaderValue*` uniform family |
| `shapes` | drawing | 2D primitives: `DrawPixel`/`Line`/`Rectangle*`/`Circle*`/`Ellipse` |
| `splines` | 3D | `DrawSplineSegment{Linear,Basis,CatmullRom,BezierCubic}` |
| `text` | drawing | `DrawText`, `DrawFPS`, `MeasureText` |
| `textures` | media | GPU textures and framebuffers, reached through `rlgl`'s scalar layer |
| `util` | window & platform | libc `time`/`localtime`, plus raylib's hash and base64 helpers |

`native` has no group partner: it exists because struct layouts and staging
helpers are needed by more than one of the other modules, and duplicating
them would have been worse than one dependency-free leaf the rest can reach.

## Depending on it from another jolt project

**In this repo**, the example project depends on the library by path, since
they live side by side in the same checkout:

```clojure
;; deps.edn (repo root)
{:deps {net.b12n/raylib {:local/root "lib"}}}
```

**From a separate repository**, the library isn't published anywhere but its
own git history, so a consumer pins a commit the same way this project's own
`deps.edn` pins its other git dependencies: `:git/url` plus `:git/sha`, with
`:git/tag` alongside once a tagged release exists to name. Because the
library lives in `lib/` rather than at the repo root, the coordinate also
needs `:deps/root`:

```clojure
;; deps.edn, in a project that is NOT this repo
{:deps {net.b12n/raylib
        {:git/url "https://github.com/jlt-commons/raylib-jlt"
         :git/tag "vX.Y.Z"                          ; once a release is tagged
         :git/sha "<the full sha that tag points at>"
         :deps/root "lib"}}}
```

This repo has not cut a tagged release yet, so pin `:git/sha` to a specific
commit on `main` in the meantime and add `:git/tag` once there is one to pin
against; jolt accepts a git dependency with no `:git/tag` at all, it's the
human-readable half of the pin, not the part that resolves.

Either way, the dependency is on `net.b12n/raylib`: the name this guide's own
snippets use for it, not a coordinate `lib/deps.edn` declares anywhere.
tools.deps gives a library no way to name itself; a consumer's `:deps` map
picks whatever symbol it wants to call the dependency, and `lib/deps.edn`
only ever describes what's inside `lib/` itself (its paths, its native
library, its own deps). What it is not, under any of these forms, is this
repo's own `net.b12n.raylib-jlt` example namespaces.

## `:jolt/native` comes with it

`lib/deps.edn` declares where to find `libraylib`:

```clojure
:jolt/native [{:name "raylib"
               :darwin ["/opt/homebrew/lib/libraylib.dylib" "libraylib.dylib"]
               :linux  ["libraylib.so.6" "libraylib.so"]}]
```

jolt resolves a dependency's native candidates against the *declaring* root,
so a project that depends on `net.b12n/raylib` inherits this lookup rather
than restating it. The only thing a consumer still has to do themselves is
install the actual shared library (`brew install raylib` on macOS, or the
distro raylib package on Linux) at version 6.0 or newer; see the
[README](../../README.md#libraylib) for why the floor is 6.0 specifically.

## The jolt version floor

`lib/deps.edn` also declares `:jolt/min-version "0.8.0"`, for one specific
reason: `jolt.ffi/write` took its arguments in a different order before
0.8.0, and both orderings are plain integers, so a runtime below the floor
does not fail to compile or throw. It writes the value to the wrong address
and reports nothing. The floor makes jolt refuse to load the project
instead.

## Linting against it

A consumer's own clj-kondo run needs the same hook this repo uses:
`jolt.ffi/defcfn` is a macro, and without a hook to see through it,
clj-kondo cannot tell that the vars it defines exist. Copying an isolated
`lib/src` into a project with no config produces hundreds of spurious
errors, one per binding, which is enough to make the linter useless as a
gate. The hook is
[`.clj-kondo/hooks/jolt_ffi.clj`](../../.clj-kondo/hooks/jolt_ffi.clj),
wired up via `.clj-kondo/config.edn`'s `:hooks` map; copy both into a
consuming project the same way.

## `net.b12n.raylib.all` is generated, never hand-edited

`net.b12n.raylib.all` is what a caller actually requires:

```clojure
(:require [net.b12n.raylib.all :as rl])
```

It re-exports every public var of all 19 modules under one namespace, with
zero name collisions (the generator aborts rather than allow one) and the
exact count stated in `all.clj`'s own generated header, which `bb
check:aggregator` keeps honest. `rl/rect!`, `rl/RED` and `rl/with-camera-3d`
all resolve without requiring 19 namespaces by hand. `bb gen:all` produces
it by asking a live jolt process for each module's `ns-publics` and writing
a `def` plus an `alter-meta!` (to carry `:doc`, `:arglists` and `:const`
across the alias) for each one; it is not a parse of the module source text,
so a var's docs and constant-ness survive the re-export exactly.

`bb check:aggregator` fails the build if `all.clj` no longer matches what the
generator would produce right now, so a module that grows a new public var
without a regenerate is caught before it ships silently unaggregated. Run
`bb gen:all` after adding, renaming or removing anything public in a module,
and commit the regenerated file alongside the change that caused it.

## See also

- [`example-catalog.md`](example-catalog.md): every example in this repo is
  written against `net.b12n.raylib.all`.
- [`structs-by-value.md`](structs-by-value.md),
  [`color-by-value.md`](color-by-value.md),
  [`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md),
  [`rlgl-immediate-mode.md`](rlgl-immediate-mode.md),
  [`textures-via-rlgl.md`](textures-via-rlgl.md): why each module's bindings
  are shaped the way they are.
