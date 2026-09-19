# The example catalog: 175 raylib demos in jolt

A map of the whole suite. Each example is one namespace under
`src/net/b12n/raylib_jlt/`, runnable by a friendly `bb <name>` task or the underlying
`jolt -M:<alias>`. `bb info` prints this grouping live; this page adds the "how it's
wired" recipe at the end.

Run one, list them, or reel through all of them:

```sh
bb <name>          # e.g. bb asteroids   (opens a window)
bb examples        # flat list with descriptions
bb info            # the grouped cheat-sheet below
bb run-all [secs]  # every example, N seconds each (unattended)
```

## games (11)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/asteroids.gif" width="80">](demos.md#asteroids) | `asteroids` | the classic vector shooter, splitting rocks |
| [<img src="../demos/tetris.gif" width="80">](demos.md#tetris) | `tetris` | 10×20 well, 7 tetrominoes, lines, levels |
| [<img src="../demos/pong.gif" width="80">](demos.md#pong) | `pong` | two-paddle classic, you (W/S) vs a CPU |
| [<img src="../demos/vampire-survivors.gif" width="80">](demos.md#vampire-survivors) | `vampire-survivors` | auto-firing survivors-like: waves, XP, levels |
| [<img src="../demos/snake.gif" width="80">](demos.md#snake) | `snake` | the classic snake (arrow keys, grow, don't crash) |
| [<img src="../demos/breakout.gif" width="80">](demos.md#breakout) | `breakout` | paddle + ball + brick grid (mouse paddle) |
| [<img src="../demos/space-invaders.gif" width="80">](demos.md#space-invaders) | `space-invaders` | marching aliens (arrows + SPACE to shoot) |
| [<img src="../demos/flappy-bird.gif" width="80">](demos.md#flappy-bird) | `flappy-bird` | flap through the pipe gaps (SPACE) |
| [<img src="../demos/game-2048.gif" width="80">](demos.md#game-2048) | `game-2048` | 2048: 4x4 tile-merge puzzle (arrow keys) |
| [<img src="../demos/minesweeper.gif" width="80">](demos.md#minesweeper) | `minesweeper` | reveal/flag grid (mouse L reveal, R flag) |
| [<img src="../demos/pacman.gif" width="80">](demos.md#pacman) | `pacman` | the four classic ghost personalities, buffered turns |

## core (34)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/basic-window.gif" width="80">](demos.md#basic-window) | `basic-window` | the minimal window + text (`jolt -M:run`) |
| [<img src="../demos/input-keys.gif" width="80">](demos.md#input-keys) | `input-keys` | `IsKeyDown`, steer a ball with the arrow keys |
| [<img src="../demos/input-mouse.gif" width="80">](demos.md#input-mouse) | `input-mouse` | `GetMouseX/Y` + click to recolor |
| [<img src="../demos/input-mouse-wheel.gif" width="80">](demos.md#input-mouse-wheel) | `input-mouse-wheel` | `GetMouseWheelMove` scrolls a box |
| [<img src="../demos/camera-2d.gif" width="80">](demos.md#camera-2d) | `camera-2d` | a 2D camera over a skyline (struct by value) |
| [<img src="../demos/delta-time.gif" width="80">](demos.md#delta-time) | `delta-time` | per-frame vs `GetFrameTime` movement |
| [<img src="../demos/scissor-test.gif" width="80">](demos.md#scissor-test) | `scissor-test` | `BeginScissorMode` clips a grid |
| [<img src="../demos/basic-screen-manager.gif" width="80">](demos.md#basic-screen-manager) | `basic-screen-manager` | a LOGO / TITLE / GAMEPLAY / ENDING state flow |
| [<img src="../demos/random-values.gif" width="80">](demos.md#random-values) | `random-values` | `GetRandomValue`, a new value every 2s |
| [<img src="../demos/window-letterbox.gif" width="80">](demos.md#window-letterbox) | `window-letterbox` | a fixed picture letterboxed into the window |
| [<img src="../demos/window-flags.gif" width="80">](demos.md#window-flags) | `window-flags` | toggle vsync/resizable/topmost live |
| [<img src="../demos/monitor-detector.gif" width="80">](demos.md#monitor-detector) | `monitor-detector` | every attached display, current one lit |
| [<img src="../demos/clipboard-text.gif" width="80">](demos.md#clipboard-text) | `clipboard-text` | type, C copies to the system clipboard, V pastes |
| [<img src="../demos/input-gamepad.gif" width="80">](demos.md#input-gamepad) | `input-gamepad` | live sticks, triggers and buttons for gamepad 0 |
| [<img src="../demos/input-multitouch.gif" width="80">](demos.md#input-multitouch) | `input-multitouch` | touch points (the mouse is point 0) |
| [<img src="../demos/input-virtual-controls.gif" width="80">](demos.md#input-virtual-controls) | `input-virtual-controls` | an on-screen D-pad and action button |
| [<img src="../demos/undo-redo.gif" width="80">](demos.md#undo-redo) | `undo-redo` | a bounded history, CTRL-Z and CTRL-Y walking it |
| [<img src="../demos/window-should-close.gif" width="80">](demos.md#window-should-close) | `window-should-close` | `SetExitKey` so a close request can be answered |
| [<img src="../demos/input-gestures.gif" width="80">](demos.md#input-gestures) | `input-gestures` | `GetGestureDetected`, named as they happen |
| [<img src="../demos/random-sequence.gif" width="80">](demos.md#random-sequence) | `random-sequence` | a shuffled sequence, each height used once |
| [<img src="../demos/camera-2d-mouse-zoom.gif" width="80">](demos.md#camera-2d-mouse-zoom) | `camera-2d-mouse-zoom` | zoom pinned to the point under the cursor |
| [<img src="../demos/storage-values.gif" width="80">](demos.md#storage-values) | `storage-values` | values that survive a restart, via a file |
| [<img src="../demos/camera-2d-platformer.gif" width="80">](demos.md#camera-2d-platformer) | `camera-2d-platformer` | five ways a camera can follow a jumping player |
| [<img src="../demos/camera-2d-split-screen.gif" width="80">](demos.md#camera-2d-split-screen) | `camera-2d-split-screen` | two players, two cameras, two render textures |
| [<img src="../demos/keyboard-testbed.gif" width="80">](demos.md#keyboard-testbed) | `keyboard-testbed` | an on-screen ENG-US keyboard, every key lit |
| [<img src="../demos/input-actions.gif" width="80">](demos.md#input-actions) | `input-actions` | abstract input actions: keyboard + gamepad |
| [<img src="../demos/smooth-pixelperfect.gif" width="80">](demos.md#smooth-pixelperfect) | `smooth-pixelperfect` | sub-pixel camera smoothing at 5x upscale |
| [<img src="../demos/viewport-scaling.gif" width="80">](demos.md#viewport-scaling) | `viewport-scaling` | 6 viewport-scaling policies, resize live |
| [<img src="../demos/highdpi-testbed.gif" width="80">](demos.md#highdpi-testbed) | `highdpi-testbed` | diagnostic overlay: monitors, DPI, crosshair |
| [<img src="../demos/compute-hash.gif" width="80">](demos.md#compute-hash) | `compute-hash` | CRC32/MD5/SHA1/SHA256 + Base64 of typed text |
| [<img src="../demos/drop-files.gif" width="80">](demos.md#drop-files) | `drop-files` | drag files in: FilePathList by value |
| [<img src="../demos/directory-files.gif" width="80">](demos.md#directory-files) | `directory-files` | a file browser over LoadDirectoryFilesEx |
| [<img src="../demos/custom-logging.gif" width="80">](demos.md#custom-logging) | `custom-logging` | raylib's log captured by a jolt callback |
| [<img src="../demos/highdpi-demo.gif" width="80">](demos.md#highdpi-demo) | `highdpi-demo` | logical points vs physical pixels, two rulers |

## shapes (44)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/bouncing-ball.gif" width="80">](demos.md#bouncing-ball) | `bouncing-ball` | animation, `IsKeyPressed` pause, `DrawFPS` |
| [<img src="../demos/basic-shapes.gif" width="80">](demos.md#basic-shapes) | `basic-shapes` | rect / circle / ellipse / line + an rlgl triangle |
| [<img src="../demos/colors-palette.gif" width="80">](demos.md#colors-palette) | `colors-palette` | every named raylib color in a grid |
| [<img src="../demos/gradient.gif" width="80">](demos.md#gradient) | `gradient` | `DrawRectangleGradientV` (two by-value Colors) |
| [<img src="../demos/following-eyes.gif" width="80">](demos.md#following-eyes) | `following-eyes` | pupils track the mouse (scalar trig) |
| [<img src="../demos/starfield.gif" width="80">](demos.md#starfield) | `starfield` | `GetRandomValue` + bulk draw, per-star twinkle |
| [<img src="../demos/logo-raylib.gif" width="80">](demos.md#logo-raylib) | `logo-raylib` | rectangles + text, positioned with `MeasureText` |
| [<img src="../demos/mouse-trail.gif" width="80">](demos.md#mouse-trail) | `mouse-trail` | a fading cursor trail (alpha) |
| [<img src="../demos/recursive-tree.gif" width="80">](demos.md#recursive-tree) | `recursive-tree` | a binary fractal tree (lines + trig) |
| [<img src="../demos/math-sine-cosine.gif" width="80">](demos.md#math-sine-cosine) | `math-sine-cosine` | a live unit-circle sine/cosine visualization |
| [<img src="../demos/bullet-hell.gif" width="80">](demos.md#bullet-hell) | `bullet-hell` | a rotating bullet spiral |
| [<img src="../demos/triangle-strip.gif" width="80">](demos.md#triangle-strip) | `triangle-strip` | a rainbow strip via rlgl immediate mode |
| [<img src="../demos/collision-area.gif" width="80">](demos.md#collision-area) | `collision-area` | AABB collision between two boxes |
| [<img src="../demos/dashed-line.gif" width="80">](demos.md#dashed-line) | `dashed-line` | a dashed line follows the mouse |
| [<img src="../demos/double-pendulum.gif" width="80">](demos.md#double-pendulum) | `double-pendulum` | chaotic double-pendulum motion + trail |
| [<img src="../demos/kaleidoscope.gif" width="80">](demos.md#kaleidoscope) | `kaleidoscope` | strokes mirrored with 6-fold symmetry |
| [<img src="../demos/hilbert-curve.gif" width="80">](demos.md#hilbert-curve) | `hilbert-curve` | a rainbow Hilbert space-filling curve |
| [<img src="../demos/math-angle-rotation.gif" width="80">](demos.md#math-angle-rotation) | `math-angle-rotation` | fixed spokes + a spinning line |
| [<img src="../demos/ball-physics.gif" width="80">](demos.md#ball-physics) | `ball-physics` | 2D balls under gravity, SPACE respawns |
| [<img src="../demos/lines-bezier.gif" width="80">](demos.md#lines-bezier) | `lines-bezier` | a cubic Bézier that follows the mouse |
| [<img src="../demos/color-wheel.gif" width="80">](demos.md#color-wheel) | `color-wheel` | an HSV wheel as an rlgl triangle fan |
| [<img src="../demos/pie-chart.gif" width="80">](demos.md#pie-chart) | `pie-chart` | labelled pie slices via `rl/sector!` + a legend |
| [<img src="../demos/splines.gif" width="80">](demos.md#splines) | `splines` | Catmull-Rom / Bézier / B-spline (SPACE cycles) |
| [<img src="../demos/vector-angle.gif" width="80">](demos.md#vector-angle) | `vector-angle` | the signed angle between two vectors (`atan2`) |
| [<img src="../demos/easings.gif" width="80">](demos.md#easings) | `easings` | a grid of balls, each on a different easing |
| [<img src="../demos/penrose-tiling.gif" width="80">](demos.md#penrose-tiling) | `penrose-tiling` | a P3 Penrose tiling by golden-ratio deflation |
| [<img src="../demos/analog-clock.gif" width="80">](demos.md#analog-clock) | `analog-clock` | a live analog clock (libc local time) |
| [<img src="../demos/digital-clock.gif" width="80">](demos.md#digital-clock) | `digital-clock` | a seven-segment `HH:MM:SS` display |
| [<img src="../demos/ring-drawing.gif" width="80">](demos.md#ring-drawing) | `ring-drawing` | an animated annulus via `rl/ring!` |
| [<img src="../demos/rounded-rectangle.gif" width="80">](demos.md#rounded-rectangle) | `rounded-rectangle` | rounded rects from `sector!` corners |
| [<img src="../demos/rectangle-scaling.gif" width="80">](demos.md#rectangle-scaling) | `rectangle-scaling` | drag the corner handle to resize a rect |
| [<img src="../demos/lines-drawing.gif" width="80">](demos.md#lines-drawing) | `lines-drawing` | a rotating fan of thick `rl/line-ex!` lines |
| [<img src="../demos/ellipse-collision.gif" width="80">](demos.md#ellipse-collision) | `ellipse-collision` | two ellipses reddening when they overlap |
| [<img src="../demos/rlgl-triangle.gif" width="80">](demos.md#rlgl-triangle) | `rlgl-triangle` | per-vertex colour interpolated across a face |
| [<img src="../demos/rlgl-color-wheel.gif" width="80">](demos.md#rlgl-color-wheel) | `rlgl-color-wheel` | a hue wheel as a triangle fan |
| [<img src="../demos/circle-sector-drawing.gif" width="80">](demos.md#circle-sector-drawing) | `circle-sector-drawing` | a sector starved of segments |
| [<img src="../demos/easings-rectangles.gif" width="80">](demos.md#easings-rectangles) | `easings-rectangles` | size and rotation on one easing curve |
| [<img src="../demos/easings-ball.gif" width="80">](demos.md#easings-ball) | `easings-ball` | slide, swell and fade, one curve each |
| [<img src="../demos/easings-box.gif" width="80">](demos.md#easings-box) | `easings-box` | drop, flatten, spin, grow, fade: five curves |
| [<img src="../demos/logo-anim.gif" width="80">](demos.md#logo-anim) | `logo-anim` | the raylib logo assembling itself, unsmoothed |
| [<img src="../demos/rectangle-advanced.gif" width="80">](demos.md#rectangle-advanced) | `rectangle-advanced` | per-side roundness with a horizontal gradient |
| [<img src="../demos/easings-testbed.gif" width="80">](demos.md#easings-testbed) | `easings-testbed` | one curve at a time, plotted and run |
| [<img src="../demos/starfield-effect.gif" width="80">](demos.md#starfield-effect) | `starfield-effect` | flying starfield (wheel=speed, SPACE=mode) |
| [<img src="../demos/top-down-lights.gif" width="80">](demos.md#top-down-lights) | `top-down-lights` | lights and shadow volumes in an alpha mask |

## text (8)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/font-sizes.gif" width="80">](demos.md#font-sizes) | `font-sizes` | font sizes + `MeasureText` centering |
| [<img src="../demos/writing-anim.gif" width="80">](demos.md#writing-anim) | `writing-anim` | a self-typing message |
| [<img src="../demos/format-text.gif" width="80">](demos.md#format-text) | `format-text` | `format` padded score + MM:SS timer |
| [<img src="../demos/words-alignment.gif" width="80">](demos.md#words-alignment) | `words-alignment` | align a word inside a box with `MeasureText` |
| [<img src="../demos/input-box.gif" width="80">](demos.md#input-box) | `input-box` | type into a text box (GetCharPressed) |
| [<img src="../demos/rectangle-bounds.gif" width="80">](demos.md#rectangle-bounds) | `rectangle-bounds` | draggable word-wrap text container |
| [<img src="../demos/strings-management.gif" width="80">](demos.md#strings-management) | `strings-management` | bouncing text you slice, shatter and glue |
| [<img src="../demos/inline-styling.gif" width="80">](demos.md#inline-styling) | `inline-styling` | colour markup inside the string itself |

## 3d (27)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/camera-3d.gif" width="80">](demos.md#camera-3d) | `camera-3d` | an orbiting 3D camera, `Camera3D` by value |
| [<img src="../demos/waving-cubes.gif" width="80">](demos.md#waving-cubes) | `waving-cubes` | 196 cubes rippling in 3D (shared `rl/cube!`) |
| [<img src="../demos/camera-3d-first-person.gif" width="80">](demos.md#camera-3d-first-person) | `camera-3d-first-person` | WASD + mouse-look walk through columns |
| [<img src="../demos/tesseract-view.gif" width="80">](demos.md#tesseract-view) | `tesseract-view` | a rotating 4D hypercube projected 4D→3D→2D |
| [<img src="../demos/wireframe-shapes.gif" width="80">](demos.md#wireframe-shapes) | `wireframe-shapes` | pyramid / torus / helix as rlgl 3D lines |
| [<img src="../demos/rlgl-solar-system.gif" width="80">](demos.md#rlgl-solar-system) | `rlgl-solar-system` | Sun / Earth / Moon via the rlgl matrix stack |
| [<img src="../demos/box-collisions.gif" width="80">](demos.md#box-collisions) | `box-collisions` | a player cube vs. boxes, 3D AABB highlight |
| [<img src="../demos/rotating-cube.gif" width="80">](demos.md#rotating-cube) | `rotating-cube` | a single cube spinning via the rlgl matrix stack |
| [<img src="../demos/spinning-cubes.gif" width="80">](demos.md#spinning-cubes) | `spinning-cubes` | a row of cubes each spinning with a phase offset |
| [<img src="../demos/orthographic-projection.gif" width="80">](demos.md#orthographic-projection) | `orthographic-projection` | perspective vs orthographic (SPACE toggles) |
| [<img src="../demos/point-cloud.gif" width="80">](demos.md#point-cloud) | `point-cloud` | ~1500 points as tiny rlgl cubes, rotating |
| [<img src="../demos/bouncing-spheres.gif" width="80">](demos.md#bouncing-spheres) | `bouncing-spheres` | spheres bouncing in a 3D box (rl/sphere!) |
| [<img src="../demos/lorenz-attractor.gif" width="80">](demos.md#lorenz-attractor) | `lorenz-attractor` | the Lorenz attractor traced in 3D |
| [<img src="../demos/dna-helix.gif" width="80">](demos.md#dna-helix) | `dna-helix` | a turning double helix with coloured base pairs |
| [<img src="../demos/yaw-pitch-roll.gif" width="80">](demos.md#yaw-pitch-roll) | `yaw-pitch-roll` | the three aircraft rotations in 3D |
| [<img src="../demos/first-person-maze.gif" width="80">](demos.md#first-person-maze) | `first-person-maze` | walk a grid maze in first person, with a minimap |
| [<img src="../demos/world-screen.gif" width="80">](demos.md#world-screen) | `world-screen` | a 2D label tracks a cube via `GetWorldToScreen` |
| [<img src="../demos/geometric-shapes.gif" width="80">](demos.md#geometric-shapes) | `geometric-shapes` | cubes, spheres, cylinders, cones, capsules |
| [<img src="../demos/camera-3d-split-screen.gif" width="80">](demos.md#camera-3d-split-screen) | `camera-3d-split-screen` | two players, two render-texture halves |
| [<img src="../demos/camera-3d-free.gif" width="80">](demos.md#camera-3d-free) | `camera-3d-free` | a free-look camera around a cube, UpdateCamera |
| [<img src="../demos/textured-cube.gif" width="80">](demos.md#textured-cube) | `textured-cube` | two cubes, one textured atlas, one sub-rect |
| [<img src="../demos/billboard-rendering.gif" width="80">](demos.md#billboard-rendering) | `billboard-rendering` | camera-facing quads, one spins |
| [<img src="../demos/directional-billboard.gif" width="80">](demos.md#directional-billboard) | `directional-billboard` | a billboard whose facing row turns with the camera |
| [<img src="../demos/helitorus.gif" width="80">](demos.md#helitorus) | `helitorus` | a helix wound around a torus, projected and depth-sorted in jolt |
| [<img src="../demos/doom.gif" width="80">](demos.md#doom) | `doom` | a textured raycaster: one ray per screen column, no 3D geometry |
| [<img src="../demos/basic-voxel.gif" width="80">](demos.md#basic-voxel) | `basic-voxel` | an 8x8x8 voxel block, click one out |
| [<img src="../demos/picking-3d.gif" width="80">](demos.md#picking-3d) | `picking-3d` | click a box: a real GetScreenToWorldRay pick |

Most of the 3D set stands on two building blocks from
[`rlgl-immediate-mode.md`](rlgl-immediate-mode.md) and
[`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md): `with-camera-3d`
(the camera, by pointer) and `cube!` (the geometry, by rlgl vertices).

`helitorus` and `doom` are the two that don't, and that is why they're worth
reading next to the others. `helitorus` does projection, lighting and
hidden-surface removal itself, so it shows how far the rlgl layer alone reaches
without a `Camera3D`. `doom` casts one ray per screen column and draws each hit as
a textured vertical strip, with no 3D geometry and no camera matrix anywhere; put
it beside `first-person-maze`, which walks a grid of real cubes under a
`Camera3D`, and the two techniques for the same picture sit side by side.

## generative (10)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/game-of-life.gif" width="80">](demos.md#game-of-life) | `game-of-life` | Conway's Game of Life (SPACE reseeds) |
| [<img src="../demos/boids.gif" width="80">](demos.md#boids) | `boids` | flocking birds (separation/alignment/cohesion) |
| [<img src="../demos/fireworks.gif" width="80">](demos.md#fireworks) | `fireworks` | rockets + fading particle bursts |
| [<img src="../demos/fourier-epicycles.gif" width="80">](demos.md#fourier-epicycles) | `fourier-epicycles` | rotating circles trace a square wave |
| [<img src="../demos/spirograph.gif" width="80">](demos.md#spirograph) | `spirograph` | animated hypotrochoid roulette curves |
| [<img src="../demos/l-system.gif" width="80">](demos.md#l-system) | `l-system` | an L-system fractal plant (grows + regrows) |
| [<img src="../demos/flow-field.gif" width="80">](demos.md#flow-field) | `flow-field` | particles steered by a flow field (trails) |
| [<img src="../demos/cellular-automata.gif" width="80">](demos.md#cellular-automata) | `cellular-automata` | Wolfram's elementary automata |
| [<img src="../demos/clock-of-clocks.gif" width="80">](demos.md#clock-of-clocks) | `clock-of-clocks` | six digits spelled by a grid of clock hands |
| [<img src="../demos/particles.gif" width="80">](demos.md#particles) | `particles` | water/smoke/fire particles follow the mouse |

All nine are pure math + the drawing API, no new bindings. They showcase the
suite as a generative-art canvas: cellular automata, agent flocking, particle
systems, rotating-vector Fourier series, parametric roulette curves, string-rewrite
fractals, and noise-steered flow fields.

## textures (26)

raylib's texture API returns structs by value and has no binding here; these go
through rlgl's scalar layer instead, so every texture is built pixel by pixel in
native memory rather than loaded from a file. See
[`textures-via-rlgl.md`](textures-via-rlgl.md).

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/texture-procedural.gif" width="80">](demos.md#texture-procedural) | `texture-procedural` | four textures built pixel by pixel |
| [<img src="../demos/texture-tiling.gif" width="80">](demos.md#texture-tiling) | `texture-tiling` | one tile repeated across the window |
| [<img src="../demos/render-texture.gif" width="80">](demos.md#render-texture) | `render-texture` | a scene drawn off-screen, then reused |
| [<img src="../demos/bunnymark.gif" width="80">](demos.md#bunnymark) | `bunnymark` | the sprite-count benchmark (click to add) |
| [<img src="../demos/mouse-painting.gif" width="80">](demos.md#mouse-painting) | `mouse-painting` | a paint program on a render-texture canvas |
| [<img src="../demos/particles-blending.gif" width="80">](demos.md#particles-blending) | `particles-blending` | 200 sparks trail the mouse, alpha vs additive |
| [<img src="../demos/polygon-drawing.gif" width="80">](demos.md#polygon-drawing) | `polygon-drawing` | hue-wheel texture on a spinning polygon |
| [<img src="../demos/srcrec-dstrec.gif" width="80">](demos.md#srcrec-dstrec) | `srcrec-dstrec` | srcrec picks the frame, dstrec scales+spins it |
| [<img src="../demos/blend-modes.gif" width="80">](demos.md#blend-modes) | `blend-modes` | four 2D blend modes over a night skyline |
| [<img src="../demos/screen-buffer.gif" width="80">](demos.md#screen-buffer) | `screen-buffer` | the classic DOS fire effect |
| [<img src="../demos/background-scrolling.gif" width="80">](demos.md#background-scrolling) | `background-scrolling` | three parallax skyline layers, each scrolling |
| [<img src="../demos/fog-of-war.gif" width="80">](demos.md#fog-of-war) | `fog-of-war` | fog lifted by a 25x15 render texture |
| [<img src="../demos/framebuffer-rendering.gif" width="80">](demos.md#framebuffer-rendering) | `framebuffer-rendering` | two cameras, two framebuffers, one scene |
| [<img src="../demos/image-generation.gif" width="80">](demos.md#image-generation) | `image-generation` | nine procedural textures, none of them loaded |
| [<img src="../demos/image-processing.gif" width="80">](demos.md#image-processing) | `image-processing` | nine CPU-side image operations, picked live |
| [<img src="../demos/image-kernel.gif" width="80">](demos.md#image-kernel) | `image-kernel` | sharpen, sobel and gaussian, one call each |
| *not recorded yet* | `image-drawing` | shapes baked once, then drawn live each frame |
| *not recorded yet* | `image-text` | text baked into the image, pixelates at 4x |
| *not recorded yet* | `image-rotate` | 0/90/180/270 exact, one angle grows the buffer |
| *not recorded yet* | `image-channel` | R/G/B/A split; alpha masked to show structure |
| [<img src="../demos/npatch-drawing.gif" width="80">](demos.md#npatch-drawing) | `npatch-drawing` | nine-patch stretching, corners held fixed |
| [<img src="../demos/sprite-button.gif" width="80">](demos.md#sprite-button) | `sprite-button` | one sheet, three states, sliced by v |
| *not recorded yet* | `sprite-stacking` | 40 generated slices faking a 3D car |
| *not recorded yet* | `to-image` | one image, VRAM to RAM to VRAM and back up |
| *not recorded yet* | `raw-data` | textures built from a hand-filled byte buffer |
| *not recorded yet* | `sprite-animation` | six generated poses, one source rectangle |

## shaders (16)

GLSL compiled at runtime and run over a full-screen quad. raylib's `LoadShader`
returns a `Shader` by value, which needed jolt's `[:by-value [:struct ...]]` -
so these were the first examples to put a hard jolt floor under the suite, at
0.7.23. The floor is higher now, 0.8.0, for an unrelated `ffi/write` change the
[README](../../README.md#jolt) explains. The source
lives as a string in each namespace rather than a `.glsl` file, so an example
stays one self-contained file.

That `[:by-value ...]` support also supersedes the workarounds described in
[`textures-via-rlgl.md`](textures-via-rlgl.md) and
[`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md); those
pages still describe what the suite currently does elsewhere. A page on the
by-value API itself is not written yet.

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/julia-set.gif" width="80">](demos.md#julia-set) | `julia-set` | the Julia set, mouse-steered, in a shader |
| [<img src="../demos/mandelbrot-set.gif" width="80">](demos.md#mandelbrot-set) | `mandelbrot-set` | the Mandelbrot set, zoomable, in a shader |
| [<img src="../demos/raymarching.gif" width="80">](demos.md#raymarching) | `raymarching` | a raymarched SDF scene in a shader |
| [<img src="../demos/rounded-rect-shader.gif" width="80">](demos.md#rounded-rect-shader) | `rounded-rect-shader` | SDF rounded rects: fill, border, shadow |
| [<img src="../demos/palette-switch.gif" width="80">](demos.md#palette-switch) | `palette-switch` | bands recolored by an ivec3 palette |
| [<img src="../demos/shader-hot-reload.gif" width="80">](demos.md#shader-hot-reload) | `shader-hot-reload` | swap and recompile the GLSL at runtime |
| [<img src="../demos/postprocessing.gif" width="80">](demos.md#postprocessing) | `postprocessing` | post-process shaders cycled over a scene |
| [<img src="../demos/custom-uniform.gif" width="80">](demos.md#custom-uniform) | `custom-uniform` | a mouse-steered swirl over a scene |
| [<img src="../demos/texture-painting.gif" width="80">](demos.md#texture-painting) | `texture-painting` | a blank texture painted by a shader |
| [<img src="../demos/multi-sampler.gif" width="80">](demos.md#multi-sampler) | `multi-sampler` | two textures blended by a second sampler |
| [<img src="../demos/eratosthenes-sieve.gif" width="80">](demos.md#eratosthenes-sieve) | `eratosthenes-sieve` | the Sieve of Eratosthenes, one test per pixel |
| [<img src="../demos/texture-rendering.gif" width="80">](demos.md#texture-rendering) | `texture-rendering` | a grid of squares painted entirely by a shader |
| [<img src="../demos/texture-waves.gif" width="80">](demos.md#texture-waves) | `texture-waves` | a starfield rippled by a UV-displacement shader |
| [<img src="../demos/color-correction.gif" width="80">](demos.md#color-correction) | `color-correction` | contrast/saturation/brightness shader |
| [<img src="../demos/texture-outline.gif" width="80">](demos.md#texture-outline) | `texture-outline` | a shader outline around a sprite's alpha edge |
| [<img src="../demos/ascii-rendering.gif" width="80">](demos.md#ascii-rendering) | `ascii-rendering` | ascii art from a post-process shader |

## audio (3)

raylib's raudio, in both directions. Two examples push, refilling a
caller-managed ring buffer every frame with `IsAudioStreamProcessed` /
`UpdateAudioStream`. The third lets raudio pull, from its own audio thread,
through a jolt callback. None of them loads a file, so there is no `LoadSound`
here yet.

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/audio-raw-stream.gif" width="80">](demos.md#audio-raw-stream) | `audio-raw-stream` | arrow keys steer a live sine tone's pitch/pan |
| [<img src="../demos/amp-envelope.gif" width="80">](demos.md#amp-envelope) | `amp-envelope` | ADSR amplitude envelope on a 440Hz tone |
| [<img src="../demos/audio-stream-callback.gif" width="80">](demos.md#audio-stream-callback) | `audio-stream-callback` | raudio pulls samples from its own thread |

## Adding an example: the five touchpoints

The suite is deliberately mechanical to grow. One new example touches five places:

1. **Source**: `src/net/b12n/raylib_jlt/<name>.clj`, a namespace with a `-main` that
   runs the canonical loop (see [`headless-smoke-testing.md`](headless-smoke-testing.md))
   against the `net.b12n.raylib.all` API.
2. **`deps.edn` alias**: `:<name> {:main-opts ["-m" "net.b12n.raylib-jlt.<name>"]}` so
   `jolt -M:<name>` works.
3. **`check.clj` require**: add `net.b12n.raylib-jlt.<name>` to the `:require` list in
   `net.b12n.raylib-jlt.check`, so the headless compile-check covers it.
4. **Registry row**: add `["<display-name>" "<alias>" "<group>" "<desc>"]` to the
   `examples` vector in [`scripts/examples_registry.clj`](../../scripts/examples_registry.clj),
   the single source of truth that `bb.edn` and the demo recorder both read.
5. **`bb.edn` task**: add a matching `<name> {:doc "..." :task (run-example "<name>")}`
   row, so `bb <name>` works alongside `bb info` / `bb examples` / `bb run-all`.

`bb check:registration` verifies all five without needing jolt or libraylib, and
CI runs it before the compile gate. Touchpoint 3 is why it exists: `bb check`
compiles what `check.clj` requires, so an example missing from that list is
never compiled and the run still reports success.

Note that the alias is not always the display name, and the namespace is
derivable from neither. `["basic-window" "run" ...]` reaches
`net.b12n.raylib-jlt.core` through the `:run` alias, and twelve rows differ this
way, so `deps.edn` is the source of truth for which namespace a row means.

A new group needs two more edits: the group list in `bb.edn`'s `info` task and the
`:groups` vector in `scripts/demo_manifest.edn`.

```mermaid
flowchart LR
  src["src/…/<name>.clj<br/>the example"] --> deps["deps.edn<br/>:<name> alias"]
  src --> chk["check.clj<br/>require (headless compile)"]
  src --> reg["scripts/examples_registry.clj<br/>registry row"]
  reg --> bb["bb.edn<br/>bb &lt;name&gt; task"]
```

Filenames use underscores (`basic_screen_manager.clj`); the namespace uses hyphens
(`net.b12n.raylib-jlt.basic-screen-manager`), Clojure's standard file↔ns mapping.

One ordering rule applies inside every file: since **jolt 0.4.0** an unresolved
symbol is a compile error rather than a late-bound reference, so a definition must
appear before its first use, in a fn body and in an `:or` destructuring default
just as much as at top level. It bites hardest in the library's modules under
`lib/src/net/b12n/raylib/`, where one misordered symbol stops that module
compiling and, because every example requires `net.b12n.raylib.all`, *every*
example along with it. Only the first offender is reported. `bb check:lib`
and `jolt -M:check` are the quick confirmation; see the
[jolt note in the README](../../README.md#requirements).

## See also

- [`kwarg-drawing-api.md`](kwarg-drawing-api.md): the API every example is written
  against.
- [`headless-smoke-testing.md`](headless-smoke-testing.md): the loop shape and how
  `bb run-all` / `bb check` verify the catalog.
