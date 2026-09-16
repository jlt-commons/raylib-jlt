(ns net.b12n.raylib-jlt.mouse
  "raylib [core] example - mouse input (`joltc -M:mouse`).

  Ported from examples/core/core_input_mouse.c: a circle follows the cursor and
  turns LIME while the left button is held. Uses the scalar GetMouseX / GetMouseY
  (not GetMousePosition, which returns a Vector2 by value)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(defn -main
  [& _]
  (rl/window! {:title "raylib [core] example - mouse input"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [color (if (rl/mouse-down? rl/MOUSE-LEFT) rl/LIME rl/DARKBLUE)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/circle! {:x (rl/get-mouse-x)
                       :y (rl/get-mouse-y)
                       :radius 40
                       :color color})
          (rl/text! "move the mouse; hold left button to change color"
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
