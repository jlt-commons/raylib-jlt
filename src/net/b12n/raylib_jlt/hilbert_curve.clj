(ns net.b12n.raylib-jlt.hilbert-curve
  "A Hilbert curve (`joltc -M:hilbert-curve`).

  A rainbow Hilbert space-filling curve generated recursively (the classic turtle
  algorithm) into a point list, then drawn as connected line segments."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const ORDER 5)
(def ^:const PI 3.141592653589793)

(defn- hilbert
  "Classic recursive Hilbert curve; appends midpoints to `pts` and returns it. The
  geometry is grouped into vectors, origin `o` [x y] and the two basis vectors
  `ei` [xi xj], `ej` [yi yj], rather than eight loose scalars. (Left positional,
  not keyword args: a 4-way recursion reads more clearly with the vectors inline.)"
  [[x y :as o] [xi xj] [yi yj] n pts]
  (if (<= n 0)
    (conj pts [(+ x (/ (+ xi yi) 2.0)) (+ y (/ (+ xj yj) 2.0))])
    (as-> pts p
      (hilbert o [(/ yi 2.0) (/ yj 2.0)] [(/ xi 2.0) (/ xj 2.0)] (dec n) p)
      (hilbert [(+ x (/ xi 2.0)) (+ y (/ xj 2.0))]
               [(/ xi 2.0) (/ xj 2.0)] [(/ yi 2.0) (/ yj 2.0)] (dec n) p)
      (hilbert [(+ x (/ xi 2.0) (/ yi 2.0)) (+ y (/ xj 2.0) (/ yj 2.0))]
               [(/ xi 2.0) (/ xj 2.0)] [(/ yi 2.0) (/ yj 2.0)] (dec n) p)
      (hilbert [(+ x (/ xi 2.0) yi) (+ y (/ xj 2.0) yj)]
               [(/ (- yi) 2.0) (/ (- yj) 2.0)] [(/ (- xi) 2.0) (/ (- xj) 2.0)] (dec n) p))))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "hilbert curve")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        size 400.0
        ox (/ (- W size) 2.0) oy (/ (- H size) 2.0)
        pts (hilbert [ox oy] [size 0.0] [0.0 size] ORDER [])
        n   (count pts)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background (rl/rgba 12 12 20 255))
        (doseq [i (range 1 n)]
          (let [[ax ay] (nth pts (dec i))
                [bx by] (nth pts i)
                t (/ (double i) n)
                hue (rl/rgba (int (* 255 (max 0.0 (Math/sin (* PI t)))))
                             (int (* 255 (max 0.0 (Math/sin (* PI (+ t 0.33))))))
                             (int (* 255 (max 0.0 (Math/sin (* PI (+ t 0.66)))))) 255)]
            (rl/line! :x1 (int ax) :y1 (int ay) :x2 (int bx) :y2 (int by) :color hue)))
        (rl/text! "a rainbow Hilbert curve" :x 10 :y 10 :size 20 :color rl/RAYWHITE)
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
