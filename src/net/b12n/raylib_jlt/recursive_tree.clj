(ns net.b12n.raylib-jlt.recursive-tree
  "raylib [shapes] example - recursive tree (`joltc -M:recursive-tree`).

  A binary fractal tree drawn with lines: each branch spawns two shorter branches
  at ±angle until a depth limit, brown trunk fading to green tips. Pure trig over
  draw-line."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PI 3.141592653589793)
(def ^:const SPREAD 0.5)

(defn- branch
  "Draw one branch and recurse into two shorter child branches. Keyword args:
  :pos [x y] start point, :len length, :angle radians, :depth remaining levels."
  [& {:keys [pos len angle depth]}]
  (when (pos? depth)
    (let [[x y] pos
          x2    (+ x (* len (Math/cos angle)))
          y2    (- y (* len (Math/sin angle)))
          color (if (<= depth 3) (rl/rgba 34 139 34 255) (rl/rgba 101 67 33 255))]
      (rl/line! {:x1 (int x)
                 :y1 (int y)
                 :x2 (int x2)
                 :y2 (int y2)
                 :color color})
      (branch :pos [x2 y2] :len (* len 0.72) :angle (+ angle SPREAD) :depth (dec depth))
      (branch :pos [x2 y2] :len (* len 0.72) :angle (- angle SPREAD) :depth (dec depth)))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - recursive tree"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (branch :pos [(/ W 2.0) (- H 20)] :len 110.0 :angle (/ PI 2) :depth 10)  ; up, depth 10
        (rl/text! "a binary fractal tree" {:x 10
                                           :y 10
                                           :size 20
                                           :color rl/DARKGRAY})
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
