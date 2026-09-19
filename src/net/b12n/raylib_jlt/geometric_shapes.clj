(ns net.b12n.raylib-jlt.geometric-shapes
  "raylib [models] example - geometric shapes.

  A static 3D scene of cubes, spheres, cylinders, cones and capsules (solid
  + wireframe) over a grid, viewed through a fixed perspective camera. The
  first example to use the genuine by-value Draw{Cube,Sphere,Cylinder,
  Capsule}* calls (draw-cube!, draw-sphere!, etc. in net.b12n.raylib.models)
  rather than the rlgl immediate-mode stand-ins rl/cube!/rl/sphere! use -- a cone here
  is a cylinder with a zero top radius, the same trick raylib's own example
  uses. Ported from raylib's examples/models/models_geometric_shapes.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - geometric shapes"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        cam {:pos-x 0.0
             :pos-y 10.0
             :pos-z 10.0
             :target-x 0.0
             :target-y 0.0
             :target-z 0.0
             :up-x 0.0
             :up-y 1.0
             :up-z 0.0
             :fovy 45.0
             :projection 0}]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/with-camera-3d
          cam
          (fn []
            (rl/draw-cube! {:pos [-4.0 0.0 2.0]
                            :width 2.0
                            :height 5.0
                            :length 2.0
                            :color rl/RED})
            (rl/draw-cube-wires! {:pos [-4.0 0.0 2.0]
                                  :width 2.0
                                  :height 5.0
                                  :length 2.0
                                  :color rl/GOLD})
            (rl/draw-cube-wires! {:pos [-4.0 0.0 -2.0]
                                  :width 3.0
                                  :height 6.0
                                  :length 2.0
                                  :color rl/MAROON})

            (rl/draw-sphere! [-1.0 0.0 -2.0] 1.0 rl/GREEN)
            (rl/draw-sphere-wires! {:pos [1.0 0.0 2.0]
                                    :radius 2.0
                                    :rings 16
                                    :slices 16
                                    :color rl/LIME})

            (rl/draw-cylinder! {:pos [4.0 0.0 -2.0]
                                :radius-top 1.0
                                :radius-bottom 2.0
                                :height 3.0
                                :slices 4
                                :color rl/SKYBLUE})
            (rl/draw-cylinder-wires! {:pos [4.0 0.0 -2.0]
                                      :radius-top 1.0
                                      :radius-bottom 2.0
                                      :height 3.0
                                      :slices 4
                                      :color rl/DARKBLUE})
            (rl/draw-cylinder-wires! {:pos [4.5 -1.0 2.0]
                                      :radius-top 1.0
                                      :radius-bottom 1.0
                                      :height 2.0
                                      :slices 6
                                      :color rl/BROWN})

            ;; A cone is a cylinder with a zero top radius.
            (rl/draw-cylinder! {:pos [1.0 0.0 -4.0]
                                :radius-top 0.0
                                :radius-bottom 1.5
                                :height 3.0
                                :slices 8
                                :color rl/GOLD})
            (rl/draw-cylinder-wires! {:pos [1.0 0.0 -4.0]
                                      :radius-top 0.0
                                      :radius-bottom 1.5
                                      :height 3.0
                                      :slices 8
                                      :color rl/PINK})

            (rl/draw-capsule! {:start-pos [-3.0 1.5 -4.0]
                               :end-pos [-4.0 -1.0 -4.0]
                               :radius 1.2
                               :slices 8
                               :rings 8
                               :color rl/VIOLET})
            (rl/draw-capsule-wires! {:start-pos [-3.0 1.5 -4.0]
                                     :end-pos [-4.0 -1.0 -4.0]
                                     :radius 1.2
                                     :slices 8
                                     :rings 8
                                     :color rl/PURPLE})

            (rl/draw-grid 10 1.0)))
        (rl/fps! {:x 10
                  :y 10})
        (rl/text! "geometric shapes" {:x 10
                                      :y 40
                                      :size 10
                                      :color rl/GRAY})
        (app/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
