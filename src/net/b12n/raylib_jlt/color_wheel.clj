(ns net.b12n.raylib-jlt.color-wheel
  "raylib [shapes] example - rlgl color wheel. A hue ring drawn as an rlgl triangle
  fan: each slice's rim vertices carry an HSV->RGB color (s=v=1), the center is white.
  Port of shapes_rlgl_color_wheel (minus the raygui value slider); the hue offset
  rotates slowly so the wheel animates."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(defn- hsv->color
  "HSV -> packed Color for s=1, v=1. h in degrees (wrapped)."
  [h]
  (let [h' (/ (mod h 360.0) 60.0)
        i  (int (Math/floor h'))
        f  (- h' i)
        q  (- 1.0 f)
        [r g b] (cond
                  (= i 0) [1.0 f 0.0]
                  (= i 1) [q 1.0 0.0]
                  (= i 2) [0.0 1.0 f]
                  (= i 3) [0.0 q 1.0]
                  (= i 4) [f 0.0 1.0]
                  :else   [1.0 0.0 q])]
    (rl/rgba (int (* 255 r)) (int (* 255 g)) (int (* 255 b)) 255)))

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - rlgl color wheel"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        n 64 cx 400 cy 235 radius 165
        two-pi (* 2.0 Math/PI)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background (rl/rgba 20 20 28 255))
        (let [hue-off (* frame 0.6)]
          (rl/rl-begin rl/RL-TRIANGLES)
          (dotimes [i n]
            (let [a0 (* two-pi (/ (double i) n))
                  a1 (* two-pi (/ (double (inc i)) n))
                  h0 (+ hue-off (* 360.0 (/ (double i) n)))
                  h1 (+ hue-off (* 360.0 (/ (double (inc i)) n)))
                  x0 (+ cx (* radius (Math/sin a0)))
                  y0 (- cy (* radius (Math/cos a0)))
                  x1 (+ cx (* radius (Math/sin a1)))
                  y1 (- cy (* radius (Math/cos a1)))]
              (rl/rl-color! (hsv->color h0))
              (rl/rl-vertex-2f (double x0) (double y0))
              (rl/rl-color! rl/WHITE)
              (rl/rl-vertex-2f (double cx) (double cy))
              (rl/rl-color! (hsv->color h1))
              (rl/rl-vertex-2f (double x1) (double y1))))
          (rl/rl-end))
        (rl/text! "HSV color wheel (rlgl triangle fan)" {:x 10
                                                         :y 10
                                                         :size 20
                                                         :color rl/RAYWHITE})
        (app/maybe-screenshot! frame 12)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
