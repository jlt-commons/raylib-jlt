(ns net.b12n.raylib-jlt.shapes
  "raylib [shapes] example - basic shapes (`joltc -M:shapes`).

  A tour of the scalar shape primitives: filled and outlined rectangles and
  circles, an ellipse, a line, and a triangle drawn via rlgl immediate mode.
  raylib's DrawTriangle takes Vector2 args by value; rlBegin / rlVertex2f is the
  scalar path (see net.b12n.raylib-jlt.raylib's rl-* bindings)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(defn- triangle!
  "An immediate-mode filled triangle (scalar, avoids DrawTriangle's by-value
  Vector2). Corners :p1 :p2 :p3 are [x y]; :color is a packed Color."
  [& {:keys [p1 p2 p3 color]}]
  (let [[x1 y1] p1 [x2 y2] p2 [x3 y3] p3]
    (rl/rl-begin rl/RL-TRIANGLES)
    (rl/rl-color! color)
    (rl/rl-vertex-2f (double x1) (double y1))
    (rl/rl-vertex-2f (double x2) (double y2))
    (rl/rl-vertex-2f (double x3) (double y3))
    (rl/rl-end)))

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - basic shapes"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/text! "scalar shape primitives" {:x 10
                                             :y 10
                                             :size 20
                                             :color rl/DARKGRAY})
        (rl/rect!         {:x 60
                           :y 80
                           :width 120
                           :height 90
                           :color rl/RED})
        (rl/rect-lines!   {:x 210
                           :y 80
                           :width 120
                           :height 90
                           :color rl/BLUE})
        (rl/circle!       {:x 430
                           :y 125
                           :radius 50
                           :color rl/GREEN})
        (rl/circle-lines! {:x 560
                           :y 125
                           :radius 50
                           :color rl/PURPLE})
        (rl/ellipse!      {:x 690
                           :y 125
                           :rx 60
                           :ry 40
                           :color rl/ORANGE})
        (rl/line!         {:x1 60
                           :y1 250
                           :x2 740
                           :y2 250
                           :color rl/DARKGRAY})
        (triangle! :p1 [400 290] :p2 [330 410] :p3 [470 410] :color rl/VIOLET)
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
