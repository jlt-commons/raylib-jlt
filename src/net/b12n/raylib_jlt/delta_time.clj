(ns net.b12n.raylib-jlt.delta-time
  "raylib [core] example - delta time (`joltc -M:delta-time`).

  Two boxes cross the screen: the top one moves a fixed amount PER FRAME (so its
  speed depends on the frame rate), the bottom one moves by GetFrameTime * speed
  (frame-rate independent). Shows why delta time matters."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - delta time"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 xf 0.0 xd 0.0]
      (when (rl/keep-running? deadline)
        (let [dt (rl/get-frame-time)
              xf (mod (+ xf 4.0) W)             ; per-frame: 4 px per frame
              xd (mod (+ xd (* 240.0 dt)) W)]   ; delta-time: 240 px per second
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "per-frame (top) vs delta-time (bottom)" {:x 10
                                                              :y 10
                                                              :size 20
                                                              :color rl/DARKGRAY})
          (rl/rect! {:x (int xf)
                     :y 150
                     :width 40
                     :height 40
                     :color rl/MAROON})
          (rl/rect! {:x (int xd)
                     :y 280
                     :width 40
                     :height 40
                     :color rl/DARKBLUE})
          (rl/fps! {:x 10
                    :y (- H 30)})
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) xf xd)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
