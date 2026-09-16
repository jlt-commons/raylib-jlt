(ns net.b12n.raylib-jlt.easings
  "raylib [shapes] example - easings. A 3x4 grid of tracks, each animating a ball
  across the lane with a different easing function of a ping-ponging t in [0,1].
  Spirit of shapes_easings_*; all easings are pure math (pow / sin / cos)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:private PI Math/PI)

(defn- out-bounce
  [t]
  (let [n1 7.5625 d1 2.75]
    (cond
      (< t (/ 1.0 d1)) (* n1 t t)
      (< t (/ 2.0 d1)) (let [t (- t (/ 1.5 d1))] (+ (* n1 t t) 0.75))
      (< t (/ 2.5 d1)) (let [t (- t (/ 2.25 d1))] (+ (* n1 t t) 0.9375))
      :else            (let [t (- t (/ 2.625 d1))] (+ (* n1 t t) 0.984375)))))

(def ^:private easings
  [["linear"     (fn [t] t)]
   ["inQuad"     (fn [t] (* t t))]
   ["outQuad"    (fn [t] (- 1.0 (* (- 1.0 t) (- 1.0 t))))]
   ["inOutCubic" (fn [t]
                   (if (< t 0.5)
                     (* 4.0 t t t)
                     (- 1.0 (/ (Math/pow (+ (* -2.0 t) 2.0) 3.0) 2.0))))]
   ["inSine"     (fn [t] (- 1.0 (Math/cos (/ (* t PI) 2.0))))]
   ["outSine"    (fn [t] (Math/sin (/ (* t PI) 2.0)))]
   ["inOutSine"  (fn [t] (/ (- (Math/cos (* PI t)) 1.0) -2.0))]
   ["inExpo"     (fn [t] (if (<= t 0.0) 0.0 (Math/pow 2.0 (- (* 10.0 t) 10.0))))]
   ["outElastic" (fn [t]
                   (cond (<= t 0.0) 0.0 (>= t 1.0) 1.0
                         :else (+ 1.0 (* (Math/pow 2.0 (* -10.0 t))
                                         (Math/sin (* (- (* 10.0 t) 0.75) (/ (* 2.0 PI) 3.0)))))))]
   ["outBounce"  out-bounce]
   ["inBack"     (fn [t] (let [c1 1.70158 c3 (+ c1 1.0)] (- (* c3 t t t) (* c1 t t))))]
   ["inOutQuart" (fn [t]
                   (if (< t 0.5)
                     (* 8.0 t t t t)
                     (- 1.0 (/ (Math/pow (+ (* -2.0 t) 2.0) 4.0) 2.0))))]])

(def ^:private lane-colors
  [rl/RED rl/ORANGE rl/GOLD rl/LIME rl/GREEN rl/SKYBLUE
   rl/BLUE rl/VIOLET rl/PURPLE rl/PINK rl/MAROON rl/DARKBLUE])

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - easings"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cols 3 track-w 190 x-pad 30]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [p (/ (double (mod frame 240)) 120.0)   ; 0..2
              t (if (<= p 1.0) p (- 2.0 p))]         ; ping-pong 0..1..0
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "easing functions" {:x 10
                                        :y 8
                                        :size 20
                                        :color rl/DARKGRAY})
          (dotimes [i (count easings)]
            (let [[label f] (nth easings i)
                  col (mod i cols)
                  row (quot i cols)
                  x0 (+ x-pad (* col (+ track-w 70)))
                  y  (+ 70 (* row 92))
                  eased (f t)
                  bx (+ x0 (* eased track-w))]
              (rl/text! label {:x x0
                               :y (- y 22)
                               :size 14
                               :color rl/DARKGRAY})
              (rl/line! {:x1 x0
                         :y1 y
                         :x2 (+ x0 track-w)
                         :y2 y
                         :color rl/LIGHTGRAY})
              (rl/circle! {:x (int bx)
                           :y y
                           :radius 9
                           :color (nth lane-colors i)})))
          (rl/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
