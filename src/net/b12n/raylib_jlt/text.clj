(ns net.b12n.raylib-jlt.text
  "raylib [text] example - font sizes & centering (`joltc -M:text`).

  Draws lines at several font sizes and colors, and uses MeasureText to
  horizontally center two lines (the built-in bitmap font; no external font
  loading)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)

(defn- centered!
  [s size y color]
  (rl/text! s
            :x (int (/ (- W (rl/text-width s :size size)) 2))
            :y y :size size :color color))

(defn -main
  [& _]
  (rl/window! :width W :height 450 :title "raylib [text] example - font sizes")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (centered! "raylib text - default font" 40 30 rl/DARKBLUE)
        (rl/text! "size 10" :x 40 :y 120 :size 10 :color rl/DARKGRAY)
        (rl/text! "size 20" :x 40 :y 150 :size 20 :color rl/MAROON)
        (rl/text! "size 30" :x 40 :y 190 :size 30 :color rl/DARKGREEN)
        (rl/text! "size 40" :x 40 :y 240 :size 40 :color rl/PURPLE)
        (centered! "MeasureText centers this line" 24 390 rl/GRAY)
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
