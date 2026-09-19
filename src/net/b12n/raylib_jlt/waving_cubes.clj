(ns net.b12n.raylib-jlt.waving-cubes
  "raylib [models] example - waving cubes (`joltc -M:waving-cubes`).

  An N×N grid of cubes whose heights ripple like water via a sine wave of position
  + time, coloured by position, under a slowly orbiting 3D camera. Same 3D path as
  camera-3d: Camera3D by value (pointer) + rlgl immediate-mode geometry
  (net.b12n.raylib.models/cube!), since raylib's DrawCube takes a by-value Vector3.

  Each cube is 36 rlVertex3f FFI calls, so the grid is kept modest (N=14 → 196
  columns) to stay smooth; DrawFPS shows the real rate."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const N 14)
(def ^:const SPACING 1.5)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - waving cubes"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        half (* 0.5 (dec N) SPACING)
        span (* SPACING N)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [t     (* 0.06 frame)
              a     (* 0.012 frame)                  ; slow camera orbit
              cam-x (* span 1.3 (Math/cos a))
              cam-z (* span 1.3 (Math/sin a))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 18 18 32 255))
          (rl/with-camera-3d {:pos-x cam-x
                              :pos-y (* span 0.9)
                              :pos-z cam-z
                              :target-x 0.0
                              :target-y 1.5
                              :target-z 0.0
                              :fovy 45.0
                              :projection 0}
            (fn []
              (doseq [ix (range N)
                      iz (range N)]
                (let [wx   (- (* ix SPACING) half)
                      wz   (- (* iz SPACING) half)
                      wave (Math/sin (+ (* 0.6 ix) (* 0.6 iz) t))    ; -1..1
                      hgt  (+ 0.6 (* 2.4 (+ 1.0 wave)))              ; 0.6 .. 5.4
                      hue  (rl/rgba (int (+ 128 (* 127 (Math/sin (+ (* 0.35 ix) t)))))
                                    (int (+ 128 (* 127 (Math/sin (+ (* 0.35 iz) t 2.0)))))
                                    (int (+ 128 (* 127 (Math/sin (+ (* 0.35 (+ ix iz)) t 4.0)))))
                                    255)]
                  (rl/cube! {:pos [wx (/ hgt 2.0) wz]
                             :size [1.0 hgt 1.0]
                             :color hue})))))
          (rl/text! (str "waving cubes - " (* N N) " columns") {:x 10
                                                                :y 10
                                                                :size 20
                                                                :color rl/RAYWHITE})
          (rl/fps! {:x 10
                    :y (- H 30)})
          (app/maybe-screenshot! frame 120)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
