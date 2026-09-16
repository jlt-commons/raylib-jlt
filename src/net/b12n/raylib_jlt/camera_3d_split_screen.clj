(ns net.b12n.raylib-jlt.camera-3d-split-screen
  "raylib [core] example - 3d camera split screen.

  Two players roam a plane of cube trees, each seeing the world from their
  own camera in half the screen: player 1 moves with W/S (along z), player
  2 with UP/DOWN (along x); each is drawn as a colored cube in both views.
  Each half is rendered into its own render texture, then blitted side by
  side, same y-flip convention every other render-texture blit in this
  suite uses. The one new binding this needed was DrawPlane, genuinely by
  value now that Vector2/Vector3 by-value args work (see draw-cube! and
  friends in raylib.clj). Ported from raylib's
  examples/core/core_3d_camera_split_screen.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const HALF-W 400)
(def ^:const COUNT 5)
(def ^:const SPACING 4.0)

(defn- draw-scene!
  "The shared world + player cubes, from the two players' moving
  coordinates (p1 at z1 on the z axis, p2 at x2 on the x axis)."
  [z1 x2]
  (rl/draw-plane! {:pos [0.0 0.0 0.0]
                   :size [50.0 50.0]
                   :color rl/BEIGE})
  (doseq [i (range (- COUNT) (inc COUNT))
          j (range (- COUNT) (inc COUNT))]
    (let [x (* i SPACING)
          z (* j SPACING)]
      (rl/draw-cube! {:pos [x 1.5 z]
                      :width 1.0
                      :height 1.0
                      :length 1.0
                      :color rl/LIME})
      (rl/draw-cube! {:pos [x 0.5 z]
                      :width 0.25
                      :height 1.0
                      :length 0.25
                      :color rl/BROWN})))
  (rl/draw-cube! {:pos [0.0 1.0 z1]
                  :width 1.0
                  :height 1.0
                  :length 1.0
                  :color rl/RED})
  (rl/draw-cube! {:pos [x2 3.0 0.0]
                  :width 1.0
                  :height 1.0
                  :length 1.0
                  :color rl/BLUE}))

(defn- draw-half!
  "Render one player's view into its own render texture: the 3D scene plus
  a translucent label bar."
  [target cam z1 x2 label color]
  (rl/with-render-texture
    target
    (fn []
      (rl/clear-background rl/SKYBLUE)
      (rl/with-camera-3d cam (fn [] (draw-scene! z1 x2)))
      (rl/rect! {:x 0
                 :y 0
                 :width HALF-W
                 :height 40
                 :color (rl/rgba 255 255 255 204)})
      (rl/text! label {:x 10
                       :y 10
                       :size 20
                       :color color}))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - 3d camera split screen"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        screen1 (rl/render-texture HALF-W H)
        screen2 (rl/render-texture HALF-W H)]
    (loop [frame 0 z1 -3.0 x2 -3.0]
      (when (rl/keep-running? deadline)
        (let [step (* 10.0 (rl/get-frame-time))
              z1 (cond (rl/key-down? rl/KEY-W) (+ z1 step)
                       (rl/key-down? rl/KEY-S) (- z1 step)
                       :else z1)
              x2 (cond (rl/key-down? rl/KEY-UP) (+ x2 step)
                       (rl/key-down? rl/KEY-DOWN) (- x2 step)
                       :else x2)
              cam1 {:pos-x 0.0
                    :pos-y 1.0
                    :pos-z z1
                    :target-x 0.0
                    :target-y 1.0
                    :target-z (+ z1 3.0)
                    :up-x 0.0
                    :up-y 1.0
                    :up-z 0.0
                    :fovy 45.0
                    :projection 0}
              cam2 {:pos-x x2
                    :pos-y 3.0
                    :pos-z 0.0
                    :target-x (+ x2 3.0)
                    :target-y 3.0
                    :target-z 0.0
                    :up-x 0.0
                    :up-y 1.0
                    :up-z 0.0
                    :fovy 45.0
                    :projection 0}]
          (draw-half! screen1 cam1 z1 x2 "PLAYER1: W/S to move" rl/MAROON)
          (draw-half! screen2 cam2 z1 x2 "PLAYER2: UP/DOWN to move" rl/DARKBLUE)
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (rl/texture! (:texture screen1)
                       {:x 0
                        :y 0
                        :width HALF-W
                        :height H
                        :v0 1.0
                        :v1 0.0
                        :tint rl/WHITE})
          (rl/texture! (:texture screen2)
                       {:x HALF-W
                        :y 0
                        :width HALF-W
                        :height H
                        :v0 1.0
                        :v1 0.0
                        :tint rl/WHITE})
          (rl/rect! {:x (- HALF-W 2)
                     :y 0
                     :width 4
                     :height H
                     :color rl/LIGHTGRAY})
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) z1 x2))))
    (rl/unload-render-texture! screen1)
    (rl/unload-render-texture! screen2))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
