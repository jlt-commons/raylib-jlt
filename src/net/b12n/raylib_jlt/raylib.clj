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
   [jolt.host]))

;; --- Color -------------------------------------------------------------------
;; Defined first: every drawing binding below takes a packed Color :uint, and
;; shade-color / cube! / sphere! reference `rgba` and the palette. Since jolt 0.4.0
;; ("unresolved symbols are compile errors") a symbol must be defined before its
;; first use in the file, in a fn body and in an :or destructuring default just as
;; much as at top level. Keep this section above its first use.
;; #region rgba
(defn rgba
  "Pack an RGBA color into the little-endian uint32 that raylib's `Color` struct
  is (r | g<<8 | b<<16 | a<<24), so it can cross the FFI boundary as a :uint."
  [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))
;; #endregion

;; raylib's named color palette (values from src/raylib.h).
(def LIGHTGRAY (rgba 200 200 200 255))   (def GRAY       (rgba 130 130 130 255))
(def DARKGRAY  (rgba 80 80 80 255))      (def YELLOW     (rgba 253 249 0 255))
(def GOLD      (rgba 255 203 0 255))     (def ORANGE     (rgba 255 161 0 255))
(def PINK      (rgba 255 109 194 255))   (def RED        (rgba 230 41 55 255))
(def MAROON    (rgba 190 33 55 255))     (def GREEN      (rgba 0 228 48 255))
(def LIME      (rgba 0 158 47 255))      (def DARKGREEN  (rgba 0 117 44 255))
(def SKYBLUE   (rgba 102 191 255 255))   (def BLUE       (rgba 0 121 241 255))
(def DARKBLUE  (rgba 0 82 172 255))      (def PURPLE     (rgba 200 122 255 255))
(def VIOLET    (rgba 135 60 190 255))    (def DARKPURPLE (rgba 112 31 126 255))
(def BEIGE     (rgba 211 176 131 255))   (def BROWN      (rgba 127 106 79 255))
(def DARKBROWN (rgba 76 63 47 255))      (def WHITE      (rgba 255 255 255 255))
(def BLACK     (rgba 0 0 0 255))         (def MAGENTA    (rgba 255 0 255 255))
(def RAYWHITE  (rgba 245 245 245 255))

;; --- window / lifecycle ------------------------------------------------------
(ffi/defcfn init-window    "InitWindow"   [:int :int :string] :void)
(ffi/defcfn set-target-fps "SetTargetFPS" [:int] :void)
;; Reassigns the key that closes the window. Passing KEY-NULL takes ESC away, so
;; an example can put its own confirmation in front of a close request rather
;; than being closed out from under it. See window-should-close.
(ffi/defcfn set-exit-key   "SetExitKey"   [:int] :void)
(ffi/defcfn close-window   "CloseWindow"  [] :void)
(ffi/defcfn ^:private should-close-raw "WindowShouldClose" [] :int)

;; --- frame -------------------------------------------------------------------
(ffi/defcfn begin-drawing      "BeginDrawing"     [] :void)
(ffi/defcfn end-drawing        "EndDrawing"       [] :void)
(ffi/defcfn clear-background   "ClearBackground"  [:uint] :void)       ; Color
(ffi/defcfn get-frame-time     "GetFrameTime"     [] :float)           ; seconds since last frame
(ffi/defcfn begin-scissor-mode "BeginScissorMode" [:int :int :int :int] :void)
(ffi/defcfn end-scissor-mode   "EndScissorMode"   [] :void)

;; --- 2D shapes + text (scalar variants; Color is the only by-value struct) ---
(ffi/defcfn draw-text            "DrawText"            [:string :int :int :int :uint] :void)
(ffi/defcfn draw-fps             "DrawFPS"             [:int :int] :void)
(ffi/defcfn measure-text         "MeasureText"         [:string :int] :int)
(ffi/defcfn draw-pixel           "DrawPixel"           [:int :int :uint] :void)
(ffi/defcfn draw-line            "DrawLine"            [:int :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle       "DrawRectangle"       [:int :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle-lines "DrawRectangleLines"  [:int :int :int :int :uint] :void)
(ffi/defcfn draw-rectangle-grad-v "DrawRectangleGradientV" [:int :int :int :int :uint :uint] :void)
;; #region draw-circle-binding
(ffi/defcfn draw-circle          "DrawCircle"          [:int :int :float :uint] :void)
;; #endregion
(ffi/defcfn draw-circle-lines    "DrawCircleLines"     [:int :int :float :uint] :void)
(ffi/defcfn draw-ellipse         "DrawEllipse"         [:int :int :float :float :uint] :void)

;; --- rlgl immediate mode (all scalar), for triangles / points ---------------
(ffi/defcfn rl-begin     "rlBegin"     [:int] :void)   ; RL-LINES / RL-TRIANGLES
(ffi/defcfn rl-end       "rlEnd"       [] :void)
(ffi/defcfn ^:private rl-vertex-2f-raw "rlVertex2f"  [:float :float] :void)

(defn rl-vertex-2f
  "One vertex, in whatever space the current matrix defines.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1]
  (rl-vertex-2f-raw (double a0) (double a1)))

(ffi/defcfn rl-color-4ub "rlColor4ub"  [:int :int :int :int] :void)  ; u8 args
(def ^:const RL-LINES 1)
(def ^:const RL-TRIANGLES 4)

(defn rl-color!
  "rlColor4ub from a packed rgba Color, so rlgl immediate mode can use the same
  Color values as the rest of the API."
  [color]
  (rl-color-4ub (bit-and color 0xff)
                (bit-and (bit-shift-right color 8) 0xff)
                (bit-and (bit-shift-right color 16) 0xff)
                (bit-and (bit-shift-right color 24) 0xff)))

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
(ffi/defcfn ^:private key-down-raw     "IsKeyDown"          [:int] :int)
(ffi/defcfn ^:private key-pressed-raw  "IsKeyPressed"       [:int] :int)
(ffi/defcfn ^:private mouse-down-raw   "IsMouseButtonDown"  [:int] :int)
(ffi/defcfn ^:private mouse-pressed-raw "IsMouseButtonPressed" [:int] :int)
(ffi/defcfn get-mouse-x      "GetMouseX"         [] :int)
(ffi/defcfn get-mouse-y      "GetMouseY"         [] :int)
(ffi/defcfn get-mouse-wheel  "GetMouseWheelMove" [] :float)
(ffi/defcfn get-random-value "GetRandomValue"    [:int :int] :int)
(ffi/defcfn get-char-pressed "GetCharPressed"    [] :int)   ; unicode codepoint; 0 = queue empty
(ffi/defcfn get-key-pressed  "GetKeyPressed"     [] :int)   ; keycode; 0 = queue empty

;; --- libc time (the one NON-raylib FFI) --------------------------------------
;; time()/localtime() live in libc (always loaded); jolt.ffi resolves them exactly
;; like raylib's symbols. localtime returns a pointer to a struct tm whose first
;; three ints are tm_sec, tm_min, tm_hour (offsets 0/4/8 on Darwin and glibc). This
;; is the repo's only non-raylib FFI call, proof jolt binds any C ABI symbol.
(ffi/defcfn ^:private c-time      "time"      [:pointer] :long)
(ffi/defcfn ^:private c-localtime "localtime" [:pointer] :pointer)

(defn local-time
  "Current wall-clock local time as [hour minute second] via libc time()/localtime()."
  []
  (let [buf (ffi/alloc 8)]
    (try
      (c-time buf)
      (let [tm (c-localtime buf)]
        [(ffi/read tm :int 8) (ffi/read tm :int 4) (ffi/read tm :int 0)])
      (finally (ffi/free buf)))))

;; --- screenshot hook plumbing (headless smoke tests) -------------------------
(ffi/defcfn take-screenshot       "TakeScreenshot"          [:string] :void)
(ffi/defcfn ^:private flush-batch "rlDrawRenderBatchActive" [] :void)

;; C-bool returns arrive in the low byte; mask so only 0/1 counts.
(defn window-should-close?
  []
  (not (zero? (bit-and (should-close-raw) 0xff))))

(defn key-down?
  [k]
  (not (zero? (bit-and (key-down-raw k) 0xff))))

(defn key-pressed?
  [k]
  (not (zero? (bit-and (key-pressed-raw k) 0xff))))

(defn mouse-down?
  [b]
  (not (zero? (bit-and (mouse-down-raw b) 0xff))))

(defn mouse-pressed?
  [b]
  (not (zero? (bit-and (mouse-pressed-raw b) 0xff))))

;; --- constants (raylib KeyboardKey / MouseButton) ----------------------------
(def ^:const KEY-NULL  0)   ; not a key: "nothing closes the window"
(def ^:const KEY-SPACE 32)  (def ^:const KEY-R     82)
(def ^:const KEY-W     87)  (def ^:const KEY-A     65)
(def ^:const KEY-S     83)  (def ^:const KEY-D     68)
(def ^:const KEY-RIGHT 262) (def ^:const KEY-LEFT  263)
(def ^:const KEY-DOWN  264) (def ^:const KEY-UP    265)
(def ^:const MOUSE-LEFT 0)
(def ^:const MOUSE-RIGHT 1)
(def ^:const MOUSE-MIDDLE 2)
(def ^:const KEY-BACKSPACE 259) (def ^:const KEY-ENTER 257)

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
(defn auto-quit-deadline
  "RAYLIB_APP_AUTO_QUIT_MS=<n> ends the loop after n ms, so a window example is
  smoke-testable with no person at the keyboard. Returns an absolute ms deadline
  or nil."
  []
  (when-let [v (System/getenv "RAYLIB_APP_AUTO_QUIT_MS")]
    (try (let [ms (Integer/parseInt v)]
           (when (pos? ms) (+ (System/currentTimeMillis) ms)))
         (catch Exception _ nil))))

(defn keep-running?
  "True while the window is open and any RAYLIB_APP_AUTO_QUIT_MS deadline is unmet."
  [deadline]
  (and (not (window-should-close?))
       (or (nil? deadline) (< (System/currentTimeMillis) deadline))))

(def ^:private shot-path (System/getenv "RAYLIB_APP_SHOT"))

;; Which frame to dump. Each example picks a frame that shows it at its best, and
;; that is the right default. Overriding it is how you ask a different question:
;; capture the same example at two distant frames and compare, and an identical
;; pair means the example does not animate unattended. That matters before
;; recording a GIF, because a static example records as a technically valid
;; animation of one repeated image, which no frame-count check can tell from a
;; real one.
(def ^:private shot-at
  (when-let [v (System/getenv "RAYLIB_APP_SHOT_AT")]
    (try (Integer/parseInt v) (catch Exception _ nil))))

(defn maybe-screenshot!
  "RAYLIB_APP_SHOT=/path dumps one PNG on frame `at`, or on RAYLIB_APP_SHOT_AT
  when that is set. Headless visual proof a
  frame rendered. Flushes raylib's batched geometry first (DrawText etc. is
  deferred until EndDrawing, so a mid-frame TakeScreenshot would miss it). raylib
  writes the file's basename into the current working directory."
  [frame at]
  (when (and shot-path (= frame (or shot-at at)))
    (flush-batch)
    (take-screenshot shot-path)
    (binding [*out* *err*] (println "[net.b12n.raylib-jlt] SHOT" shot-path))))

;; =============================================================================
;; Scalar extensions
;; =============================================================================
;; Everything below is appended rather than slotted into the sections above on
;; purpose: since jolt 0.4.0 a symbol must be defined before its first use in the
;; file, and the ordering of the sections above is load-bearing (see the Color
;; note at the top). Appending cannot disturb it. Nothing above refers to
;; anything here.

;; --- window state / config flags ---------------------------------------------
;; SetConfigFlags must be called BEFORE InitWindow; SetWindowState/ClearWindowState
;; take the same FLAG-* bits at runtime.
(ffi/defcfn set-config-flags   "SetConfigFlags"   [:uint] :void)
(ffi/defcfn set-window-state   "SetWindowState"   [:uint] :void)
(ffi/defcfn clear-window-state "ClearWindowState" [:uint] :void)
(ffi/defcfn toggle-fullscreen  "ToggleFullscreen" [] :void)
(ffi/defcfn get-screen-width   "GetScreenWidth"   [] :int)
(ffi/defcfn get-screen-height  "GetScreenHeight"  [] :int)
(ffi/defcfn get-time           "GetTime"          [] :double)
(ffi/defcfn ^:private window-state-raw   "IsWindowState"   [:uint] :int)
(ffi/defcfn ^:private window-resized-raw "IsWindowResized" [] :int)

(def ^:const FLAG-WINDOW-RESIZABLE   0x00000004)
(def ^:const FLAG-WINDOW-UNDECORATED 0x00000008)
(def ^:const FLAG-MSAA-4X-HINT       0x00000020)
(def ^:const FLAG-VSYNC-HINT         0x00000040)
(def ^:const FLAG-WINDOW-TOPMOST     0x00001000)
(def ^:const FLAG-WINDOW-HIGHDPI     0x00002000)

(defn window-state?
  "IsWindowState, is this FLAG-* bit currently set?"
  [flag]
  (not (zero? (bit-and (window-state-raw flag) 0xff))))

(defn window-resized?
  "IsWindowResized, did the window change size on the last frame?"
  []
  (not (zero? (bit-and (window-resized-raw) 0xff))))

;; --- monitors ----------------------------------------------------------------
;; GetMonitorPosition returns a Vector2 by value and so has no binding; the
;; scalar width/height/refresh/name queries cover what a monitor listing needs.
(ffi/defcfn get-monitor-count        "GetMonitorCount"       [] :int)
(ffi/defcfn get-current-monitor      "GetCurrentMonitor"     [] :int)
(ffi/defcfn get-monitor-width        "GetMonitorWidth"       [:int] :int)
(ffi/defcfn get-monitor-height       "GetMonitorHeight"      [:int] :int)
(ffi/defcfn get-monitor-refresh-rate "GetMonitorRefreshRate" [:int] :int)
(ffi/defcfn get-monitor-name         "GetMonitorName"        [:int] :string)

;; --- clipboard ---------------------------------------------------------------
;; GetClipboardText returns a const char* raylib owns; :string copies it out.
(ffi/defcfn set-clipboard-text "SetClipboardText" [:string] :void)
(ffi/defcfn get-clipboard-text "GetClipboardText" [] :string)

;; --- gamepad -----------------------------------------------------------------
(ffi/defcfn get-gamepad-axis-count    "GetGamepadAxisCount"    [:int] :int)
(ffi/defcfn get-gamepad-axis-movement "GetGamepadAxisMovement" [:int :int] :float)
(ffi/defcfn get-gamepad-name          "GetGamepadName"         [:int] :string)
(ffi/defcfn ^:private gamepad-available-raw "IsGamepadAvailable"     [:int] :int)
(ffi/defcfn ^:private gamepad-down-raw      "IsGamepadButtonDown"    [:int :int] :int)
(ffi/defcfn ^:private gamepad-pressed-raw   "IsGamepadButtonPressed" [:int :int] :int)
(ffi/defcfn ^:private gamepad-released-raw  "IsGamepadButtonReleased" [:int :int] :int)

(defn gamepad-available?
  [pad]
  (not (zero? (bit-and (gamepad-available-raw pad) 0xff))))

(defn gamepad-down?
  [pad button]
  (not (zero? (bit-and (gamepad-down-raw pad button) 0xff))))

(defn gamepad-pressed?
  [pad button]
  (not (zero? (bit-and (gamepad-pressed-raw pad button) 0xff))))

(defn gamepad-released?
  [pad button]
  (not (zero? (bit-and (gamepad-released-raw pad button) 0xff))))

;; raylib GamepadButton / GamepadAxis
(def ^:const PAD-UP     1)  (def ^:const PAD-RIGHT  2)
(def ^:const PAD-DOWN   3)  (def ^:const PAD-LEFT   4)
(def ^:const PAD-Y      5)  (def ^:const PAD-B      6)
(def ^:const PAD-A      7)  (def ^:const PAD-X      8)
(def ^:const PAD-L1     9)  (def ^:const PAD-L2    10)
(def ^:const PAD-R1    11)  (def ^:const PAD-R2    12)
(def ^:const PAD-SELECT 13) (def ^:const PAD-MENU  14)
(def ^:const PAD-START 15)
(def ^:const AXIS-LEFT-X 0) (def ^:const AXIS-LEFT-Y 1)
(def ^:const AXIS-RIGHT-X 2) (def ^:const AXIS-RIGHT-Y 3)

;; --- touch / gestures --------------------------------------------------------
;; On desktop raylib synthesises touch point 0 from the mouse, so these read as a
;; one-finger stream with no touchscreen attached.
(ffi/defcfn get-touch-point-count "GetTouchPointCount" [] :int)
(ffi/defcfn get-touch-point-id    "GetTouchPointId"    [:int] :int)
(ffi/defcfn get-touch-x           "GetTouchX"          [] :int)
(ffi/defcfn get-touch-y           "GetTouchY"          [] :int)
(ffi/defcfn get-gesture-detected  "GetGestureDetected" [] :int)
(ffi/defcfn set-gestures-enabled  "SetGesturesEnabled" [:uint] :void)

(def ^:const GESTURE-NONE 0)        (def ^:const GESTURE-TAP 1)
(def ^:const GESTURE-DOUBLETAP 2)   (def ^:const GESTURE-HOLD 4)
(def ^:const GESTURE-DRAG 8)        (def ^:const GESTURE-SWIPE-RIGHT 16)
(def ^:const GESTURE-SWIPE-LEFT 32) (def ^:const GESTURE-SWIPE-UP 64)
(def ^:const GESTURE-SWIPE-DOWN 128)
(def ^:const GESTURE-PINCH-IN 256)  (def ^:const GESTURE-PINCH-OUT 512)

;; --- remaining input predicates ----------------------------------------------
(ffi/defcfn set-mouse-cursor "SetMouseCursor" [:int] :void)
(ffi/defcfn ^:private key-released-raw   "IsKeyReleased"          [:int] :int)
(ffi/defcfn ^:private mouse-released-raw "IsMouseButtonReleased"  [:int] :int)

(defn key-released?
  [k]
  (not (zero? (bit-and (key-released-raw k) 0xff))))

(defn mouse-released?
  [b]
  (not (zero? (bit-and (mouse-released-raw b) 0xff))))

;; --- more KeyboardKey constants ----------------------------------------------
(def ^:const KEY-ESCAPE 256) (def ^:const KEY-TAB   258)
(def ^:const KEY-DELETE 261) (def ^:const KEY-HOME  268)
(def ^:const KEY-END    269) (def ^:const KEY-F1    290)
(def ^:const KEY-F2     291) (def ^:const KEY-F3    292)
(def ^:const KEY-LEFT-SHIFT 340) (def ^:const KEY-LEFT-CONTROL 341)
(def ^:const KEY-LEFT-SUPER 343)
(def ^:const KEY-ZERO 48) (def ^:const KEY-ONE   49) (def ^:const KEY-TWO   50)
(def ^:const KEY-THREE 51) (def ^:const KEY-FOUR 52) (def ^:const KEY-FIVE  53)
(def ^:const KEY-SIX  54) (def ^:const KEY-SEVEN 55) (def ^:const KEY-EIGHT 56)
(def ^:const KEY-NINE 57)
(def ^:const KEY-B 66) (def ^:const KEY-C 67) (def ^:const KEY-E 69)
(def ^:const KEY-F 70)
(def ^:const KEY-G 71) (def ^:const KEY-H 72) (def ^:const KEY-M 77)
(def ^:const KEY-N 78) (def ^:const KEY-P 80) (def ^:const KEY-Q 81)
(def ^:const KEY-T 84) (def ^:const KEY-V 86) (def ^:const KEY-X 88)
(def ^:const KEY-Y 89) (def ^:const KEY-Z 90)

;; --- extra scalar drawing ----------------------------------------------------
;; raylib 6.0 takes the centre as a by-value Vector2; 5.5 took two ints. The C
;; symbol name did not change, so a symbol-existence check (nm) says nothing and
;; only a header diff catches it - the 5.5 binding against a 6.0 library passes
;; two ints where a struct is expected and draws somewhere else entirely.
(ffi/defcfn ^:private draw-circle-gradient-raw "DrawCircleGradient"
  [[:by-value [:struct [[:x :float] [:y :float]]]] :float :uint :uint] :void)
(def ^:private vector2-layout (ffi/layout [:struct [[:x :float] [:y :float]]]))
(ffi/defcfn draw-rectangle-grad-h "DrawRectangleGradientH" [:int :int :int :int :uint :uint] :void)
(ffi/defcfn begin-blend-mode      "BeginBlendMode"         [:int] :void)
(ffi/defcfn end-blend-mode        "EndBlendMode"           [] :void)

(def ^:const BLEND-ALPHA 0)      (def ^:const BLEND-ADDITIVE 1)
(def ^:const BLEND-MULTIPLIED 2) (def ^:const BLEND-ADD-COLORS 3)
(def ^:const BLEND-SUBTRACT-COLORS 4)
(def ^:const BLEND-CUSTOM 6)     ; 5 is ALPHA_PREMULTIPLY, which nothing here uses

;; rlSetBlendFactors hands its three arguments straight to glBlendFunc and
;; glBlendEquation, so they are raw GL enums rather than raylib ones. rlgl only
;; reads them while BLEND-CUSTOM is the current mode, and it re-applies on a mode
;; change or a factor edit, so the order is: set the factors, then begin the mode.
(ffi/defcfn set-blend-factors "rlSetBlendFactors" [:int :int :int] :void)
(def ^:const GL-SRC-ALPHA 0x0302)
(def ^:const GL-MIN 0x8007)      (def ^:const GL-MAX 0x8008)

(defn circle-gradient!
  "DrawCircleGradient. :x :y :radius :inner :outer."
  [& {:keys [x y radius inner outer]
      :or {x 0
           y 0
           radius 10
           inner WHITE
           outer BLACK}}]
  ;; The kwarg surface stays scalar - the Vector2 is staged here so callers never
  ;; see the struct.
  (ffi/with-layout [c vector2-layout]
    (ffi/write-field c vector2-layout :x (double x))
    (ffi/write-field c vector2-layout :y (double y))
    (draw-circle-gradient-raw c (double radius) inner outer)))

(defn rect-pro!
  "A rotated rectangle as an rlgl quad, the immediate-mode stand-in for
  DrawRectanglePro. That call takes a Rectangle AND a Vector2 origin, both by
  value, so neither the packed-uint trick nor the pointer trick reaches it.

  :x :y place the ORIGIN, not the top-left corner, matching raylib: the rectangle
  is offset by :origin-x :origin-y from that point and then rotated about it. So
  a hand pinned at its base uses an origin of [0, half-thickness], and a shape
  spinning about its middle uses half its width and height.

  :rotation is in degrees, clockwise, because y grows downward. Emitted as two
  triangles rather than a quad, since RL-QUADS is not bound here."
  [& {:keys [x y width height origin-x origin-y rotation color]
      :or {x 0
           y 0
           width 10
           height 10
           origin-x 0
           origin-y 0
           rotation 0
           color BLACK}}]
  (let [t   (Math/toRadians (double rotation))
        cs  (Math/cos t)
        sn  (Math/sin t)
        ;; Corners relative to the origin, before rotation.
        pts (for [[dx dy] [[(- origin-x) (- origin-y)]
                           [(- width origin-x) (- origin-y)]
                           [(- width origin-x) (- height origin-y)]
                           [(- origin-x) (- height origin-y)]]]
              [(+ x (- (* dx cs) (* dy sn)))
               (+ y (+ (* dx sn) (* dy cs)))])
        [a b c d] (vec pts)]
    (rl-begin RL-TRIANGLES)
    (rl-color! color)
    ;; a-d-c then a-c-b, not a-b-c then a-c-d. raylib culls back faces, and the
    ;; clockwise order reads as a back face in screen coordinates where y grows
    ;; downward, so the quad is discarded and draws nothing at all. Same trap
    ;; rlgl-triangle documents, and the winding triangle-strip already uses.
    (doseq [[px py] [a d c a c b]]
      (rl-vertex-2f px py))
    (rl-end)))

(defn rect-gradient-h!
  "DrawRectangleGradientH (left->right). :x :y :width :height :left :right."
  [& {:keys [x y width height left right]
      :or {x 0
           y 0
           width 10
           height 10
           left WHITE
           right BLACK}}]
  (draw-rectangle-grad-h x y width height left right))

;; --- rlgl textures -----------------------------------------------------------
;; raylib's own texture API is unreachable from jolt: LoadTexture returns a
;; 20-byte Texture2D BY VALUE, which the AArch64 ABI hands back through the x8
;; indirect-result register, and Chez's foreign-procedure cannot express that.
;; rlgl's layer underneath it is entirely scalar, though, rlLoadTexture takes a
;; raw pixel pointer and returns the GL texture id as an unsigned int, and
;; rlSetTexture/rlTexCoord2f draw with it in immediate mode. So a texture here is
;; just that id: an int, no struct anywhere. What is lost is raylib's file
;; loaders (LoadTexture/LoadImage decode PNGs into an Image struct); textures in
;; this suite are therefore built pixel by pixel in native memory instead.
(ffi/defcfn rl-load-texture       "rlLoadTexture"       [:pointer :int :int :int :int] :uint)
(ffi/defcfn rl-unload-texture     "rlUnloadTexture"     [:uint] :void)
(ffi/defcfn rl-update-texture     "rlUpdateTexture"     [:uint :int :int :int :int :int :pointer] :void)
(ffi/defcfn rl-texture-parameters "rlTextureParameters" [:uint :int :int] :void)
(ffi/defcfn rl-set-texture        "rlSetTexture"        [:uint] :void)
(ffi/defcfn ^:private rl-tex-coord-2f-raw       "rlTexCoord2f"        [:float :float] :void)

(defn rl-tex-coord-2f
  "Texture coordinate for the next vertex.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1]
  (rl-tex-coord-2f-raw (double a0) (double a1)))

(ffi/defcfn ^:private rl-normal-3f-raw          "rlNormal3f"          [:float :float :float] :void)

(defn rl-normal-3f
  "Normal for the next vertex.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-normal-3f-raw (double a0) (double a1) (double a2)))

(def ^:const RL-QUADS 7)
(def ^:const PIXELFORMAT-R8G8B8A8 7)          ; rlPixelFormat, 32bpp RGBA
(def ^:const RL-TEXTURE-WRAP-S 0x2802)        (def ^:const RL-TEXTURE-WRAP-T 0x2803)
(def ^:const RL-TEXTURE-WRAP-REPEAT 0x2901)   (def ^:const RL-TEXTURE-WRAP-CLAMP 0x812F)
(def ^:const RL-TEXTURE-MAG-FILTER 0x2800)    (def ^:const RL-TEXTURE-MIN-FILTER 0x2801)
(def ^:const RL-TEXTURE-FILTER-NEAREST 0x2600)
(def ^:const RL-TEXTURE-FILTER-LINEAR 0x2601)

(defn texture-filter!
  "Set both min and mag filters on a texture id (RL-TEXTURE-FILTER-NEAREST for
  crisp pixel art, RL-TEXTURE-FILTER-LINEAR for smooth scaling)."
  [id filter]
  (rl-texture-parameters id RL-TEXTURE-MIN-FILTER filter)
  (rl-texture-parameters id RL-TEXTURE-MAG-FILTER filter))

(defn texture-wrap!
  "Set both S and T wrap modes on a texture id (REPEAT lets texcoords past 1.0
  tile the image, CLAMP stretches the edge pixel)."
  [id wrap]
  (rl-texture-parameters id RL-TEXTURE-WRAP-S wrap)
  (rl-texture-parameters id RL-TEXTURE-WRAP-T wrap))

(defn texture-from-fn
  "Build a `w` x `h` RGBA8 texture on the GPU from (f x y) -> packed Color, and
  return its rlgl texture id. Frees the staging buffer once rlLoadTexture has
  copied it to the GPU. Pair with `unload-texture!` when done.

  A packed Color is already r | g<<8 | b<<16 | a<<24, which is byte-for-byte what
  RGBA8 wants on a little-endian machine, so each pixel is one :uint write."
  [w h f]
  (let [buf (ffi/alloc (* w h 4))]
    (try
      (dotimes [y h]
        (dotimes [x w]
          (ffi/write buf :uint (f x y) (* 4 (+ x (* y w))))))
      (let [id (rl-load-texture buf w h PIXELFORMAT-R8G8B8A8 1)]
        (texture-filter! id RL-TEXTURE-FILTER-NEAREST)
        (texture-wrap! id RL-TEXTURE-WRAP-REPEAT)
        id)
      (finally (ffi/free buf)))))

(defn update-texture-from-fn!
  "rlUpdateTexture - re-upload the whole `w` x `h` RGBA8 surface behind an
  existing texture id from (f x y) -> packed Color. Cheaper than unloading and
  reloading, and every quad already drawing that id picks the new texels up with
  no change of its own."
  [id w h f]
  (let [buf (ffi/alloc (* w h 4))]
    (try
      (dotimes [y h]
        (dotimes [x w]
          (ffi/write buf :uint (f x y) (* 4 (+ x (* y w))))))
      (rl-update-texture id 0 0 w h PIXELFORMAT-R8G8B8A8 buf)
      (finally (ffi/free buf)))))

(defn unload-texture!
  "rlUnloadTexture, release a texture id created by texture-from-fn."
  [id]
  (rl-unload-texture id))

(defn texture!
  "Draw a texture id as an axis-aligned quad, the immediate-mode stand-in for
  DrawTexturePro (whose Rectangle/Vector2 args are by value). Emits the same
  topLeft -> bottomLeft -> bottomRight -> topRight winding raylib's own
  DrawTexturePro uses, so it batches identically.
    :x :y :width :height   destination rectangle in screen space
    :u0 :v0 :u1 :v1        source texcoords (default the whole texture; values
                           past 1.0 tile when the wrap mode is REPEAT, and
                           v0 > v1 flips vertically, which is what a framebuffer
                           texture needs)
    :tint                  packed Color multiplied into the texels (default WHITE)"
  [id & {:keys [x y width height u0 v0 u1 v1 tint]
         :or {x 0
              y 0
              width 100
              height 100
              u0 0.0
              v0 0.0
              u1 1.0
              v1 1.0
              tint WHITE}}]
  (let [x0 (double x) y0 (double y)
        x1 (double (+ x width)) y1 (double (+ y height))]
    (rl-set-texture id)
    (rl-begin RL-QUADS)
    (rl-color! tint)
    (rl-normal-3f 0.0 0.0 1.0)
    (rl-tex-coord-2f (double u0) (double v0)) (rl-vertex-2f x0 y0)
    (rl-tex-coord-2f (double u0) (double v1)) (rl-vertex-2f x0 y1)
    (rl-tex-coord-2f (double u1) (double v1)) (rl-vertex-2f x1 y1)
    (rl-tex-coord-2f (double u1) (double v0)) (rl-vertex-2f x1 y0)
    (rl-end)
    (rl-set-texture 0)))

;; --- rlgl framebuffers (render textures) -------------------------------------
;; raylib's LoadRenderTexture returns a RenderTexture2D by value and so is out of
;; reach for the same reason LoadTexture is, but rlgl's framebuffer calls are all
;; scalar: rlLoadFramebuffer returns the FBO id, rlFramebufferAttach wires a color
;; texture and a depth renderbuffer to it, and rlEnableFramebuffer binds it. What
;; BeginTextureMode adds on top is viewport and projection bookkeeping, which
;; with-render-texture replicates below.
(ffi/defcfn rl-load-framebuffer      "rlLoadFramebuffer"      [] :uint)
(ffi/defcfn rl-framebuffer-attach    "rlFramebufferAttach"    [:uint :uint :int :int :int] :void)
(ffi/defcfn rl-enable-framebuffer    "rlEnableFramebuffer"    [:uint] :void)
(ffi/defcfn rl-disable-framebuffer   "rlDisableFramebuffer"   [] :void)
(ffi/defcfn rl-unload-framebuffer    "rlUnloadFramebuffer"    [:uint] :void)
(ffi/defcfn rl-load-texture-depth    "rlLoadTextureDepth"     [:int :int :int] :uint)
(ffi/defcfn rl-viewport              "rlViewport"             [:int :int :int :int] :void)
(ffi/defcfn rl-matrix-mode           "rlMatrixMode"           [:int] :void)
(ffi/defcfn rl-load-identity         "rlLoadIdentity"         [] :void)
(ffi/defcfn rl-ortho                 "rlOrtho"                [:double :double :double :double :double :double] :void)
(ffi/defcfn rl-set-framebuffer-width  "rlSetFramebufferWidth"  [:int] :void)
(ffi/defcfn rl-set-framebuffer-height "rlSetFramebufferHeight" [:int] :void)
(ffi/defcfn rl-get-framebuffer-width  "rlGetFramebufferWidth"  [] :int)
(ffi/defcfn rl-get-framebuffer-height "rlGetFramebufferHeight" [] :int)
(ffi/defcfn rl-mult-matrix-f         "rlMultMatrixf"          [:pointer] :void)
(ffi/defcfn get-render-width         "GetRenderWidth"         [] :int)
(ffi/defcfn get-render-height        "GetRenderHeight"        [] :int)
(ffi/defcfn ^:private framebuffer-complete-raw "rlFramebufferComplete" [:uint] :int)

(def ^:const RL-PROJECTION 0x1701)
(def ^:const RL-MODELVIEW  0x1700)
(def ^:const RL-ATTACHMENT-COLOR-CHANNEL0 0)
(def ^:const RL-ATTACHMENT-DEPTH 100)
(def ^:const RL-ATTACHMENT-TEXTURE2D 100)
(def ^:const RL-ATTACHMENT-RENDERBUFFER 200)

(defn render-texture
  "Create an off-screen render target: an FBO with a `w` x `h` RGBA8 color
  texture and a depth renderbuffer. Returns {:fbo :texture :width :height}, or
  nil if the driver reports the framebuffer incomplete. Pair with
  `unload-render-texture!`.

  The color texture starts as an uninitialised buffer of the right size, rgba
  black is written so a target that is drawn before it is first rendered into
  reads as transparent rather than as whatever was in that allocation."
  [w h]
  (let [fbo (rl-load-framebuffer)
        tex (texture-from-fn w h (fn [_ _] (rgba 0 0 0 0)))
        depth (rl-load-texture-depth w h 1)]     ; useRenderBuffer = true
    (texture-filter! tex RL-TEXTURE-FILTER-LINEAR)
    (texture-wrap! tex RL-TEXTURE-WRAP-CLAMP)
    (rl-framebuffer-attach fbo tex RL-ATTACHMENT-COLOR-CHANNEL0 RL-ATTACHMENT-TEXTURE2D 0)
    (rl-framebuffer-attach fbo depth RL-ATTACHMENT-DEPTH RL-ATTACHMENT-RENDERBUFFER 0)
    (when-not (zero? (bit-and (framebuffer-complete-raw fbo) 0xff))
      {:fbo fbo
       :texture tex
       :width w
       :height h})))

(defn unload-render-texture!
  "Release the FBO and its color texture. The depth renderbuffer goes with the
  FBO, so it needs no separate call."
  [{:keys [fbo texture]}]
  (rl-unload-texture texture)
  (rl-unload-framebuffer fbo))

(defn- restore-screen-projection!
  "Put the viewport and both matrices back the way raylib leaves them for window
  drawing. This is EndTextureMode's SetupViewport call plus the screen-scale
  matrix BeginDrawing multiplies in, reproduced from the two scalar getters that
  expose what CORE holds privately.

  The scale matters and is easy to miss. On a HiDPI display raylib keeps the
  window at its logical size (GetScreenWidth) while rendering at the physical one
  (GetRenderWidth), projects in physical pixels, and bridges the two with a
  modelview scale of render/screen. Restoring only the viewport and the
  projection leaves that scale at identity, and every subsequent frame draws at
  half size in the lower-left corner. rlGetFramebufferWidth is NOT that number:
  it reports the logical size, so it cannot stand in for GetRenderWidth here."
  []
  (let [rw (get-render-width)
        rh (get-render-height)
        sx (/ (double rw) (max 1 (get-screen-width)))
        sy (/ (double rh) (max 1 (get-screen-height)))
        m (ffi/alloc 64)]                       ; 16 floats, column-major
    (try
      (rl-viewport 0 0 rw rh)
      (rl-set-framebuffer-width rw)
      (rl-set-framebuffer-height rh)
      (rl-matrix-mode RL-PROJECTION)
      (rl-load-identity)
      (rl-ortho 0.0 (double rw) (double rh) 0.0 0.0 1.0)
      (rl-matrix-mode RL-MODELVIEW)
      (rl-load-identity)
      (dotimes [i 16] (ffi/write m :float 0.0 (* 4 i)))
      (ffi/write m :float sx 0)
      (ffi/write m :float sy 20)
      (ffi/write m :float 1.0 40)
      (ffi/write m :float 1.0 60)
      (rl-mult-matrix-f m)
      (finally (ffi/free m)))))

(defn with-render-texture
  "Run (f) with drawing redirected into `rt`, then restore the screen - the
  BeginTextureMode/EndTextureMode pair, spelled out in scalar rlgl calls.

  Both halves flush the batch first: rlgl defers geometry until a draw call is
  forced, so without the flush the shapes queued before the switch would be
  rendered into whichever target happens to be bound afterwards.

  Note the resulting texture is bottom-up in GL's convention: draw it back with
  :v0 1.0 :v1 0.0 (as `texture!`'s docstring notes) or the image appears
  upside down."
  [{:keys [fbo width height]} f]
  (flush-batch)
  (rl-enable-framebuffer fbo)
  (rl-viewport 0 0 width height)
  (rl-set-framebuffer-width width)
  (rl-set-framebuffer-height height)
  (rl-matrix-mode RL-PROJECTION)
  (rl-load-identity)
  (rl-ortho 0.0 (double width) (double height) 0.0 0.0 1.0)
  (rl-matrix-mode RL-MODELVIEW)
  (rl-load-identity)
  (try
    (f)
    (finally
      (flush-batch)
      (rl-disable-framebuffer)
      (restore-screen-projection!))))

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
(defn- staged
  [write-type values f]
  (let [p (ffi/alloc (* 4 (count values)))]
    (try
      (dotimes [i (count values)]
        (ffi/write p write-type
                   (if (= write-type :float)
                     (double (nth values i))
                     (int (nth values i)))
                   (* 4 i)))
      (f p)
      (finally (ffi/free p)))))

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

(def texture2d-layout
  (ffi/layout [:struct [[:id :uint] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]))

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
;; InitWindow reaches AppKit through GLFW, and macOS only lets NSApplication
;; initialize on the process main thread. An nREPL eval runs on a worker thread,
;; so calling an example's -main straight from a connected editor traps the whole
;; process: EXC_BREAKPOINT inside -[NSApplication run], with no Clojure exception
;; to catch and nothing in the REPL but a dropped connection.
;;
;; jolt.host/call-on-main-thread-async marshals the call onto the thread `jolt
;; nrepl-server` parks in its main pump, and invokes it inline when no pump is
;; running, which is what `bb <example>` does. So the one call is right from both.

(defn run!
  "Run an example's entry point `f` on the process main thread, the only thread
  macOS lets raylib open a window from. This is how an example starts from a
  connected editor:

      (comment
        (rl/run! -main))

  Calling `(-main)` directly over nREPL instead kills the whole jolt process,
  editor connection included, because the eval runs on a worker thread and macOS
  traps any thread but the main one initializing AppKit.

  Returns immediately under `jolt nrepl-server`: the window loop takes the main
  thread and the REPL stays free. Under `bb <example>` there is no pump and the
  caller is already the main thread, so `f` runs inline and this wrapper changes
  nothing."
  [f]
  (jolt.host/call-on-main-thread-async f))

;; --- backface culling --------------------------------------------------------
;; raylib culls back faces by default, which is why the fans and quads above are
;; wound to raylib's front-facing order (see the note in sector!). An example
;; that decides visibility ITSELF needs the cull switched off, because a
;; screen-space test is not a winding rule and the two disagree: helitorus keeps
;; a triangle when the 2D cross product of its edges is positive, which is
;; exactly the orientation raylib treats as back-facing, so with culling on the
;; faces it keeps are the faces raylib drops and the surface renders inside-out.
;; Disable it, do the test, and both windings reach the rasterizer.
(ffi/defcfn rl-disable-backface-culling "rlDisableBackfaceCulling" [] :void)
(ffi/defcfn rl-enable-backface-culling  "rlEnableBackfaceCulling"  [] :void)

;; --- mouse position / cursor -------------------------------------------------
;; SetMousePosition warps the pointer; HideCursor and ShowCursor toggle whether
;; it is drawn. Together they are mouse-look: read the offset from the window
;; centre, turn by it, warp back to the centre, and the pointer can turn forever
;; without leaving the window or being visible while it does (doom).
(ffi/defcfn set-mouse-position "SetMousePosition" [:int :int] :void)
(ffi/defcfn hide-cursor        "HideCursor"       [] :void)
(ffi/defcfn show-cursor        "ShowCursor"       [] :void)

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
(def ^:private vector3-layout
  (ffi/layout [:struct [[:x :float] [:y :float] [:z :float]]]))

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
(def ^:const KEY-I 73) (def ^:const KEY-J 74) (def ^:const KEY-K 75)
(def ^:const KEY-L 76) (def ^:const KEY-O 79) (def ^:const KEY-U 85)
(def ^:const KEY-APOSTROPHE 39) (def ^:const KEY-COMMA 44)
(def ^:const KEY-MINUS 45) (def ^:const KEY-PERIOD 46)
(def ^:const KEY-SLASH 47) (def ^:const KEY-SEMICOLON 59)
(def ^:const KEY-EQUAL 61) (def ^:const KEY-LEFT-BRACKET 91)
(def ^:const KEY-BACKSLASH 92) (def ^:const KEY-RIGHT-BRACKET 93)
(def ^:const KEY-GRAVE 96) (def ^:const KEY-INSERT 260)
(def ^:const KEY-PAGE-UP 266) (def ^:const KEY-PAGE-DOWN 267)
(def ^:const KEY-CAPS-LOCK 280) (def ^:const KEY-PRINT-SCREEN 283)
(def ^:const KEY-PAUSE 284) (def ^:const KEY-F4 293)
(def ^:const KEY-F5 294) (def ^:const KEY-F6 295) (def ^:const KEY-F7 296)
(def ^:const KEY-F8 297) (def ^:const KEY-F9 298) (def ^:const KEY-F10 299)
(def ^:const KEY-F11 300) (def ^:const KEY-F12 301)
(def ^:const KEY-LEFT-ALT 342) (def ^:const KEY-RIGHT-SHIFT 344)
(def ^:const KEY-RIGHT-CONTROL 345) (def ^:const KEY-RIGHT-ALT 346)

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
(defn- vec2->ptr!
  "Allocate a vector2-layout buffer and write [x y] into it. Caller frees."
  [[x y]]
  (let [p (ffi/alloc (ffi/layout-size vector2-layout))]
    (ffi/write-field p vector2-layout :x (double x))
    (ffi/write-field p vector2-layout :y (double y))
    p))

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
(ffi/defcfn toggle-borderless-windowed! "ToggleBorderlessWindowed" [] :void)

(ffi/defcfn ^:private get-window-scale-dpi-raw "GetWindowScaleDPI"
  []
  [:by-value [:struct [[:x :float] [:y :float]]]])

(defn get-window-scale-dpi
  "GetWindowScaleDPI. Returns [x y]."
  []
  (let [out (ffi/alloc (ffi/layout-size vector2-layout))]
    (try
      (get-window-scale-dpi-raw out)
      [(ffi/read-field out vector2-layout :x)
       (ffi/read-field out vector2-layout :y)]
      (finally (ffi/free out)))))

(ffi/defcfn ^:private get-window-position-raw "GetWindowPosition"
  []
  [:by-value [:struct [[:x :float] [:y :float]]]])

(defn get-window-position
  "GetWindowPosition. Returns [x y]."
  []
  (let [out (ffi/alloc (ffi/layout-size vector2-layout))]
    (try
      (get-window-position-raw out)
      [(ffi/read-field out vector2-layout :x)
       (ffi/read-field out vector2-layout :y)]
      (finally (ffi/free out)))))

;; --- hashing + base64 (compute-hash) --------------------------------------
(ffi/defcfn compute-crc32 "ComputeCRC32" [:string :int] :uint)
(ffi/defcfn ^:private compute-md5-raw "ComputeMD5" [:string :int] :pointer)
(ffi/defcfn ^:private compute-sha1-raw "ComputeSHA1" [:string :int] :pointer)
(ffi/defcfn ^:private compute-sha256-raw "ComputeSHA256" [:string :int] :pointer)
(ffi/defcfn ^:private encode-base64-raw "EncodeDataBase64" [:string :int :pointer] :string)

(defn- read-words
  "n consecutive :uint (4-byte) words at ptr, as a vector. ComputeMD5/SHA1/
  SHA256 return a pointer into raylib's own static buffer (per raylib.h's
  own comment), so there's nothing to free here."
  [ptr n]
  (mapv (fn [i] (bit-and (ffi/read ptr :int (* i 4)) 0xffffffff)) (range n)))

(defn compute-md5
  "ComputeMD5. 4 u32 words."
  [s]
  (read-words (compute-md5-raw s (count s)) 4))

(defn compute-sha1
  "ComputeSHA1. 5 u32 words."
  [s]
  (read-words (compute-sha1-raw s (count s)) 5))

(defn compute-sha256
  "ComputeSHA256. 8 u32 words."
  [s]
  (read-words (compute-sha256-raw s (count s)) 8))

(defn base64-encode
  "EncodeDataBase64. The C's own comment admits every recompute leaks the
  malloc'd result (\"memory must be MemFree()\", never called in the
  upstream example either); a demo box's worth of Base64 text per ENTER
  press is not worth chasing across this FFI boundary."
  [s]
  (let [out-size (ffi/alloc 4)]
    (try (or (encode-base64-raw s (count s) out-size) "")
         (finally (ffi/free out-size)))))

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
