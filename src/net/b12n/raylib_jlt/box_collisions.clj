(ns net.b12n.raylib-jlt.box-collisions
  "raylib [models] example - box collisions (`joltc -M:box-collisions`).

  A player cube moves with WASD across a grid; each static obstacle box turns red
  when the player's box overlaps it (3D AABB overlap, computed in Clojure, no
  by-value Rectangle/BoundingBox needed). Reuses the 3D path (Camera3D by value +
  rl/cube!) under a fixed 3/4 camera. The player spawns already touching one box,
  so the collision highlight is visible from frame 0."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PS 2.0)      ; player cube size
(def ^:const SPEED 0.18)

;; static obstacles on the ground: {:x :z :s}
(def ^:private boxes
  [{:x  4.0
    :z  0.0
    :s 2.0}
   {:x -4.0
    :z  3.0
    :s 2.6}
   {:x  0.0
    :z -5.0
    :s 3.0}
   {:x  1.6
    :z  0.0
    :s 2.0}     ; near the spawn, overlaps the player at frame 0
   {:x -6.0
    :z -3.0
    :s 2.2}])

(defn- fabs
  [x]
  (if (neg? x) (- x) x))

(defn- hit?
  "3D AABB overlap between the player (centre px,pz, size PS) and box b. Both sit on
  the ground so the vertical axis always overlaps, only x/z are tested."
  [px pz b]
  (let [ph (/ PS 2.0) bh (/ (:s b) 2.0)]
    (and (< (fabs (- px (:x b))) (+ ph bh))
         (< (fabs (- pz (:z b))) (+ ph bh)))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - box collisions"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 px 0.0 pz 0.0]
      (when (rl/keep-running? deadline)
        (let [px (+ px (* SPEED (+ (if (rl/key-down? rl/KEY-D) 1.0 0.0)
                                   (if (rl/key-down? rl/KEY-A) -1.0 0.0))))
              pz (+ pz (* SPEED (+ (if (rl/key-down? rl/KEY-S) 1.0 0.0)
                                   (if (rl/key-down? rl/KEY-W) -1.0 0.0))))
              colliding? (boolean (some (fn [b] (hit? px pz b)) boxes))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 230 235 245 255))
          (rl/with-camera-3d {:pos-x 11.0
                              :pos-y 12.0
                              :pos-z 11.0
                              :target-x 0.0
                              :target-y 0.5
                              :target-z 0.0
                              :fovy 45.0
                              :projection 0}
            (fn []
              (rl/draw-grid 20 1.0)
              (doseq [b boxes]
                (let [s (:s b)]
                  (rl/cube! {:pos [(:x b) (/ s 2.0) (:z b)]
                             :size s
                             :color (if (hit? px pz b) rl/RED rl/GRAY)})))
              (rl/cube! {:pos [px (/ PS 2.0) pz]
                         :size PS
                         :color rl/LIME})))  ; player
          (rl/text! (if colliding? "COLLISION!" "WASD move the player")
                    {:x 10
                     :y 10
                     :size 20
                     :color (if colliding? rl/MAROON rl/DARKGRAY)})
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) px pz)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
