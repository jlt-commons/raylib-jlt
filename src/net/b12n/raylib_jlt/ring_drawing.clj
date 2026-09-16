(ns net.b12n.raylib-jlt.ring-drawing
  "raylib [shapes] example - ring drawing. A filled annulus via rl/ring! whose sweep
  angle and inner radius breathe over time, with a stroked outline (inner + outer arcs
  and radial end caps). Port of shapes_ring_drawing (minus raygui sliders)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:private d->r (/ Math/PI 180.0))

(defn- polar
  [cx cy r deg]
  (let [t (* deg d->r)]
    [(+ cx (* r (Math/sin t))) (- cy (* r (Math/cos t)))]))

(defn- arc-stroke
  [cx cy r start end color]
  (let [n 96 span (- end start)]
    (dotimes [i n]
      (let [[x0 y0] (polar cx cy r (+ start (* span (/ (double i) n))))
            [x1 y1] (polar cx cy r (+ start (* span (/ (double (inc i)) n))))]
        (rl/line-ex! :x1 x0 :y1 y0 :x2 x1 :y2 y1 :thick 3 :color color)))))

(defn -main
  [& _]
  (rl/window! :title "raylib [shapes] example - ring drawing")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cx 400 cy 235 outer 150]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [inner (+ 75 (* 25.0 (Math/sin (* frame 0.03))))
              start (* frame 0.5)
              end   (+ start 120 (* 130.0 (+ 1.0 (Math/sin (* frame 0.017)))))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 22 24 30 255))
          (rl/ring! :cx cx :cy cy :inner inner :outer outer :start-deg start :end-deg end
                    :segments 120 :color (rl/rgba 90 170 240 255))
          ;; outline (dark, contrasts with the light-blue fill)
          (let [edge (rl/rgba 20 40 72 255)]
            (arc-stroke cx cy outer start end edge)
            (arc-stroke cx cy inner start end edge)
            (let [[ox0 oy0] (polar cx cy outer start) [ix0 iy0] (polar cx cy inner start)
                  [ox1 oy1] (polar cx cy outer end)   [ix1 iy1] (polar cx cy inner end)]
              (rl/line-ex! :x1 ix0 :y1 iy0 :x2 ox0 :y2 oy0 :thick 3 :color edge)
              (rl/line-ex! :x1 ix1 :y1 iy1 :x2 ox1 :y2 oy1 :thick 3 :color edge)))
          (rl/text! "ring! - rlgl annulus (animated)" :x 10 :y 10 :size 20 :color rl/RAYWHITE)
          (rl/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
