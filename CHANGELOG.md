# Changelog

Notable changes to raylib-jlt, newest first. The format follows
[babashka's changelog](https://github.com/babashka/babashka/blob/master/CHANGELOG.md):
one bullet per user-visible change, written as what a reader would
notice rather than what a commit did.

Sections are dated, not numbered. This is an example suite rather than a
released library, so "what changed, and when" is the useful question.

Examples read at <https://jlt-commons.github.io/raylib-jlt/>.

## 2026-09-20

- **`basic-lighting` is the first example here with a custom vertex shader**,
  taking the suite to 184. `rl/shader-vf` compiles a vertex and fragment source
  together; the existing `rl/shader` passes NULL for the vertex stage so raylib
  supplies its own, which cannot pass through the world position and
  transformed normal that per-fragment lighting needs.
- **`rl/material-shader!` points a Material at a Shader.** `DrawMesh` reads the
  shader out of the material it is handed, so `BeginShaderMode` has no effect
  on it: the shader mode rlgl tracks governs the default batch that the 2D
  calls and immediate-mode 3D helpers use. A `DrawMesh` wrapped in
  `with-shader` draws unlit and looks exactly like a shader that failed to
  link.
- **There is no helper for writing a Shader's locs array, on purpose.**
  `SHADER_LOC_VECTOR_VIEW` appears nowhere in raylib's source except its own
  enum, so nothing reads that slot; the C examples use it to stash a location
  they then push themselves every frame. Measured rather than assumed: adding
  and removing the write left the rendered frame byte-identical.
- **raylib's real mesh API is bound, and `mesh-generation` uses it**, taking the
  suite to 183. Eight `GenMesh*` generators, `LoadMaterialDefault`, `DrawMesh`
  and `UnloadMesh`. Everything 3D here went through rlgl immediate mode until
  now, because Mesh is 120 bytes, Material 40 and Matrix 64, and jolt could not
  carry a struct that size by value before 0.7.23.
- **Two FFI rules worth knowing before binding anything else of this shape.** A
  struct-returning call takes a caller-owned destination pointer as its FIRST
  argument and writes the C return there, so `(mesh-cube! m w h l)` is four
  arguments; getting it wrong is an arity error, not a crash. A struct
  argument is a pointer to the bytes, so `DrawMesh` takes three pointers even
  though its C signature is three values.
- **Layouts are read from the installed header, not the raylib source tree.**
  The checkout most people have sitting beside this repo is well past the 6.0
  tag and reorders Mesh bone fields, so a layout taken from it disagrees with
  the struct the linked library actually writes, silently.
- **Three by-value rectangle outline calls are bound**, taking the suite to 182
  with `outlines-thickness` and `textured-curve`. `DrawRectangleLinesEx`,
  `DrawRectangleRounded` and `DrawRectangleRoundedLinesEx` each take a
  Rectangle by value, which is why the suite went without them; jolt 0.7.23
  made that possible and `rl/rect-lines-ex!`, `rl/rect-rounded!` and
  `rl/rect-rounded-lines-ex!` call them directly. `rounded-rectangle` keeps its
  hand-rolled sectors, like the rest of the suite's rlgl stand-ins, and its
  docstring no longer calls the real function unbindable.
- **A negative outline thickness draws nothing, whatever the slider suggests.**
  Both rectangle calls guard their body with a thickness-above-zero test, so
  there is no outward band. `outlines-thickness` says so on screen, and uses
  `ring!` for its circle since raylib 6.0 exports no `DrawCircleLinesEx` at all.
- **`rl/texture!` can rotate.** It takes `:rotation` in degrees plus
  `:origin-x`/`:origin-y` naming the pivot as an offset into the destination
  rectangle, which is what `DrawTexturePro` means by those arguments. Until
  now the rlgl stand-in only emitted axis-aligned quads, which put every
  rotated-sprite example out of reach. Existing calls are unaffected: at the
  default origin with no rotation the arithmetic collapses to the old corner
  positions, checked by byte-comparing four texture-heavy examples against
  the previous commit.
- **Five more textures examples, taking the suite to 180.**
  `sprite-stacking` fakes a 3D car out of 40 stacked slices, `sprite-animation`
  walks a source rectangle along a six-pose strip, `raw-data` builds textures
  from a byte buffer it fills itself, `to-image` walks one image from VRAM to
  RAM and back, and `magnifying-glass` shows a round lens that reveals markers
  drawn nowhere else.
- **All five generate their own pixels.** No image files ship here, so the
  sprite sheets, the walk cycle and the backdrop are all painted at startup
  with the `ImageDraw*` family or computed per pixel. Upstream loads a `.png`
  for each.
- **`magnifying-glass` cuts its lens with a textured triangle fan**, not the
  separate-blend-factor mask the C uses. `rlSetBlendFactorsSeparate` is not
  bound, and a six-argument blend-factor call is a lot of surface to add for
  one example. The disc is built from the render target's own texels instead,
  so there is no mask and no square to hide.
- **The gallery is nine examples behind the suite.** 171 GIFs against 180
  examples, the nine newest marked "not recorded yet" in the catalog.
  `bb record` needs the capture tool and takes the screen over, so recording
  is its own pass.
- **The raylib bindings are a library now, not one shared file in this repo.**
  `net.b12n.raylib` lives in `lib/` as its own jolt project: 19 focused
  modules, aggregated as `net.b12n.raylib.all` so a caller still only
  requires one namespace. Any jolt project can depend on it the way this one
  does, in-repo with `:local/root "lib"` or from anywhere else with
  `:git/url` and `:git/sha`. See
  [`docs/guide/the-library.md`](docs/guide/the-library.md).
- **`src/net/b12n/raylib_jlt/raylib.clj` is gone.** Its 2506 lines moved into
  the library, split by concern (color, core, input, shapes, text, kwargs,
  rlgl, camera, models, rays, splines, images, textures, shaders, audio,
  native, log, files, util), and every example now requires
  `net.b12n.raylib.all` in its place.
- **`bb check:lib` compiles the library headlessly**, the same way `bb check`
  already did for the examples, and CI runs both. `bb gen:all` regenerates
  `net.b12n.raylib.all` from the 19 modules' public vars, and
  `bb check:aggregator` fails the build if the checked-in file no longer
  matches what the generator would produce.
- **No example behaves differently.** The split is a pure extraction for the
  bindings themselves: every FFI signature and constant crossed into its new
  module unchanged apart from indentation and qualifying a symbol that moved
  elsewhere, so `bb check` and `bb test` stay green throughout. Comments and
  docstrings are not held to that same word-for-word bar: several were
  rewritten where the move left them pointing at something that no longer
  exists, such as a comment citing "raylib.clj's still-unextracted shaders
  section" once that section had a real module name to cite instead.
- **`raygui-jlt` can drop its own copy of these bindings now.** It carries a
  229-line `raylib.clj` of its own, duplicated rather than shared because
  there was nothing to depend on before. Pulling the library out is what
  makes sharing it possible, which was the whole point of this arc.
- **Four examples show raylib's CPU-side Image API for the first time, taking
  the suite to 175.** `image-drawing`, `image-text`, `image-rotate` and
  `image-channel` composite pixels into an `Image` buffer before any of them
  reach the GPU, something nothing else in the suite did. Binding the
  `ImageDraw*` family added eleven signatures: the five `ImageDraw*`
  shape/text calls plus `ImageClearBackground`, `ImageRotate`/
  `ImageRotateCW`/`ImageRotateCCW`, `ImageFromChannel` and `ImageAlphaMask`.
- **All four are adaptations, not ports.** Their upstream originals load a
  `.png` from disk, and this repo ships no image files at all, so each one
  generates its own source with a `GenImage*` call instead and says so on
  screen, the same substitution every textures example here already makes.
- **`image-rotate` at a non-multiple of 90 changes the image's own size.**
  raylib grows the buffer to fit the rotated bounds rather than cropping it,
  so that panel comes back wider and taller than it went in. The example
  measures and displays the change, since that is the point of the panel
  rather than something to hide.
- **`net.b12n.raylib.images` gained `image-width` and `image-height`.**
  Reading an `Image`'s own dimensions used to mean hand-rolling an
  `ffi/layout` and `ffi/read-field`, which `image-rotate` did until this
  library gap closed underneath it.

## 2026-09-18 (night)

- **Every example has an animated GIF now, so the gallery is 171 for 171.**
  Twenty had no recording at all and three stood in with a PNG still. All 23
  were recorded and the stills are gone, which means the site shows a preview
  for every example rather than for 151 of them. Nineteen of the 171 come back
  as a single frame, which is the honest result for an example that draws
  something fixed and only moves once you drive it. `drop-files` is the one
  that will not improve: it waits on an operating-system drop event that no
  synthetic input produces.
- **`splines` had been showing the wrong thing for a month.** Its recording was
  made on 2026-08-14, and the change that made it call the real
  `DrawSplineSegment*` functions instead of a hand-rolled reimplementation
  landed on 2026-09-17. The GIF on the site was still the old maths.
  Re-recorded.
- **`bb check:demos` gates the gallery, and runs in CI.** It asserts that the
  ledger, both galleries and every stated count agree about which examples have
  a recording: each `## group (N)` heading matches the entries beneath it, every
  image reference and catalog anchor resolves, and prose that counts GIFs says
  what is on disk. It checks consistency rather than completeness, so an example
  with no recording stays fine as long as the catalog marks it `*not recorded
  yet*` and the counts leave it out. That matters because `bb record` needs an
  unreleased capture tool: a gate demanding a GIF per example would fail every
  outside contribution on a step CONTRIBUTING.md says to skip.
- **`bb record` reports nothing to do when there is nothing to do.** It had been
  offering to re-record 117 examples whose sources were touched only by the
  kwargs codemod and some comment edits. Those ledger entries were refreshed
  without re-recording, so a genuinely stale recording now stands out instead of
  hiding among false positives.

## 2026-09-18 (late)

- **Three more textures examples, taking the suite to 171.** `image-kernel` runs
  the same picture through a sharpen, a Sobel and six Gaussian passes;
  `npatch-drawing` stretches panels whose corners hold their size; `sprite-button`
  picks its state by moving a window down one sheet.
- **raylib convolves the alpha channel, and does not clamp it.** A zero-sum
  kernel like Sobel therefore drives alpha to zero across every flat region,
  which is every interior pixel of an opaque picture: the edges are computed
  perfectly and drawn completely transparent, so `image-kernel`'s third panel
  came back blank white with the compile gate green. Dropping the image to
  `PIXELFORMAT_UNCOMPRESSED_R8G8B8`, which has no alpha channel at all, and
  converting back is the cheapest way to say opaque again. Found by looking at
  the screenshot rather than by reading the header.
- **`npatch!` is `DrawTextureNPatch` written out**, nine `texture!` quads whose
  source rectangles carve the image into a 3x3 and whose destinations put the
  corners back at their original size. Written that way the two three-patch
  modes stop being separate modes: a horizontal one is the same routine with no
  top or bottom border. It also needs CLAMP rather than the REPEAT
  `texture-from-fn` leaves behind, since a stretched edge cell samples right up
  to its border and REPEAT would wrap the far side of the image into it.
- **`ImageKernelConvolution`, `ImageCrop` and `ImageResize` are bound**, all in
  place on an `Image *`, with crop's `Rectangle` by value. The convolution takes
  the kernel's COUNT rather than its side, so a 3x3 is passed as 9.

## 2026-09-18 (evening)

- **Six more examples, taking the suite to 168.** `inline-styling` parses a
  colour markup that lives inside the string itself. `fog-of-war` lifts a fog
  where the player has walked. `framebuffer-rendering` draws the same scene into
  two framebuffers side by side, one of them showing the other camera as its own
  view frustum. `highdpi-demo` lays a ruler of logical points over a ruler of
  physical pixels. `image-generation` and `image-processing` are the first two
  on the new Image bindings.
- **raylib's `Image` is bound, and with it the nine `GenImage*` generators.**
  `Image` is `{void *data; int width, height, mipmaps, format;}`, 24 bytes
  returned by value and taken by value. The generators are why it was worth
  binding: they ARE raylib's procedural textures, and a suite that ships no
  image files has no other way to get a picture out of raylib. What
  `rl/image-*` answers is an rlgl texture id rather than the `Image` or the
  `Texture2D`, so `texture!`, `texture-filter!` and `unload-texture!` keep
  working unchanged and a generated image draws through the same path a
  `texture-from-fn` one does.
- **The processors bind the other way round, and the contrast is the lesson.**
  Every `Image*` operation works IN PLACE, so `ImageColorInvert`,
  `ImageBlurGaussian`, both flips and the rest are plain pointer arguments with
  no by-value dance at all. Only `ImageCopy` and `LoadImageFromTexture` move a
  whole `Image` across the boundary.
- **`LoadImageFromTexture` reads a texture back off the GPU**, which is the only
  route by which something authored in Clojure reaches raylib's own pixel
  operations. `image-processing` draws its source with `texture-from-fn`, pulls
  it back to CPU memory and hands it to raylib from there, in place of the
  `parrots.png` the C opens.
- **`highdpi-demo` is the one example that sets `FLAG_WINDOW_HIGHDPI`.**
  Measured rather than assumed: without the flag `GetScreenWidth` and
  `GetRenderWidth` both answer 800 and the DPI scale is 1.0, so its two rulers
  would be identical and there would be nothing to show. With it the render
  target is 1600 against 800 logical points. The cost is a headless screenshot
  that fills one quadrant, which `highdpi-testbed` already documents as a
  property of the capture path rather than of the example.

## 2026-09-18 (later still)

- **A second callback into jolt, and a harder one: `audio-stream-callback`,
  taking the suite to 162.** `custom-logging` gives raylib a pointer raylib
  calls on the thread that called into it. raudio runs its own audio thread, so
  this one needs jolt's `:collect-safe`, which reactivates the thread before any
  jolt code runs on it; without it the process dies with a memory fault no
  handler can catch. It was probed in isolation before a line of the example was
  written: 20 calls, 8820 frames, clean exit.
- **Being on the audio thread is a budget, not just a thread.** The callback
  owes raudio its samples before the device underruns, so it writes floats
  straight into raudio's buffer with `ffi/write` and allocates nothing per
  sample. The scope under the waveform is the same discipline: a native ring
  buffer the callback writes a second copy into, with its cursor in the last
  four bytes of the same block, so drawing what played costs one more float
  store. It shows about four cycles whatever the pitch, which is what keeps a
  12kHz square from arriving as a picket fence.
- **The audio group now shows raudio in both directions.** `audio-raw-stream`
  and `amp-envelope` push, asking `IsAudioStreamProcessed` and refilling with
  `UpdateAudioStream`; `audio-stream-callback` is pulled from. Same sound,
  opposite direction, and the push versions never leave the main thread.

## 2026-09-18 (later)

- **Four more examples, taking the suite to 161**, all of them from the set that
  loads nothing off disk. `picking-3d` clicks a box in a 3D scene,
  `drop-files` catches files dragged onto the window, `directory-files` is a
  file browser, and `custom-logging` shows raylib's own log captured by a jolt
  function and drawn in the window.
- **`GetScreenToWorldRay` is bound, and it is the exact inverse of the
  `GetWorldToScreen` the suite already had.** A by-value Vector2 in, a by-value
  Camera3D in, a by-value Ray out. `GetRayCollisionBox` follows it, taking that
  Ray with a by-value BoundingBox and returning a by-value RayCollision, whose
  first field is a one-byte C `_Bool`: declared `:bool` the layout puts
  `distance` at offset 4, declared as an int it would not. `raylib.clj` asserts
  both struct sizes at load rather than trusting that, because a wrong offset
  reads a plausible float out of the wrong bytes and never errors. `DrawRay`
  and `IsCursorHidden` came along with them.
- **`FilePathList` is bound, and with it the `char **` behind it.** It is
  `{unsigned int count; char **paths;}`, the same 16-byte shape as `Shader`, so
  the by-value binding is the one the shader section already used. What is new
  is that raylib owns that array and every string in it until the matching
  Unload, so `rl/dropped-files` and `rl/directory-files` copy the strings into a
  Clojure vector and unload inside the same call. The filter string's behaviour
  is measured rather than read off the header: `"*.*"` answers directories and
  files, `"DIRS*"` and `"FILES*"` answer one each, and an empty filter quietly
  means files only.
- **The first callback INTO jolt.** Every other binding here calls out of jolt
  into C. `SetTraceLogCallback` hands raylib a function pointer built from an
  ordinary jolt fn by `ffi/foreign-callable`, and raylib calls it for every
  message it would otherwise print. The awkward part is that raylib's callback
  signature ends in a `va_list`, which no FFI type describes, so it is taken as
  an opaque pointer and handed to libc's `vsnprintf` with the format string.
  That step is what turns `"Target time per frame: %02.03f milliseconds"` into
  the line with the number in it. C can call the pointer until `free-callable`
  runs and not one instruction longer, so it is freed after `CloseWindow`.
- **`directory-files` draws its own list view.** The C builds its browser out of
  raygui, a separate single-header library this suite does not bind. The list,
  the selection, the scrollbar and the path bar are `rect!` and `text!` here,
  which is a fair trade: a list view over a vector of strings is not what makes
  the C example interesting.
- **The headless-testing guide now says what a slept display looks like.**
  raylib warns `Failed to initialize platform` and carries on, so the process
  dies later with `invalid memory reference` pointing at whatever line was last,
  which reads as a bug in the example you just wrote. It hits every example at
  once, so the check is to re-run one that already worked.

## 2026-09-18

- **Three more examples, taking the suite to 157**, one each from the
  categories the suite had thinnest cover of. `top-down-lights` is the
  last of raylib's `shapes` examples to land here: nothing in it draws
  light, every light renders a full-screen mask whose alpha is the whole
  payload, the masks merge into one, and that one goes over the scene as
  black, so alpha 0 reads as lit and alpha 1 as dark. `basic-voxel` is an
  8x8x8 block you walk around and take apart a cube at a time.
  `strings-management` is a sentence as a bouncing text particle you can
  cut in half, shatter into characters, shake, and glue back together.
- **`top-down-lights` and `basic-voxel` animate until you touch them.**
  Neither moves on its own in the C, and `scripts/demo_manifest.edn` has the
  measurement for why that matters here: no synthetic input actuates a
  raylib/GLFW window, not clicks and not keys either, so an example with no
  motion of its own records as a single frame. Light #1 now walks a slow
  figure-eight that sweeps its shadows across most of the boxes, and the voxel
  block is orbited from above. The first drag, WASD press or click hands
  control over for good. A mouse move deliberately does not: `GetMouseX`
  reports 0 on the first frame and the real position on the second, and the
  window-relative coordinates shift again whenever the window is placed, so
  "the pointer moved" is not evidence that a person moved it.
- **`rlSetBlendFactors` is bound**, with `BLEND-CUSTOM` and the three GL
  enums it takes. `top-down-lights` needs two custom blend equations that
  work on alpha alone: `GL_MIN` punches a light's transparent centre into
  a mask cleared to opaque white, and `GL_MAX` cuts the shadow volumes
  back out of it. Both equations ignore the src/dst factors, which is why
  the same `SRC_ALPHA` pair goes to each, and rlgl only re-reads the
  factors when the blend mode changes, so the order is always set the
  factors, then begin the mode.
- **`strings-management` is the tour of raylib's string helpers, done in
  Clojure.** The C exists because C has no string library, so it leans on
  `TextCopy`, `TextSubtext`, `TextSplit`, `TextLength`, `TextFormat` and
  the six `TextTo*` case conversions. All of those are `subs`, `count`,
  `str` and `clojure.string` here, and the only raylib call left in the
  text path is `MeasureText`, which has to be raylib's because only
  raylib knows how wide its font draws.
- **Both new 3D-ish examples avoid a binding rather than adding one.**
  `basic-voxel` picks with a ray, but `GetScreenToWorldRay` is not bound
  and does not need to be: a ray through the centre of the screen is the
  look direction the camera was built from, and the hit test is the slab
  clip `GetRayCollisionBox` does. It also keeps the suite's own yaw/pitch
  walk instead of `UpdateCamera`'s first-person mode, which runs on raw
  `GetMouseDelta` and drifts whenever the pointer moves at all, including
  while the window is still taking focus, so no headless screenshot ever
  landed on the same frame twice.

## 2026-09-17

- **The splines example draws through raylib's own `DrawSplineSegment*`
  now, not a math reimplementation.** It predated jolt's by-value struct
  support and said so honestly: `DrawSpline*` take a `Vector2` array by
  value, unbindable at the time, so the curve was evaluated in pure
  Clojure and drawn as a `line!` polyline. jolt 0.7.23 changed that, so
  four new bindings (`spline-segment-linear!`/`-basis!`/`-catmull-rom!`/
  `-bezier-cubic!`) replace the old basis-function math with the real
  calls, one `Vector2`-by-value argument per point staged through the
  existing `vec2->ptr!` helper. Same control points, same three modes,
  same control-polygon and drag targets.
- **Three more examples, taking the suite to 154.** `texture-outline`
  traces a sprite's alpha edge in a fragment shader by sampling the four
  diagonal texels around each pixel. `directional-billboard` combines
  this batch's own camera-facing quad math with the UV sub-rect slicing
  `textured-cube` introduced, so a sprite-sheet character's facing row
  turns with the orbiting camera while its column cycles a walk
  animation. `ascii-rendering` re-renders a render-texture scene as
  ASCII glyphs, each cell's character picked from one of eight 5x5
  bitmaps packed as bits of an int. All three are zero new FFI.
  `texture-outline`'s first recording came back a single frame, since
  its only motion was mouse-wheel input the capture tool doesn't drive;
  a slow sine drift on top of the wheel-set base fixed it, the same
  class of gap `textured-cube` hit the round before.
- **Two more examples, taking the suite to 151.** `textured-cube` draws
  two rlgl-textured cubes from one shared atlas, one showing the whole
  thing and one a source-rect slice (`DrawCubeTextureRec`'s idea).
  `billboard-rendering` rebuilds a camera-facing quad from cross
  products of the camera's own forward vector, one billboard spinning
  via a Rodrigues rotation around that same normal. The first winding
  order for that quad was geometrically correct and permanently
  invisible, backface-culled from every angle because its
  `cross(edge1, edge2)` pointed away from the camera instead of toward
  it; caught by looking at the rendered PNG rather than trusting the
  compile, fixed by reversing the draw order.
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
