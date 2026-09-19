(ns net.b12n.raylib.splines
  "DrawSplineSegment{Linear,Basis,CatmullRom,BezierCubic}, each taking its
  control points as a genuine by-value Vector2, staged through
  net.b12n.raylib.native/vec2->ptr!, the same struct DrawPlane and
  GetWorldToScreen use."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.color :as color]
   [net.b12n.raylib.native :as native]))

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
           color color/BLACK}}]
  (let [a (native/vec2->ptr! p1)
        b (native/vec2->ptr! p2)]
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
           color color/BLACK}}]
  (let [a (native/vec2->ptr! p1)
        b (native/vec2->ptr! p2)
        c (native/vec2->ptr! p3)
        d (native/vec2->ptr! p4)]
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
           color color/BLACK}}]
  (let [a (native/vec2->ptr! p1)
        b (native/vec2->ptr! p2)
        c (native/vec2->ptr! p3)
        d (native/vec2->ptr! p4)]
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
           color color/BLACK}}]
  (let [a (native/vec2->ptr! p1)
        b (native/vec2->ptr! c2)
        c (native/vec2->ptr! c3)
        d (native/vec2->ptr! p4)]
    (try (draw-spline-segment-bezier-cubic-raw a b c d (double thick) color)
         (finally (ffi/free a) (ffi/free b) (ffi/free c) (ffi/free d)))))
