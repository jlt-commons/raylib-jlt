(ns net.b12n.raylib-jlt.rlgl-solar-system
  "raylib [models] example - rlgl solar system (`joltc -M:rlgl-solar-system`).

  Sun, Earth and Moon via the rlgl matrix stack: `rlPushMatrix` / `rlRotatef` /
  `rlTranslatef` nest the transforms so Earth orbits the Sun and the Moon orbits
  the Earth (and each body spins). Bodies are the shared rl/cube! (raylib's
  DrawSphere takes a by-value Vector3), under a fixed 3D camera. rlgl applies the
  active matrix to each rlVertex3f at submit time, which is what makes the nested
  push/rotate/translate move each cube."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [models] example - rlgl solar system")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [earth-orbit (double (mod (* 0.5 frame) 360))   ; degrees
              earth-spin  (double (mod (* 1.0 frame) 360))
              moon-orbit  (double (mod (* 2.0 frame) 360))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 10 10 24 255))
          (rl/with-camera-3d {:pos-x 16.0
                              :pos-y 16.0
                              :pos-z 16.0
                              :target-x 0.0
                              :target-y 0.0
                              :target-z 0.0
                              :fovy 45.0
                              :projection 0}
            (fn []
                               ;; Sun at the origin
              (rl/cube! :pos [0.0 0.0 0.0] :size 3.0 :color rl/GOLD)
                               ;; Earth: orbit the Sun
              (rl/rl-push-matrix)
              (rl/rl-rotatef earth-orbit 0.0 1.0 0.0)
              (rl/rl-translatef 9.0 0.0 0.0)
                               ;;   Earth body, spinning in place
              (rl/rl-push-matrix)
              (rl/rl-rotatef earth-spin 0.0 1.0 0.0)
              (rl/cube! :pos [0.0 0.0 0.0] :size 1.4 :color rl/BLUE)
              (rl/rl-pop-matrix)
                               ;;   Moon: orbit the Earth (still inside Earth's orbit transform)
              (rl/rl-rotatef moon-orbit 0.0 1.0 0.0)
              (rl/rl-translatef 2.6 0.0 0.0)
              (rl/cube! :pos [0.0 0.0 0.0] :size 0.7 :color rl/LIGHTGRAY)
              (rl/rl-pop-matrix)))
          (rl/text! "rlgl matrix stack: Earth orbits Sun, Moon orbits Earth"
                    :x 10 :y 10 :size 20 :color rl/RAYWHITE)
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
