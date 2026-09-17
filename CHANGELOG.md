# Changelog

Notable changes to raylib-jlt, newest first. The format follows
[babashka's changelog](https://github.com/babashka/babashka/blob/master/CHANGELOG.md):
one bullet per user-visible change, written as what a reader would
notice rather than what a commit did.

Sections are dated, not numbered. This is an example suite rather than a
released library, so "what changed, and when" is the useful question.

Examples read at <https://jlt-commons.github.io/raylib-jlt/>.

## 2026-09-17

- **Three more examples, taking the suite to 149.** `camera-3d-free` is the
  first example to use raylib's own `UpdateCamera`, which reads the
  mouse/wheel/keys itself and writes position/target/up back into the
  Camera3D it's given, so the free-look feel is one function call, nothing
  reimplemented. That call mutates its struct in place across frames,
  unlike `with-camera-3d`'s per-frame map, so three new bindings
  (`camera3d-alloc`/`camera3d-free!`/`camera3d-set-target!`) manage a
  persistent native buffer for it, and `begin-mode-3d-ptr` is now public
  so an example can draw through that pointer directly.
  `screen-buffer` is the classic DOS fire effect, zero new FFI: the ember
  grid is a plain Clojure vector rather than a native buffer (matching
  `doom.clj`'s own zbuffer), measured empirically at ~56fps average
  running the simulation at 200x112 rather than the C's 400x225.
  `background-scrolling` is three procedurally generated parallax
  skyline layers, also zero new FFI; the first version drew every layer
  fully opaque, which hid two of the three behind the front one almost
  entirely, fixed by making each layer's sky transparent and giving each
  a different building width so gaps let the layers behind show through.
  Docs caught up to match: both demo galleries, the catalog, `README.md`,
  `docs/site.edn`, `docs/guide/index.md` and the homepage template all
  gained the new rows and counts. The homepage template also had a
  `group-counts` summary line stale since `core` was 23 and `shapes` was
  42 (long before this changelog's earliest entry) and an "117 animated
  GIFs" comment stale at the same vintage; both corrected alongside the
  three-example count bump they were sitting next to.
- **Eight more examples, taking the suite to 146.** `texture-rendering` and
  `texture-waves` are shader-only ports (a grid of squares painted by an
  original fragment shader, and a procedural starfield rippled by a
  UV-displacement shader); `texture-rendering` needed `gl_FragCoord`
  rather than `fragTexCoord`, since `rl/rect!`'s flat-color draw path
  gives `fragTexCoord` a degenerate constant value across the whole
  rect. `srcrec-dstrec` reimplements `DrawTexturePro` directly over rlgl
  immediate mode, no binding for it existing. `amp-envelope` and
  `blend-modes` are zero-new-FFI, reusing `audio-raw-stream`'s refill
  pattern and `particles-blending`'s blend-mode bindings respectively.
  `color-correction` is the standard contrast/saturation/brightness
  grading formula over procedurally generated pictures. `highdpi-testbed`
  needed three new bindings (`get-window-scale-dpi` and
  `get-window-position`, genuinely by-value `Vector2` returns; a plain
  `toggle-borderless-windowed!`), and deliberately skips
  `FLAG_WINDOW_HIGHDPI` after that flag doubled the render scale on top
  of an already-doubled headless screenshot capture in this suite's
  verification environment. `compute-hash` needed five new bindings for
  CRC32/MD5/SHA1/SHA256 and Base64, all scalar or raw-pointer reads, no
  structs; verified against the canonical test vectors for its own input
  string (MD5 intentionally reads in raylib's native word order, not the
  usual byte-swapped hex). Docs caught up to match: both demo galleries,
  the catalog, `README.md`, `docs/site.edn`, `docs/guide/index.md` and
  the homepage template (`docs/templates/home.html`, stale at 122 since
  before this changelog's earliest entries) all gained the new rows and
  counts, and `docs/demos/README.md` got its usual `bb record`
  hand-patch for the 3 still-frame entries the tool has no ledger memory
  of.
- **Twelve more examples, taking the suite to 138.** Eight are zero-new-FFI
  ports from the jank raylib port: `eratosthenes-sieve` (a fragment shader
  computing primality per pixel), `keyboard-testbed`, `mouse-painting`,
  `input-actions`, `particles`, `particles-blending`, `smooth-pixelperfect`
  and `viewport-scaling`. `geometric-shapes` and `camera-3d-split-screen` are
  the first two examples to use jolt's genuine `[:by-value [:struct ...]]`
  for `DrawCube`/`DrawSphere`/`DrawCylinder`/`DrawCapsule`/`DrawPlane`,
  rather than the rlgl immediate-mode `cube!`/`sphere!` stand-ins the rest of
  the suite draws through, and every one of the new raylib.clj functions
  taking more than 3 arguments uses the same `[& {:keys [...] :or {...}}]`
  kwargs style `cube!`/`sphere!` already established. `rectangle-bounds` and
  `polygon-drawing` round out the batch: the first needs no new FFI at all
  (word-wrap only needs `MeasureText`'s int width, not `MeasureTextEx`'s
  per-glyph `Font` indexing), and the second reuses the same low-level rlgl
  calls `rl/texture!` is built from (`rlSetTexture`/`rlBegin`/
  `rlTexCoord2f`/`rlVertex2f`) with a procedurally-generated hue-wheel
  texture standing in for the C example's `cat.png`, since this suite loads
  no textures from disk. Docs caught up to match: `docs/guide/demos.md`,
  `example-catalog.md` (which also picked up a missing `audio` section left
  over from the prior batch), `README.md`, `docs/site.edn` and
  `docs/guide/index.md` all gained the new rows and counts, and
  `docs/demos/README.md` needed its usual `bb record`-regeneration
  hand-patch, since the tool has no memory of the 3 hand-captured still
  frames (`camera-2d-split-screen`, `rectangle-advanced`, `rlgl-triangle`).

## 2026-09-16

- **Four more examples ported from the jank raylib port**, catching this suite
  up: `audio-raw-stream` (the first raudio binding, `AudioStream` passed by
  value, a sine wave streamed with zero bundled audio assets), `starfield-effect`,
  `world-screen` (the first genuinely by-value `Camera3D`, since
  `with-camera-3d`'s pointer trick is wrong on x86-64 by its own docstring), and
  `camera-2d-split-screen`. 126 examples total now. Both demo galleries and the
  README caught up to match: `docs/guide/demos.md`, `example-catalog.md`,
  `README.md` and `docs/site.edn` all gained the new rows and counts, and
  `docs/demos/README.md` got a rare hand-edit (its own `bb record`-owned
  entries stayed put, but `camera-2d-split-screen` needed one added by hand --
  the capture tool's simulated key presses don't land for it yet, so it is a
  still frame like `rectangle-advanced` and `rlgl-triangle`, which the same
  hand-edit backfilled since they had never made it into that gallery).

## 2026-09-05

- **Relicensed from zlib to the Eclipse Public License 2.0**, matching the rest of
  jlt-commons and jolt itself. zlib was chosen to match raylib, since many examples
  are ports of raylib's own, but one exception across the organisation was harder to
  explain than that symmetry was worth. Nothing that arrived under another licence
  is relicensed: the ported examples remain derived from raylib's zlib originals,
  whose notice now travels in `NOTICE`, and the README table naming each upstream
  source is what satisfies zlib's altered-source marking.

## 2026-09-02

- **BREAKING: the suite now needs jolt 0.8.0 or newer.** `deps.edn` declares
  `:jolt/min-version "0.8.0"`, so a runtime that reads the key and sits below the
  floor refuses to load the project instead of running it. jolt 0.8.0 moved
  `jolt.ffi/write`'s value argument in front of the offset, `(write p type value offset)`, matching
  `babashka.ffi`. All 25 call sites in the binding layer were rewritten. The two
  spellings are both integers, so nothing raises and nothing warns: an older jolt
  would write every camera, matrix and uniform field to the wrong address and
  draw something subtly wrong rather than failing. The floor is a forward
  guard rather than a fix for that: jolt reads `:jolt/min-version` only from the
  release that added the key, and that commit is the direct child of the one that
  moved `ffi/write`, so every runtime old enough to have the old order is also too
  old to read the key and ignores it. It will stop the next break, not this one.
  jolt v0.8.0 was released on 2026-09-01, so a current jolt satisfies the floor
  directly. Only a build made from `main` between the `ffi/write` change and that
  tag needs `JOLT_SKIP_MIN_VERSION=1`, because it reports a version like
  `v0.7.29-25-gd4e92a43` whose numeric prefix sorts below 0.8.0 despite carrying
  the new behaviour. `read` and `write-field` are unchanged, and this suite has no
  `[:array ...]` layouts, so jolt's other breaking change in the same release does
  not reach it.

- **Three more examples, taking the suite to 122.** `helitorus` (3d) winds a
  helix around a torus and sweeps it into a tube, doing projection, lighting and
  hidden-surface removal in jolt rather than in raylib, which shows how far the
  rlgl layer reaches on its own. `doom` (3d) is a textured raycaster: one ray per
  screen column, each hit drawn as a vertical strip, with the per-column distance
  doubling as the z-buffer the sprite pass tests against. It sits next to
  `first-person-maze` deliberately, since that walks real 3D cubes under a
  `Camera3D` and this uses no 3D geometry at all. `pacman` (games) has the four
  classic ghost personalities and buffers a turn until the next legal tile
  centre. Four new bindings come with them:
  `rl-disable-backface-culling` / `rl-enable-backface-culling`, and
  `set-mouse-position` with `hide-cursor` / `show-cursor` for `doom`'s
  mouse-look. None of the three is recorded yet, so the galleries still show 119
  recordings.

## 2026-08-30

- **The site's diagrams fit the column they are drawn in.** The homepage's
  "How it fits together" flowchart was laid out left to right and came out
  1458px wide against a 1120px content column, so the browser scaled the whole
  SVG down to fit and shrank the text with it: 77% at a 1440px window, about
  half size at 768px, which is where it was reported as unreadable. Top-down
  puts the same six nodes in 596px, inside the column at every width, so
  nothing is scaled at all.
- **`rl/run!` starts an example from a connected editor.** Evaluating `(-main)`
  over nREPL killed the whole jolt process on macOS, editor connection included,
  with no Clojure exception to explain it: raylib opens its window through GLFW,
  macOS only lets AppKit initialize on the process main thread, and an nREPL eval
  runs on a worker thread. `(rl/run! -main)` marshals onto the main thread that
  `jolt nrepl-server` parks in its pump, and invokes inline when no pump is
  running, so it is also correct under `bb <example>`. A running loop picks up
  redefined vars, which makes this a real interactive loop and not just a
  launcher. New guide page: `docs/guide/repl-driven-development.md`.

- **Eighteen more examples, taking the suite to 115.** `clock-of-clocks`,
  `undo-redo`, `window-should-close`, `ellipse-collision`, `input-gestures`,
  `rlgl-triangle`, `random-sequence`, `camera-2d-mouse-zoom`,
  `rlgl-color-wheel`, `circle-sector-drawing` and `easings-rectangles`, each a
  port of its upstream raylib counterpart. `SetExitKey`, `KEY-NULL` and `KEY-B`
  are newly bound, and `rect-pro!` draws a rotated rectangle as an rlgl quad,
  standing in for DrawRectanglePro. `reasings` is the shared counterpart of
  raylib's `reasings.h`, keeping its `(t, b, c, d)` signature.
- **The drawing API coerces its numeric parameters, both ways.** `rect!`, `line!`, `circle!`,
  `ellipse!`, `text!` and the rest forward to C functions whose positions are
  int, and a double reaching one aborted the process on the first frame with
  `invalid foreign-procedure argument`. The rlgl vertex and matrix calls have
  the same hazard in reverse, taking floats and aborting on an integer. Both
  directions are now coerced at the boundary, because callers compute
  coordinates in floating point constantly and pixel indices in integers just
  as often. Nothing that worked before behaves differently.
- **The gallery carries still frames for the new eighteen.** They have no animated
  GIFs yet, so both galleries show a captured frame instead and say so. That
  keeps the catalog a complete list, with `bb record` left an obvious gap to
  fill.

## 2026-08-29

- **The project moved to the jlt-commons organization**, from `burinc/b12n-raylib-jlt`
  to `jlt-commons/raylib-jlt`. GitHub redirects the old URLs, so existing clones and
  links keep working.
- **The documentation site moved with it**, to
  <https://jlt-commons.github.io/raylib-jlt/>. The old address,
  `raylib-jlt.b12n.app`, ran on a private engine, a personal site repository and a
  personal AWS account, none of which the organization could take over. Nothing was
  lost in the move: the same guide, the same catalog, the same bespoke homepage.
- **Publishing is no longer a maintainer task.** `bb docs-sync` is gone, along with
  the S3 upload, the CloudFront invalidation and the wiki mirror it drove. In its
  place `.github/workflows/site.yml` builds the site on every pull request and
  deploys it from `main`, so a docs change is live on merge and a contributor can
  see their own change rendered before it lands.
- **The site generator is now a shared organization tool**,
  [jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine), pinned by
  tag. This repository keeps what belongs to it: `docs/site.edn` for configuration
  and `docs/templates/home.html` for the homepage, which used to live inside the
  private engine where nobody maintaining this project could reach it. Preview
  locally with `bb site:serve`.
- **The full-size gallery was missing the whole shaders group.**
  `docs/guide/demos.md` showed 91 of 97 demos while claiming to show every one, so
  `julia-set`, `mandelbrot-set`, `raymarching`, `rounded-rect-shader`,
  `palette-switch` and `shader-hot-reload` never appeared. All 101 are there now.

## 2026-08-23

- **The suite tracks raylib 6.0**, up from 5.5, and `bb lib:check` now refuses
  anything older. macOS is `brew upgrade raylib`; Linux keeps the apt-or-source
  path with the CI pin moved to the 6.0 tag.
- **`DrawCircleGradient` takes its centre as a by-value `Vector2` in 6.0**, where
  5.5 took two ints. The C symbol name did not change, so this is the kind of
  break nothing loud catches: all 109 symbols the project binds resolve in both
  versions, and an old binding against a new library simply draws in the wrong
  place. Only a header signature diff finds it. It is the reason `lib:check`
  gates the version rather than warning.
- **jolt 0.7.23 is now a hard floor**, because that binding uses
  `[:by-value [:struct ...]]`. Older jolt fails at compile, not at runtime.
- Most of the upgrade was nothing, which is worth recording: every struct layout
  is byte-identical between 5.5 and 6.0 (`Color` 4, `Vector2` 8, `Vector3` 12,
  `Camera2D` 24, `Camera3D` 44, `Texture2D` 20), all 108 hardcoded constants
  still match, and none of the four functions 6.0 removes were bound. The packed
  `Color`, both pointer-trick cameras and every rlgl path are untouched.
- One constant is worth knowing for later: `SHADER_UNIFORM_SAMPLER2D` moved from
  8 to 12, because 6.0 inserted four UINT variants ahead of it. Nothing here uses
  it yet.
- clj-kondo learned that `jolt.ffi`'s `with-layout` / `with-alloc` / `with-out` /
  `with-c-string` bind their first symbol, so the lint gate stops reporting it as
  unresolved.

- **The suite is 101 examples, up from 75.** 16 took it to 91, which is parity
  with the JVM port: 4 textures, 7 core (window flags, monitors, clipboard,
  gamepad, touch, virtual controls, letterboxing), 4 in 3D (Lorenz attractor,
  DNA helix, yaw/pitch/roll, first-person maze) and elementary cellular
  automata.
- **Shaders, a category that was closed.** 10 more. Six run a fragment shader
  over a full-screen quad - `julia-set`, `mandelbrot-set`, `raymarching`,
  `rounded-rect-shader`, `palette-switch`, `shader-hot-reload` - and four run
  one over a render texture: `postprocessing`, `custom-uniform`,
  `texture-painting`, `multi-sampler`. `LoadShader` returns a `Shader` by value, which Chez's
  `foreign-procedure` cannot express - jolt 0.7.23's `[:by-value [:struct ...]]`
  is what opened it, and it **raises the project's jolt floor to 0.7.23**, the
  first hard version floor this suite has had. GLSL lives as a string in each
  namespace rather than a `.glsl` file, so an example stays self-contained.
- `multi-sampler` is the one that pushed hardest on the FFI:
  `SetShaderValueTexture` passes a `Shader` **and** a `Texture2D` by value in one
  signature, and on arm64 those take different ABI paths - 16 bytes in registers,
  20 bytes passed indirectly. jolt 0.7.23 handles the pair.
- Sampler uniforms must be set INSIDE the shader mode, unlike value uniforms.
  `EndShaderMode` forces a batch draw and clears rlgl's active-texture table, so
  a sampler registered earlier is silently dropped and samples `texture0`
  instead - it renders a plausible image, which is what makes it worth stating.
- All 101 examples now have a committed demo GIF. `multi-sampler` needed a fix
  to earn one: neither of its textures animates and its mix came from the mouse,
  so unattended it recorded as a still. Off-window now sweeps the mix with time,
  and a LEFT/RIGHT press latches manual control.
- **raylib's textures are reachable now**, which they were not before.
  `LoadTexture` returns a 20-byte `Texture2D` by value, which AArch64
  hands back through the `x8` indirect-result register and Chez's
  `foreign-procedure` cannot express at all, so the whole texture, image,
  font, shader, model and audio half of raylib was off the table. rlgl's
  layer underneath is entirely scalar, so a texture here is just the GL
  id `rlLoadTexture` returns: an int, no struct anywhere. Textures are
  built pixel by pixel in native memory rather than decoded from a file,
  which is the part of `LoadTexture` that does not come back.
  [`textures-via-rlgl.md`](docs/guide/textures-via-rlgl.md) is the new
  guide page.
- Render textures too: `rl/render-texture` and `rl/with-render-texture`
  spell out `BeginTextureMode`/`EndTextureMode` in scalar rlgl calls,
  including the HiDPI screen-scale matrix raylib reestablishes across
  `EndTextureMode` and the following `BeginDrawing`. Leaving that at
  identity draws every later frame at half size in the corner.
- New scalar bindings for the rest: window state and config flags,
  monitors, clipboard, gamepad, touch and gestures, key and mouse release
  predicates, `DrawCircleGradient`, `DrawRectangleGradientH` and blend
  modes.
- The registry moved to `scripts/examples_registry.clj` some time ago but
  the catalog still described it as a `bb.edn` row; adding an example is
  five touchpoints, not four, and the guide now says which.

## 2026-08-22

- `bb docs-sync` says which kind of deploy failure it hit, unreachable
  AWS, missing or expired credentials, a 403, or a genuinely absent
  bucket. Instead of blaming missing infrastructure for all four and
  recommending `tofu:apply`. On a restricted laptop `HTTPS_PROXY` is the
  usual cause, and it now detects that and prints the unset-and-retry
  line.
- `bb docs-sync` exits non-zero when the deploy does not happen. It
  printed its complaint and exited 0 before, so nothing chaining off it
  could tell a publish from a no-op.
- Em-dashes removed from the docs.

## 2026-08-21

- `bb docs-sync` could never push a change that was already committed:
  it looked only at its own run's result, so a commit left behind by an
  earlier run stayed local forever.

## 2026-08-20: Public launch

Highlights:

- 75 raylib examples in [Jolt](https://github.com/jolt-lang/jolt), native
  Clojure on Chez Scheme, with no JVM anywhere: 32 shapes, 12 in 3D, 10
  games, 9 core, 7 generative pieces and 5 text.
- They call the real `libraylib` directly over its C ABI through
  `jolt.ffi`, no wrapper library, no codegen, no C shim. The bindings
  and a keyword-argument drawing API live in one shared namespace,
  `net.b12n.raylib-jlt.raylib`, and each example is a small namespace on
  top of it.
- The interesting part is the ABI. raylib passes structs *by value*
  everywhere and Chez's `foreign-procedure` cannot, so each struct gets
  the treatment its size earns: `Color` rides in one register as a
  packed `:uint`, `Camera2D`/`Camera3D` go by pointer, and
  `Vector2`/`Vector3` geometry is drawn through rlgl's scalar immediate
  mode.
- An animated GIF for every one of the 75, in [`docs/demos/`](docs/demos),
  with a guide and full gallery published at `raylib-jlt.b12n.app`. That
  address is retired; the site now lives at
  <https://jlt-commons.github.io/raylib-jlt/>.
- A CI workflow that falls back to building raylib from source when the
  distro package is absent.

Other changes:

- `bb record` regenerates the demo GIFs from
  [`scripts/demo_manifest.edn`](scripts/demo_manifest.edn) via
  [screen-grab](https://github.com/burinc/b12n-screen-grab). Maintainer-only.
- `bb lint` / `bb lint:fix`, with clj-kondo taught about `jolt.ffi/defcfn`
  so its bindings stop reading as unresolved symbols. Formatting moved
  from cljfmt to clojure-lsp.
- `bb lib:check` / `bb lib:install` resolve a cross-platform `libraylib`.
- The jolt launcher is resolved rather than hardcoded to `joltc`.
- Third-party attribution split out of `LICENSE` into `NOTICE`, and the
  zlib header added for the vendored raylib material.

## 2026-08-14

- **Breaking:** the namespace root moved from `net.b12n.rljlt` to
  `net.b12n.raylib-jlt`. Anything requiring the old root needs updating.
- Demo recording migrated to [screen-grab](https://github.com/burinc/b12n-screen-grab),
  replacing the repo's own hand-rolled batch capture script.

## 2026-08-03

- Jolt 0.4.0 resolves strictly in definition order, so the `Color`
  section had to move above its first use. The requirement is noted
  under Requirements in the README. It bites any example that grows a
  forward reference.

## 2026-07-18: First cut

- Grew from 42 examples to 75 in a day: 8 assorted, then 6 classic
  games, 7 generative pieces, and two rounds of 6 shapes examples.
- Along the way the shared drawing API picked up what those examples
  needed: `sphere!`, `sector!` (an rlgl fan for filled arcs, which is
  what makes pie charts and colour wheels possible), `ring!`, `line-ex!`,
  `mouse-pressed?`, and `local-time` over libc for the clock examples.
