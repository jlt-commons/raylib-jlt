(ns net.b12n.raylib-jlt.vector-angle
  "raylib [shapes] example - vector angle. Two vectors share an origin; vector A is
  fixed, vector B rotates. The signed angle between them is filled as an arc via
  rl/sector! and read out in degrees. Port of shapes_vector_angle (B is time-driven
  rather than mouse-driven). Screen-space clockwise-from-up angle uses atan2(vx,-vy)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:private r->d (/ 180.0 Math/PI))

(defn- screen-deg
  "Clockwise-from-up angle (deg) of a screen vector (vx, vy) with y pointing down."
  [vx vy]
  (* r->d (Math/atan2 vx (- vy))))

(defn- norm180
  [d]
  (cond (> d 180.0) (- d 360.0)
        (< d -180.0) (+ d 360.0)
        :else d))

(defn -main
  [& _]
  (rl/window! :title "raylib [shapes] example - vector angle")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        ox 400 oy 235 len 150.0
        ;; A fixed, pointing up-right
        a-deg 35.0
        ad (* a-deg (/ Math/PI 180.0))
        ax (* len (Math/sin ad)) ay (- (* len (Math/cos ad)))]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [b-rad (* frame 0.02)
              bx (* len (Math/sin b-rad)) by (- (* len (Math/cos b-rad)))
              da (screen-deg ax ay)
              db (screen-deg bx by)
              delta (norm180 (- db da))
              lo (min da (+ da delta))
              hi (max da (+ da delta))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; the angle arc (translucent) between the two vectors
          (rl/sector! :cx ox :cy oy :radius 70 :start-deg lo :end-deg hi
                      :segments 48 :color (rl/rgba 255 200 0 110))
          ;; vectors
          (rl/line! :x1 ox :y1 oy :x2 (int (+ ox ax)) :y2 (int (+ oy ay)) :color rl/RED)
          (rl/line! :x1 ox :y1 oy :x2 (int (+ ox bx)) :y2 (int (+ oy by)) :color rl/BLUE)
          (rl/circle! :x (int (+ ox ax)) :y (int (+ oy ay)) :radius 5 :color rl/RED)
          (rl/circle! :x (int (+ ox bx)) :y (int (+ oy by)) :radius 5 :color rl/BLUE)
          (rl/circle! :x ox :y oy :radius 4 :color rl/DARKGRAY)
          (rl/text! "A" :x (int (+ ox ax 8)) :y (int (+ oy ay -6)) :size 20 :color rl/RED)
          (rl/text! "B" :x (int (+ ox bx 8)) :y (int (+ oy by -6)) :size 20 :color rl/BLUE)
          (rl/text! (format "angle: %.1f deg" (Math/abs delta))
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 110)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
