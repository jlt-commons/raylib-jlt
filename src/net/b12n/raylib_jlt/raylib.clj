(ns net.b12n.raylib-jlt.raylib
  "Shared jolt.ffi bindings for raylib, the surface used by the example programs
  in this project (net.b12n.raylib-jlt.core, net.b12n.raylib-jlt.input, net.b12n.raylib-jlt.bounce, net.b12n.raylib-jlt.colors, net.b12n.raylib-jlt.mouse,
  net.b12n.raylib-jlt.wheel, net.b12n.raylib-jlt.shapes, net.b12n.raylib-jlt.text, net.b12n.raylib-jlt.logo, net.b12n.raylib-jlt.gradient, net.b12n.raylib-jlt.stars, net.b12n.raylib-jlt.camera2d).

  raylib is the upstream C game library (raysan5/raylib); jolt calls it directly
  over its C ABI. The system libraylib is declared as a :jolt/native lib in
  deps.edn. Almost every call here uses raylib's scalar-argument variants, so the
  only by-value struct that crosses the FFI boundary is `Color`, a 4-byte
  {u8 r,g,b,a} packed into a :uint (see `rgba` and README.md). The one exception,
  Camera2D (24 bytes, passed by pointer), lives in net.b12n.raylib-jlt.camera2d, not here."
  ;; run! is the REPL entry point (see the bottom of this file); the shadowed
  ;; clojure.core/run! is a reducing form this suite never uses.
  (:refer-clojure :exclude [run!])
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.color :as color]
   [net.b12n.raylib.core :as core]
   [net.b12n.raylib.files :as files]
   [net.b12n.raylib.images :as images]
   [net.b12n.raylib.input :as input]
   [net.b12n.raylib.log :as log]
   [net.b12n.raylib.native :as native]
   [net.b12n.raylib.rlgl :as rlgl]
   [net.b12n.raylib.shapes :as shapes]
   [net.b12n.raylib.text :as text]
   [net.b12n.raylib.textures :as textures]
   [net.b12n.raylib.util :as util]))

;; --- Color -------------------------------------------------------------------
;; Moved to net.b12n.raylib.color. Re-exported here so every example that says
;; rl/rgba or rl/RAYWHITE keeps working unchanged. This whole file becomes
;; aliases like these, and is then replaced by the generated
;; net.b12n.raylib.all.
(def rgba color/rgba)

(def LIGHTGRAY color/LIGHTGRAY)   (def GRAY       color/GRAY)
(def DARKGRAY  color/DARKGRAY)    (def YELLOW     color/YELLOW)
(def GOLD      color/GOLD)        (def ORANGE     color/ORANGE)
(def PINK      color/PINK)        (def RED        color/RED)
(def MAROON    color/MAROON)      (def GREEN      color/GREEN)
(def LIME      color/LIME)        (def DARKGREEN  color/DARKGREEN)
(def SKYBLUE   color/SKYBLUE)     (def BLUE       color/BLUE)
(def DARKBLUE  color/DARKBLUE)    (def PURPLE     color/PURPLE)
(def VIOLET    color/VIOLET)      (def DARKPURPLE color/DARKPURPLE)
(def BEIGE     color/BEIGE)       (def BROWN      color/BROWN)
(def DARKBROWN color/DARKBROWN)   (def WHITE      color/WHITE)
(def BLACK     color/BLACK)       (def MAGENTA    color/MAGENTA)
(def RAYWHITE  color/RAYWHITE)

;; --- window / lifecycle ------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/init-window or rl/close-window keeps working unchanged.
(def init-window core/init-window)
(def set-target-fps core/set-target-fps)
(def set-exit-key core/set-exit-key)
(def close-window core/close-window)

;; --- frame -------------------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/begin-drawing or rl/clear-background keeps working unchanged.
(def begin-drawing core/begin-drawing)
(def end-drawing core/end-drawing)
(def clear-background core/clear-background)
(def get-frame-time core/get-frame-time)
(def begin-scissor-mode core/begin-scissor-mode)
(def end-scissor-mode core/end-scissor-mode)

;; --- 2D shapes + text ----------------------------------------------------------
;; text moved to net.b12n.raylib.text, shapes to net.b12n.raylib.shapes.
;; Re-exported here so every example that says rl/draw-text or rl/draw-circle
;; keeps working unchanged.
(def draw-text text/draw-text)
(def draw-fps text/draw-fps)
(def measure-text text/measure-text)
(def draw-pixel shapes/draw-pixel)
(def draw-line shapes/draw-line)
(def draw-rectangle shapes/draw-rectangle)
(def draw-rectangle-lines shapes/draw-rectangle-lines)
(def draw-rectangle-grad-v shapes/draw-rectangle-grad-v)
;; #region draw-circle-binding
(def draw-circle shapes/draw-circle)
;; #endregion
(def draw-circle-lines shapes/draw-circle-lines)
(def draw-ellipse shapes/draw-ellipse)

;; --- rlgl immediate mode (all scalar), for triangles / points ---------------
;; Moved to net.b12n.raylib.rlgl. Re-exported here so every example that says
;; rl/rl-begin or rl/rl-color! keeps working unchanged.
(def rl-begin rlgl/rl-begin)
(def rl-end rlgl/rl-end)
(def rl-vertex-2f rlgl/rl-vertex-2f)
(def rl-color-4ub rlgl/rl-color-4ub)
(def RL-LINES rlgl/RL-LINES)
(def RL-TRIANGLES rlgl/RL-TRIANGLES)
(def rl-color! rlgl/rl-color!)

;; #region camera2d-by-value
;; --- Camera2D: a struct passed BY VALUE (the one non-Color by-value struct) ---
;; raylib's BeginMode2D(Camera2D) takes {Vector2 offset; Vector2 target; float
;; rotation; float zoom}, 24 bytes, passed by value. On the AArch64 (Apple) ABI a
;; composite larger than 16 bytes is passed INDIRECTLY: the caller allocates a
;; copy and passes a POINTER to it, so the binding is [:pointer] and we build the
;; struct (six little-endian floats) in native memory. NOTE: this is AArch64-
;; specific: on the x86-64 SysV ABI the 24 bytes are passed on the stack, which
;; a [:pointer] binding does NOT do (see README). For a portable alternative,
;; apply the same transform with the scalar rlgl matrix ops instead.
(ffi/defcfn ^:private begin-mode-2d-ptr "BeginMode2D" [:pointer] :void)
;; #endregion
(ffi/defcfn end-mode-2d "EndMode2D" [] :void)

(defn with-camera-2d
  "Run (f) with a Camera2D active. Allocates the 24-byte struct, writes the six
  floats (offset.x, offset.y, target.x, target.y, rotation, zoom), passes a
  pointer to BeginMode2D, runs f, then EndMode2D and frees. See the ABI note above."
  [{:keys [offset-x offset-y target-x target-y rotation zoom]
    :or {offset-x 0
         offset-y 0
         target-x 0
         target-y 0
         rotation 0
         zoom 1.0}} f]
  (let [p (ffi/alloc 24)]
    (try
      (ffi/write p :float (double offset-x) 0)
      (ffi/write p :float (double offset-y) 4)
      (ffi/write p :float (double target-x) 8)
      (ffi/write p :float (double target-y) 12)
      (ffi/write p :float (double rotation) 16)
      (ffi/write p :float (double zoom) 20)
      (begin-mode-2d-ptr p)
      (f)
      (end-mode-2d)
      (finally (ffi/free p)))))

;; --- Camera3D + 3D geometry --------------------------------------------------
;; Camera3D is 44 bytes (three Vector3 + a float + an int), passed BY VALUE to
;; BeginMode3D, the same >16-byte-struct-by-pointer approach as Camera2D. 3D
;; shape helpers like DrawCube take a Vector3 BY VALUE (a 12-byte float struct
;; passed in FP registers, which the pointer trick does NOT cover), so draw 3D
;; geometry with rlgl immediate mode (rl-vertex-3f) instead. DrawGrid is scalar.
(ffi/defcfn draw-grid    "DrawGrid"    [:int :float] :void)
(ffi/defcfn ^:private rl-vertex-3f-raw "rlVertex3f"  [:float :float :float] :void)

(defn rl-vertex-3f
  "One vertex in 3D.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-vertex-3f-raw (double a0) (double a1) (double a2)))

(ffi/defcfn begin-mode-3d-ptr "BeginMode3D" [:pointer] :void)
(ffi/defcfn end-mode-3d "EndMode3D" [] :void)

;; rlgl matrix stack, nested transforms for immediate-mode geometry. rlgl applies
;; the current transform to each rlVertex* at submit time, so push/rotate/translate
;; around a cube! call moves it (used by rlgl-solar-system).
(ffi/defcfn rl-push-matrix "rlPushMatrix" [] :void)
(ffi/defcfn rl-pop-matrix  "rlPopMatrix"  [] :void)
(ffi/defcfn ^:private rl-translatef-raw  "rlTranslatef" [:float :float :float] :void)

(defn rl-translatef
  "Translate the current matrix.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-translatef-raw (double a0) (double a1) (double a2)))

(ffi/defcfn ^:private rl-rotatef-raw     "rlRotatef"    [:float :float :float :float] :void)

(defn rl-rotatef
  "Rotate the current matrix, angle first.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2 a3]
  (rl-rotatef-raw (double a0) (double a1) (double a2) (double a3)))

(ffi/defcfn ^:private rl-scalef-raw      "rlScalef"     [:float :float :float] :void)

(defn rl-scalef
  "Scale the current matrix.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-scalef-raw (double a0) (double a1) (double a2)))

(defn with-camera-3d
  "Run (f) with a Camera3D active (BeginMode3D → f → EndMode3D). Builds the
  44-byte struct in native memory (nine floats + fovy + projection int) and passes
  a pointer. Keys: :pos-x/y/z :target-x/y/z :up-x/y/z :fovy :projection (0 =
  perspective). See the ABI note above."
  [{:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z fovy projection]
    :or {pos-x 0
         pos-y 0
         pos-z 0
         target-x 0
         target-y 0
         target-z 0
         up-x 0
         up-y 1
         up-z 0
         fovy 45
         projection 0}} f]
  (let [p (ffi/alloc 44)]
    (try
      (ffi/write p :float (double pos-x) 0)
      (ffi/write p :float (double pos-y) 4)
      (ffi/write p :float (double pos-z) 8)
      (ffi/write p :float (double target-x) 12)
      (ffi/write p :float (double target-y) 16)
      (ffi/write p :float (double target-z) 20)
      (ffi/write p :float (double up-x) 24)
      (ffi/write p :float (double up-y) 28)
      (ffi/write p :float (double up-z) 32)
      (ffi/write p :float (double fovy) 36)
      (ffi/write p :int (int projection) 40)
      (begin-mode-3d-ptr p)
      (f)
      (end-mode-3d)
      (finally (ffi/free p)))))

(defn- shade-color
  "Darken a packed Color by factor f (fakes lighting so cube faces read as 3D)."
  [color f]
  (rgba (int (* f (bit-and color 0xff)))
        (int (* f (bit-and (bit-shift-right color 8) 0xff)))
        (int (* f (bit-and (bit-shift-right color 16) 0xff)))
        255))

(defn- quad-3f
  "Two rlgl triangles for a quad, given a shaded color and a vector of its four
  [x y z] corners in a→b→c→d winding order."
  [color [a b c d]]
  (rl-color! color)
  (let [[ax ay az] a [bx by bz] b [cx cy cz] c [dx dy dz] d]
    (rl-vertex-3f ax ay az) (rl-vertex-3f bx by bz) (rl-vertex-3f cx cy cz)
    (rl-vertex-3f ax ay az) (rl-vertex-3f cx cy cz) (rl-vertex-3f dx dy dz)))

(defn cube!
  "Draw an axis-aligned box via rlgl immediate mode, its faces shaded from the
  packed `:color` for depth. Must be called inside a BeginMode3D block (see
  with-camera-3d). Keyword args:
    :pos   [x y z] centre           (default [0 0 0])
    :size  a number for a uniform cube, or [sx sy sz]  (default 1)
    :color a packed Color           (default BLACK)"
  [& {:keys [pos size color]
      :or {pos [0.0 0.0 0.0]
           size 1.0
           color BLACK}}]
  (let [[cx cy cz] pos
        [sx sy sz] (if (number? size) [size size size] size)
        hx (/ sx 2.0) hy (/ sy 2.0) hz (/ sz 2.0)
        x0 (- cx hx) x1 (+ cx hx) y0 (- cy hy) y1 (+ cy hy) z0 (- cz hz) z1 (+ cz hz)
        ;; the eight corners, named a<x><y><z> by which extreme each axis takes
        a000 [x0 y0 z0] a100 [x1 y0 z0] a010 [x0 y1 z0] a110 [x1 y1 z0]
        a001 [x0 y0 z1] a101 [x1 y0 z1] a011 [x0 y1 z1] a111 [x1 y1 z1]]
    (rl-begin RL-TRIANGLES)
    (quad-3f (shade-color color 1.0)  [a001 a101 a111 a011])   ; front  +z
    (quad-3f (shade-color color 0.5)  [a100 a000 a010 a110])   ; back   -z
    (quad-3f (shade-color color 0.7)  [a000 a001 a011 a010])   ; left   -x
    (quad-3f (shade-color color 0.85) [a101 a100 a110 a111])   ; right  +x
    (quad-3f (shade-color color 1.0)  [a011 a111 a110 a010])   ; top    +y
    (quad-3f (shade-color color 0.4)  [a000 a100 a101 a001])   ; bottom -y
    (rl-end)))

(defn sphere!
  "Draw a sphere via rlgl immediate mode (lat/long tessellation), faces shaded
  from the packed `:color` for depth (brighter toward +y). Must be called inside
  a BeginMode3D block (see with-camera-3d). Keyword args:
    :pos    [x y z] centre        (default [0 0 0])
    :radius a number              (default 0.5)
    :rings  latitude bands        (default 12)
    :slices longitude sectors     (default 16)
    :color  a packed Color        (default BLACK)"
  [& {:keys [pos radius rings slices color]
      :or {pos [0.0 0.0 0.0]
           radius 0.5
           rings 12
           slices 16
           color BLACK}}]
  (let [[cx cy cz] pos
        two-pi (* 2.0 Math/PI)]
    (rl-begin RL-TRIANGLES)
    (dotimes [i rings]
      (let [lat0 (- (* Math/PI (/ (double i) rings)) (/ Math/PI 2.0))
            lat1 (- (* Math/PI (/ (double (inc i)) rings)) (/ Math/PI 2.0))
            y0 (Math/sin lat0) y1 (Math/sin lat1)
            r0 (Math/cos lat0) r1 (Math/cos lat1)
            brightness (+ 0.45 (* 0.55 (/ (+ y0 y1 2.0) 4.0)))
            shaded (shade-color color brightness)]
        (dotimes [j slices]
          (let [lon0 (* two-pi (/ (double j) slices))
                lon1 (* two-pi (/ (double (inc j)) slices))
                s0 (Math/sin lon0) c0 (Math/cos lon0)
                s1 (Math/sin lon1) c1 (Math/cos lon1)
                p00 [(+ cx (* radius r0 c0)) (+ cy (* radius y0)) (+ cz (* radius r0 s0))]
                p01 [(+ cx (* radius r0 c1)) (+ cy (* radius y0)) (+ cz (* radius r0 s1))]
                p10 [(+ cx (* radius r1 c0)) (+ cy (* radius y1)) (+ cz (* radius r1 s0))]
                p11 [(+ cx (* radius r1 c1)) (+ cy (* radius y1)) (+ cz (* radius r1 s1))]]
            (quad-3f shaded [p00 p10 p11 p01])))))
    (rl-end)))

;; --- input -------------------------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/get-mouse-x or rl/get-random-value keeps working unchanged.
(def get-mouse-x input/get-mouse-x)
(def get-mouse-y input/get-mouse-y)
(def get-mouse-wheel input/get-mouse-wheel)
(def get-random-value input/get-random-value)
(def get-char-pressed input/get-char-pressed)
(def get-key-pressed input/get-key-pressed)

;; --- libc time (the one NON-raylib FFI) --------------------------------------
;; Moved to net.b12n.raylib.util. Re-exported here so every example that says
;; rl/local-time keeps working unchanged.
(def local-time util/local-time)

;; --- screenshot hook plumbing (headless smoke tests) -------------------------
;; take-screenshot and window-should-close? moved to net.b12n.raylib.core, the
;; four predicates to net.b12n.raylib.input and flush-batch to
;; net.b12n.raylib.rlgl -- none of which this banner ever described.
(def take-screenshot core/take-screenshot)

(def window-should-close? core/window-should-close?)

(def key-down? input/key-down?)
(def key-pressed? input/key-pressed?)
(def mouse-down? input/mouse-down?)
(def mouse-pressed? input/mouse-pressed?)

;; --- constants (raylib KeyboardKey / MouseButton) ----------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/KEY-SPACE or rl/MOUSE-LEFT keeps working unchanged.
(def KEY-NULL input/KEY-NULL)
(def KEY-SPACE input/KEY-SPACE)
(def KEY-R input/KEY-R)
(def KEY-W input/KEY-W)
(def KEY-A input/KEY-A)
(def KEY-S input/KEY-S)
(def KEY-D input/KEY-D)
(def KEY-RIGHT input/KEY-RIGHT)
(def KEY-LEFT input/KEY-LEFT)
(def KEY-DOWN input/KEY-DOWN)
(def KEY-UP input/KEY-UP)
(def MOUSE-LEFT input/MOUSE-LEFT)
(def MOUSE-RIGHT input/MOUSE-RIGHT)
(def MOUSE-MIDDLE input/MOUSE-MIDDLE)
(def KEY-BACKSPACE input/KEY-BACKSPACE)
(def KEY-ENTER input/KEY-ENTER)

;; --- ergonomic keyword-argument drawing API ----------------------------------
;; raylib's C functions are positional; these wrappers take keyword arguments so
;; example code reads self-descriptively, e.g. (rl/text! "hi" :x 10 :y 20
;; :color rl/RED) instead of (draw-text "hi" 10 20 20 rl/RED). The raw bindings
;; above remain the FFI boundary; these just name the arguments.

(defn window!
  "InitWindow with keyword args. :width :height :title."
  [& {:keys [width height title]
      :or {width 800
           height 450
           title "raylib"}}]
  (init-window width height title))

(defn text!
  "DrawText. :x :y :size :color."
  [s & {:keys [x y size color]
        :or {x 0
             y 0
             size 20
             color BLACK}}]
  (draw-text s (int x) (int y) (int size) color))

(defn text-width
  "MeasureText. :size."
  [s & {:keys [size]
        :or {size 20}}]
  (measure-text s size))

(defn fps!
  "DrawFPS. :x :y."
  [& {:keys [x y]
      :or {x 10
           y 10}}]
  (draw-fps (int x) (int y)))

;; --- keyword-argument drawing API --------------------------------------------
;; The C functions behind these take their positions and sizes as int. A double
;; reaching one throws "invalid foreign-procedure argument 0.0" on the first
;; draw, which compiling, linting and formatting all miss: it surfaces only when
;; a frame actually renders. Callers compute positions in floating point all the
;; time (mouse deltas, interpolation, trigonometry), so the coercion lives here
;; rather than at every call site.
(defn rect!
  "DrawRectangle. :x :y :width :height :color."
  [& {:keys [x y width height color]
      :or {x 0
           y 0
           width 10
           height 10
           color BLACK}}]
  (draw-rectangle (int x) (int y) (int width) (int height) color))

(defn rect-lines!
  "DrawRectangleLines. :x :y :width :height :color."
  [& {:keys [x y width height color]
      :or {x 0
           y 0
           width 10
           height 10
           color BLACK}}]
  (draw-rectangle-lines (int x) (int y) (int width) (int height) color))

(defn rect-gradient!
  "DrawRectangleGradientV (top->bottom). :x :y :width :height :top :bottom."
  [& {:keys [x y width height top bottom]
      :or {x 0
           y 0
           width 10
           height 10
           top WHITE
           bottom BLACK}}]
  (draw-rectangle-grad-v x y width height top bottom))

(defn circle!
  "DrawCircle. :x :y :radius :color."
  [& {:keys [x y radius color]
      :or {x 0
           y 0
           radius 10
           color BLACK}}]
  (draw-circle (int x) (int y) (double radius) color))

(defn circle-lines!
  "DrawCircleLines. :x :y :radius :color."
  [& {:keys [x y radius color]
      :or {x 0
           y 0
           radius 10
           color BLACK}}]
  (draw-circle-lines (int x) (int y) (double radius) color))

(defn ellipse!
  "DrawEllipse. :x :y :rx :ry :color."
  [& {:keys [x y rx ry color]
      :or {x 0
           y 0
           rx 10
           ry 6
           color BLACK}}]
  (draw-ellipse (int x) (int y) (double rx) (double ry) color))

(defn line!
  "DrawLine. :x1 :y1 :x2 :y2 :color."
  [& {:keys [x1 y1 x2 y2 color]
      :or {x1 0
           y1 0
           x2 0
           y2 0
           color BLACK}}]
  (draw-line (int x1) (int y1) (int x2) (int y2) color))

(defn pixel!
  "DrawPixel. :x :y :color."
  [& {:keys [x y color]
      :or {x 0
           y 0
           color BLACK}}]
  (draw-pixel (int x) (int y) color))

(defn sector!
  "A filled circular sector (pie slice / arc) drawn as an rlgl triangle fan, the
  immediate-mode stand-in for DrawCircleSector, whose Vector2 center is by-value and
  so unbindable (see rlgl-immediate-mode.md). The fan runs from the center across
  [start-deg, end-deg] in `segments` sub-triangles, a single packed `:color`.
  0 deg points up and the angle increases clockwise (rim = (sin, -cos)); vertices are
  emitted rim -> center -> rim so the fan carries raylib's front-facing winding and is
  not backface-culled. Callers must pass start-deg < end-deg.
    :cx :cy    center
    :radius    outer radius
    :start-deg :end-deg   sweep in degrees (0 = up, clockwise, increasing)
    :segments  fan resolution (default 32)
    :color     packed Color"
  [& {:keys [cx cy radius start-deg end-deg segments color]
      :or {cx 0
           cy 0
           radius 10
           start-deg 0
           end-deg 90
           segments 32
           color BLACK}}]
  (let [d->r (/ Math/PI 180.0)
        span (- end-deg start-deg)
        rim (fn [deg]
              (let [t (* deg d->r)]
                [(+ cx (* radius (Math/sin t)))
                 (- cy (* radius (Math/cos t)))]))]
    (rl-begin RL-TRIANGLES)
    (rl-color! color)
    (dotimes [k segments]
      (let [[x0 y0] (rim (+ start-deg (* span (/ (double k) segments))))
            [x1 y1] (rim (+ start-deg (* span (/ (double (inc k)) segments))))]
        (rl-vertex-2f (double x0) (double y0))
        (rl-vertex-2f (double cx) (double cy))
        (rl-vertex-2f (double x1) (double y1))))
    (rl-end)))

(defn ring!
  "A filled annulus (donut sector) as an rlgl quad strip between :inner and :outer
  radius over [start-deg, end-deg], the immediate-mode stand-in for DrawRing (Vector2
  center by value). Same angle convention as sector! (0 deg up, clockwise, increasing).
  Each segment is two front-wound triangles.
    :cx :cy    center
    :inner :outer   radii
    :start-deg :end-deg   sweep in degrees (increasing)
    :segments  resolution (default 48)
    :color     packed Color"
  [& {:keys [cx cy inner outer start-deg end-deg segments color]
      :or {cx 0
           cy 0
           inner 20
           outer 40
           start-deg 0
           end-deg 360
           segments 48
           color BLACK}}]
  (let [d->r (/ Math/PI 180.0)
        span (- end-deg start-deg)
        pt (fn [deg r]
             (let [t (* deg d->r)]
               [(+ cx (* r (Math/sin t))) (- cy (* r (Math/cos t)))]))]
    (rl-begin RL-TRIANGLES)
    (rl-color! color)
    (dotimes [k segments]
      (let [d0 (+ start-deg (* span (/ (double k) segments)))
            d1 (+ start-deg (* span (/ (double (inc k)) segments)))
            [ix0 iy0] (pt d0 inner) [ox0 oy0] (pt d0 outer)
            [ix1 iy1] (pt d1 inner) [ox1 oy1] (pt d1 outer)]
        (rl-vertex-2f (double ox0) (double oy0))
        (rl-vertex-2f (double ix0) (double iy0))
        (rl-vertex-2f (double ix1) (double iy1))
        (rl-vertex-2f (double ox0) (double oy0))
        (rl-vertex-2f (double ix1) (double iy1))
        (rl-vertex-2f (double ox1) (double oy1))))
    (rl-end)))

(defn line-ex!
  "A thick line (rlgl quad) from (x1,y1) to (x2,y2), :thick pixels wide, the
  immediate-mode stand-in for DrawLineEx (Vector2 endpoints by value). The quad is
  front-wound at every line direction (perpendicular = (dy,-dx)/len).
    :x1 :y1 :x2 :y2   endpoints
    :thick   width in px (default 2)
    :color   packed Color"
  [& {:keys [x1 y1 x2 y2 thick color]
      :or {x1 0
           y1 0
           x2 0
           y2 0
           thick 2
           color BLACK}}]
  (let [dx (- x2 x1) dy (- y2 y1)
        len (Math/sqrt (+ (* dx dx) (* dy dy)))
        len (if (zero? len) 1.0 len)
        h  (/ thick 2.0)
        px (* (/ dy len) h)        ; perpendicular (dy,-dx) * half-thick
        py (* (/ (- dx) len) h)
        ax (+ x1 px) ay (+ y1 py)
        bx (- x1 px) by (- y1 py)
        cx (- x2 px) cy (- y2 py)
        ex (+ x2 px) ey (+ y2 py)]
    (rl-begin RL-TRIANGLES)
    (rl-color! color)
    (rl-vertex-2f (double ax) (double ay))
    (rl-vertex-2f (double bx) (double by))
    (rl-vertex-2f (double cx) (double cy))
    (rl-vertex-2f (double ax) (double ay))
    (rl-vertex-2f (double cx) (double cy))
    (rl-vertex-2f (double ex) (double ey))
    (rl-end)))

;; --- smoke-test loop guards --------------------------------------------------
;; Moved to net.b12n.raylib-jlt.app. It is the example suite's own harness, not
;; a raylib binding, so it does not go in the library.
(def auto-quit-deadline app/auto-quit-deadline)
(def keep-running?      app/keep-running?)
(def maybe-screenshot!  app/maybe-screenshot!)

;; =============================================================================
;; Scalar extensions
;; =============================================================================
;; Everything below is appended rather than slotted into the sections above on
;; purpose: since jolt 0.4.0 a symbol must be defined before its first use in the
;; file, and the ordering of the sections above is load-bearing (see the Color
;; note at the top). Appending cannot disturb it. Nothing above refers to
;; anything here.

;; --- window state / config flags ---------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/set-config-flags or rl/window-state? keeps working unchanged.
(def set-config-flags core/set-config-flags)
(def set-window-state core/set-window-state)
(def clear-window-state core/clear-window-state)
(def toggle-fullscreen core/toggle-fullscreen)
(def get-screen-width core/get-screen-width)
(def get-screen-height core/get-screen-height)
(def get-time core/get-time)
(def FLAG-WINDOW-RESIZABLE core/FLAG-WINDOW-RESIZABLE)
(def FLAG-WINDOW-UNDECORATED core/FLAG-WINDOW-UNDECORATED)
(def FLAG-MSAA-4X-HINT core/FLAG-MSAA-4X-HINT)
(def FLAG-VSYNC-HINT core/FLAG-VSYNC-HINT)
(def FLAG-WINDOW-TOPMOST core/FLAG-WINDOW-TOPMOST)
(def FLAG-WINDOW-HIGHDPI core/FLAG-WINDOW-HIGHDPI)
(def window-state? core/window-state?)
(def window-resized? core/window-resized?)

;; --- monitors ----------------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/get-monitor-count or rl/get-monitor-name keeps working unchanged.
(def get-monitor-count core/get-monitor-count)
(def get-current-monitor core/get-current-monitor)
(def get-monitor-width core/get-monitor-width)
(def get-monitor-height core/get-monitor-height)
(def get-monitor-refresh-rate core/get-monitor-refresh-rate)
(def get-monitor-name core/get-monitor-name)

;; --- clipboard ---------------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/set-clipboard-text or rl/get-clipboard-text keeps working unchanged.
(def set-clipboard-text core/set-clipboard-text)
(def get-clipboard-text core/get-clipboard-text)

;; --- gamepad -----------------------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/gamepad-down? or rl/PAD-A keeps working unchanged.
(def get-gamepad-axis-count input/get-gamepad-axis-count)
(def get-gamepad-axis-movement input/get-gamepad-axis-movement)
(def get-gamepad-name input/get-gamepad-name)
(def gamepad-available? input/gamepad-available?)
(def gamepad-down? input/gamepad-down?)
(def gamepad-pressed? input/gamepad-pressed?)
(def gamepad-released? input/gamepad-released?)
(def PAD-UP input/PAD-UP)
(def PAD-RIGHT input/PAD-RIGHT)
(def PAD-DOWN input/PAD-DOWN)
(def PAD-LEFT input/PAD-LEFT)
(def PAD-Y input/PAD-Y)
(def PAD-B input/PAD-B)
(def PAD-A input/PAD-A)
(def PAD-X input/PAD-X)
(def PAD-L1 input/PAD-L1)
(def PAD-L2 input/PAD-L2)
(def PAD-R1 input/PAD-R1)
(def PAD-R2 input/PAD-R2)
(def PAD-SELECT input/PAD-SELECT)
(def PAD-MENU input/PAD-MENU)
(def PAD-START input/PAD-START)
(def AXIS-LEFT-X input/AXIS-LEFT-X)
(def AXIS-LEFT-Y input/AXIS-LEFT-Y)
(def AXIS-RIGHT-X input/AXIS-RIGHT-X)
(def AXIS-RIGHT-Y input/AXIS-RIGHT-Y)

;; --- touch / gestures --------------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/get-touch-x or rl/GESTURE-TAP keeps working unchanged.
(def get-touch-point-count input/get-touch-point-count)
(def get-touch-point-id input/get-touch-point-id)
(def get-touch-x input/get-touch-x)
(def get-touch-y input/get-touch-y)
(def get-gesture-detected input/get-gesture-detected)
(def set-gestures-enabled input/set-gestures-enabled)
(def GESTURE-NONE input/GESTURE-NONE)
(def GESTURE-TAP input/GESTURE-TAP)
(def GESTURE-DOUBLETAP input/GESTURE-DOUBLETAP)
(def GESTURE-HOLD input/GESTURE-HOLD)
(def GESTURE-DRAG input/GESTURE-DRAG)
(def GESTURE-SWIPE-RIGHT input/GESTURE-SWIPE-RIGHT)
(def GESTURE-SWIPE-LEFT input/GESTURE-SWIPE-LEFT)
(def GESTURE-SWIPE-UP input/GESTURE-SWIPE-UP)
(def GESTURE-SWIPE-DOWN input/GESTURE-SWIPE-DOWN)
(def GESTURE-PINCH-IN input/GESTURE-PINCH-IN)
(def GESTURE-PINCH-OUT input/GESTURE-PINCH-OUT)

;; --- remaining input predicates ----------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/set-mouse-cursor or rl/key-released? keeps working unchanged.
(def set-mouse-cursor input/set-mouse-cursor)
(def key-released? input/key-released?)
(def mouse-released? input/mouse-released?)

;; --- more KeyboardKey constants ----------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/KEY-ESCAPE or rl/KEY-Z keeps working unchanged.
(def KEY-ESCAPE input/KEY-ESCAPE)
(def KEY-TAB input/KEY-TAB)
(def KEY-DELETE input/KEY-DELETE)
(def KEY-HOME input/KEY-HOME)
(def KEY-END input/KEY-END)
(def KEY-F1 input/KEY-F1)
(def KEY-F2 input/KEY-F2)
(def KEY-F3 input/KEY-F3)
(def KEY-LEFT-SHIFT input/KEY-LEFT-SHIFT)
(def KEY-LEFT-CONTROL input/KEY-LEFT-CONTROL)
(def KEY-LEFT-SUPER input/KEY-LEFT-SUPER)
(def KEY-ZERO input/KEY-ZERO)
(def KEY-ONE input/KEY-ONE)
(def KEY-TWO input/KEY-TWO)
(def KEY-THREE input/KEY-THREE)
(def KEY-FOUR input/KEY-FOUR)
(def KEY-FIVE input/KEY-FIVE)
(def KEY-SIX input/KEY-SIX)
(def KEY-SEVEN input/KEY-SEVEN)
(def KEY-EIGHT input/KEY-EIGHT)
(def KEY-NINE input/KEY-NINE)
(def KEY-B input/KEY-B)
(def KEY-C input/KEY-C)
(def KEY-E input/KEY-E)
(def KEY-F input/KEY-F)
(def KEY-G input/KEY-G)
(def KEY-H input/KEY-H)
(def KEY-M input/KEY-M)
(def KEY-N input/KEY-N)
(def KEY-P input/KEY-P)
(def KEY-Q input/KEY-Q)
(def KEY-T input/KEY-T)
(def KEY-V input/KEY-V)
(def KEY-X input/KEY-X)
(def KEY-Y input/KEY-Y)
(def KEY-Z input/KEY-Z)

;; --- extra scalar drawing ------------------------------------------------------
;; Moved to net.b12n.raylib.shapes. Re-exported here so every example that says
;; rl/circle-gradient! or rl/rect-pro! keeps working unchanged.
;; vector2-layout stays a local alias: world <-> screen and splines below still
;; read it bare, and neither has been extracted yet.
;; moved to net.b12n.raylib.native
(def ^:private vector2-layout native/vector2-layout)
(def draw-rectangle-grad-h shapes/draw-rectangle-grad-h)
(def begin-blend-mode shapes/begin-blend-mode)
(def end-blend-mode shapes/end-blend-mode)
(def BLEND-ALPHA shapes/BLEND-ALPHA)
(def BLEND-ADDITIVE shapes/BLEND-ADDITIVE)
(def BLEND-MULTIPLIED shapes/BLEND-MULTIPLIED)
(def BLEND-ADD-COLORS shapes/BLEND-ADD-COLORS)
(def BLEND-SUBTRACT-COLORS shapes/BLEND-SUBTRACT-COLORS)
(def BLEND-CUSTOM shapes/BLEND-CUSTOM)
(def set-blend-factors shapes/set-blend-factors)
(def GL-SRC-ALPHA shapes/GL-SRC-ALPHA)
(def GL-MIN shapes/GL-MIN)
(def GL-MAX shapes/GL-MAX)
(def circle-gradient! shapes/circle-gradient!)
(def rect-pro! shapes/rect-pro!)
(def rect-gradient-h! shapes/rect-gradient-h!)

;; --- rlgl textures -----------------------------------------------------------
;; Moved to net.b12n.raylib.textures. Re-exported here so every example that
;; says rl/texture! or rl/texture-from-fn keeps working unchanged.
(def rl-load-texture textures/rl-load-texture)
(def rl-unload-texture textures/rl-unload-texture)
(def rl-update-texture textures/rl-update-texture)
(def rl-texture-parameters textures/rl-texture-parameters)
(def rl-set-texture textures/rl-set-texture)
(def rl-tex-coord-2f textures/rl-tex-coord-2f)
(def rl-normal-3f textures/rl-normal-3f)
(def RL-QUADS textures/RL-QUADS)
(def PIXELFORMAT-R8G8B8A8 textures/PIXELFORMAT-R8G8B8A8)
(def PIXELFORMAT-R8G8B8 textures/PIXELFORMAT-R8G8B8)
(def RL-TEXTURE-WRAP-S textures/RL-TEXTURE-WRAP-S)
(def RL-TEXTURE-WRAP-T textures/RL-TEXTURE-WRAP-T)
(def RL-TEXTURE-WRAP-REPEAT textures/RL-TEXTURE-WRAP-REPEAT)
(def RL-TEXTURE-WRAP-CLAMP textures/RL-TEXTURE-WRAP-CLAMP)
(def RL-TEXTURE-MAG-FILTER textures/RL-TEXTURE-MAG-FILTER)
(def RL-TEXTURE-MIN-FILTER textures/RL-TEXTURE-MIN-FILTER)
(def RL-TEXTURE-FILTER-NEAREST textures/RL-TEXTURE-FILTER-NEAREST)
(def RL-TEXTURE-FILTER-LINEAR textures/RL-TEXTURE-FILTER-LINEAR)
(def texture-filter! textures/texture-filter!)
(def texture-wrap! textures/texture-wrap!)
(def texture-from-fn textures/texture-from-fn)
(def update-texture-from-fn! textures/update-texture-from-fn!)
(def unload-texture! textures/unload-texture!)
(def texture! textures/texture!)

;; --- rlgl framebuffers (render textures) -------------------------------------
;; Moved to net.b12n.raylib.textures. Re-exported here so every example that
;; says rl/render-texture or rl/with-render-texture keeps working unchanged.
(def rl-load-framebuffer textures/rl-load-framebuffer)
(def rl-framebuffer-attach textures/rl-framebuffer-attach)
(def rl-enable-framebuffer textures/rl-enable-framebuffer)
(def rl-disable-framebuffer textures/rl-disable-framebuffer)
(def rl-unload-framebuffer textures/rl-unload-framebuffer)
(def rl-load-texture-depth textures/rl-load-texture-depth)
(def rl-viewport textures/rl-viewport)
(def rl-matrix-mode textures/rl-matrix-mode)
(def rl-load-identity textures/rl-load-identity)
(def rl-ortho textures/rl-ortho)
(def rl-set-framebuffer-width textures/rl-set-framebuffer-width)
(def rl-set-framebuffer-height textures/rl-set-framebuffer-height)
(def rl-get-framebuffer-width textures/rl-get-framebuffer-width)
(def rl-get-framebuffer-height textures/rl-get-framebuffer-height)
(def rl-mult-matrix-f textures/rl-mult-matrix-f)
(def get-render-width textures/get-render-width)
(def get-render-height textures/get-render-height)
(def RL-PROJECTION textures/RL-PROJECTION)
(def RL-MODELVIEW textures/RL-MODELVIEW)
(def RL-ATTACHMENT-COLOR-CHANNEL0 textures/RL-ATTACHMENT-COLOR-CHANNEL0)
(def RL-ATTACHMENT-DEPTH textures/RL-ATTACHMENT-DEPTH)
(def RL-ATTACHMENT-TEXTURE2D textures/RL-ATTACHMENT-TEXTURE2D)
(def RL-ATTACHMENT-RENDERBUFFER textures/RL-ATTACHMENT-RENDERBUFFER)
(def render-texture textures/render-texture)
(def unload-render-texture! textures/unload-render-texture!)
(def with-render-texture textures/with-render-texture)

;; --- shaders -----------------------------------------------------------------
;; raylib's Shader is {unsigned int id; int *locs;} - 16 bytes, passed and
;; returned BY VALUE. jolt 0.7.23's [:by-value [:struct ...]] expresses that
;; directly, so this calls raylib's real shader API rather than reaching under it
;; to rlgl the way the texture section above has to. In particular
;; LoadShaderFromMemory fills the locations array itself; nothing here builds one.
;;
;; The struct descriptor is spelled out in every signature on purpose: it is a
;; compile-time literal and a def'd alias is rejected with
;;   jolt.ffi return type must be a keyword or [:by-value [:struct ...]], got V2
(ffi/defcfn ^:private load-shader-from-memory "LoadShaderFromMemory" [:pointer :string]
  [:by-value [:struct [[:id :uint] [:locs :pointer]]]])
(ffi/defcfn ^:private begin-shader-mode "BeginShaderMode"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]]] :void)
(ffi/defcfn end-shader-mode "EndShaderMode" [] :void)
(ffi/defcfn ^:private get-shader-location "GetShaderLocation"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]] :string] :int)
(ffi/defcfn ^:private set-shader-value-raw "SetShaderValue"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]] :int :pointer :int] :void)
(ffi/defcfn ^:private set-shader-value-v-raw "SetShaderValueV"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]] :int :pointer :int :int] :void)
(ffi/defcfn ^:private unload-shader-raw "UnloadShader"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]]] :void)

;; Two by-value structs in one signature, and they take different ABI paths on
;; arm64: Shader is 16 bytes and rides in general-purpose registers, Texture2D is
;; 20 and so is passed INDIRECTLY, by a pointer the caller supplies. Both
;; measured with clang, not assumed.
(ffi/defcfn ^:private set-shader-value-texture-raw "SetShaderValueTexture"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]]
   :int
   [:by-value [:struct [[:id :uint] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]]] :void)

(def shader-layout (ffi/layout [:struct [[:id :uint] [:locs :pointer]]]))

;; ShaderUniformDataType, raylib 6.0. The UINT variants at 8-11 are new in 6.0
;; and pushed SAMPLER2D from 8 to 12 - a silent break for anything carrying the
;; 5.5 value, since a wrong type tag binds the wrong slot without erroring.
(def ^:const UNIFORM-FLOAT 0)  (def ^:const UNIFORM-VEC2 1)
(def ^:const UNIFORM-VEC3 2)   (def ^:const UNIFORM-VEC4 3)
(def ^:const UNIFORM-INT 4)    (def ^:const UNIFORM-IVEC2 5)
(def ^:const UNIFORM-IVEC3 6)  (def ^:const UNIFORM-IVEC4 7)
(def ^:const UNIFORM-SAMPLER2D 12)

(defn shader
  "Compile `fs-source` as a fragment shader against raylib's default vertex
  shader. Returns a pointer to the Shader struct, or nil if the program did not
  link (raylib prints the compiler log to stderr). Pair with `unload-shader!`.

  GLSL is a string here rather than a file, because LoadShaderFromMemory takes
  source: nothing is read from disk, so each example stays self-contained and the
  demo recorder never has a working-directory question. The source must open with
  `#version 330` - the desktop backend is GL 3.3 core.

  ffi/null rather than nil for the vertex stage, meaning \"use raylib's default\".
  jolt carries nil across a :string as NULL only since jolt#708, which is merged
  but not in a release, so the :pointer spelling keeps this working on a stock
  0.7.23."
  [fs-source]
  (let [p (ffi/alloc (ffi/layout-size shader-layout))]
    (load-shader-from-memory p ffi/null fs-source)
    (if (pos? (ffi/read-field p shader-layout :id))
      p
      (do (ffi/free p) nil))))

(defn unload-shader!
  "UnloadShader, then release the struct this side."
  [sh]
  (unload-shader-raw sh)
  (ffi/free sh))

(defn uniform-loc
  "The location of a named uniform, or -1 if the shader does not declare it (or
  the compiler optimised it away). Look these up once, outside the frame loop -
  each call is a GL query."
  [sh name]
  (get-shader-location sh name))

(defn with-shader
  "Run (f) with `sh` active. BeginShaderMode / EndShaderMode, so raylib does the
  batch flush on both edges."
  [sh f]
  (begin-shader-mode sh)
  (try
    (f)
    (finally (end-shader-mode))))

;; SetShaderValue takes a POINTER to the value, so each setter stages its floats
;; or ints in native memory for the length of the call. An undeclared uniform
;; gives -1, which the nat-int? guards skip: an example whose shader drops an
;; unused uniform keeps working rather than erroring.
;; moved to net.b12n.raylib.native
(def ^:private staged native/staged)

(defn set-uniform-float!
  [sh loc v]
  (when (nat-int? loc)
    (staged :float [v] (fn [p] (set-shader-value-raw sh loc p UNIFORM-FLOAT)))))

(defn set-uniform-vec2!
  [sh loc x y]
  (when (nat-int? loc)
    (staged :float [x y] (fn [p] (set-shader-value-raw sh loc p UNIFORM-VEC2)))))

(defn set-uniform-vec3!
  [sh loc x y z]
  (when (nat-int? loc)
    (staged :float [x y z] (fn [p] (set-shader-value-raw sh loc p UNIFORM-VEC3)))))

(defn set-uniform-vec4!
  [sh loc x y z w]
  (when (nat-int? loc)
    (staged :float [x y z w] (fn [p] (set-shader-value-raw sh loc p UNIFORM-VEC4)))))

(defn set-uniform-int!
  [sh loc v]
  (when (nat-int? loc)
    (staged :int [v] (fn [p] (set-shader-value-raw sh loc p UNIFORM-INT)))))

(defn set-uniform-ivec3-array!
  "An array of `n` ivec3s from a flat sequence of 3n ints - how a palette reaches
  a shader as `uniform ivec3 palette[8]`."
  [sh loc ints n]
  (when (nat-int? loc)
    (staged :int ints (fn [p] (set-shader-value-v-raw sh loc p UNIFORM-IVEC3 n)))))

;; texture2d-layout stays a local alias: set-uniform-texture! below still reads
;; it bare, and shaders hasn't been extracted yet.
;; moved to net.b12n.raylib.images
(def ^:private texture2d-layout images/texture2d-layout)

(defn set-uniform-texture!
  "Bind a texture id to a `sampler2D` uniform - the second and later samplers,
  since raylib binds the drawn texture to slot 0 itself.

  This suite carries textures as bare rlgl ids, so the Texture2D raylib wants is
  staged here from the id plus its dimensions. mipmaps 1 and format RGBA8 match
  what `texture-from-fn` uploads; raylib only reads `id` for this call, but the
  rest is filled in truthfully rather than left as whatever the allocation held."
  [sh loc tex-id w h]
  (when (nat-int? loc)
    (ffi/with-layout [t texture2d-layout]
      (ffi/write-field t texture2d-layout :id tex-id)
      (ffi/write-field t texture2d-layout :width (int w))
      (ffi/write-field t texture2d-layout :height (int h))
      (ffi/write-field t texture2d-layout :mipmaps 1)
      (ffi/write-field t texture2d-layout :format PIXELFORMAT-R8G8B8A8)
      (set-shader-value-texture-raw sh loc t))))

;; --- REPL entry point --------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; (rl/run! -main) keeps working unchanged.
(def run! core/run!)

;; --- backface culling --------------------------------------------------------
;; Moved to net.b12n.raylib.rlgl. Re-exported here so every example that says
;; rl/rl-disable-backface-culling keeps working unchanged.
(def rl-disable-backface-culling rlgl/rl-disable-backface-culling)
(def rl-enable-backface-culling rlgl/rl-enable-backface-culling)

;; --- mouse position / cursor -------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/set-mouse-position or rl/hide-cursor keeps working unchanged.
(def set-mouse-position input/set-mouse-position)
(def hide-cursor input/hide-cursor)
(def show-cursor input/show-cursor)

;; --- audio (raudio) -----------------------------------------------------
;; AudioStream is {rAudioBuffer* buffer; rAudioProcessor* processor; uint
;; sampleRate; uint sampleSize; uint channels} -- two pointers and three u32s,
;; passed BY VALUE everywhere raudio touches it. That is the same [:by-value
;; [:struct ...]] mechanism circle-gradient! already uses for its Vector2
;; centre; LoadAudioStream also RETURNS one by value, so its binding takes a
;; caller-allocated destination pointer FIRST (jolt's calling convention for
;; an aggregate return) and hands that same pointer back.
(def ^:private audio-stream-layout
  (ffi/layout [:struct [[:buffer :pointer]
                        [:processor :pointer]
                        [:sample-rate :uint32]
                        [:sample-size :uint32]
                        [:channels :uint32]]]))

(ffi/defcfn init-audio-device  "InitAudioDevice"  [] :void)
(ffi/defcfn close-audio-device "CloseAudioDevice" [] :void)
(ffi/defcfn set-audio-stream-buffer-size-default
  "SetAudioStreamBufferSizeDefault" [:int] :void)

(ffi/defcfn ^:private load-audio-stream-raw "LoadAudioStream"
  [:uint32 :uint32 :uint32]
  [:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                       [:sample-rate :uint32] [:sample-size :uint32]
                       [:channels :uint32]]]])
(ffi/defcfn ^:private unload-audio-stream-raw "UnloadAudioStream"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]]
  :void)
(ffi/defcfn ^:private play-audio-stream-raw "PlayAudioStream"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]]
  :void)
(ffi/defcfn ^:private is-audio-stream-processed-raw "IsAudioStreamProcessed"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]]
  :int)
(ffi/defcfn ^:private update-audio-stream-raw "UpdateAudioStream"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]
   :pointer :int]
  :void)
(ffi/defcfn ^:private set-audio-stream-pan-raw "SetAudioStreamPan"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]
   :float]
  :void)

(defn load-audio-stream
  "LoadAudioStream. Returns an opaque native pointer to the by-value AudioStream
  -- pass it to every other audio-stream fn below and release it with
  unload-audio-stream."
  [sample-rate sample-size channels]
  (let [stream (ffi/alloc (ffi/layout-size audio-stream-layout))]
    (load-audio-stream-raw stream sample-rate sample-size channels)))

(defn unload-audio-stream
  [stream]
  (unload-audio-stream-raw stream)
  (ffi/free stream))

(defn play-audio-stream
  [stream]
  (play-audio-stream-raw stream))

(defn audio-stream-processed?
  [stream]
  (not (zero? (bit-and (is-audio-stream-processed-raw stream) 0xff))))

(defn update-audio-stream
  "UpdateAudioStream. `samples` is a seq of floats for one refill; its count
  must match the stream's own frame-count-per-channel (mono here). Stages a
  scratch native buffer via `staged` the same way the shader uniform setters
  do -- a few refills a second is not a hot path."
  [stream samples]
  (staged :float samples (fn [p] (update-audio-stream-raw stream p (count samples)))))

(defn set-audio-stream-pan
  [stream pan]
  (set-audio-stream-pan-raw stream (double pan)))

;; --- world <-> screen (genuine by-value Camera3D) -----------------------
;; with-camera-3d's Camera3D pointer trick above is correct on AArch64 by
;; accident of the ABI (a struct too large for registers goes via a hidden
;; pointer there) and wrong on x86-64 SysV, where it goes on the stack
;; instead (see raylib.clj's file-level ABI note). GetWorldToScreen is new
;; code, not a migration of with-camera-3d, so it uses jolt's real
;; [:by-value [:struct ...]] passing for BOTH the Vector3 and the Camera3D --
;; correct on either ABI, and the pattern the rest of the by-value bindings
;; above already follow.
;; moved to net.b12n.raylib.native
(def ^:private vector3-layout native/vector3-layout)

(def ^:private camera3d-layout
  (ffi/layout [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]))

(ffi/defcfn ^:private get-world-to-screen-raw "GetWorldToScreen"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]]]
  [:by-value [:struct [[:x :float] [:y :float]]]])

(defn world-to-screen
  "GetWorldToScreen. `pos` is [x y z] in world space; `camera` takes the same
  keys as with-camera-3d's opts map (share one map between both calls to
  project a point through the exact camera a frame draws with). Returns
  [screen-x screen-y] as doubles."
  [[px py pz]
   {:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z
           fovy projection]
    :or {pos-x 0
         pos-y 0
         pos-z 0
         target-x 0
         target-y 0
         target-z 0
         up-x 0
         up-y 1
         up-z 0
         fovy 45
         projection 0}}]
  (let [p   (ffi/alloc (ffi/layout-size vector3-layout))
        cam (ffi/alloc (ffi/layout-size camera3d-layout))
        out (ffi/alloc (ffi/layout-size vector2-layout))]
    (try
      (ffi/write-field p vector3-layout :x (double px))
      (ffi/write-field p vector3-layout :y (double py))
      (ffi/write-field p vector3-layout :z (double pz))
      (ffi/write-field cam camera3d-layout [:position :x] (double pos-x))
      (ffi/write-field cam camera3d-layout [:position :y] (double pos-y))
      (ffi/write-field cam camera3d-layout [:position :z] (double pos-z))
      (ffi/write-field cam camera3d-layout [:target :x] (double target-x))
      (ffi/write-field cam camera3d-layout [:target :y] (double target-y))
      (ffi/write-field cam camera3d-layout [:target :z] (double target-z))
      (ffi/write-field cam camera3d-layout [:up :x] (double up-x))
      (ffi/write-field cam camera3d-layout [:up :y] (double up-y))
      (ffi/write-field cam camera3d-layout [:up :z] (double up-z))
      (ffi/write-field cam camera3d-layout :fovy (double fovy))
      (ffi/write-field cam camera3d-layout :projection (int projection))
      (get-world-to-screen-raw out p cam)
      [(ffi/read-field out vector2-layout :x)
       (ffi/read-field out vector2-layout :y)]
      (finally
        (ffi/free p)
        (ffi/free cam)
        (ffi/free out)))))

;; --- more keyboard constants (keyboard-testbed) --------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/KEY-I or rl/KEY-RIGHT-ALT keeps working unchanged.
(def KEY-I input/KEY-I)
(def KEY-J input/KEY-J)
(def KEY-K input/KEY-K)
(def KEY-L input/KEY-L)
(def KEY-O input/KEY-O)
(def KEY-U input/KEY-U)
(def KEY-APOSTROPHE input/KEY-APOSTROPHE)
(def KEY-COMMA input/KEY-COMMA)
(def KEY-MINUS input/KEY-MINUS)
(def KEY-PERIOD input/KEY-PERIOD)
(def KEY-SLASH input/KEY-SLASH)
(def KEY-SEMICOLON input/KEY-SEMICOLON)
(def KEY-EQUAL input/KEY-EQUAL)
(def KEY-LEFT-BRACKET input/KEY-LEFT-BRACKET)
(def KEY-BACKSLASH input/KEY-BACKSLASH)
(def KEY-RIGHT-BRACKET input/KEY-RIGHT-BRACKET)
(def KEY-GRAVE input/KEY-GRAVE)
(def KEY-INSERT input/KEY-INSERT)
(def KEY-PAGE-UP input/KEY-PAGE-UP)
(def KEY-PAGE-DOWN input/KEY-PAGE-DOWN)
(def KEY-CAPS-LOCK input/KEY-CAPS-LOCK)
(def KEY-PRINT-SCREEN input/KEY-PRINT-SCREEN)
(def KEY-PAUSE input/KEY-PAUSE)
(def KEY-F4 input/KEY-F4)
(def KEY-F5 input/KEY-F5)
(def KEY-F6 input/KEY-F6)
(def KEY-F7 input/KEY-F7)
(def KEY-F8 input/KEY-F8)
(def KEY-F9 input/KEY-F9)
(def KEY-F10 input/KEY-F10)
(def KEY-F11 input/KEY-F11)
(def KEY-F12 input/KEY-F12)
(def KEY-LEFT-ALT input/KEY-LEFT-ALT)
(def KEY-RIGHT-SHIFT input/KEY-RIGHT-SHIFT)
(def KEY-RIGHT-CONTROL input/KEY-RIGHT-CONTROL)
(def KEY-RIGHT-ALT input/KEY-RIGHT-ALT)

;; --- geometric primitives, genuinely by value (geometric-shapes) --------
;; raylib's real Draw{Cube,Sphere,Cylinder,Capsule}* calls, now that
;; [:by-value [:struct ...]] works -- named draw-*! rather than reusing
;; cube!/sphere! (the existing rlgl immediate-mode stand-ins), since these
;; are a genuinely different code path, not a replacement for them.
(defn- vec3->ptr!
  "Allocate a vector3-layout buffer and write [x y z] into it. Caller frees."
  [[x y z]]
  (let [p (ffi/alloc (ffi/layout-size vector3-layout))]
    (ffi/write-field p vector3-layout :x (double x))
    (ffi/write-field p vector3-layout :y (double y))
    (ffi/write-field p vector3-layout :z (double z))
    p))

(ffi/defcfn ^:private draw-cube-raw "DrawCube"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :uint]
  :void)
(ffi/defcfn ^:private draw-cube-wires-raw "DrawCubeWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :uint]
  :void)
(ffi/defcfn ^:private draw-sphere-raw "DrawSphere"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :uint]
  :void)
(ffi/defcfn ^:private draw-sphere-wires-raw "DrawSphereWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :int :int :uint]
  :void)
(ffi/defcfn ^:private draw-cylinder-raw "DrawCylinder"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :int :uint]
  :void)
(ffi/defcfn ^:private draw-cylinder-wires-raw "DrawCylinderWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :int :uint]
  :void)
(ffi/defcfn ^:private draw-capsule-raw "DrawCapsule"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :int :int :uint]
  :void)
(ffi/defcfn ^:private draw-capsule-wires-raw "DrawCapsuleWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :int :int :uint]
  :void)

(defn draw-cube!
  "DrawCube. :pos :width :height :length :color."
  [& {:keys [pos width height length color]
      :or {pos [0.0 0.0 0.0]
           width 1.0
           height 1.0
           length 1.0
           color BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cube-raw p (double width) (double height) (double length) color)
         (finally (ffi/free p)))))

(defn draw-cube-wires!
  "DrawCubeWires. :pos :width :height :length :color."
  [& {:keys [pos width height length color]
      :or {pos [0.0 0.0 0.0]
           width 1.0
           height 1.0
           length 1.0
           color BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cube-wires-raw p (double width) (double height) (double length) color)
         (finally (ffi/free p)))))

(defn draw-sphere!
  [pos radius color]
  (let [p (vec3->ptr! pos)]
    (try (draw-sphere-raw p (double radius) color)
         (finally (ffi/free p)))))

(defn draw-sphere-wires!
  "DrawSphereWires. :pos :radius :rings :slices :color."
  [& {:keys [pos radius rings slices color]
      :or {pos [0.0 0.0 0.0]
           radius 0.5
           rings 16
           slices 16
           color BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-sphere-wires-raw p (double radius) (int rings) (int slices) color)
         (finally (ffi/free p)))))

(defn draw-cylinder!
  "DrawCylinder. :pos :radius-top :radius-bottom :height :slices :color."
  [& {:keys [pos radius-top radius-bottom height slices color]
      :or {pos [0.0 0.0 0.0]
           radius-top 1.0
           radius-bottom 1.0
           height 1.0
           slices 16
           color BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cylinder-raw p (double radius-top) (double radius-bottom) (double height)
                            (int slices) color)
         (finally (ffi/free p)))))

(defn draw-cylinder-wires!
  "DrawCylinderWires. :pos :radius-top :radius-bottom :height :slices :color."
  [& {:keys [pos radius-top radius-bottom height slices color]
      :or {pos [0.0 0.0 0.0]
           radius-top 1.0
           radius-bottom 1.0
           height 1.0
           slices 16
           color BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cylinder-wires-raw p (double radius-top) (double radius-bottom) (double height)
                                  (int slices) color)
         (finally (ffi/free p)))))

(defn draw-capsule!
  "DrawCapsule. :start-pos :end-pos :radius :slices :rings :color."
  [& {:keys [start-pos end-pos radius slices rings color]
      :or {start-pos [0.0 0.0 0.0]
           end-pos [0.0 1.0 0.0]
           radius 0.5
           slices 8
           rings 8
           color BLACK}}]
  (let [p1 (vec3->ptr! start-pos)
        p2 (vec3->ptr! end-pos)]
    (try (draw-capsule-raw p1 p2 (double radius) (int slices) (int rings) color)
         (finally (ffi/free p1) (ffi/free p2)))))

(defn draw-capsule-wires!
  "DrawCapsuleWires. :start-pos :end-pos :radius :slices :rings :color."
  [& {:keys [start-pos end-pos radius slices rings color]
      :or {start-pos [0.0 0.0 0.0]
           end-pos [0.0 1.0 0.0]
           radius 0.5
           slices 8
           rings 8
           color BLACK}}]
  (let [p1 (vec3->ptr! start-pos)
        p2 (vec3->ptr! end-pos)]
    (try (draw-capsule-wires-raw p1 p2 (double radius) (int slices) (int rings) color)
         (finally (ffi/free p1) (ffi/free p2)))))

;; --- ground plane, genuinely by value (camera-3d-split-screen) ----------
;; moved to net.b12n.raylib.native
(def ^:private vec2->ptr! native/vec2->ptr!)

(ffi/defcfn ^:private draw-plane-raw "DrawPlane"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   :uint]
  :void)

(defn draw-plane!
  "DrawPlane. :pos :size :color."
  [& {:keys [pos size color]
      :or {pos [0.0 0.0 0.0]
           size [1.0 1.0]
           color BLACK}}]
  (let [p (vec3->ptr! pos)
        s (vec2->ptr! size)]
    (try (draw-plane-raw p s color)
         (finally (ffi/free p) (ffi/free s)))))

;; --- window/monitor diagnostics, genuinely by value (highdpi-testbed) ---
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/toggle-borderless-windowed! or rl/get-window-scale-dpi keeps working
;; unchanged.
(def toggle-borderless-windowed! core/toggle-borderless-windowed!)
(def get-window-scale-dpi core/get-window-scale-dpi)
(def get-window-position core/get-window-position)

;; --- hashing + base64 (compute-hash) --------------------------------------
;; Moved to net.b12n.raylib.util. Re-exported here so every example that says
;; rl/compute-md5 or rl/base64-encode keeps working unchanged.
(def compute-crc32 util/compute-crc32)
(def compute-md5 util/compute-md5)
(def compute-sha1 util/compute-sha1)
(def compute-sha256 util/compute-sha256)
(def base64-encode util/base64-encode)

;; --- a persistent native Camera3D, mutated by UpdateCamera (camera-3d-free) --
;; UpdateCamera reads the mouse/wheel/keys itself and writes position/target/up
;; back into the SAME struct, so (unlike with-camera-3d's per-frame map) this
;; buffer has to survive across frames -- allocate it once outside the loop.
(ffi/defcfn update-camera! "UpdateCamera" [:pointer :int] :void)
(ffi/defcfn disable-cursor! "DisableCursor" [] :void)
(ffi/defcfn enable-cursor! "EnableCursor" [] :void)

(def ^:const CAMERA-CUSTOM 0)
(def ^:const CAMERA-FREE 1)
(def ^:const CAMERA-ORBITAL 2)
(def ^:const CAMERA-FIRST-PERSON 3)
(def ^:const CAMERA-THIRD-PERSON 4)

(defn camera3d-alloc
  "A persistent native Camera3D from the same keys with-camera-3d takes.
  Pair with camera3d-free!."
  [& {:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z fovy projection]
      :or {pos-x 0
           pos-y 0
           pos-z 0
           target-x 0
           target-y 0
           target-z 0
           up-x 0
           up-y 1
           up-z 0
           fovy 45
           projection 0}}]
  (let [cam (ffi/alloc (ffi/layout-size camera3d-layout))]
    (ffi/write-field cam camera3d-layout [:position :x] (double pos-x))
    (ffi/write-field cam camera3d-layout [:position :y] (double pos-y))
    (ffi/write-field cam camera3d-layout [:position :z] (double pos-z))
    (ffi/write-field cam camera3d-layout [:target :x] (double target-x))
    (ffi/write-field cam camera3d-layout [:target :y] (double target-y))
    (ffi/write-field cam camera3d-layout [:target :z] (double target-z))
    (ffi/write-field cam camera3d-layout [:up :x] (double up-x))
    (ffi/write-field cam camera3d-layout [:up :y] (double up-y))
    (ffi/write-field cam camera3d-layout [:up :z] (double up-z))
    (ffi/write-field cam camera3d-layout :fovy (double fovy))
    (ffi/write-field cam camera3d-layout :projection (int projection))
    cam))

(defn camera3d-free!
  [cam]
  (ffi/free cam))

(defn camera3d-set-target!
  [cam [x y z]]
  (ffi/write-field cam camera3d-layout [:target :x] (double x))
  (ffi/write-field cam camera3d-layout [:target :y] (double y))
  (ffi/write-field cam camera3d-layout [:target :z] (double z)))

;; --- splines by Vector2, genuinely by value (splines-drawing) -----------
;; DrawSplineSegment* takes every point as a Vector2 by value; each is staged
;; via vec2->ptr! above, the same struct DrawPlane/DrawCircleGradient/
;; GetWorldToScreen already use. Point-in-circle hit testing and drawing a
;; line/circle from an [x y] pair have no genuine need for the Vector2 ABI
;; here (a plain distance check, and the scalar rl/line!/rl/circle! already
;; in this file), so this suite skips CheckCollisionPointCircle/DrawLineV/
;; DrawCircleV rather than binding a redundant path to the same result.

(ffi/defcfn ^:private draw-spline-segment-linear-raw "DrawSplineSegmentLinear"
  [[:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   :float :uint]
  :void)

(defn spline-segment-linear!
  "DrawSplineSegmentLinear. :p1 :p2 :thick :color."
  [& {:keys [p1 p2 thick color]
      :or {p1 [0.0 0.0]
           p2 [0.0 0.0]
           thick 1.0
           color BLACK}}]
  (let [a (vec2->ptr! p1)
        b (vec2->ptr! p2)]
    (try (draw-spline-segment-linear-raw a b (double thick) color)
         (finally (ffi/free a) (ffi/free b)))))

(ffi/defcfn ^:private draw-spline-segment-basis-raw "DrawSplineSegmentBasis"
  [[:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   :float :uint]
  :void)

(defn spline-segment-basis!
  "DrawSplineSegmentBasis. :p1 :p2 :p3 :p4 :thick :color, a B-spline segment
  over four control points."
  [& {:keys [p1 p2 p3 p4 thick color]
      :or {p1 [0.0 0.0]
           p2 [0.0 0.0]
           p3 [0.0 0.0]
           p4 [0.0 0.0]
           thick 1.0
           color BLACK}}]
  (let [a (vec2->ptr! p1)
        b (vec2->ptr! p2)
        c (vec2->ptr! p3)
        d (vec2->ptr! p4)]
    (try (draw-spline-segment-basis-raw a b c d (double thick) color)
         (finally (ffi/free a) (ffi/free b) (ffi/free c) (ffi/free d)))))

(ffi/defcfn ^:private draw-spline-segment-catmullrom-raw "DrawSplineSegmentCatmullRom"
  [[:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   :float :uint]
  :void)

(defn spline-segment-catmull-rom!
  "DrawSplineSegmentCatmullRom. :p1 :p2 :p3 :p4 :thick :color, the curve
  passing through p2..p3 shaped by the p1/p4 tangent points."
  [& {:keys [p1 p2 p3 p4 thick color]
      :or {p1 [0.0 0.0]
           p2 [0.0 0.0]
           p3 [0.0 0.0]
           p4 [0.0 0.0]
           thick 1.0
           color BLACK}}]
  (let [a (vec2->ptr! p1)
        b (vec2->ptr! p2)
        c (vec2->ptr! p3)
        d (vec2->ptr! p4)]
    (try (draw-spline-segment-catmullrom-raw a b c d (double thick) color)
         (finally (ffi/free a) (ffi/free b) (ffi/free c) (ffi/free d)))))

(ffi/defcfn ^:private draw-spline-segment-bezier-cubic-raw "DrawSplineSegmentBezierCubic"
  [[:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   :float :uint]
  :void)

(defn spline-segment-bezier-cubic!
  "DrawSplineSegmentBezierCubic. :p1 :c2 :c3 :p4 :thick :color, a cubic
  Bezier segment from p1 to p4 with control points c2/c3."
  [& {:keys [p1 c2 c3 p4 thick color]
      :or {p1 [0.0 0.0]
           c2 [0.0 0.0]
           c3 [0.0 0.0]
           p4 [0.0 0.0]
           thick 1.0
           color BLACK}}]
  (let [a (vec2->ptr! p1)
        b (vec2->ptr! c2)
        c (vec2->ptr! c3)
        d (vec2->ptr! p4)]
    (try (draw-spline-segment-bezier-cubic-raw a b c d (double thick) color)
         (finally (ffi/free a) (ffi/free b) (ffi/free c) (ffi/free d)))))

;; --- rays: picking a point in the 3D scene, all by value -----------------
;; GetScreenToWorldRay is the exact inverse of GetWorldToScreen above, and it is
;; bound the same way: a by-value Vector2 in, a by-value Camera3D in, a by-value
;; Ray out. GetRayCollisionBox then takes that Ray with an axis-aligned
;; BoundingBox and hands back a RayCollision, whose first field is a one-byte C
;; _Bool rather than an int. `:bool` is what reads that correctly, and it is also
;; what makes the layout land `distance` at offset 4 instead of 1. Sizes are
;; asserted at load rather than assumed, since a wrong offset here reads a
;; plausible float out of the wrong bytes and never errors.
(def ^:private ray-layout
  (ffi/layout [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]))

(def ^:private ray-collision-layout
  (ffi/layout [:struct [[:hit :bool]
                        [:distance :float]
                        [:point  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:normal [:struct [[:x :float] [:y :float] [:z :float]]]]]]))

(assert (= 24 (ffi/layout-size ray-layout)) "Ray is two Vector3s, 24 bytes")
(assert (= 32 (ffi/layout-size ray-collision-layout))
        "RayCollision is bool + pad + float + two Vector3s, 32 bytes")

(ffi/defcfn ^:private get-screen-to-world-ray-raw "GetScreenToWorldRay"
  [[:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]]]
  [:by-value [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                       [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]])

(ffi/defcfn ^:private get-ray-collision-box-raw "GetRayCollisionBox"
  [[:by-value [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]]
   [:by-value [:struct [[:min [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:max [:struct [[:x :float] [:y :float] [:z :float]]]]]]]]
  [:by-value [:struct [[:hit :bool]
                       [:distance :float]
                       [:point  [:struct [[:x :float] [:y :float] [:z :float]]]]
                       [:normal [:struct [[:x :float] [:y :float] [:z :float]]]]]]])

(ffi/defcfn ^:private draw-ray-raw "DrawRay"
  [[:by-value [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]]
   :uint]
  :void)

(ffi/defcfn ^:private cursor-hidden-raw "IsCursorHidden" [] :int)

(defn cursor-hidden?
  "IsCursorHidden, so an example can ask whether it currently owns the pointer
  rather than tracking a flag of its own alongside disable-cursor!."
  []
  (not (zero? (bit-and (cursor-hidden-raw) 0xff))))

(defn- ray->ptr!
  "Allocate a ray-layout buffer from {:position [x y z] :direction [x y z]}.
  Caller frees."
  [{:keys [position direction]}]
  (let [[px py pz] position
        [dx dy dz] direction
        p (ffi/alloc (ffi/layout-size ray-layout))]
    (ffi/write-field p ray-layout [:position :x] (double px))
    (ffi/write-field p ray-layout [:position :y] (double py))
    (ffi/write-field p ray-layout [:position :z] (double pz))
    (ffi/write-field p ray-layout [:direction :x] (double dx))
    (ffi/write-field p ray-layout [:direction :y] (double dy))
    (ffi/write-field p ray-layout [:direction :z] (double dz))
    p))

(defn screen-to-world-ray
  "GetScreenToWorldRay. `screen` is [x y] in window coordinates and `camera`
  takes the same keys as with-camera-3d's opts map, so one map can be shared
  between the ray and the frame it is picking in. Returns
  {:position [x y z] :direction [x y z]}, the unit direction included."
  [[sx sy] camera]
  (let [pos (ffi/alloc (ffi/layout-size vector2-layout))
        cam (camera3d-alloc camera)
        out (ffi/alloc (ffi/layout-size ray-layout))]
    (try
      (ffi/write-field pos vector2-layout :x (double sx))
      (ffi/write-field pos vector2-layout :y (double sy))
      (get-screen-to-world-ray-raw out pos cam)
      {:position [(ffi/read-field out ray-layout [:position :x])
                  (ffi/read-field out ray-layout [:position :y])
                  (ffi/read-field out ray-layout [:position :z])]
       :direction [(ffi/read-field out ray-layout [:direction :x])
                   (ffi/read-field out ray-layout [:direction :y])
                   (ffi/read-field out ray-layout [:direction :z])]}
      (finally
        (ffi/free pos)
        (camera3d-free! cam)
        (ffi/free out)))))

(defn ray-collision-box
  "GetRayCollisionBox against the axis-aligned box spanning `lo` to `hi`, both
  [x y z]. Returns {:hit? :distance :point :normal}; everything but :hit? is
  meaningless when :hit? is false, exactly as in the C."
  [ray lo hi]
  (let [r (ray->ptr! ray)
        box (ffi/alloc (ffi/layout-size ray-layout))     ; BoundingBox is two Vector3s too
        out (ffi/alloc (ffi/layout-size ray-collision-layout))
        [lx ly lz] lo
        [hx hy hz] hi]
    (try
      (ffi/write-field box ray-layout [:position :x] (double lx))
      (ffi/write-field box ray-layout [:position :y] (double ly))
      (ffi/write-field box ray-layout [:position :z] (double lz))
      (ffi/write-field box ray-layout [:direction :x] (double hx))
      (ffi/write-field box ray-layout [:direction :y] (double hy))
      (ffi/write-field box ray-layout [:direction :z] (double hz))
      (get-ray-collision-box-raw out r box)
      {:hit? (ffi/read-field out ray-collision-layout :hit)
       :distance (ffi/read-field out ray-collision-layout :distance)
       :point [(ffi/read-field out ray-collision-layout [:point :x])
               (ffi/read-field out ray-collision-layout [:point :y])
               (ffi/read-field out ray-collision-layout [:point :z])]
       :normal [(ffi/read-field out ray-collision-layout [:normal :x])
                (ffi/read-field out ray-collision-layout [:normal :y])
                (ffi/read-field out ray-collision-layout [:normal :z])]}
      (finally
        (ffi/free r)
        (ffi/free box)
        (ffi/free out)))))

(defn draw-ray!
  "DrawRay: the ray drawn as a long line from its origin. Inside a BeginMode3D
  block, like the other draw-*! calls."
  [ray color]
  (let [r (ray->ptr! ray)]
    (try (draw-ray-raw r color)
         (finally (ffi/free r)))))

;; --- files: FilePathList, another 16-byte struct returned by value -------
;; Moved to net.b12n.raylib.files. Re-exported here so every example that says
;; rl/dropped-files or rl/directory-files keeps working unchanged.
(def get-working-directory files/get-working-directory)
(def get-prev-directory-path files/get-prev-directory-path)
(def get-file-name files/get-file-name)
(def file-dropped? files/file-dropped?)
(def directory-exists? files/directory-exists?)
(def dropped-files files/dropped-files)
(def directory-files files/directory-files)

;; --- the trace log, and the suite's first callback INTO jolt -------------
;; Moved to net.b12n.raylib.log. Re-exported here so every example that says
;; rl/on-trace-log! or rl/LOG-WARNING keeps working unchanged.
(def set-trace-log-callback log/set-trace-log-callback)
(def LOG-TRACE log/LOG-TRACE)
(def LOG-DEBUG log/LOG-DEBUG)
(def LOG-INFO log/LOG-INFO)
(def LOG-WARNING log/LOG-WARNING)
(def LOG-ERROR log/LOG-ERROR)
(def LOG-FATAL log/LOG-FATAL)
(def TRACE-LOG-BUFFER log/TRACE-LOG-BUFFER)
(def on-trace-log! log/on-trace-log!)
(def free-callable! log/free-callable!)

;; --- the audio stream callback, on a thread jolt never started ----------
;; on-trace-log! above is a callback raylib invokes on whichever thread called
;; into it, which is this one. SetAudioStreamCallback is the harder case: raudio
;; runs its own audio thread and calls back from there, so the entry point needs
;; jolt's :collect-safe, which reactivates the thread before any jolt code runs
;; on it. Without it the process dies with a memory fault no handler can catch.
;;
;; What happens inside is the caller's problem and a real-time one: the callback
;; owes raudio `frames` samples before the device underruns. Write them straight
;; into `buffer` with ffi/write and keep allocation out of the loop.
(ffi/defcfn ^:private set-audio-stream-callback-raw "SetAudioStreamCallback"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]
   :pointer]
  :void)

(defn on-audio-stream!
  "SetAudioStreamCallback with a jolt fn. `f` is called as (f buffer frames) on
  raudio's audio thread and must fill `buffer` with `frames` samples, written as
  :float at 4-byte strides for a 32-bit mono stream.

  Returns the callable pointer. Clear the callback with
  clear-audio-stream-callback! BEFORE freeing that pointer, or raudio is left
  calling a dead address from another thread."
  [stream f]
  (let [entry (ffi/foreign-callable f [:pointer :uint32] :void :collect-safe)]
    (set-audio-stream-callback-raw stream entry)
    entry))

(defn clear-audio-stream-callback!
  "Hand raudio a NULL callback, so it goes back to waiting for
  update-audio-stream refills and stops calling into jolt."
  [stream]
  (set-audio-stream-callback-raw stream ffi/null))

;; --- window placement ----------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/set-window-min-size or rl/set-window-monitor keeps working unchanged.
(def set-window-min-size core/set-window-min-size)
(def set-window-monitor core/set-window-monitor)

;; --- Image: raylib's CPU-side pixel buffer, by value ---------------------
;; Moved to net.b12n.raylib.images. Re-exported here so every example that says
;; rl/image-color or rl/image-text keeps working unchanged.
(def image-color images/image-color)
(def image-checked images/image-checked)
(def image-gradient-linear images/image-gradient-linear)
(def image-gradient-radial images/image-gradient-radial)
(def image-gradient-square images/image-gradient-square)
(def image-white-noise images/image-white-noise)
(def image-perlin-noise images/image-perlin-noise)
(def image-cellular images/image-cellular)
(def image-text images/image-text)

;; --- Image processing: raylib's own pixel operations ---------------------
;; Moved to net.b12n.raylib.images. Re-exported here so every example that says
;; rl/image-from-texture! or rl/image->texture keeps working unchanged.
(def image-from-texture! images/image-from-texture!)
(def image-copy! images/image-copy!)
(def unload-image! images/unload-image!)
(def image->texture images/image->texture)
(def image-format! images/image-format!)
(def image-color-invert! images/image-color-invert!)
(def image-color-grayscale! images/image-color-grayscale!)
(def image-color-tint! images/image-color-tint!)
(def image-color-contrast! images/image-color-contrast!)
(def image-color-brightness! images/image-color-brightness!)
(def image-flip-horizontal! images/image-flip-horizontal!)
(def image-flip-vertical! images/image-flip-vertical!)
(def image-blur-gaussian! images/image-blur-gaussian!)

;; --- Image geometry and convolution --------------------------------------
;; Moved to net.b12n.raylib.images. Re-exported here so every example that says
;; rl/image-crop! or rl/image-convolve! keeps working unchanged.
(def image-crop! images/image-crop!)
(def image-resize! images/image-resize!)
(def image-convolve! images/image-convolve!)
