(ns net.b12n.raylib-jlt.input
  "raylib [core] example - input keys (`joltc -M:input`).

  Ported from examples/core/core_input_keys.c: move a circle with the arrow keys.
  The C original uses a Vector2 + DrawCircleV; here the position is two doubles
  and the ball is drawn with the scalar DrawCircle, so no by-value Vector2 crosses
  the FFI boundary (only Color does)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(defn -main
  [& _]
  (rl/window! {:title "raylib [core] example - input keys"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 x 400.0 y 225.0]
      (when (rl/keep-running? deadline)
        (let [x (cond-> x
                  (rl/key-down? rl/KEY-RIGHT) (+ 2.0)
                  (rl/key-down? rl/KEY-LEFT)  (- 2.0))
              y (cond-> y
                  (rl/key-down? rl/KEY-UP)   (- 2.0)
                  (rl/key-down? rl/KEY-DOWN) (+ 2.0))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "move the ball with arrow keys" {:x 10
                                                     :y 10
                                                     :size 20
                                                     :color rl/DARKGRAY})
          (rl/circle! {:x (int x)
                       :y (int y)
                       :radius 50
                       :color rl/MAROON})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) x y)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
