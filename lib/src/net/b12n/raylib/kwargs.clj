(ns net.b12n.raylib.kwargs
  "The keyword-argument drawing API: raylib's C functions are positional,
  so these wrappers take keyword arguments so example code reads
  self-descriptively, e.g. (rl/text! \"hi\" :x 10 :y 20 :color rl/RED)
  instead of (draw-text \"hi\" 10 20 20 rl/RED). This is the top of the
  library's dependency graph and what almost every example actually
  calls; it defines no FFI binding of its own, only names the arguments
  of ones that live in color, core, rlgl, shapes and text.

  The C functions behind the shape wrappers take their positions and
  sizes as int. A double reaching one throws \"invalid foreign-procedure
  argument 0.0\" on the first draw, which compiling, linting and
  formatting all miss: it surfaces only when a frame actually renders.
  Callers compute positions in floating point all the time (mouse
  deltas, interpolation, trigonometry), so the coercion lives here
  rather than at every call site."
  (:require
   [net.b12n.raylib.color :as color]
   [net.b12n.raylib.core :as core]
   [net.b12n.raylib.rlgl :as rlgl]
   [net.b12n.raylib.shapes :as shapes]
   [net.b12n.raylib.text :as text]))

;; --- keyword-argument drawing API ---------------------------------------------
;; raylib's C functions are positional; these wrappers take keyword arguments so
;; example code reads self-descriptively, e.g. (rl/text! "hi" :x 10 :y 20
;; :color rl/RED) instead of (draw-text "hi" 10 20 20 rl/RED). The raw bindings
;; live in color, core, rlgl, shapes and text, required above; these just name
;; their arguments.

(defn window!
  "InitWindow with keyword args. :width :height :title."
  [& {:keys [width height title]
      :or {width 800
           height 450
           title "raylib"}}]
  (core/init-window width height title))

(defn text!
  "DrawText. :x :y :size :color."
  [s & {:keys [x y size color]
        :or {x 0
             y 0
             size 20
             color color/BLACK}}]
  (text/draw-text s (int x) (int y) (int size) color))

(defn text-width
  "MeasureText. :size."
  [s & {:keys [size]
        :or {size 20}}]
  (text/measure-text s size))

(defn fps!
  "DrawFPS. :x :y."
  [& {:keys [x y]
      :or {x 10
           y 10}}]
  (text/draw-fps (int x) (int y)))

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
           color color/BLACK}}]
  (shapes/draw-rectangle (int x) (int y) (int width) (int height) color))

(defn rect-lines!
  "DrawRectangleLines. :x :y :width :height :color."
  [& {:keys [x y width height color]
      :or {x 0
           y 0
           width 10
           height 10
           color color/BLACK}}]
  (shapes/draw-rectangle-lines (int x) (int y) (int width) (int height) color))

(defn rect-gradient!
  "DrawRectangleGradientV (top->bottom). :x :y :width :height :top :bottom."
  [& {:keys [x y width height top bottom]
      :or {x 0
           y 0
           width 10
           height 10
           top color/WHITE
           bottom color/BLACK}}]
  (shapes/draw-rectangle-grad-v x y width height top bottom))

(defn circle!
  "DrawCircle. :x :y :radius :color."
  [& {:keys [x y radius color]
      :or {x 0
           y 0
           radius 10
           color color/BLACK}}]
  (shapes/draw-circle (int x) (int y) (double radius) color))

(defn circle-lines!
  "DrawCircleLines. :x :y :radius :color."
  [& {:keys [x y radius color]
      :or {x 0
           y 0
           radius 10
           color color/BLACK}}]
  (shapes/draw-circle-lines (int x) (int y) (double radius) color))

(defn ellipse!
  "DrawEllipse. :x :y :rx :ry :color."
  [& {:keys [x y rx ry color]
      :or {x 0
           y 0
           rx 10
           ry 6
           color color/BLACK}}]
  (shapes/draw-ellipse (int x) (int y) (double rx) (double ry) color))

(defn line!
  "DrawLine. :x1 :y1 :x2 :y2 :color."
  [& {:keys [x1 y1 x2 y2 color]
      :or {x1 0
           y1 0
           x2 0
           y2 0
           color color/BLACK}}]
  (shapes/draw-line (int x1) (int y1) (int x2) (int y2) color))

(defn pixel!
  "DrawPixel. :x :y :color."
  [& {:keys [x y color]
      :or {x 0
           y 0
           color color/BLACK}}]
  (shapes/draw-pixel (int x) (int y) color))

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
           color color/BLACK}}]
  (let [d->r (/ Math/PI 180.0)
        span (- end-deg start-deg)
        rim (fn [deg]
              (let [t (* deg d->r)]
                [(+ cx (* radius (Math/sin t)))
                 (- cy (* radius (Math/cos t)))]))]
    (rlgl/rl-begin rlgl/RL-TRIANGLES)
    (rlgl/rl-color! color)
    (dotimes [k segments]
      (let [[x0 y0] (rim (+ start-deg (* span (/ (double k) segments))))
            [x1 y1] (rim (+ start-deg (* span (/ (double (inc k)) segments))))]
        (rlgl/rl-vertex-2f (double x0) (double y0))
        (rlgl/rl-vertex-2f (double cx) (double cy))
        (rlgl/rl-vertex-2f (double x1) (double y1))))
    (rlgl/rl-end)))

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
           color color/BLACK}}]
  (let [d->r (/ Math/PI 180.0)
        span (- end-deg start-deg)
        pt (fn [deg r]
             (let [t (* deg d->r)]
               [(+ cx (* r (Math/sin t))) (- cy (* r (Math/cos t)))]))]
    (rlgl/rl-begin rlgl/RL-TRIANGLES)
    (rlgl/rl-color! color)
    (dotimes [k segments]
      (let [d0 (+ start-deg (* span (/ (double k) segments)))
            d1 (+ start-deg (* span (/ (double (inc k)) segments)))
            [ix0 iy0] (pt d0 inner) [ox0 oy0] (pt d0 outer)
            [ix1 iy1] (pt d1 inner) [ox1 oy1] (pt d1 outer)]
        (rlgl/rl-vertex-2f (double ox0) (double oy0))
        (rlgl/rl-vertex-2f (double ix0) (double iy0))
        (rlgl/rl-vertex-2f (double ix1) (double iy1))
        (rlgl/rl-vertex-2f (double ox0) (double oy0))
        (rlgl/rl-vertex-2f (double ix1) (double iy1))
        (rlgl/rl-vertex-2f (double ox1) (double oy1))))
    (rlgl/rl-end)))

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
           color color/BLACK}}]
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
    (rlgl/rl-begin rlgl/RL-TRIANGLES)
    (rlgl/rl-color! color)
    (rlgl/rl-vertex-2f (double ax) (double ay))
    (rlgl/rl-vertex-2f (double bx) (double by))
    (rlgl/rl-vertex-2f (double cx) (double cy))
    (rlgl/rl-vertex-2f (double ax) (double ay))
    (rlgl/rl-vertex-2f (double cx) (double cy))
    (rlgl/rl-vertex-2f (double ex) (double ey))
    (rlgl/rl-end)))
