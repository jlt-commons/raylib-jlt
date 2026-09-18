(ns net.b12n.raylib-jlt.basic-voxel
  "raylib [models] example - basic voxel (`jolt -M:basic-voxel`).

  Port of raylib's examples/models/models_basic_voxel.c. An 8x8x8 block of
  voxels you walk around in first person, WASD to move and the mouse to look.
  Left-click removes whichever voxel the crosshair is on, so the block hollows
  out a cube at a time.

  Zero new FFI. The C builds a cube mesh, wraps it in a Model and calls
  DrawModel once per voxel, which for a unit cube is exactly what the by-value
  `rl/draw-cube!` already draws, with `rl/draw-cube-wires!` for the outline.

  Two deviations, both about the camera. UpdateCamera's first-person mode turns
  on the raw GetMouseDelta, which means the view drifts whenever the pointer
  moves at all, including while the window is still taking focus, so a headless
  screenshot never lands on the same frame twice. This keeps the yaw/pitch walk
  camera-3d-first-person already uses: absolute GetMouseX/GetMouseY, no delta,
  nothing to drift. And the eye starts backed off above the block rather than at
  ground level beside it, so the opening frame shows what there is to click.

  The pick is the C's method, brute force over every remaining voxel rather than
  a DDA march through the grid. GetScreenToWorldRay is not bound, and for a ray
  through the centre of the screen it does not need to be: that ray is the look
  direction the camera was just built from. `hit-distance` is the slab test
  GetRayCollisionBox does, clipping the ray against each axis pair in turn and
  keeping the voxel whose surviving interval starts nearest."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SIZE 8)
(def ^:const HALF 0.5)
(def ^:const SPEED 0.15)
(def ^:const SENS 0.004)

(defn- slab
  "Clip [t0 t1] against one axis of the box, or nil once the interval is empty.
  A ray parallel to this axis survives only if it already lies between the two
  planes, which is the case the division would otherwise turn into infinity."
  [o d lo hi t0 t1]
  (if (< (Math/abs d) 1e-9)
    (when (<= lo o hi) [t0 t1])
    (let [a (/ (- lo o) d)
          b (/ (- hi o) d)
          t0 (max t0 (min a b))
          t1 (min t1 (max a b))]
      (when (<= t0 t1) [t0 t1]))))

(defn- hit-distance
  "Distance along the ray to the unit cube centred on c, or nil for a miss."
  [[ox oy oz] [dx dy dz] [cx cy cz]]
  (when-let [[t0 t1] (slab ox dx (- cx HALF) (+ cx HALF) 0.0 Double/MAX_VALUE)]
    (when-let [[t0 t1] (slab oy dy (- cy HALF) (+ cy HALF) t0 t1)]
      (when-let [[t0 _] (slab oz dz (- cz HALF) (+ cz HALF) t0 t1)]
        (when-not (neg? t0) t0)))))

(defn- pick
  "The nearest voxel the ray enters, or nil if it leaves the block untouched."
  [voxels origin dir]
  (first
   (reduce (fn [[_ best-t :as acc] v]
             (let [t (hit-distance origin dir v)]
               (if (and t (or (nil? best-t) (< t best-t)))
                 [v t]
                 acc)))
           [nil nil]
           voxels)))

(defn- full-block
  []
  (into #{} (for [x (range SIZE)
                  y (range SIZE)
                  z (range SIZE)]
              [(double x) (double y) (double z)])))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - basic voxel"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           voxels (full-block)
           px -8.0 py 10.0 pz -8.0
           yaw (/ Math/PI 4.0) pitch -0.38
           last-mx nil last-my nil]
      (when (rl/keep-running? deadline)
        (let [mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              yaw (if last-mx (+ yaw (* SENS (- mx last-mx))) yaw)
              pitch (if last-my
                      (-> (- pitch (* SENS (- my last-my))) (max -1.4) (min 1.4))
                      pitch)
              cp (Math/cos pitch)
              ;; the look direction, which is also the ray under the crosshair
              dir [(* cp (Math/cos yaw)) (Math/sin pitch) (* cp (Math/sin yaw))]
              fwx (Math/cos yaw) fwz (Math/sin yaw)          ; horizontal forward
              rgx (- fwz) rgz fwx                            ; right = cross(forward, up)
              dx (+ (if (rl/key-down? rl/KEY-W) fwx 0.0) (if (rl/key-down? rl/KEY-S) (- fwx) 0.0)
                    (if (rl/key-down? rl/KEY-D) rgx 0.0) (if (rl/key-down? rl/KEY-A) (- rgx) 0.0))
              dz (+ (if (rl/key-down? rl/KEY-W) fwz 0.0) (if (rl/key-down? rl/KEY-S) (- fwz) 0.0)
                    (if (rl/key-down? rl/KEY-D) rgz 0.0) (if (rl/key-down? rl/KEY-A) (- rgz) 0.0))
              px (+ px (* SPEED dx))
              pz (+ pz (* SPEED dz))
              origin [px py pz]
              voxels (if (rl/mouse-pressed? rl/MOUSE-LEFT)
                       (if-let [v (pick voxels origin dir)]
                         (disj voxels v)
                         voxels)
                       voxels)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x px
                              :pos-y py
                              :pos-z pz
                              :target-x (+ px (nth dir 0))
                              :target-y (+ py (nth dir 1))
                              :target-z (+ pz (nth dir 2))
                              :fovy 45.0
                              :projection 0}
            (fn []
              (rl/draw-grid 10 1.0)
              (doseq [v voxels]
                (rl/draw-cube! {:pos v
                                :width 1.0
                                :height 1.0
                                :length 1.0
                                :color rl/BEIGE})
                (rl/draw-cube-wires! {:pos v
                                      :width 1.0
                                      :height 1.0
                                      :length 1.0
                                      :color rl/BLACK}))))
          ;; The crosshair IS the ray: whatever sits under it is what a click takes out.
          (rl/circle! {:x (/ W 2)
                       :y (/ H 2)
                       :radius 4
                       :color rl/RED})
          (rl/text! "left-click a voxel to remove it" {:x 10
                                                       :y 10
                                                       :size 20
                                                       :color rl/DARKGRAY})
          (rl/text! (str "WASD move, mouse look - " (count voxels) " voxels left")
                    {:x 10
                     :y 35
                     :size 10
                     :color rl/GRAY})
          (rl/fps! {:x (- W 80)
                    :y 10})
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) voxels px py pz yaw pitch mx my)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
