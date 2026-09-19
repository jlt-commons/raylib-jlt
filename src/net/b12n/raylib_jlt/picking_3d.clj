(ns net.b12n.raylib-jlt.picking-3d
  "raylib [core] example - 3d picking (`jolt -M:picking-3d`).

  Port of raylib's examples/core/core_3d_picking.c. Click the box and it lights
  up, click again to let it go. The ray the click cast stays drawn in the world,
  so you can see where it went when you miss. Right-click captures the pointer
  and hands the camera over to mouse-look; right-click again gives it back.

  New FFI, and the reason this one is worth reading: `GetScreenToWorldRay` is
  the exact inverse of the `GetWorldToScreen` the suite already had, and it is
  bound the same way. A by-value Vector2 in, a by-value Camera3D in, a by-value
  Ray out. `GetRayCollisionBox` then takes that Ray with a by-value BoundingBox
  and returns a by-value RayCollision, whose first field is a one-byte C _Bool,
  so the layout only lands `distance` at offset 4 if the field is declared
  `:bool` rather than an int. raylib.clj asserts both struct sizes at load
  instead of trusting that, because a wrong offset here reads a plausible float
  out of the wrong bytes and never errors.

  Contrast with `basic-voxel`, which picks without any of this. There the ray
  goes through the centre of the screen, so it is just the camera's own look
  direction and a hand-rolled slab test does the rest. Here the ray starts
  wherever the cursor is, which needs the projection matrix raylib is holding,
  and reaching for the real call is cheaper than rebuilding it.

  One deviation: the camera orbits the box until you capture the pointer,
  because no synthetic input actuates a raylib window (see the note in
  scripts/demo_manifest.edn) and the C's camera does not move until a person
  moves it."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CUBE-Y 1.0)
(def ^:const CUBE-SIZE 2.0)
(def ^:const ORBIT-RADIUS 14.0)
(def ^:const ORBIT-HEIGHT 10.0)
(def ^:const ORBIT-SPEED 0.005)
(def ^:const SPEED 0.2)
(def ^:const SENS 0.004)

(def ^:private cube-lo [(- (/ CUBE-SIZE 2.0)) (- CUBE-Y (/ CUBE-SIZE 2.0)) (- (/ CUBE-SIZE 2.0))])
(def ^:private cube-hi [(/ CUBE-SIZE 2.0) (+ CUBE-Y (/ CUBE-SIZE 2.0)) (/ CUBE-SIZE 2.0)])

(defn- orbit-angles
  "Where the camera sits on frame `n` of the idle orbit, and the yaw/pitch that
  look from there back at the box. Keeping those two in step means capturing the
  pointer never makes the view jump."
  [n]
  (let [a (* n ORBIT-SPEED)
        px (* ORBIT-RADIUS (Math/cos a))
        py ORBIT-HEIGHT
        pz (* ORBIT-RADIUS (Math/sin a))
        dx (- px) dy (- CUBE-Y py) dz (- pz)
        flat (Math/sqrt (+ (* dx dx) (* dz dz)))]
    {:pos [px py pz]
     :yaw (Math/atan2 dz dx)
     :pitch (Math/atan2 dy flat)}))

(defn- look-dir
  [yaw pitch]
  (let [cp (Math/cos pitch)]
    [(* cp (Math/cos yaw)) (Math/sin pitch) (* cp (Math/sin yaw))]))

(defn- camera-map
  "The opts map both with-camera-3d and screen-to-world-ray take, so the ray is
  cast through the very camera the frame is drawn with."
  [[px py pz] [dx dy dz]]
  {:pos-x px
   :pos-y py
   :pos-z pz
   :target-x (+ px dx)
   :target-y (+ py dy)
   :target-z (+ pz dz)
   :fovy 45.0
   :projection 0})

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - 3d picking"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           orbit-frame 0
           pos (:pos (orbit-angles 0))
           yaw (:yaw (orbit-angles 0))
           pitch (:pitch (orbit-angles 0))
           ray nil
           hit? false
           last-mx nil
           last-my nil]
      (when (app/keep-running? deadline)
        (let [mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              toggling? (rl/mouse-pressed? rl/MOUSE-RIGHT)
              _ (when toggling?
                  (if (rl/cursor-hidden?) (rl/enable-cursor!) (rl/disable-cursor!)))
              captured? (rl/cursor-hidden?)
              ;; A capture that started THIS frame has a stale last-mx behind
              ;; it, so the look delta waits a frame rather than jumping.
              looking? (and captured? (not toggling?) last-mx last-my)
              idle (when-not captured? (orbit-angles orbit-frame))
              orbit-frame (if captured? orbit-frame (inc orbit-frame))
              yaw (cond
                    idle (:yaw idle)
                    looking? (+ yaw (* SENS (- mx last-mx)))
                    :else yaw)
              pitch (cond
                      idle (:pitch idle)
                      looking? (-> (- pitch (* SENS (- my last-my))) (max -1.4) (min 1.4))
                      :else pitch)
              dir (look-dir yaw pitch)
              fwx (Math/cos yaw) fwz (Math/sin yaw)
              rgx (- fwz) rgz fwx
              pos (if idle
                    (:pos idle)
                    (let [[px py pz] pos
                          dx (+ (if (rl/key-down? rl/KEY-W) fwx 0.0) (if (rl/key-down? rl/KEY-S) (- fwx) 0.0)
                                (if (rl/key-down? rl/KEY-D) rgx 0.0) (if (rl/key-down? rl/KEY-A) (- rgx) 0.0))
                          dz (+ (if (rl/key-down? rl/KEY-W) fwz 0.0) (if (rl/key-down? rl/KEY-S) (- fwz) 0.0)
                                (if (rl/key-down? rl/KEY-D) rgz 0.0) (if (rl/key-down? rl/KEY-A) (- rgz) 0.0))]
                      [(+ px (* SPEED dx)) py (+ pz (* SPEED dz))]))
              camera (camera-map pos dir)
              ;; The C latches: a click while something is selected clears the
              ;; selection instead of casting again.
              clicked? (rl/mouse-pressed? rl/MOUSE-LEFT)
              [ray hit?] (cond
                           (and clicked? hit?) [ray false]
                           clicked? (let [r (rl/screen-to-world-ray [mx my] camera)]
                                      [r (:hit? (rl/ray-collision-box r cube-lo cube-hi))])
                           :else [ray hit?])]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d camera
            (fn []
              (rl/draw-cube! {:pos [0.0 CUBE-Y 0.0]
                              :width CUBE-SIZE
                              :height CUBE-SIZE
                              :length CUBE-SIZE
                              :color (if hit? rl/RED rl/GRAY)})
              (rl/draw-cube-wires! {:pos [0.0 CUBE-Y 0.0]
                                    :width CUBE-SIZE
                                    :height CUBE-SIZE
                                    :length CUBE-SIZE
                                    :color (if hit? rl/MAROON rl/DARKGRAY)})
              (when hit?
                (rl/draw-cube-wires! {:pos [0.0 CUBE-Y 0.0]
                                      :width (+ CUBE-SIZE 0.2)
                                      :height (+ CUBE-SIZE 0.2)
                                      :length (+ CUBE-SIZE 0.2)
                                      :color rl/GREEN}))
              (when ray
                (rl/draw-ray! ray rl/MAROON))
              (rl/draw-grid 10 1.0)))
          (rl/text! "try clicking on the box with your mouse" {:x 240
                                                               :y 10
                                                               :size 20
                                                               :color rl/DARKGRAY})
          (when hit?
            (rl/text! "BOX SELECTED" {:x (/ (- W (rl/text-width "BOX SELECTED" {:size 30})) 2)
                                      :y (* H 0.1)
                                      :size 30
                                      :color rl/GREEN}))
          (rl/text! (if captured?
                      "right click to release the pointer, WASD to move"
                      "right click to take the pointer and look around")
                    {:x 10
                     :y 430
                     :size 10
                     :color rl/GRAY})
          (rl/fps! {:x 10
                    :y 10})
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) orbit-frame pos yaw pitch ray hit? mx my)))))
  (rl/enable-cursor!)
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
