(ns net.b12n.raylib-jlt.world-screen
  "raylib [core] example - world screen.

  A floating 2D label pinned above a 3D cube: each frame the cube's world
  position projects to screen space via GetWorldToScreen, so the label tracks
  it as the camera orbits. First use of a genuine by-value Camera3D (see
  rl/world-to-screen); the SAME camera opts map drives both with-camera-3d and
  rl/world-to-screen, so the label always matches what actually got drawn.
  Ported from raylib's examples/core/core_world_screen.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CAM-R 14.14)
(def ^:const LABEL "Enemy: 100/100")

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - world screen"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 t 0.7854]
      (when (rl/keep-running? deadline)
        (let [t      (+ t (* 0.3 (rl/get-frame-time)))
              cam    {:pos-x (* CAM-R (Math/cos t))
                      :pos-y 10.0
                      :pos-z (* CAM-R (Math/sin t))
                      :target-x 0.0
                      :target-y 0.0
                      :target-z 0.0
                      :up-x 0.0
                      :up-y 1.0
                      :up-z 0.0
                      :fovy 45.0
                      :projection 0}
              [sx sy] (rl/world-to-screen [0.0 2.5 0.0] cam)
              sx      (int sx)
              sy      (int sy)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d cam
            (fn []
              (rl/cube! {:pos [0.0 0.0 0.0]
                         :size 2.0
                         :color rl/RED})
              (rl/draw-grid 10 1.0)))
          ;; The 2D label, centered over the projected point.
          (rl/text! LABEL {:x (- sx (quot (rl/text-width LABEL :size 20) 2))
                           :y sy
                           :size 20
                           :color rl/BLACK})
          (rl/text! (str "Cube screen position: [" sx ", " sy "]")
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/LIME})
          (rl/text! "Text 2D always stays on top of the cube"
                    {:x 10
                     :y 40
                     :size 20
                     :color rl/GRAY})
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) t)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
