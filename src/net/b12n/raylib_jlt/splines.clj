(ns net.b12n.raylib-jlt.splines
  "raylib [shapes] example - spline drawing. Five control points bob vertically; a
  spline is evaluated in pure Clojure (raylib's DrawSpline* take Vector2 arrays by
  value, unbindable) and drawn as a line! polyline. SPACE cycles Catmull-Rom /
  cubic Bezier / uniform B-spline. Port of shapes_splines_drawing (minus raygui)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

;; scalar spline bases: (f a b c d t) over 4 successive control values, t in [0,1]
(defn- cr
  [a b c d t]
  (let [t2 (* t t) t3 (* t2 t)]
    (* 0.5 (+ (* 2.0 b)
              (* (+ (- a) c) t)
              (* (+ (* 2.0 a) (* -5.0 b) (* 4.0 c) (- d)) t2)
              (* (+ (- a) (* 3.0 b) (* -3.0 c) d) t3)))))

(defn- bez
  [a b c d t]
  (let [u (- 1.0 t)]
    (+ (* u u u a) (* 3.0 u u t b) (* 3.0 u t t c) (* t t t d))))

(defn- bspl
  [a b c d t]
  (let [t2 (* t t) t3 (* t2 t)]
    (/ (+ (* (+ (- a) (* 3.0 b) (* -3.0 c) d) t3)
          (* (+ (* 3.0 a) (* -6.0 b) (* 3.0 c)) t2)
          (* (+ (* -3.0 a) (* 3.0 c)) t)
          (+ a (* 4.0 b) c))
       6.0)))

(def ^:private modes
  [[:catmull "Catmull-Rom" cr]
   [:bezier  "cubic Bezier" bez]
   [:bspline "uniform B-spline" bspl]])

(defn- control-points
  [frame]
  ;; five points across the window, each bobbing vertically with a per-point phase
  (vec (for [i (range 5)]
         (let [x (+ 90 (* i 155))
               y (+ 225 (* 70.0 (Math/sin (+ (* frame 0.03) (* i 1.3)))))]
           [x y]))))

(defn- polyline
  [f pts]
  ;; pad ends so the curve spans all points, then sample each 4-window between its
  ;; middle two control points
  (let [padded (vec (concat [(first pts)] pts [(last pts)]))
        steps 20]
    (loop [i 0 out []]
      (if (<= (+ i 3) (dec (count padded)))
        (let [[ax ay] (nth padded i)
              [bx by] (nth padded (+ i 1))
              [cx cy] (nth padded (+ i 2))
              [dx dy] (nth padded (+ i 3))
              seg (vec (for [s (range (inc steps))]
                         (let [t (/ (double s) steps)]
                           [(f ax bx cx dx t) (f ay by cy dy t)])))]
          (recur (inc i) (into out seg)))
        out))))

(defn -main
  [& _]
  (rl/window! :title "raylib [shapes] example - splines drawing")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 mode-idx 0]
      (when (rl/keep-running? deadline)
        (let [mode-idx (if (rl/key-pressed? rl/KEY-SPACE)
                         (mod (inc mode-idx) (count modes))
                         mode-idx)
              [_ label f] (nth modes mode-idx)
              pts (control-points frame)
              line-pts (polyline f pts)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; control polygon
          (dotimes [i (dec (count pts))]
            (let [[x1 y1] (nth pts i) [x2 y2] (nth pts (inc i))]
              (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color rl/LIGHTGRAY)))
          ;; the spline
          (dotimes [i (dec (count line-pts))]
            (let [[x1 y1] (nth line-pts i) [x2 y2] (nth line-pts (inc i))]
              (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color rl/RED)))
          ;; control point handles
          (doseq [[x y] pts]
            (rl/circle-lines! :x (int x) :y (int y) :radius 8 :color rl/DARKBLUE))
          (rl/text! (format "%s  (SPACE cycles)" label) :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame) mode-idx)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
