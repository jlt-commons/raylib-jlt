# Full-size demo gallery

Every recorded example at full size, linked from
[the example catalog](example-catalog.md)'s preview thumbnails. All 146 recordings
are here, covering 146 of the suite's 149 examples: `helitorus`, `doom` and
`pacman` have not been recorded yet, so run them with `bb helitorus`, `bb doom`
or `bb pacman`.

143 are animated GIFs. Three are still frames. `rectangle-advanced` and
`rlgl-triangle` draw something fixed and only move when you drive them, so a
recording of either would be a valid animation of one repeated image, and a still
is the honest form for those two. `camera-2d-split-screen` needs a player moving
to show anything at all, and the capture tool's simulated key presses aren't
landing for it yet, so a still stands in until that's sorted.

## games (10)

### asteroids

the classic vector shooter (rotate/thrust/fire)

![asteroids](../demos/asteroids.gif)

### tetris

the block-stacking puzzle (move/rotate/drop)

![tetris](../demos/tetris.gif)

### pong

two-paddle classic, you (W/S) vs a CPU

![pong](../demos/pong.gif)

### vampire-survivors

auto-fire survival: move, waves chase you

![vampire-survivors](../demos/vampire-survivors.gif)

### snake

the classic snake (arrow keys, grow, don't crash)

![snake](../demos/snake.gif)

### breakout

paddle + ball + brick grid (mouse paddle)

![breakout](../demos/breakout.gif)

### space-invaders

marching aliens (arrows + SPACE to shoot)

![space-invaders](../demos/space-invaders.gif)

### flappy-bird

flap through the pipe gaps (SPACE)

![flappy-bird](../demos/flappy-bird.gif)

### game-2048

2048: 4x4 tile-merge puzzle (arrow keys)

![game-2048](../demos/game-2048.gif)

### minesweeper

reveal/flag grid (mouse L reveal, R flag)

![minesweeper](../demos/minesweeper.gif)

## core (30)

### basic-window

the minimal raylib window + text

![basic-window](../demos/basic-window.gif)

### input-keys

steer a ball with the arrow keys

![input-keys](../demos/input-keys.gif)

### input-mouse

a ball follows the mouse; click to recolor

![input-mouse](../demos/input-mouse.gif)

### input-mouse-wheel

scroll a box with the mouse wheel

![input-mouse-wheel](../demos/input-mouse-wheel.gif)

### camera-2d

a 2D camera over a skyline (struct-by-value)

![camera-2d](../demos/camera-2d.gif)

### delta-time

per-frame vs delta-time movement

![delta-time](../demos/delta-time.gif)

### scissor-test

a scissor rectangle clips a grid

![scissor-test](../demos/scissor-test.gif)

### basic-screen-manager

a LOGO/TITLE/GAMEPLAY/ENDING flow

![basic-screen-manager](../demos/basic-screen-manager.gif)

### random-values

a new random value every two seconds

![random-values](../demos/random-values.gif)

### window-letterbox

a fixed picture letterboxed into the window

![window-letterbox](../demos/window-letterbox.gif)

### window-flags

toggle vsync/resizable/topmost live

![window-flags](../demos/window-flags.gif)

### monitor-detector

every attached display, current one lit

![monitor-detector](../demos/monitor-detector.gif)

### clipboard-text

type, C copies, V pastes

![clipboard-text](../demos/clipboard-text.gif)

### input-gamepad

sticks, triggers and buttons for pad 0

![input-gamepad](../demos/input-gamepad.gif)

### input-multitouch

touch points (the mouse is point 0)

![input-multitouch](../demos/input-multitouch.gif)

### input-virtual-controls

an on-screen D-pad and action button

![input-virtual-controls](../demos/input-virtual-controls.gif)

### undo-redo

a bounded history, CTRL-Z and CTRL-Y walking it

![undo-redo](../demos/undo-redo.gif)

### window-should-close

`SetExitKey` so a close request can be answered

![window-should-close](../demos/window-should-close.gif)

### input-gestures

`GetGestureDetected`, named as they happen

![input-gestures](../demos/input-gestures.gif)

### random-sequence

a shuffled sequence, each height used once

![random-sequence](../demos/random-sequence.gif)

### camera-2d-mouse-zoom

zoom pinned to the point under the cursor

![camera-2d-mouse-zoom](../demos/camera-2d-mouse-zoom.gif)

### storage-values

values that survive a restart, via a file

![storage-values](../demos/storage-values.gif)

### camera-2d-platformer

five ways a camera can follow a jumping player

![camera-2d-platformer](../demos/camera-2d-platformer.gif)

### camera-2d-split-screen

two players, two cameras, two render textures

![camera-2d-split-screen](../demos/camera-2d-split-screen.png)

### keyboard-testbed

an on-screen ENG-US keyboard, every key lit

![keyboard-testbed](../demos/keyboard-testbed.gif)

### input-actions

abstract input actions: keyboard + gamepad

![input-actions](../demos/input-actions.gif)

### smooth-pixelperfect

sub-pixel camera smoothing at 5x upscale

![smooth-pixelperfect](../demos/smooth-pixelperfect.gif)

### viewport-scaling

6 viewport-scaling policies, resize live

![viewport-scaling](../demos/viewport-scaling.gif)

### highdpi-testbed

diagnostic overlay: monitors, DPI, crosshair

![highdpi-testbed](../demos/highdpi-testbed.gif)

### compute-hash

CRC32/MD5/SHA1/SHA256 + Base64 of typed text

![compute-hash](../demos/compute-hash.gif)

## shapes (43)

### bouncing-ball

a ball bouncing around the window

![bouncing-ball](../demos/bouncing-ball.gif)

### basic-shapes

shape primitives + an rlgl triangle

![basic-shapes](../demos/basic-shapes.gif)

### colors-palette

every named raylib color in a grid

![colors-palette](../demos/colors-palette.gif)

### gradient

a vertical two-color gradient

![gradient](../demos/gradient.gif)

### following-eyes

two eyes track the mouse

![following-eyes](../demos/following-eyes.gif)

### starfield

a twinkling starfield

![starfield](../demos/starfield.gif)

### logo-raylib

the raylib logo from rectangles + text

![logo-raylib](../demos/logo-raylib.gif)

### mouse-trail

a fading trail follows the cursor

![mouse-trail](../demos/mouse-trail.gif)

### recursive-tree

a binary fractal tree

![recursive-tree](../demos/recursive-tree.gif)

### math-sine-cosine

a live unit-circle trig visualization

![math-sine-cosine](../demos/math-sine-cosine.gif)

### bullet-hell

a rotating bullet spiral

![bullet-hell](../demos/bullet-hell.gif)

### triangle-strip

a rainbow strip via rlgl immediate mode

![triangle-strip](../demos/triangle-strip.gif)

### collision-area

AABB collision between two boxes

![collision-area](../demos/collision-area.gif)

### dashed-line

a dashed line follows the mouse

![dashed-line](../demos/dashed-line.gif)

### double-pendulum

chaotic double-pendulum motion + trail

![double-pendulum](../demos/double-pendulum.gif)

### kaleidoscope

strokes mirrored with 6-fold symmetry

![kaleidoscope](../demos/kaleidoscope.gif)

### hilbert-curve

a rainbow Hilbert space-filling curve

![hilbert-curve](../demos/hilbert-curve.gif)

### math-angle-rotation

fixed spokes + a spinning line

![math-angle-rotation](../demos/math-angle-rotation.gif)

### ball-physics

2D balls under gravity, SPACE respawns

![ball-physics](../demos/ball-physics.gif)

### lines-bezier

a cubic Bézier that follows the mouse

![lines-bezier](../demos/lines-bezier.gif)

### color-wheel

an HSV color wheel (rlgl triangle fan)

![color-wheel](../demos/color-wheel.gif)

### pie-chart

labelled pie slices via rl/sector!

![pie-chart](../demos/pie-chart.gif)

### splines

Catmull-Rom / Bezier / B-spline (SPACE cycles)

![splines](../demos/splines.gif)

### vector-angle

the angle between two vectors (arc + readout)

![vector-angle](../demos/vector-angle.gif)

### easings

a grid of balls, each on a different easing curve

![easings](../demos/easings.gif)

### penrose-tiling

a P3 Penrose rhombus tiling (deflation)

![penrose-tiling](../demos/penrose-tiling.gif)

### analog-clock

a live analog clock (libc local time)

![analog-clock](../demos/analog-clock.gif)

### digital-clock

a seven-segment HH:MM:SS clock (libc time)

![digital-clock](../demos/digital-clock.gif)

### ring-drawing

an animated annulus via rl/ring!

![ring-drawing](../demos/ring-drawing.gif)

### rounded-rectangle

rounded rects via sector! corners

![rounded-rectangle](../demos/rounded-rectangle.gif)

### rectangle-scaling

drag the corner handle to resize a rect

![rectangle-scaling](../demos/rectangle-scaling.gif)

### lines-drawing

a rotating fan of thick lines (line-ex!)

![lines-drawing](../demos/lines-drawing.gif)

### ellipse-collision

two ellipses reddening when they overlap

![ellipse-collision](../demos/ellipse-collision.gif)

### rlgl-triangle

per-vertex colour interpolated across a face

![rlgl-triangle](../demos/rlgl-triangle.png)

### rlgl-color-wheel

a hue wheel as a triangle fan

![rlgl-color-wheel](../demos/rlgl-color-wheel.gif)

### circle-sector-drawing

a sector starved of segments

![circle-sector-drawing](../demos/circle-sector-drawing.gif)

### easings-rectangles

size and rotation on one easing curve

![easings-rectangles](../demos/easings-rectangles.gif)

### easings-ball

slide, swell and fade, one curve each

![easings-ball](../demos/easings-ball.gif)

### easings-box

drop, flatten, spin, grow, fade: five curves

![easings-box](../demos/easings-box.gif)

### logo-anim

the raylib logo assembling itself, unsmoothed

![logo-anim](../demos/logo-anim.gif)

### rectangle-advanced

per-side roundness with a horizontal gradient

![rectangle-advanced](../demos/rectangle-advanced.png)

### easings-testbed

one curve at a time, plotted and run

![easings-testbed](../demos/easings-testbed.gif)

### starfield-effect

flying starfield (wheel=speed, SPACE=mode)

![starfield-effect](../demos/starfield-effect.gif)

## text (6)

### font-sizes

font sizes + MeasureText centering

![font-sizes](../demos/font-sizes.gif)

### writing-anim

a message types itself out

![writing-anim](../demos/writing-anim.gif)

### format-text

padded score + MM:SS timer readouts

![format-text](../demos/format-text.gif)

### words-alignment

align a word inside a box (MeasureText)

![words-alignment](../demos/words-alignment.gif)

### input-box

type into a text box (GetCharPressed)

![input-box](../demos/input-box.gif)

### rectangle-bounds

draggable word-wrap text container

![rectangle-bounds](../demos/rectangle-bounds.gif)

## 3d (20)

### camera-3d

an orbiting 3D camera (Camera3D by value)

![camera-3d](../demos/camera-3d.gif)

### waving-cubes

an NxN grid of cubes rippling in 3D

![waving-cubes](../demos/waving-cubes.gif)

### camera-3d-first-person

walk a yard of columns in first person

![camera-3d-first-person](../demos/camera-3d-first-person.gif)

### tesseract-view

a rotating 4D hypercube projected to 2D

![tesseract-view](../demos/tesseract-view.gif)

### wireframe-shapes

pyramid/octahedron/torus/helix in 3D lines

![wireframe-shapes](../demos/wireframe-shapes.gif)

### rlgl-solar-system

Sun/Earth/Moon via the rlgl matrix stack

![rlgl-solar-system](../demos/rlgl-solar-system.gif)

### box-collisions

a player cube colliding with 3D boxes

![box-collisions](../demos/box-collisions.gif)

### rotating-cube

a single cube spinning via the rlgl matrix stack

![rotating-cube](../demos/rotating-cube.gif)

### spinning-cubes

a row of cubes each spinning with a phase offset

![spinning-cubes](../demos/spinning-cubes.gif)

### orthographic-projection

perspective vs orthographic (SPACE toggles)

![orthographic-projection](../demos/orthographic-projection.gif)

### point-cloud

~1500 points as tiny rlgl cubes, rotating

![point-cloud](../demos/point-cloud.gif)

### bouncing-spheres

spheres bouncing in a 3D box (rl/sphere!)

![bouncing-spheres](../demos/bouncing-spheres.gif)

### lorenz-attractor

the Lorenz attractor traced in 3D

![lorenz-attractor](../demos/lorenz-attractor.gif)

### dna-helix

a turning double helix, coloured bases

![dna-helix](../demos/dna-helix.gif)

### yaw-pitch-roll

the three aircraft rotations in 3D

![yaw-pitch-roll](../demos/yaw-pitch-roll.gif)

### first-person-maze

walk a grid maze, with a minimap

![first-person-maze](../demos/first-person-maze.gif)

### world-screen

a 2D label tracks a cube via GetWorldToScreen

![world-screen](../demos/world-screen.gif)

### geometric-shapes

cubes, spheres, cylinders, cones, capsules

![geometric-shapes](../demos/geometric-shapes.gif)

### camera-3d-split-screen

two players, two render-texture halves

![camera-3d-split-screen](../demos/camera-3d-split-screen.gif)

### camera-3d-free

a free-look camera around a cube, UpdateCamera

![camera-3d-free](../demos/camera-3d-free.gif)

## generative (10)

### game-of-life

Conway's Game of Life (SPACE reseeds)

![game-of-life](../demos/game-of-life.gif)

### boids

flocking birds (separation/alignment/cohesion)

![boids](../demos/boids.gif)

### fireworks

rockets + fading particle bursts

![fireworks](../demos/fireworks.gif)

### fourier-epicycles

rotating circles trace a square wave

![fourier-epicycles](../demos/fourier-epicycles.gif)

### spirograph

animated hypotrochoid roulette curves

![spirograph](../demos/spirograph.gif)

### l-system

an L-system fractal plant (grows + regrows)

![l-system](../demos/l-system.gif)

### flow-field

particles steered by a flow field (trails)

![flow-field](../demos/flow-field.gif)

### cellular-automata

Wolfram's elementary automata

![cellular-automata](../demos/cellular-automata.gif)

### clock-of-clocks

six digits spelled by a grid of clock hands

![clock-of-clocks](../demos/clock-of-clocks.gif)

### particles

water/smoke/fire particles follow the mouse

![particles](../demos/particles.gif)

## textures (11)

### texture-procedural

four textures built pixel by pixel

![texture-procedural](../demos/texture-procedural.gif)

### texture-tiling

one tile repeated across the window

![texture-tiling](../demos/texture-tiling.gif)

### render-texture

a scene drawn off-screen, then reused

![render-texture](../demos/render-texture.gif)

### bunnymark

the sprite-count benchmark (click to add)

![bunnymark](../demos/bunnymark.gif)

### mouse-painting

a paint program on a render-texture canvas

![mouse-painting](../demos/mouse-painting.gif)

### particles-blending

200 sparks trail the mouse, alpha vs additive

![particles-blending](../demos/particles-blending.gif)

### polygon-drawing

hue-wheel texture on a spinning polygon

![polygon-drawing](../demos/polygon-drawing.gif)

### srcrec-dstrec

srcrec picks the frame, dstrec scales+spins it

![srcrec-dstrec](../demos/srcrec-dstrec.gif)

### blend-modes

four 2D blend modes over a night skyline

![blend-modes](../demos/blend-modes.gif)

### screen-buffer

the classic DOS fire effect

![screen-buffer](../demos/screen-buffer.gif)

### background-scrolling

three parallax skyline layers, each scrolling

![background-scrolling](../demos/background-scrolling.gif)

## shaders (14)

### julia-set

the Julia set, mouse-steered, in a shader

![julia-set](../demos/julia-set.gif)

### mandelbrot-set

the Mandelbrot set, zoomable, in a shader

![mandelbrot-set](../demos/mandelbrot-set.gif)

### raymarching

a raymarched SDF scene in a shader

![raymarching](../demos/raymarching.gif)

### rounded-rect-shader

SDF rounded rects: fill, border, shadow

![rounded-rect-shader](../demos/rounded-rect-shader.gif)

### palette-switch

bands recolored by an ivec3 palette

![palette-switch](../demos/palette-switch.gif)

### shader-hot-reload

swap and recompile the GLSL at runtime

![shader-hot-reload](../demos/shader-hot-reload.gif)

### postprocessing

post-process shaders cycled over a scene

![postprocessing](../demos/postprocessing.gif)

### custom-uniform

a mouse-steered swirl over a scene

![custom-uniform](../demos/custom-uniform.gif)

### texture-painting

a blank texture painted by a shader

![texture-painting](../demos/texture-painting.gif)

### multi-sampler

two textures blended by a second sampler

![multi-sampler](../demos/multi-sampler.gif)

### eratosthenes-sieve

the Sieve of Eratosthenes, one test per pixel

![eratosthenes-sieve](../demos/eratosthenes-sieve.gif)

### texture-rendering

a grid of squares painted entirely by a shader

![texture-rendering](../demos/texture-rendering.gif)

### texture-waves

a starfield rippled by a UV-displacement shader

![texture-waves](../demos/texture-waves.gif)

### color-correction

contrast/saturation/brightness shader

![color-correction](../demos/color-correction.gif)

## audio (2)

### audio-raw-stream

arrow keys steer a live sine tone's pitch/pan

![audio-raw-stream](../demos/audio-raw-stream.gif)

### amp-envelope

ADSR amplitude envelope on a 440Hz tone

![amp-envelope](../demos/amp-envelope.gif)
