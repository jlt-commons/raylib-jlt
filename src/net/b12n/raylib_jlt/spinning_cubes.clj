(ns net.b12n.raylib-jlt.spinning-cubes
  "raylib [models] example - a row of cubes each spinning in place with a
  per-index phase offset (distinct from waving-cubes, which translates a grid).
  See docs/guide/rlgl-immediate-mode.md."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def palette [rl/RED rl/ORANGE rl/GREEN rl/BLUE rl/VIOLET])

(defn -main
  [& _]
  (rl/window! :width 800 :height 450 :title "raylib [models] example - spinning cubes")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/with-camera-3d {:pos-x 0.0
                            :pos-y 6.0
                            :pos-z 10.0}
          (fn []
            (rl/draw-grid 12 1.0)
            (dotimes [i 5]
              (let [x (- (* i 2.0) 4.0)
                    angle (+ (* frame 2.0) (* i 30.0))]
                (rl/rl-push-matrix)
                (rl/rl-translatef x 0.5 0.0)
                (rl/rl-rotatef angle 0.3 1.0 0.0)
                (rl/cube! :pos [0.0 0.0 0.0] :size 1.0 :color (nth palette i))
                (rl/rl-pop-matrix)))))
        (rl/text! "Five cubes spinning with a phase offset"
                  :x 10 :y 10 :size 20 :color rl/DARKGRAY)
        (rl/maybe-screenshot! frame 10)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
