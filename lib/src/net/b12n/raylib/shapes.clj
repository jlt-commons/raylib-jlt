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

;; --- thick and rounded outlines ----------------------------------------------
;; These four take their Rectangle or Vector2 by value, which is why the suite
;; went without them for so long and why rounded-rectangle hand-rolls its corners
;; out of sectors. jolt 0.7.23 made [:by-value [:struct ...]] work, so new code
;; can call them directly. The descriptor has to be spelled out per signature: a
;; def'd alias is rejected, because it must be a compile-time literal.
;;
;; The struct itself is staged into a native buffer and passed by pointer, the
;; way net.b12n.raylib.splines stages its control points. Handing the raw call a
;; Clojure map instead fails at RUN time with a ClassCastException, not at
;; compile time, so the descriptor in the signature says nothing about how the
;; argument has to be built.
;;
;; No docstrings on these forms. The clj-kondo hook at .clj-kondo/hooks/jolt_ffi.clj
;; destructures ffi/defcfn positionally, so a docstring shifts the children along
;; and the var never gets interned. Documentation goes on the wrapper below.
(ffi/defcfn ^:private draw-rectangle-lines-ex-raw "DrawRectangleLinesEx"
  [[:by-value [:struct [[:x :float] [:y :float] [:width :float] [:height :float]]]]
   :float :uint] :void)

(ffi/defcfn ^:private draw-rectangle-rounded-raw "DrawRectangleRounded"
  [[:by-value [:struct [[:x :float] [:y :float] [:width :float] [:height :float]]]]
   :float :int :uint] :void)

(ffi/defcfn ^:private draw-rectangle-rounded-lines-ex-raw "DrawRectangleRoundedLinesEx"
  [[:by-value [:struct [[:x :float] [:y :float] [:width :float] [:height :float]]]]
   :float :int :float :uint] :void)

;; No DrawCircleLinesEx here. raylib's own header declares it, but the released
;; 6.0 this suite links does not export it: it arrived after the tag. Callers
;; wanting a thick circle outline use ring! with inner and outer radii, which is
;; what raylib implements DrawCircleLinesEx as anyway.

(defn rect-lines-ex!
  "DrawRectangleLinesEx: a rectangle outline `:thick` pixels wide, drawn inward
  from the edge. raylib clamps the thickness to half the shorter side, and
  guards the whole body with `if (thick > 0)`, so zero or less draws nothing at
  all rather than an outward band.
    :x :y :width :height   the rectangle
    :thick                 band width, inward; nothing is drawn at <= 0
    :color"
  [& {:keys [x y width height thick color]
      :or {x 0
           y 0
           width 10
           height 10
           thick 1.0
           color color/BLACK}}]
  (let [r (native/rect->ptr! [x y width height])]
    (try (draw-rectangle-lines-ex-raw r (double thick) color)
         (finally (ffi/free r)))))

(defn rect-rounded!
  "DrawRectangleRounded: a filled rectangle with rounded corners. `:roundness`
  runs 0.0 (square) to 1.0 (the corner radius is half the shorter side), and
  `:segments` is how many triangles each corner arc is built from.
    :x :y :width :height   the rectangle
    :roundness :segments   corner shape
    :color"
  [& {:keys [x y width height roundness segments color]
      :or {x 0
           y 0
           width 10
           height 10
           roundness 0.2
           segments 9
           color color/BLACK}}]
  (let [r (native/rect->ptr! [x y width height])]
    (try (draw-rectangle-rounded-raw r (double roundness) (int segments) color)
         (finally (ffi/free r)))))

(defn rect-rounded-lines-ex!
  "DrawRectangleRoundedLinesEx: the outline of a rounded rectangle, `:thick`
  pixels wide, drawn inward. Guarded by `if (thick >= 0)` the same way
  rect-lines-ex! is, so a negative thickness collapses to a hairline rather than
  growing outward. A `:roundness` of 0 delegates to DrawRectangleLinesEx.
    :x :y :width :height   the rectangle
    :roundness :segments   corner shape
    :thick                 band width, inward; degenerate at < 0
    :color"
  [& {:keys [x y width height roundness segments thick color]
      :or {x 0
           y 0
           width 10
           height 10
           roundness 0.2
           segments 9
           thick 1.0
           color color/BLACK}}]
  (let [r (native/rect->ptr! [x y width height])]
    (try (draw-rectangle-rounded-lines-ex-raw r (double roundness) (int segments)
                                              (double thick) color)
         (finally (ffi/free r)))))

