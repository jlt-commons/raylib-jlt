(ns net.b12n.raylib-jlt.logo
  "raylib logo (`joltc -M:logo`).

  A static render of raylib's signature logo, a thick black square border with
  'raylib' tucked into the bottom-right corner, built from two rectangles and a
  text label positioned with MeasureText."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SIZE 256)
(def ^:const BORDER 16)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib logo")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        x  (int (/ (- W SIZE) 2))
        y  (int (/ (- H SIZE) 2))
        ls 40
        lw (rl/text-width "raylib" :size ls)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        ;; thick border = a black square with an inner background-colored square
        (rl/rect! :x x :y y :width SIZE :height SIZE :color rl/BLACK)
        (rl/rect! :x (+ x BORDER) :y (+ y BORDER)
                  :width (- SIZE (* 2 BORDER)) :height (- SIZE (* 2 BORDER))
                  :color rl/RAYWHITE)
        (rl/text! "raylib"
                  :x (- (+ x SIZE) lw BORDER 4)
                  :y (- (+ y SIZE) ls BORDER 4)
                  :size ls :color rl/BLACK)
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
