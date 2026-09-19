(ns net.b12n.raylib-jlt.scissor-test
  "raylib [core] example - scissor test (`joltc -M:scissor-test`).

  A colorful grid is drawn across the whole window, but a scissor rectangle clips
  drawing so only the part inside the box is visible. Uses the scalar
  BeginScissorMode / EndScissorMode."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn- draw-grid
  []
  (doseq [gy (range 0 H 40)
          gx (range 0 W 40)]
    (rl/rect! {:x gx
               :y gy
               :width 38
               :height 38
               :color (rl/rgba (mod (* gx 3) 256) (mod (* gy 5) 256) 180 255)})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - scissor test"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sx 200 sy 120 sw 400 sh 220]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/begin-scissor-mode sx sy sw sh)
        (draw-grid)
        (rl/end-scissor-mode)
        (rl/rect-lines! {:x sx
                         :y sy
                         :width sw
                         :height sh
                         :color rl/RED})
        (rl/text! "only the scissor box shows the grid" {:x 10
                                                         :y 10
                                                         :size 20
                                                         :color rl/DARKGRAY})
        (app/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
