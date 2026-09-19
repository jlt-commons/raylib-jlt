(ns net.b12n.raylib-jlt.rotating-cube
  "raylib [models] example - a single cube rotating in place via the rlgl matrix
  stack. Fixed 3D camera; the cube spins on X and Y with a frame-driven angle.
  See docs/guide/rlgl-immediate-mode.md."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [models] example - rotating cube"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [angle (* frame 1.0)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x 4.0
                              :pos-y 4.0
                              :pos-z 4.0}
            (fn []
              (rl/draw-grid 10 1.0)
              (rl/rl-push-matrix)
              (rl/rl-rotatef angle 1.0 0.0 0.0)
              (rl/rl-rotatef (* angle 0.7) 0.0 1.0 0.0)
              (rl/cube! {:pos [0.0 0.0 0.0]
                         :size 2.0
                         :color rl/RED})
              (rl/rl-pop-matrix)))
          (rl/text! "A cube rotating via the rlgl matrix stack"
                    {:x 10
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
