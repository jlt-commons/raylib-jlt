(ns net.b12n.raylib-jlt.double-pendulum
  "raylib [shapes] example - double pendulum (`joltc -M:double-pendulum`).

  A chaotic double pendulum integrated with the standard equations of motion,
  with a fading trail of the lower bob. Pure math over lines + circles."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const L1 120.0)
(def ^:const L2 120.0)
(def ^:const M1 10.0)
(def ^:const M2 10.0)
(def ^:const G 1.0)
(def ^:const DT 0.06)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - double pendulum"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        ox (/ W 2.0) oy 150.0]
    (loop [frame 0 a1 2.2 a2 2.6 v1 0.0 v2 0.0 trail []]
      (when (app/keep-running? deadline)
        (let [d    (- a1 a2)
              den1 (* L1 (- (+ (* 2 M1) M2) (* M2 (Math/cos (- (* 2 a1) (* 2 a2))))))
              den2 (* L2 (- (+ (* 2 M1) M2) (* M2 (Math/cos (- (* 2 a1) (* 2 a2))))))
              acc1 (/ (+ (* (- G) (+ (* 2 M1) M2) (Math/sin a1))
                         (* (- M2) G (Math/sin (- a1 (* 2 a2))))
                         (* -2 (Math/sin d) M2
                            (+ (* v2 v2 L2) (* v1 v1 L1 (Math/cos d)))))
                      den1)
              acc2 (/ (* 2 (Math/sin d)
                         (+ (* v1 v1 L1 (+ M1 M2))
                            (* G (+ M1 M2) (Math/cos a1))
                            (* v2 v2 L2 M2 (Math/cos d))))
                      den2)
              v1' (+ v1 (* DT acc1))
              v2' (+ v2 (* DT acc2))
              a1' (+ a1 v1')
              a2' (+ a2 v2')
              x1 (+ ox (* L1 (Math/sin a1'))) y1 (+ oy (* L1 (Math/cos a1')))
              x2 (+ x1 (* L2 (Math/sin a2'))) y2 (+ y1 (* L2 (Math/cos a2')))
              trail (vec (take-last 120 (conj trail [x2 y2])))
              n (count trail)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 20 20 30 255))
          (doseq [i (range n)]
            (let [[tx ty] (nth trail i)
                  t (/ (inc i) (double n))]
              (rl/circle! {:x (int tx)
                           :y (int ty)
                           :radius (* 2.0 t)
                           :color (rl/rgba 80 200 255 (int (* 200 t)))})))
          (rl/line! {:x1 (int ox)
                     :y1 (int oy)
                     :x2 (int x1)
                     :y2 (int y1)
                     :color rl/RAYWHITE})
          (rl/line! {:x1 (int x1)
                     :y1 (int y1)
                     :x2 (int x2)
                     :y2 (int y2)
                     :color rl/RAYWHITE})
          (rl/circle! {:x (int x1)
                       :y (int y1)
                       :radius 8
                       :color rl/GOLD})
          (rl/circle! {:x (int x2)
                       :y (int y2)
                       :radius 8
                       :color rl/RED})
          (rl/text! "a chaotic double pendulum" {:x 10
                                                 :y 10
                                                 :size 20
                                                 :color rl/RAYWHITE})
          (app/maybe-screenshot! frame 70)
          (rl/end-drawing)
          (recur (inc frame) a1' a2' v1' v2' trail)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
