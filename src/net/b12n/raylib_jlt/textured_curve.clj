(ns net.b12n.raylib-jlt.textured-curve
  "raylib [textures] example - textured curve (`jolt -M:textured-curve`).

  Port of raylib's examples/textures/textures_textured_curve.c. A texture is
  laid along a cubic Bezier by walking the curve in segments and emitting one
  quad per segment, each rotated to sit square across the curve at that point.

  The two ideas worth taking away are both in draw-curve!. The quad's corners
  come from the normal of the segment's direction, which is the direction
  vector with its components swapped and one negated, so the ribbon stays the
  same width through a bend instead of pinching. And the v texture coordinate
  accumulates by arc length rather than by segment index, so the road markings
  keep an even spacing whether a segment is long or short. Dividing by index
  would bunch them up wherever the curve turns tightly.

  Because v runs well past 1.0 the texture wrap has to be REPEAT, which is the
  one setting that turns this from a stretched smear into a repeating road.

  rl/texture! is no use here: it emits one axis-aligned quad, and this needs a
  strip of many, each at its own angle. The rlgl immediate-mode calls are
  public for exactly this, the same way net.b12n.raylib-jlt.magnifying-glass
  builds its lens out of a triangle fan.

  No image files ship here, so the road is generated: asphalt, solid edge
  lines and a dashed centre line, painted with the ImageDraw* family.

  The curve flexes on its own. LEFT and RIGHT change the ribbon width, UP and
  DOWN change the segment count, and dropping the count low enough makes the
  faceting obvious, which is the honest way to see what the strip is made of."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const ROAD-W 64)
(def ^:const ROAD-H 128)

(def ^:const MIN-SEGMENTS 3)
(def ^:const MAX-SEGMENTS 48)
(def ^:const MIN-WIDTH 6.0)
(def ^:const MAX-WIDTH 80.0)

(defn- build-road!
  "A strip of road: asphalt, a solid white line down each edge and a dashed
  centre line. Tiles vertically, so the dashes have to divide ROAD-H evenly or
  the seam shows up as one short dash per repeat."
  []
  (let [seed (rl/image-color ROAD-W ROAD-H (rl/rgba 58 58 64 255))
        img (rl/image-from-texture! seed ROAD-W ROAD-H)]
    (rl/unload-texture! seed)
    (rl/image-draw-rectangle! img 4 0 5 ROAD-H (rl/rgba 235 235 235 255))
    (rl/image-draw-rectangle! img (- ROAD-W 9) 0 5 ROAD-H (rl/rgba 235 235 235 255))
    ;; four dashes of 16 on, 16 off fills 128 exactly
    (dotimes [i 4]
      (rl/image-draw-rectangle! img (- (quot ROAD-W 2) 3) (* i 32) 6 16
                                (rl/rgba 240 200 60 255)))
    (let [tex (rl/image->texture img)]
      (rl/unload-image! img)
      (rl/texture-wrap! tex rl/RL-TEXTURE-WRAP-REPEAT)
      (rl/texture-filter! tex rl/RL-TEXTURE-FILTER-LINEAR)
      tex)))

(defn- bezier-at
  "The cubic Bezier point at `t`, from the four control points."
  [[p0x p0y] [p1x p1y] [p2x p2y] [p3x p3y] t]
  (let [u (- 1.0 t)
        a (* u u u)
        b (* 3.0 u u t)
        c (* 3.0 u t t)
        d (* t t t)]
    [(+ (* a p0x) (* b p1x) (* c p2x) (* d p3x))
     (+ (* a p0y) (* b p1y) (* c p2y) (* d p3y))]))

(defn- draw-curve!
  "Lay `tex` along the Bezier as a strip of `segments` quads, each `width` out
  either side of the curve."
  [tex p0 p1 p2 p3 segments width]
  (rl/rl-set-texture tex)
  (loop [i 1
         [px py] p0
         [pnx pny] nil
         prev-v 0.0]
    (when (<= i segments)
      (let [t (/ (double i) segments)
            [cx cy] (bezier-at p0 p1 p2 p3 t)
            dx (- cx px)
            dy (- cy py)
            len (Math/sqrt (+ (* dx dx) (* dy dy)))
            ;; the normal is the delta with components swapped and one negated
            [nx ny] (if (zero? len) [0.0 0.0] [(/ (- dy) len) (/ dx len)])
            ;; the first segment has no earlier normal to carry, so it reuses
            ;; its own and the strip starts square rather than pinched
            [ppx ppy] (if pnx [pnx pny] [nx ny])
            ;; v by arc length, not by segment index: index would bunch the
            ;; markings wherever the curve turns tightly
            v (+ prev-v (/ len (* ROAD-H 2.0)))]
        (rl/rl-begin rl/RL-QUADS)
        (rl/rl-color! rl/WHITE)
        (rl/rl-normal-3f 0.0 0.0 1.0)
        (rl/rl-tex-coord-2f 0.0 prev-v)
        (rl/rl-vertex-2f (- px (* ppx width)) (- py (* ppy width)))
        (rl/rl-tex-coord-2f 1.0 prev-v)
        (rl/rl-vertex-2f (+ px (* ppx width)) (+ py (* ppy width)))
        (rl/rl-tex-coord-2f 1.0 v)
        (rl/rl-vertex-2f (+ cx (* nx width)) (+ cy (* ny width)))
        (rl/rl-tex-coord-2f 0.0 v)
        (rl/rl-vertex-2f (- cx (* nx width)) (- cy (* ny width)))
        (rl/rl-end)
        (recur (inc i) [cx cy] [nx ny] v))))
  (rl/rl-set-texture 0))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - textured curve"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        road (build-road!)]
    (loop [frame 0
           width 40.0
           segments 24]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! road)
        (let [width' (cond
                       (rl/key-down? rl/KEY-RIGHT) (min MAX-WIDTH (+ width 0.8))
                       (rl/key-down? rl/KEY-LEFT) (max MIN-WIDTH (- width 0.8))
                       :else width)
              segments' (cond
                          (rl/key-pressed? rl/KEY-UP) (min MAX-SEGMENTS (inc segments))
                          (rl/key-pressed? rl/KEY-DOWN) (max MIN-SEGMENTS (dec segments))
                          :else segments)
              t (* frame 0.015)
              ;; the two middle control points swing, so the ribbon flexes
              ;; without anyone touching it
              p0 [80.0 360.0]
              p1 [(+ 220.0 (* 120.0 (Math/sin t))) (+ 120.0 (* 60.0 (Math/cos (* t 1.3))))]
              p2 [(+ 560.0 (* 120.0 (Math/sin (* t 0.8)))) (+ 330.0 (* 70.0 (Math/sin (* t 1.1))))]
              p3 [720.0 110.0]]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 30 110 60 255))
          (draw-curve! road p0 p1 p2 p3 segments' width')
          (rl/text! "raylib [textures] example - textured curve"
                    {:x 20
                     :y 16
                     :size 20
                     :color rl/RAYWHITE})
          (rl/text! (str "width " (format "%.1f" width') "   segments " segments')
                    {:x 20
                     :y 44
                     :size 16
                     :color rl/RAYWHITE})
          (rl/text! "LEFT/RIGHT width, UP/DOWN segments"
                    {:x 20
                     :y (- H 30)
                     :size 16
                     :color rl/RAYWHITE})
          (app/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame) width' segments'))))))
