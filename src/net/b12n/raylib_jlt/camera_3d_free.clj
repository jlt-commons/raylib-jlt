(ns net.b12n.raylib-jlt.camera-3d-free
  "raylib [core] example - 3d camera free.

  A free-look 3D camera around a red cube: right-mouse drag to look,
  mouse wheel to zoom, wheel-press to pan, Z re-targets the origin. The
  cursor is captured while running.

  New FFI: rl/update-camera! is raylib's own UpdateCamera, which reads
  the mouse/wheel/keys itself and writes position/target/up back into
  the Camera3D it's given -- the whole free-look feel is one function
  call, nothing reimplemented here. That means the camera has to be a
  PERSISTENT native buffer rather than the per-frame map with-camera-3d
  takes, since UpdateCamera mutates it in place across frames:
  rl/camera3d-alloc/camera3d-free! manage that buffer, and
  rl/begin-mode-3d-ptr (now public; end-mode-3d already was) draws
  through it directly. rl/disable-cursor!/enable-cursor! are the other
  two new bindings, both plain void calls.
  Ported from raylib's examples/core/core_3d_camera_free.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - 3d camera free"})
  (let [cam (rl/camera3d-alloc {:pos-x 10.0
                                :pos-y 10.0
                                :pos-z 10.0
                                :target-x 0.0
                                :target-y 0.0
                                :target-z 0.0
                                :up-x 0.0
                                :up-y 1.0
                                :up-z 0.0
                                :fovy 45.0
                                :projection 0})
        deadline (app/auto-quit-deadline)]
    (rl/disable-cursor!)
    (rl/set-target-fps 60)
    (try
      (loop [frame 0]
        (when (app/keep-running? deadline)
          (rl/update-camera! cam rl/CAMERA-FREE)
          (when (rl/key-pressed? rl/KEY-Z)
            (rl/camera3d-set-target! cam [0.0 0.0 0.0]))

          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)

          (rl/begin-mode-3d-ptr cam)
          (rl/draw-cube! {:pos [0.0 0.0 0.0]
                          :width 2.0
                          :height 2.0
                          :length 2.0
                          :color rl/RED})
          (rl/draw-cube-wires! {:pos [0.0 0.0 0.0]
                                :width 2.0
                                :height 2.0
                                :length 2.0
                                :color rl/MAROON})
          (rl/draw-grid 10 1.0)
          (rl/end-mode-3d)

          (rl/rect! {:x 10
                     :y 10
                     :width 320
                     :height 93
                     :color (rl/rgba 102 191 255 128)})
          (rl/rect-lines! {:x 10
                           :y 10
                           :width 320
                           :height 93
                           :color rl/BLUE})
          (rl/text! "Free camera default controls:" {:x 20
                                                     :y 20
                                                     :size 10
                                                     :color rl/BLACK})
          (rl/text! "- Mouse Wheel to Zoom in-out" {:x 40
                                                    :y 40
                                                    :size 10
                                                    :color rl/DARKGRAY})
          (rl/text! "- Mouse Wheel Pressed to Pan" {:x 40
                                                    :y 60
                                                    :size 10
                                                    :color rl/DARKGRAY})
          (rl/text! "- Z to zoom to (0, 0, 0)" {:x 40
                                                :y 80
                                                :size 10
                                                :color rl/DARKGRAY})

          (app/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame))))
      (finally
        (rl/enable-cursor!)
        (rl/camera3d-free! cam))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
