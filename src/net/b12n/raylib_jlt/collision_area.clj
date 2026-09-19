(ns net.b12n.raylib-jlt.collision-area
  "raylib [shapes] example - collision area (`joltc -M:collision-area`).

  A blue box bounces horizontally; a gold box follows the mouse. When they
  overlap, the intersection rectangle is highlighted in red. AABB overlap is
  computed in Clojure, so no by-value Rectangle crosses the FFI boundary."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn- intersect
  "Overlap rectangle [x y w h] of two AABBs a and b (each given as [x y w h]), or
  nil if they don't overlap."
  [[ax ay aw ah] [bx by bw bh]]
  (let [x1 (max ax bx) y1 (max ay by)
        x2 (min (+ ax aw) (+ bx bw)) y2 (min (+ ay ah) (+ by bh))]
    (when (and (< x1 x2) (< y1 y2))
      [x1 y1 (- x2 x1) (- y2 y1)])))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - collision area"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        aw 220 ah 120 bw 140 bh 100 ay 165]
    (loop [frame 0 ax 40.0 vx 4.0]
      (when (app/keep-running? deadline)
        (let [ax (+ ax vx)
              vx (if (or (>= (+ ax aw) W) (<= ax 0)) (- vx) vx)
              bx (- (rl/get-mouse-x) (quot bw 2))
              by (- (rl/get-mouse-y) (quot bh 2))
              ov (intersect [(int ax) ay aw ah] [bx by bw bh])]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect! {:x (int ax)
                     :y ay
                     :width aw
                     :height ah
                     :color rl/SKYBLUE})
          (rl/rect! {:x bx
                     :y by
                     :width bw
                     :height bh
                     :color rl/GOLD})
          (when ov
            (rl/rect! {:x (nth ov 0)
                       :y (nth ov 1)
                       :width (nth ov 2)
                       :height (nth ov 3)
                       :color rl/RED}))
          (rl/text! (if ov "COLLISION!" "move the gold box over the blue one")
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) ax vx)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
