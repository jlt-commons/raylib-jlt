(ns net.b12n.raylib-jlt.splines
  "raylib [shapes] example - spline drawing. Five control points bob
  vertically; each spline is drawn as a chain of real DrawSplineSegment*
  calls, one Vector2-by-value argument per point, rather than a from-scratch
  math reimplementation. SPACE cycles Catmull-Rom / cubic Bezier / uniform
  B-spline. Port of shapes_splines_drawing (minus raygui).

  raylib's DrawSpline* take a Vector2 array by value, which jolt could not
  bind before 0.7.23. The per-point DrawSplineSegment* calls used here are
  the same decomposition raylib's own C example shows commented out: one
  Vector2 per point, staged via rl/spline-segment-catmull-rom!/-basis!/
  -bezier-cubic! (new FFI, shared with this session's splines-drawing work).
  This replaces an earlier scalar-math version that predated by-value struct
  support and, correctly at the time, called DrawSpline* unbindable."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:private modes [[:catmull "Catmull-Rom"] [:bezier "cubic Bezier"] [:bspline "uniform B-spline"]])

(defn- control-points
  [frame]
  ;; five points across the window, each bobbing vertically with a per-point phase
  (vec (for [i (range 5)]
         (let [x (+ 90 (* i 155))
               y (+ 225 (* 70.0 (Math/sin (+ (* frame 0.03) (* i 1.3)))))]
           [x y]))))

(defn- draw-segment!
  [mode-id a b c d]
  (case mode-id
    :catmull (rl/spline-segment-catmull-rom! {:p1 a
                                              :p2 b
                                              :p3 c
                                              :p4 d
                                              :thick 3.0
                                              :color rl/RED})
    :bspline (rl/spline-segment-basis! {:p1 a
                                        :p2 b
                                        :p3 c
                                        :p4 d
                                        :thick 3.0
                                        :color rl/RED})
    :bezier (rl/spline-segment-bezier-cubic! {:p1 a
                                              :c2 b
                                              :c3 c
                                              :p4 d
                                              :thick 3.0
                                              :color rl/RED})))

(defn- draw-spline!
  "Pad the endpoints so the curve spans all five points, then draw each
  4-point window as one real DrawSplineSegment* call."
  [mode-id pts]
  (let [padded (vec (concat [(first pts)] pts [(last pts)]))]
    (dotimes [i (- (count padded) 3)]
      (draw-segment! mode-id
                     (nth padded i) (nth padded (+ i 1))
                     (nth padded (+ i 2)) (nth padded (+ i 3))))))

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - splines drawing"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0 mode-idx 0]
      (when (app/keep-running? deadline)
        (let [mode-idx (if (rl/key-pressed? rl/KEY-SPACE)
                         (mod (inc mode-idx) (count modes))
                         mode-idx)
              [mode-id label] (nth modes mode-idx)
              pts (control-points frame)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; control polygon
          (dotimes [i (dec (count pts))]
            (let [[x1 y1] (nth pts i) [x2 y2] (nth pts (inc i))]
              (rl/line! {:x1 (int x1)
                         :y1 (int y1)
                         :x2 (int x2)
                         :y2 (int y2)
                         :color rl/LIGHTGRAY})))
          ;; the spline itself, drawn by raylib's own DrawSplineSegment*
          (draw-spline! mode-id pts)
          ;; control point handles
          (doseq [[x y] pts]
            (rl/circle-lines! {:x (int x)
                               :y (int y)
                               :radius 8
                               :color rl/DARKBLUE}))
          (rl/text! (format "%s  (SPACE cycles)" label) {:x 10
                                                         :y 10
                                                         :size 20
                                                         :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame) mode-idx)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
