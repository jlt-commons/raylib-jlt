(ns net.b12n.raylib-jlt.camera-3d
  "raylib [core] example - 3D camera (`joltc -M:camera-3d`).

  An orbiting perspective camera looks at a shaded cube standing on a ground grid.

  This is the project's 3D milestone. It proves two things at once:
   • Camera3D (a 44-byte struct) is passed BY VALUE to BeginMode3D via a pointer,
     the same >16-byte-struct-by-pointer trick as Camera2D (net.b12n.raylib-jlt.raylib/with-camera-3d).
   • The cube is built with rlgl immediate mode (net.b12n.raylib-jlt.raylib/cube! → rl-vertex-3f)
     because raylib's DrawCube takes a Vector3 BY VALUE, a 12-byte float struct
     passed in FP registers, which the pointer trick does NOT cover. DrawGrid is
     scalar."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [core] example - 3d camera")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [a     (* 0.02 frame)                ; orbit angle
              cam-x (* 12.0 (Math/cos a))
              cam-z (* 12.0 (Math/sin a))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x cam-x
                              :pos-y 8.0
                              :pos-z cam-z
                              :target-x 0.0
                              :target-y 1.0
                              :target-z 0.0
                              :fovy 45.0
                              :projection 0}
            (fn []
              (rl/draw-grid 20 1.0)
              (rl/cube! :pos [0.0 1.0 0.0] :size 2.0 :color rl/RED)))
          (rl/text! "an orbiting 3D camera (Camera3D by value + rlgl cube)"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
