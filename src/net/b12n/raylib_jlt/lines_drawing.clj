(ns net.b12n.raylib-jlt.lines-drawing
  "raylib [shapes] example - lines drawing. A rotating fan of thick lines (rl/line-ex!),
  each a different width and color with round end caps, plus a thickness-scale row.
  Port of shapes_lines_drawing (minus the texture cursor)."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:private palette
  [rl/RED rl/ORANGE rl/GOLD rl/LIME rl/GREEN rl/SKYBLUE
   rl/BLUE rl/VIOLET rl/PURPLE rl/PINK rl/MAROON rl/DARKBLUE])

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - lines drawing"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        cx 400 cy 220 n 12]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (let [rot (* frame 0.01)]
          (dotimes [i n]
            (let [a (+ rot (* (/ (* 2.0 Math/PI) n) i))
                  x2 (+ cx (* 175.0 (Math/cos a)))
                  y2 (+ cy (* 175.0 (Math/sin a)))
                  thick (+ 2.0 (* i 1.4))
                  color (nth palette (mod i (count palette)))]
              (rl/line-ex! {:x1 cx
                            :y1 cy
                            :x2 x2
                            :y2 y2
                            :thick thick
                            :color color})
              (rl/circle! {:x (int x2)
                           :y (int y2)
                           :radius (/ thick 2.0)
                           :color color}))))
        ;; thickness scale
        (dotimes [i 8]
          (rl/line-ex! {:x1 (+ 130 (* i 68))
                        :y1 420
                        :x2 (+ 178 (* i 68))
                        :y2 420
                        :thick (inc i)
                        :color rl/DARKGRAY}))
        (rl/text! "line-ex! - thick lines (rlgl quads)" {:x 10
                                                         :y 10
                                                         :size 20
                                                         :color rl/DARKGRAY})
        (app/maybe-screenshot! frame 12)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
