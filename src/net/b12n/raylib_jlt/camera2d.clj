(ns net.b12n.raylib-jlt.camera2d
  "raylib [core] example - 2D camera (`joltc -M:camera2d`).

  Ported from examples/core/core_2d_camera.c: a row of buildings with a player
  box; the camera follows the player (arrow keys move), the mouse wheel zooms,
  A/D rotate, and R resets.

  This is the project's one struct-by-value example. raylib's BeginMode2D takes a
  24-byte `Camera2D` BY VALUE; net.b12n.raylib.camera/with-camera-2d builds that struct in
  native memory and passes a pointer (the AArch64 ABI for a >16-byte struct, see
  the note in net.b12n.raylib.camera and README.md).

  Verified: the struct-by-value pointer approach renders correctly on AArch64
  (Apple silicon). If you ever hit an invalid-memory crash on another platform
  (e.g. x86-64, where a >16-byte struct is passed on the stack, not by pointer),
  fall back to applying the transform with rlgl's scalar matrix ops (rlPushMatrix
  / rlTranslatef / rlRotatef / rlScalef, flushing the batch before rlPopMatrix),
  which is what BeginMode2D does internally."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const KEY-A 65)
(def ^:const KEY-D 68)
(def ^:const GROUND-Y 280)

;; A fixed skyline: 30 buildings of varying width/height/color along the ground.
(def ^:private buildings
  (mapv (fn [i]
          {:x     (* i 60)
           :w     (+ 30 (* 11 (mod (* (inc i) 7) 15)))
           :h     (+ 60 (* 13 (mod (* (inc i) 5) 20)))
           :color (rl/rgba (+ 100 (mod (* i 37) 155))
                           (+ 80  (mod (* i 53) 120))
                           (+ 90  (mod (* i 29) 140)) 255)})
        (range 30)))

(defn- draw-world
  [px]
  (rl/rect! {:x -600
             :y GROUND-Y
             :width 2400
             :height 200
             :color rl/GRAY})   ; ground
  (doseq [{:keys [x w h color]} buildings]
    (rl/rect! {:x x
               :y (- GROUND-Y h)
               :width w
               :height h
               :color color}))
  (rl/rect! {:x (int (- px 15))
             :y (- GROUND-Y 60)
             :width 30
             :height 60
             :color rl/RED}))  ; player

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - 2d camera"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0 px 400.0 zoom 1.0 rot 0.0]
      (when (app/keep-running? deadline)
        (let [px     (cond-> px
                       (rl/key-down? rl/KEY-RIGHT) (+ 4.0)
                       (rl/key-down? rl/KEY-LEFT)  (- 4.0))
              rot    (cond-> rot
                       (rl/key-down? KEY-A) (- 1.0)
                       (rl/key-down? KEY-D) (+ 1.0))
              zoom   (-> (+ zoom (* (rl/get-mouse-wheel) 0.05)) (max 0.25) (min 3.0))
              reset? (rl/key-pressed? rl/KEY-R)
              zoom   (if reset? 1.0 zoom)
              rot    (if reset? 0.0 rot)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; world space, the camera follows the player horizontally
          (rl/with-camera-2d {:offset-x (/ W 2.0)
                              :offset-y (/ H 2.0)
                              :target-x px
                              :target-y 200.0
                              :rotation rot
                              :zoom zoom}
            (fn [] (draw-world px)))
          ;; screen space, HUD + a center reference line
          (rl/line! {:x1 (int (/ W 2))
                     :y1 0
                     :x2 (int (/ W 2))
                     :y2 H
                     :color rl/LIGHTGRAY})
          (rl/text! "arrows move - wheel zooms - A/D rotate - R resets"
                    {:x 10
                     :y 10
                     :size 18
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) px zoom rot)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
