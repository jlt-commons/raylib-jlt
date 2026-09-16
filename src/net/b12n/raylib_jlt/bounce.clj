(ns net.b12n.raylib-jlt.bounce
  "raylib [shapes] example - bouncing ball (`joltc -M:bounce`).

  Ported from examples/shapes/shapes_bouncing_ball.c: a ball bounces around the
  window; SPACE pauses. Position/velocity are plain doubles and the ball is drawn
  with the scalar DrawCircle (no by-value Vector2)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const R 20.0)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shapes] example - bouncing ball")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 x 400.0 y 225.0 vx 5.0 vy 4.0 paused? false]
      (when (rl/keep-running? deadline)
        (let [paused? (if (rl/key-pressed? rl/KEY-SPACE) (not paused?) paused?)
              [x y vx vy] (if paused?
                            [x y vx vy]
                            (let [x  (+ x vx)
                                  y  (+ y vy)
                                  vx (if (or (>= (+ x R) W) (<= (- x R) 0)) (- vx) vx)
                                  vy (if (or (>= (+ y R) H) (<= (- y R) 0)) (- vy) vy)]
                              [x y vx vy]))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/circle! :x (int x) :y (int y) :radius R :color rl/MAROON)
          (rl/text! "PRESS SPACE to PAUSE BALL MOVEMENT"
                    :x 10 :y (- H 25) :size 20 :color rl/LIGHTGRAY)
          (rl/fps! :x 10 :y 10)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) x y vx vy paused?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
