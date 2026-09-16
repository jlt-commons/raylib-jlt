(ns net.b12n.raylib-jlt.wheel
  "raylib [core] example - mouse wheel (`joltc -M:wheel`).

  Ported from examples/core/core_input_mouse_wheel.c: scroll the mouse wheel to
  move a box up and down. Exercises a float-returning binding (GetMouseWheelMove)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const BOX 80)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - mouse wheel"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 y (/ (- H BOX) 2.0)]
      (when (rl/keep-running? deadline)
        (let [y (-> (- y (* (rl/get-mouse-wheel) 20.0))
                    (max 0.0)
                    (min (double (- H BOX))))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect! {:x (int (/ (- W BOX) 2))
                     :y (int y)
                     :width BOX
                     :height BOX
                     :color rl/MAROON})
          (rl/text! "Use mouse wheel to move the box up and down!"
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) y)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
