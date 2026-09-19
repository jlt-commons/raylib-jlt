(ns net.b12n.raylib-jlt.lines-bezier
  "raylib [shapes] example - a cubic Bézier curve whose end point follows the
  mouse. DrawLineBezier takes Vector2 by value (unbindable), so the curve is
  sampled in Clojure and drawn as line! segments. See
  docs/guide/rlgl-immediate-mode.md."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def segments 30)

(defn- bezier-point
  [t [p0x p0y] [p1x p1y] [p2x p2y] [p3x p3y]]
  (let [u (- 1.0 t)
        a (* u u u)
        b (* 3.0 u u t)
        c (* 3.0 u t t)
        d (* t t t)]
    [(+ (* a p0x) (* b p1x) (* c p2x) (* d p3x))
     (+ (* a p0y) (* b p1y) (* c p2y) (* d p3y))]))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [shapes] example - lines bezier"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [p0 [100.0 225.0]
              p1 [300.0 60.0]
              p2 [500.0 390.0]
              p3 [(double (rl/get-mouse-x)) (double (rl/get-mouse-y))]
              pts (mapv (fn [i] (bezier-point (/ i (double segments)) p0 p1 p2 p3))
                        (range (inc segments)))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [[[x1 y1] [x2 y2]] (partition 2 1 pts)]
            (rl/line! {:x1 (int x1)
                       :y1 (int y1)
                       :x2 (int x2)
                       :y2 (int y2)
                       :color rl/BLUE}))
          (doseq [[px py] [p0 p1 p2 p3]]
            (rl/circle! {:x (int px)
                         :y (int py)
                         :radius 5
                         :color rl/RED}))
          (rl/text! "Cubic Bezier - move the mouse (end point follows)"
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
