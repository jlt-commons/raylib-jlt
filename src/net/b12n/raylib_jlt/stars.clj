(ns net.b12n.raylib-jlt.stars
  "A twinkling starfield (`joltc -M:stars`).

  Not a raylib example port, a small demo that scatters stars with GetRandomValue
  (once, at startup) and redraws them each frame with a per-star twinkle. Shows
  bulk scalar drawing and a computed (non-palette) Color per star."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const N 220)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "starfield"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        night    (rl/rgba 10 12 28 255)
        stars    (vec (repeatedly N (fn []
                                      [(rl/get-random-value 0 W)
                                       (rl/get-random-value 0 H)
                                       (rl/get-random-value 0 100)])))]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background night)
        (doseq [[sx sy phase] stars]
          (let [t (* 0.15 (+ frame phase))
                b (int (+ 160 (* 95 (Math/sin t))))              ; brightness 65..255
                r (+ 1.0 (* 0.8 (+ 1.0 (Math/sin (* 0.7 t)))))]  ; radius 1.0..2.6
            (rl/circle! {:x sx
                         :y sy
                         :radius r
                         :color (rl/rgba b b (min 255 (+ b 20)) 255)})))
        (rl/text! "starfield - GetRandomValue + bulk draw"
                  {:x 12
                   :y 12
                   :size 18
                   :color rl/RAYWHITE})
        (app/maybe-screenshot! frame 20)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
