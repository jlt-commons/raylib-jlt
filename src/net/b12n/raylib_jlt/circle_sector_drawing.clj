(ns net.b12n.raylib-jlt.circle-sector-drawing
  "raylib [shapes] example - circle sector drawing (`jolt -M:circle-sector-drawing`).

  Port of raylib's examples/shapes/shapes_circle_sector_drawing.c. One sector,
  with its start angle, end angle, radius and segment count all adjustable, so
  you can watch a pie slice degrade into a triangle as the segments drop.

  The lesson is the segment count. raylib needs at least one segment per 90
  degrees of arc to produce something that still reads as a curve, and it
  computes that floor itself when given fewer: `ceil((end - start)/90)`. Below
  the floor the drawing is AUTO, at or above it the count you asked for is used.
  The readout says which is in force.

  The C drives all four values with raygui sliders. raygui is a separate library
  and is not bound here, so the controls are keys instead: Q/A and W/S for the two
  angles, E/D for radius, R/F for segments. Nothing else about the example
  changes, and the arithmetic being demonstrated is untouched.

  See pie-chart for sectors used for something, and ring-drawing for the annulus
  built the same way."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn- clamp [v lo hi] (max lo (min hi v)))

(defn- held
  "Increment while `up` is held, decrement while `down` is held."
  [v up down step lo hi]
  (clamp (cond-> v
           (rl/key-down? up)   (+ step)
           (rl/key-down? down) (- step))
         lo hi))

(defn- readout!
  [y label value]
  (rl/text! (str label " " (format "%.1f" (double value)))
            {:x 600
             :y y
             :size 20
             :color rl/DARKGRAY}))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - circle sector drawing"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        cx (/ (- W 300) 2.0)
        cy (/ (double H) 2.0)]
    (loop [frame 0
           start-angle 0.0
           end-angle 180.0
           radius 180.0
           segments 10.0]
      (when (app/keep-running? deadline)
        (let [start-angle (held start-angle rl/KEY-Q rl/KEY-A 2.0 0.0 720.0)
              end-angle   (held end-angle rl/KEY-W rl/KEY-S 2.0 0.0 720.0)
              radius      (held radius rl/KEY-E rl/KEY-D 2.0 0.0 200.0)
              segments    (held segments rl/KEY-R rl/KEY-F 0.5 0.0 100.0)
              ;; raylib's own floor: one segment per 90 degrees of arc, rounded up.
              min-segments (Math/ceil (/ (- end-angle start-angle) 90.0))
              manual?     (>= segments min-segments)
              drawn       (int (max 1 (if manual? segments min-segments)))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; The panel the C fills with sliders.
          (rl/rect! {:x 500
                     :y 0
                     :width (- W 500)
                     :height H
                     :color (rl/rgba 200 200 200 77)})
          (rl/line! {:x1 500
                     :y1 0
                     :x2 500
                     :y2 H
                     :color (rl/rgba 200 200 200 153)})
          (rl/sector! {:cx cx
                       :cy cy
                       :radius radius
                       :start-deg start-angle
                       :end-deg end-angle
                       :segments drawn
                       :color (rl/rgba 190 33 55 77)})
          ;; Outline: the same fan drawn as spokes, so the segmentation is visible.
          (let [span (- end-angle start-angle)]
            (dotimes [i (inc drawn)]
              (let [a (Math/toRadians (- (+ start-angle (* span (/ (double i) drawn))) 90.0))]
                (rl/line! {:x1 cx
                           :y1 cy
                           :x2 (+ cx (* radius (Math/cos a)))
                           :y2 (+ cy (* radius (Math/sin a)))
                           :color (rl/rgba 190 33 55 153)}))))
          (readout! 40 "[Q/A] start" start-angle)
          (readout! 70 "[W/S] end" end-angle)
          (readout! 140 "[E/D] radius" radius)
          (readout! 170 "[R/F] segments" segments)
          (rl/text! (str "MODE: " (if manual? "MANUAL" "AUTO") "  (min " (int min-segments) ")")
                    {:x 600
                     :y 200
                     :size 20
                     :color (if manual? rl/MAROON rl/DARKGRAY)})
          (rl/fps! {:x 10
                    :y 10})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) start-angle end-angle radius segments)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
