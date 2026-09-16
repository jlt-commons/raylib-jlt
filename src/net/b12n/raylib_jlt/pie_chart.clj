(ns net.b12n.raylib-jlt.pie-chart
  "raylib [shapes] example - pie chart. Fixed category data drawn as filled slices
  via rl/sector! (an rlgl triangle fan), with a legend column. The whole chart
  rotates slowly. Port of shapes_pie_chart."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:private slices
  ;; [label value color]
  [["alpha"   30 rl/RED]
   ["beta"    24 rl/SKYBLUE]
   ["gamma"   18 rl/LIME]
   ["delta"   16 rl/GOLD]
   ["epsilon" 12 rl/VIOLET]])

(defn -main
  [& _]
  (rl/window! :title "raylib [shapes] example - pie chart")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cx 270 cy 235 radius 165
        total (double (reduce + (map second slices)))]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/text! "pie chart (rl/sector! fan)" :x 10 :y 10 :size 20 :color rl/DARKGRAY)
        (let [base (* frame 0.25)]
          ;; slices
          (loop [acc 0.0 items slices]
            (when (seq items)
              (let [[_ val color] (first items)
                    span (* 360.0 (/ (double val) total))]
                (rl/sector! :cx cx :cy cy :radius radius
                            :start-deg (+ base acc) :end-deg (+ base acc span)
                            :segments 60 :color color)
                (recur (+ acc span) (rest items))))))
        ;; legend
        (dotimes [i (count slices)]
          (let [[label val color] (nth slices i)
                pct (int (Math/round (* 100.0 (/ (double val) total))))
                ly (+ 90 (* i 44))]
            (rl/rect! :x 540 :y ly :width 26 :height 26 :color color)
            (rl/text! (format "%s  %d%%" label pct)
                      :x 576 :y (+ ly 4) :size 20 :color rl/DARKGRAY)))
        (rl/maybe-screenshot! frame 12)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
