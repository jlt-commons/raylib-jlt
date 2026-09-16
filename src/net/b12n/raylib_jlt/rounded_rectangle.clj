(ns net.b12n.raylib-jlt.rounded-rectangle
  "raylib [shapes] example - rounded rectangle. A rect with quarter-circle corners,
  built from a cross of rects + four sector! corner disks (raylib's DrawRectangleRounded
  takes a Rectangle by value, unbindable). The corner radius animates 0 -> max. Port of
  shapes_rounded_rectangle_drawing."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(defn- rounded-rect!
  [x y w h rad color]
  ;; floor rad to an integer and use it for BOTH the rects and the sector radius, so
  ;; the int-truncated rect edges meet the sector's radial edges exactly (no seams).
  (let [rad (double (int (max 0.0 (min rad (/ (min w h) 2.0)))))
        ri (int rad)]
    ;; body: center column full height + left/right columns between the corners
    (rl/rect! :x (int (+ x rad)) :y (int y) :width (int (- w (* 2 rad))) :height (int h) :color color)
    (rl/rect! :x (int x) :y (int (+ y rad)) :width ri :height (int (- h (* 2 rad))) :color color)
    (rl/rect! :x (int (+ x (- w rad))) :y (int (+ y rad)) :width ri :height (int (- h (* 2 rad))) :color color)
    ;; corners (sector! angles: 0 up, 90 right, 180 down, 270 left)
    (rl/sector! :cx (+ x rad) :cy (+ y rad) :radius rad :start-deg 270 :end-deg 360 :segments 20 :color color)
    (rl/sector! :cx (+ x (- w rad)) :cy (+ y rad) :radius rad :start-deg 0 :end-deg 90 :segments 20 :color color)
    (rl/sector! :cx (+ x (- w rad)) :cy (+ y (- h rad)) :radius rad :start-deg 90 :end-deg 180 :segments 20 :color color)
    (rl/sector! :cx (+ x rad) :cy (+ y (- h rad)) :radius rad :start-deg 180 :end-deg 270 :segments 20 :color color)))

(defn -main
  [& _]
  (rl/window! :title "raylib [shapes] example - rounded rectangle")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [rad (* 60.0 (+ 0.5 (* 0.5 (Math/sin (* frame 0.03)))))]  ; 0..60
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; big animated one
          (rounded-rect! 250 120 300 180 rad (rl/rgba 70 130 200 255))
          ;; a static row showing three fixed roundnesses
          (rounded-rect! 60 350 180 70 8.0  (rl/rgba 0 158 47 255))
          (rounded-rect! 310 350 180 70 22.0 (rl/rgba 255 161 0 255))
          (rounded-rect! 560 350 180 70 35.0 (rl/rgba 200 122 255 255))
          (rl/text! (format "rounded rect - corner radius %.0f (sector! corners)" rad)
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame)))))
    (rl/close-window)))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
