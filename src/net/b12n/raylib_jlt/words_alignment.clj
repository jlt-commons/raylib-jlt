(ns net.b12n.raylib-jlt.words-alignment
  "raylib [text] example - word alignment (`joltc -M:words-alignment`).

  A word aligned left / centre / right inside a box using MeasureText to compute
  the horizontal offset. The alignment cycles over time."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [text] example - word alignment"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        bx 150 by 180 bw 500 bh 90
        word "aligned" size 40]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [mode (nth [:left :center :right] (mod (quot frame 60) 3))
              tw   (rl/text-width word :size size)
              tx   (case mode
                     :left   (+ bx 10)
                     :center (+ bx (quot (- bw tw) 2))
                     :right  (- (+ bx bw) tw 10))
              ty   (+ by (quot (- bh size) 2))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect-lines! {:x bx
                           :y by
                           :width bw
                           :height bh
                           :color rl/LIGHTGRAY})
          (rl/text! word {:x tx
                          :y ty
                          :size size
                          :color rl/MAROON})
          (rl/text! (str "alignment: " (name mode)) {:x bx
                                                     :y 110
                                                     :size 20
                                                     :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
