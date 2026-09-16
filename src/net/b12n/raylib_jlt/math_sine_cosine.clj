(ns net.b12n.raylib-jlt.math-sine-cosine
  "raylib [shapes] example - sine & cosine (`joltc -M:math-sine-cosine`).

  A live unit-circle visualization: a radius rotates around a circle, and its
  vertical (sine, blue) and horizontal (cosine, green) projections are drawn as it
  sweeps."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CX 400)
(def ^:const CY 240)
(def ^:const R 160.0)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shapes] example - sine & cosine")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [a  (* 0.03 frame)
              px (+ CX (* R (Math/cos a)))
              py (- CY (* R (Math/sin a)))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/circle-lines! :x CX :y CY :radius R :color rl/LIGHTGRAY)
          (rl/line! :x1 (- CX (int R)) :y1 CY :x2 (+ CX (int R)) :y2 CY :color rl/LIGHTGRAY)
          (rl/line! :x1 CX :y1 (- CY (int R)) :x2 CX :y2 (+ CY (int R)) :color rl/LIGHTGRAY)
          (rl/line! :x1 (int px) :y1 (int py) :x2 (int px) :y2 CY :color rl/BLUE)   ; sine
          (rl/line! :x1 (int px) :y1 CY :x2 CX :y2 CY :color rl/GREEN)              ; cosine
          (rl/line! :x1 CX :y1 CY :x2 (int px) :y2 (int py) :color rl/MAROON)       ; radius
          (rl/circle! :x (int px) :y (int py) :radius 7 :color rl/MAROON)
          (rl/text! "sine (blue) & cosine (green) of a rotating radius"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
