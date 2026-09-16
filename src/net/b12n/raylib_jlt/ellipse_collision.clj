(ns net.b12n.raylib-jlt.ellipse-collision
  "raylib [shapes] example - ellipse collision (`jolt -M:ellipse-collision`).

  Port of raylib's examples/shapes/shapes_ellipse_collision.c. Two ellipses, one
  following the mouse. Both turn red when they overlap. A and B choose which one
  you are steering.

  DrawEllipse is bound, DrawEllipseLines is not, so the outlines are a line loop
  through rlgl immediate mode. That is the same trade the rest of the suite makes
  for Vector2-taking shape calls, and it costs one helper.

  The overlap test is the interesting part. Two ellipses do not have a closed-form
  intersection test the way two circles do, so raylib samples: it walks points
  around one ellipse's rim and asks whether any lands inside the other. That is
  approximate, and the sample count is the accuracy. Point-in-ellipse itself is
  exact, being the unit-circle test after dividing out each radius."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const RIM-SAMPLES 64)
(def ^:const TWO-PI (* 2.0 Math/PI))

(defn- point-in-ellipse?
  "Exact: scale the offset by each radius and test against the unit circle."
  [px py cx cy rx ry]
  (let [dx (/ (- px cx) rx)
        dy (/ (- py cy) ry)]
    (<= (+ (* dx dx) (* dy dy)) 1.0)))

(defn- ellipses-overlap?
  "Approximate, by sampling one rim against the other ellipse and then the
  reverse. Checking only one direction misses the case where one ellipse sits
  wholly inside the other, since then neither rim crosses the other's boundary
  but every point of the smaller is still inside."
  [ax ay arx ary bx by brx bry]
  (letfn [(rim-inside? [cx cy rx ry ox oy orx ory]
            (some (fn [i]
                    (let [t (* TWO-PI (/ (double i) RIM-SAMPLES))]
                      (point-in-ellipse? (+ cx (* rx (Math/cos t)))
                                         (+ cy (* ry (Math/sin t)))
                                         ox oy orx ory)))
                  (range RIM-SAMPLES)))]
    (boolean (or (rim-inside? ax ay arx ary bx by brx bry)
                 (rim-inside? bx by brx bry ax ay arx ary)))))

(defn- ellipse-outline!
  "DrawEllipseLines' stand-in: a closed line loop around the rim."
  [cx cy rx ry color]
  (rl/rl-begin rl/RL-LINES)
  (rl/rl-color! color)
  (dotimes [i RIM-SAMPLES]
    (let [t0 (* TWO-PI (/ (double i) RIM-SAMPLES))
          t1 (* TWO-PI (/ (double (inc i)) RIM-SAMPLES))]
      (rl/rl-vertex-2f (+ cx (* rx (Math/cos t0))) (+ cy (* ry (Math/sin t0))))
      (rl/rl-vertex-2f (+ cx (* rx (Math/cos t1))) (+ cy (* ry (Math/sin t1))))))
  (rl/rl-end))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shapes] example - ellipse collision")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        a-rx 120.0 a-ry 70.0
        b-rx 90.0  b-ry 140.0]
    (loop [frame 0
           controlled :a
           ax (/ (double W) 4.0) ay (/ (double H) 2.0)
           bx (* (double W) 0.75) by (/ (double H) 2.0)]
      (when (rl/keep-running? deadline)
        (let [controlled (cond (rl/key-pressed? rl/KEY-A) :a
                               (rl/key-pressed? rl/KEY-B) :b
                               :else controlled)
              mx (double (rl/get-mouse-x))
              my (double (rl/get-mouse-y))
              [ax ay] (if (= controlled :a) [mx my] [ax ay])
              [bx by] (if (= controlled :b) [mx my] [bx by])
              hit? (ellipses-overlap? ax ay a-rx a-ry bx by b-rx b-ry)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/ellipse! :x (int ax) :y (int ay) :rx a-rx :ry a-ry :color (if hit? rl/RED rl/BLUE))
          (rl/ellipse! :x (int bx) :y (int by) :rx b-rx :ry b-ry :color (if hit? rl/RED rl/GREEN))
          (ellipse-outline! ax ay a-rx a-ry rl/WHITE)
          (ellipse-outline! bx by b-rx b-ry rl/WHITE)
          (rl/circle! :x (int ax) :y (int ay) :radius 4 :color rl/WHITE)
          (rl/circle! :x (int bx) :y (int by) :radius 4 :color rl/WHITE)
          (rl/text! (str "[A]/[B] pick an ellipse - steering " (name controlled))
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/text! (if hit? "OVERLAPPING" "apart")
                    :x 10 :y 36 :size 20 :color (if hit? rl/RED rl/GRAY))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) controlled ax ay bx by)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
