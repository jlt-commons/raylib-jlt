(ns net.b12n.raylib-jlt.bullet-hell
  "raylib [shapes] example - bullet spiral (`joltc -M:bullet-hell`).

  A rotating emitter at the centre sprays bullets outward in a three-armed spiral;
  each bullet flies until it leaves the window. Pure math over draw-circle."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const ARM (/ (* 2 3.141592653589793) 3))   ; 120° between the three arms

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shapes] example - bullet spiral")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cx (/ W 2.0) cy (/ H 2.0)]
    (loop [frame 0 bullets []]
      (when (rl/keep-running? deadline)
        (let [base    (* 0.2 frame)
              spawned (for [k (range 3)]
                        (let [a (+ base (* k ARM))]
                          {:x cx
                           :y cy
                           :vx (* 3.5 (Math/cos a))
                           :vy (* 3.5 (Math/sin a))}))
              bullets (->> (into bullets spawned)
                           (map (fn [b] (assoc b :x (+ (:x b) (:vx b)) :y (+ (:y b) (:vy b)))))
                           (filterv (fn [b]
                                      (and (<= -10 (:x b) (+ W 10))
                                           (<= -10 (:y b) (+ H 10))))))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 15 15 30 255))
          (doseq [b bullets]
            (rl/circle! :x (int (:x b)) :y (int (:y b)) :radius 4 :color rl/GOLD))
          (rl/circle! :x (int cx) :y (int cy) :radius 12 :color rl/RED)
          (rl/text! "a rotating bullet spiral" :x 10 :y 10 :size 20 :color rl/RAYWHITE)
          (rl/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame) bullets)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
