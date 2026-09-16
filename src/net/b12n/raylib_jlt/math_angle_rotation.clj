(ns net.b12n.raylib-jlt.math-angle-rotation
  "raylib [shapes] example - angle rotation (`joltc -M:math-angle-rotation`).

  A ring of fixed-angle spokes plus one spoke that spins, showing angle-based line
  drawing with sin/cos."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TAU 6.283185307179586)
(def ^:const SPOKES 12)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - angle rotation"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cx (/ W 2.0) cy (/ H 2.0) r 160.0]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/circle-lines! {:x (int cx)
                           :y (int cy)
                           :radius r
                           :color rl/LIGHTGRAY})
        (doseq [k (range SPOKES)]
          (let [a (* TAU (/ (double k) SPOKES))]
            (rl/line! {:x1 (int cx)
                       :y1 (int cy)
                       :x2 (int (+ cx (* r (Math/cos a))))
                       :y2 (int (+ cy (* r (Math/sin a))))
                       :color rl/LIGHTGRAY})))
        (let [a (* 0.04 frame)]
          (rl/line! {:x1 (int cx)
                     :y1 (int cy)
                     :x2 (int (+ cx (* r (Math/cos a))))
                     :y2 (int (+ cy (* r (Math/sin a))))
                     :color rl/MAROON}))
        (rl/text! "fixed spokes + one spinning line" {:x 10
                                                      :y 10
                                                      :size 20
                                                      :color rl/DARKGRAY})
        (rl/maybe-screenshot! frame 20)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
