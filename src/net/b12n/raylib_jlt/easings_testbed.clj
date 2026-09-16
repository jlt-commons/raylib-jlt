(ns net.b12n.raylib-jlt.easings-testbed
  "raylib [shapes] example - easings testbed (`jolt -M:easings-testbed`).

  Port of raylib's examples/shapes/shapes_easings_testbed.c. One curve at a time,
  plotted across the window with a ball running along it, so you can see the
  shape and feel the timing together. RIGHT and LEFT change curve, SPACE
  replays, D toggles the plot.

  A curve alone is hard to judge. Elastic and back both leave the [0,1] band,
  which is obvious on a plot and invisible in a number; bounce and elastic feel
  similar described in words and nothing alike in motion. Showing both at once is
  the whole point of a testbed.

  The plot is drawn by sampling the curve at one point per pixel of width and
  joining them, which is also the cheapest way to see a curve overshoot: the line
  simply leaves the box.

  Curves come from reasings/by-name, the shared table that also feeds
  easings-ball and easings-box. Adding a curve there adds it here for free."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]
   [net.b12n.raylib-jlt.reasings :as ez]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const DURATION 120.0)

(def ^:private curve-names (vec (sort (keys ez/by-name))))

(def ^:const PLOT-X 100)
(def ^:const PLOT-Y 90)
(def ^:const PLOT-W 600)
(def ^:const PLOT-H 220)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - easings testbed"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           idx 0
           counter 0.0
           plot? true]
      (when (rl/keep-running? deadline)
        (let [next?   (rl/key-pressed? rl/KEY-RIGHT)
              prev?   (rl/key-pressed? rl/KEY-LEFT)
              replay? (rl/key-pressed? rl/KEY-SPACE)
              plot?   (if (rl/key-pressed? rl/KEY-D) (not plot?) plot?)
              idx     (cond
                        next? (mod (inc idx) (count curve-names))
                        prev? (mod (dec idx) (count curve-names))
                        :else idx)
              counter (if (or next? prev? replay?) 0.0 (inc counter))
              counter (if (> counter (* DURATION 1.6)) 0.0 counter)   ; loop, with a pause
              nm      (nth curve-names idx)
              f       (get ez/by-name nm)
              t       (min counter DURATION)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! (str nm "   (" (inc idx) "/" (count curve-names) ")")
                    {:x 20
                     :y 20
                     :size 30
                     :color rl/MAROON})
          (rl/text! "[LEFT]/[RIGHT] curve  -  [SPACE] replay  -  [D] plot"
                    {:x 20
                     :y 56
                     :size 20
                     :color rl/GRAY})
          (when plot?
            ;; The [0,1] band. A curve that overshoots visibly leaves it.
            (rl/rect-lines! {:x PLOT-X
                             :y PLOT-Y
                             :width PLOT-W
                             :height PLOT-H
                             :color (rl/rgba 200 200 200 255)})
            (rl/rl-begin rl/RL-LINES)
            (rl/rl-color! (rl/rgba 0 121 241 255))
            (dotimes [i PLOT-W]
              (let [px  (+ PLOT-X i)
                    px1 (+ PLOT-X (inc i))
                    v0  (f (double i) 0.0 1.0 (double PLOT-W))
                    v1  (f (double (inc i)) 0.0 1.0 (double PLOT-W))]
                ;; y grows downward, so a value of 1 sits at the top of the band.
                (rl/rl-vertex-2f px (- (+ PLOT-Y PLOT-H) (* v0 PLOT-H)))
                (rl/rl-vertex-2f px1 (- (+ PLOT-Y PLOT-H) (* v1 PLOT-H)))))
            (rl/rl-end))
          ;; The ball runs the same curve horizontally, in real time.
          (let [x (f t (double PLOT-X) (double PLOT-W) DURATION)]
            (rl/circle! {:x x
                         :y (+ PLOT-Y PLOT-H 60)
                         :radius 18
                         :color rl/MAROON})
            (rl/line! {:x1 PLOT-X
                       :y1 (+ PLOT-Y PLOT-H 60)
                       :x2 (+ PLOT-X PLOT-W)
                       :y2 (+ PLOT-Y PLOT-H 60)
                       :color (rl/rgba 220 220 220 255)}))
          (rl/maybe-screenshot! frame 70)
          (rl/end-drawing)
          (recur (inc frame) idx counter plot?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
