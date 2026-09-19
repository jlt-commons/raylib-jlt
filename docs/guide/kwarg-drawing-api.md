# The keyword-argument drawing API

raylib's C functions are positional and often long: `DrawText(text, x, y, fontSize,
color)`, `DrawRectangle(x, y, width, height, color)`. Bound literally, example code
becomes a wall of bare numbers where the fifth argument's meaning is anyone's guess.
This repo keeps the raw binds positional (they mirror C) and layers an ergonomic
**keyword-argument** API on top, so example call sites read as self-describing.

## Two layers, one boundary

The FFI boundary stays a faithful, positional mirror of the C signature: that's the
contract with the library and the thing to check against `raylib.h`:

```clojure
;; net.b12n.raylib.text / net.b12n.raylib.shapes, the FFI boundary (positional, mirrors C)
(ffi/defcfn draw-text      "DrawText"      [:string :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle "DrawRectangle" [:int :int :int :int :uint] :void)
(ffi/defcfn draw-circle    "DrawCircle"    [:int :int :float :uint] :void)
```

On top sits a thin wrapper per call that names the arguments and supplies defaults:

```clojure
(defn text!
  "DrawText. :x :y :size :color."
  [s & {:keys [x y size color] :or {x 0 y 0 size 20 color BLACK}}]
  (draw-text s x y size color))

(defn rect!
  "DrawRectangle. :x :y :width :height :color."
  [& {:keys [x y width height color] :or {x 0 y 0 width 10 height 10 color BLACK}}]
  (draw-rectangle x y width height color))

(defn circle!
  "DrawCircle. :x :y :radius :color."
  [& {:keys [x y radius color] :or {x 0 y 0 radius 10 color BLACK}}]
  (draw-circle x y (double radius) color))
```

The wrappers also absorb small coercions the raw bind is strict about, e.g.
`circle!` calls `(double radius)` so a caller can pass an int radius without a type
error at the `:float` boundary.

The payoff at the call site (`net.b12n.raylib-jlt.core`):

```clojure
(rl/text! "Congrats! You created your first window!"
          :x 190 :y 200 :size 20 :color rl/LIGHTGRAY)
;; vs. the positional (draw-text "…" 190 200 20 rl/LIGHTGRAY)
```

The `!` suffix marks these as side-effecting calls. Sixteen wrappers exist, in three
groups:

- **Window + 2D primitives, straight over a scalar bind**: `window!`, `text!`,
  `fps!`, `rect!`, `rect-lines!`, `rect-gradient!`, `circle!`, `circle-lines!`,
  `ellipse!`, `line!`, `pixel!`.
- **2D shapes rlgl has to draw**: `sector!`, `ring!`, `line-ex!`. Their raylib
  originals (`DrawCircleSector`, `DrawRing`, `DrawLineEx`) take a `Vector2` by
  value and are unbindable, so these emit triangles instead. Same keyword-arg
  surface; see [`rlgl-immediate-mode.md`](rlgl-immediate-mode.md).
- **3D**: `cube!` and `sphere!`, likewise rlgl vertex streams standing in for the
  by-value-`Vector3` `DrawCube` / `DrawSphere`.

(`rl-color!` and `maybe-screenshot!` also end in `!` but are not part of this
wrapper layer, one unpacks a `Color` for rlgl, the other is smoke-test plumbing.)

## The style convention: >3 arguments → keyword args

The rule the whole suite follows:

> **A function with more than three arguments takes keyword args** (or groups
> scalars into vectors to get to ≤3). Raw `ffi/defcfn` binds stay positional because
> they mirror C; recursive math kernels may stay positional with grouped-vector
> args.

`DrawText` has five arguments → `text!` is keyword. `cube!` would have a long
positional list (`pos`, `size`, `color`) → keyword args, and `pos`/`size` are
themselves grouped `[x y z]` vectors so each stays one argument:

```clojure
(defn cube! [& {:keys [pos size color] :or {pos [0.0 0.0 0.0] size 1.0 color BLACK}}]
  …)

(rl/cube! :pos [x 0 z] :size 0.8 :color rl/BLUE)
```

The camera wrappers take a single map argument for the same reason: a `Camera2D`
has six scalars, well past three:

```clojure
(rl/with-camera-2d {:offset-x 400 :offset-y 225 :target-x px :target-y py :zoom 1.5}
  (fn [] …))
```

## Why not just bind everything keyword?

Because the FFI boundary should stay a **1:1, positional mirror of the C signature**:
that's what you diff against the header when a call misbehaves, and what keeps the
"is this binding correct?" question answerable. Keyword ergonomics are a *caller*
concern, so they live in a caller-facing layer above the boundary, never in the
`ffi/defcfn` itself. Same reason `rgba` (packing) and `rl-color!` (unpacking) wrap
the raw `:uint`/`rlColor4ub` binds rather than replacing them.

## See also

- [`color-by-value.md`](color-by-value.md): the packed-`Color` `:uint` the draw
  wrappers pass through unchanged.
- [`rlgl-immediate-mode.md`](rlgl-immediate-mode.md): `cube!` is the keyword-arg
  wrapper over the positional `rl-vertex-3f` stream.
- [`example-catalog.md`](example-catalog.md): every example is written against this
  wrapper API.
