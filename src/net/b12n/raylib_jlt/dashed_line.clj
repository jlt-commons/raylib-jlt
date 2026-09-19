(ns net.b12n.raylib-jlt.dashed-line
  "raylib [shapes] example - dashed line (`joltc -M:dashed-line`).

  A dashed line from the screen centre to the mouse, drawn as a series of short
  segments with gaps (every other segment)."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const DASH 14.0)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - dashed line"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        cx (/ W 2.0) cy (/ H 2.0)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [mx  (rl/get-mouse-x) my (rl/get-mouse-y)
              dx  (- mx cx) dy (- my cy)
              len (Math/sqrt (+ (* dx dx) (* dy dy)))
              steps (int (/ len DASH))
              ux  (if (pos? len) (/ dx len) 0.0)
              uy  (if (pos? len) (/ dy len) 0.0)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [i (range 0 steps 2)]              ; every other segment = a dash
            (rl/line! {:x1 (int (+ cx (* ux DASH i)))
                       :y1 (int (+ cy (* uy DASH i)))
                       :x2 (int (+ cx (* ux DASH (inc i))))
                       :y2 (int (+ cy (* uy DASH (inc i))))
                       :color rl/MAROON}))
          (rl/circle! {:x (int cx)
                       :y (int cy)
                       :radius 6
                       :color rl/DARKGRAY})
          (rl/text! "a dashed line follows the mouse" {:x 10
                                                       :y 10
                                                       :size 20
                                                       :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
