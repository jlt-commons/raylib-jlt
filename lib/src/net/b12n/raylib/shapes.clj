(ns net.b12n.raylib.shapes
  "2D shape primitives (DrawPixel, DrawLine, DrawRectangle*, DrawCircle*,
  DrawEllipse) plus the extra scalar drawing built on them: a circle gradient
  staged through a Vector2, a rotated rectangle drawn via rlgl immediate
  mode, and blend-mode control. Most of these bindings use raylib's
  scalar-argument variants, so the packed colour is usually the only thing
  crossing the FFI boundary by value. `draw-circle-gradient-raw` is the
  exception: raylib 6.0 takes its centre as a genuine by-value Vector2."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.color :as color]
   [net.b12n.raylib.native :as native]
   [net.b12n.raylib.rlgl :as rlgl]))

;; --- 2D shapes (scalar variants) ----------------------------------------------
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

;; --- extra scalar drawing ----------------------------------------------------
;; raylib 6.0 takes the centre as a by-value Vector2; 5.5 took two ints. The C
;; symbol name did not change, so a symbol-existence check (nm) says nothing and
;; only a header diff catches it - the 5.5 binding against a 6.0 library passes
;; two ints where a struct is expected and draws somewhere else entirely.
(ffi/defcfn ^:private draw-circle-gradient-raw "DrawCircleGradient"
  [[:by-value [:struct [[:x :float] [:y :float]]]] :float :uint :uint] :void)
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
           inner color/WHITE
           outer color/BLACK}}]
  ;; The kwarg surface stays scalar - the Vector2 is staged here so callers never
  ;; see the struct.
  (ffi/with-layout [c native/vector2-layout]
    (ffi/write-field c native/vector2-layout :x (double x))
    (ffi/write-field c native/vector2-layout :y (double y))
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
           color color/BLACK}}]
  (let [t   (Math/toRadians (double rotation))
        cs  (Math/cos t)
        sn  (Math/sin t)
        ;; Corners relative to the origin, before rotation.
        pts (for [[dx dy] [[(- origin-x) (- origin-y)]
                           [(- width origin-x) (- origin-y)]
                           [(- width origin-x) (- height origin-y)]
                           [(- origin-x) (- height origin-y)]]]
              [(+ x (- (* dx cs) (* dy sn)))
               (+ y (* dx sn) (* dy cs))])
        [a b c d] (vec pts)]
    (rlgl/rl-begin rlgl/RL-TRIANGLES)
    (rlgl/rl-color! color)
    ;; a-d-c then a-c-b, not a-b-c then a-c-d. raylib culls back faces, and the
    ;; clockwise order reads as a back face in screen coordinates where y grows
    ;; downward, so the quad is discarded and draws nothing at all. Same trap
    ;; rlgl-triangle documents, and the winding triangle-strip already uses.
    (doseq [[px py] [a d c a c b]]
      (rlgl/rl-vertex-2f px py))
    (rlgl/rl-end)))

(defn rect-gradient-h!
  "DrawRectangleGradientH (left->right). :x :y :width :height :left :right."
  [& {:keys [x y width height left right]
      :or {x 0
           y 0
           width 10
           height 10
           left color/WHITE
           right color/BLACK}}]
  (draw-rectangle-grad-h x y width height left right))
