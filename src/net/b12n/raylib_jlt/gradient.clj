(ns net.b12n.raylib-jlt.gradient
  "raylib [shapes] example - rectangle gradient (`joltc -M:gradient`).

  A full-window vertical gradient via DrawRectangleGradientV, which takes TWO
  Colors by value, a good check that more than one 4-byte by-value struct can be
  passed in a single call."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [shapes] example - rectangle gradient"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/rect-gradient! {:x 0
                            :y 0
                            :width 800
                            :height 450
                            :top rl/SKYBLUE
                            :bottom rl/DARKPURPLE})
        (rl/text! "vertical gradient (DrawRectangleGradientV, two by-value Colors)"
                  {:x 10
                   :y 10
                   :size 20
                   :color rl/RAYWHITE})
        (app/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
