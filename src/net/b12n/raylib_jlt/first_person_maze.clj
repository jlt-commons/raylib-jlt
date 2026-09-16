(ns net.b12n.raylib-jlt.first-person-maze
  "raylib [models] example - first person maze (`jolt -M:first-person-maze`).

  Walk a grid maze in first person: W/S move along the heading, A/D strafe,
  LEFT/RIGHT turn. A minimap in the corner shows the layout and where you are
  facing. Walls stop you a little short of the surface so you never end up inside
  one and see the world from the wrong side.

  The maze is a vector of strings, which doubles as the collision map: a move is
  accepted only if the destination cell is open, tested per axis so sliding along
  a wall works instead of sticking. Walls are rl/cube! columns, so the whole scene
  is rlgl immediate-mode geometry under a Camera3D."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CELL 4.0)
(def ^:const RADIUS 0.9)           ; how close to a wall the player may stand

(def ^:private maze
  ["################"
   "#..............#"
   "#.####.#####.#.#"
   "#.#....#...#.#.#"
   "#.#.####.#.#.#.#"
   "#.#.#....#...#.#"
   "#...#.######.#.#"
   "###.#......#.#.#"
   "#...####.#.#.#.#"
   "#.####...#...#.#"
   "#....#.###.###.#"
   "####.#.#.....#.#"
   "#....#.#.#####.#"
   "#.####...#.....#"
   "#..............#"
   "################"])

(def ^:private rows (count maze))
(def ^:private cols (count (first maze)))

(defn- wall?
  [cx cy]
  (or (< cx 0) (< cy 0) (>= cx cols) (>= cy rows)
      (= \# (nth (nth maze cy) cx))))

(defn- blocked?
  "Is world position (x, z) inside a wall, allowing for the player's radius?
  Checking the four corners of the player's box rather than just its centre is
  what stops you clipping a corner diagonally."
  [x z]
  (some (fn [[dx dz]]
          (wall? (int (Math/floor (/ (+ x dx) CELL)))
                 (int (Math/floor (/ (+ z dz) CELL)))))
        [[(- RADIUS) (- RADIUS)] [RADIUS (- RADIUS)]
         [(- RADIUS) RADIUS] [RADIUS RADIUS]]))

(defn- draw-maze!
  []
  (dotimes [cy rows]
    (dotimes [cx cols]
      (when (wall? cx cy)
        (let [x (* (+ cx 0.5) CELL)
              z (* (+ cy 0.5) CELL)
              ;; A checker tint so adjacent walls read as separate blocks
              ;; instead of one flat mass.
              base (if (even? (+ cx cy))
                     (rl/rgba 120 130 160 255)
                     (rl/rgba 95 105 135 255))]
          (rl/cube! :pos [x 1.5 z] :size [CELL 3.0 CELL] :color base))))))

(defn- minimap!
  [px pz heading]
  (let [s 9
        ox 616
        oy 20]
    (rl/rect! :x (- ox 4) :y (- oy 4) :width (+ (* cols s) 8) :height (+ (* rows s) 8)
              :color (rl/rgba 0 0 0 170))
    (dotimes [cy rows]
      (dotimes [cx cols]
        (when (wall? cx cy)
          (rl/rect! :x (+ ox (* cx s)) :y (+ oy (* cy s)) :width (dec s) :height (dec s)
                    :color (rl/rgba 150 160 190 255)))))
    (let [mx (+ ox (* s (/ px CELL)))
          my (+ oy (* s (/ pz CELL)))]
      (rl/circle! :x (int mx) :y (int my) :radius 4 :color rl/RED)
      (rl/line! :x1 (int mx) :y1 (int my)
                :x2 (int (+ mx (* 12 (Math/sin heading))))
                :y2 (int (+ my (* 12 (Math/cos heading))))
                :color rl/GOLD))))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [models] example - first person maze")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           px (* 1.5 CELL)
           pz (* 1.5 CELL)
           heading 0.0]
      (when (rl/keep-running? deadline)
        (let [dt (rl/get-frame-time)
              heading (+ heading (* 2.2 dt (+ (if (rl/key-down? rl/KEY-LEFT) 1 0)
                                              (if (rl/key-down? rl/KEY-RIGHT) -1 0))))
              fwd (+ (if (rl/key-down? rl/KEY-W) 1 0) (if (rl/key-down? rl/KEY-S) -1 0))
              strafe (+ (if (rl/key-down? rl/KEY-D) 1 0) (if (rl/key-down? rl/KEY-A) -1 0))
              speed (* 5.0 dt)
              sinh (Math/sin heading)
              cosh (Math/cos heading)
              dx (* speed (+ (* fwd sinh) (* strafe cosh)))
              dz (* speed (- (* fwd cosh) (* strafe sinh)))
              ;; Resolve each axis on its own, so a diagonal into a wall keeps
              ;; the component that is still free and slides along it.
              nx (if (blocked? (+ px dx) pz) px (+ px dx))
              nz (if (blocked? nx (+ pz dz)) pz (+ pz dz))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 16 18 26 255))
          (rl/with-camera-3d
            {:pos-x nx
             :pos-y 1.6
             :pos-z nz
             :target-x (+ nx sinh)
             :target-y 1.6
             :target-z (+ nz cosh)
             :fovy 68}
            (fn []
              (rl/draw-grid 40 CELL)
              (draw-maze!)))
          (minimap! nx nz heading)
          (rl/text! "first person maze" :x 20 :y 18 :size 22 :color rl/RAYWHITE)
          (rl/text! "W/S walk   A/D strafe   LEFT/RIGHT turn"
                    :x 20 :y (- H 30) :size 14 :color rl/GRAY)
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) nx nz heading)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
