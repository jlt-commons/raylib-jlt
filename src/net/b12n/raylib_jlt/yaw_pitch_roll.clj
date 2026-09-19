(ns net.b12n.raylib-jlt.yaw-pitch-roll
  "raylib [models] example - yaw, pitch and roll (`jolt -M:yaw-pitch-roll`).

  A plane built out of boxes, flown with the three aircraft rotations: A/D yaw,
  W/S pitch, Q/E roll. Let go and each axis eases back to level, which makes the
  order the rotations compose in easy to feel: roll is applied in the plane's own
  frame, so rolling first and then pitching does not put the nose where pitching
  first and then rolling does.

  The rotations are rlgl matrix-stack operations (push, three rotatef calls, pop)
  rather than a matrix built here and multiplied in. rlgl applies whatever
  transform is current to each vertex as it is submitted, so wrapping the model in
  a push/pop moves the whole thing and leaves the rest of the scene alone."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn- ease-to-zero
  "Decay an angle back toward level, with a floor so it actually reaches 0."
  [a]
  (if (< (Math/abs a) 0.15) 0.0 (* a 0.94)))

(defn- draw-plane!
  []
  ;; Fuselage, wings, tailplane and fin, all axis-aligned boxes: the transform
  ;; above is doing the interesting work, so the model stays trivial.
  (rl/cube! {:pos [0.0 0.0 0.0]
             :size [1.1 0.7 4.4]
             :color (rl/rgba 200 205 215 255)})
  (rl/cube! {:pos [0.0 0.0 -2.6]
             :size [0.7 0.5 1.2]
             :color (rl/rgba 160 165 180 255)})
  (rl/cube! {:pos [0.0 0.0 0.2]
             :size [7.0 0.22 1.3]
             :color (rl/rgba 0 121 241 255)})
  (rl/cube! {:pos [0.0 0.0 1.9]
             :size [2.6 0.18 0.7]
             :color (rl/rgba 0 82 172 255)})
  (rl/cube! {:pos [0.0 0.7 2.0]
             :size [0.16 1.3 0.7]
             :color (rl/rgba 230 41 55 255)}))

(defn- gauge!
  [x y label v]
  (rl/text! label {:x x
                   :y y
                   :size 14
                   :color rl/GRAY})
  (rl/rect! {:x x
             :y (+ y 20)
             :width 160
             :height 12
             :color (rl/rgba 0 0 0 20)})
  ;; Centre-anchored bar: the fill grows out of the middle in whichever
  ;; direction the angle went, so level reads as empty.
  (let [half (int (* 80 (max -1.0 (min 1.0 (/ v 45.0)))))]
    (rl/rect! {:x (if (neg? half) (+ x 80 half) (+ x 80))
               :y (+ y 20)
               :width (Math/abs half)
               :height 12
               :color rl/SKYBLUE}))
  (rl/rect! {:x (+ x 79)
             :y (+ y 17)
             :width 2
             :height 18
             :color rl/DARKGRAY})
  (rl/text! (format "%6.1f deg" v) {:x (+ x 168)
                                    :y (+ y 19)
                                    :size 14
                                    :color rl/LIGHTGRAY}))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - yaw pitch roll"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           yaw 0.0
           pitch 0.0
           roll 0.0]
      (when (app/keep-running? deadline)
        (let [yaw (cond (rl/key-down? rl/KEY-A) (min 90.0 (+ yaw 1.1))
                        (rl/key-down? rl/KEY-D) (max -90.0 (- yaw 1.1))
                        :else (ease-to-zero yaw))
              pitch (cond (rl/key-down? rl/KEY-W) (min 90.0 (+ pitch 0.9))
                          (rl/key-down? rl/KEY-S) (max -90.0 (- pitch 0.9))
                          :else (ease-to-zero pitch))
              roll (cond (rl/key-down? rl/KEY-Q) (max -90.0 (- roll 1.3))
                         (rl/key-down? rl/KEY-E) (min 90.0 (+ roll 1.3))
                         :else (ease-to-zero roll))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 18 22 34 255))
          (rl/with-camera-3d
            ;; A three-quarter view rather than dead astern: from directly
            ;; behind, a yaw and a roll look alike for the first few degrees.
            {:pos-x 7.0
             :pos-y 5.0
             :pos-z 10.0
             :target-x 0.0
             :target-y 0.0
             :target-z 0.0
             :fovy 45}
            (fn []
              ;; The grid sits below the plane instead of through it, and out of
              ;; the corner where the gauges are.
              (rl/rl-push-matrix)
              (rl/rl-translatef 0.0 -3.0 0.0)
              (rl/draw-grid 12 1.0)
              (rl/rl-pop-matrix)
              (rl/rl-push-matrix)
              ;; Yaw about the vertical, then pitch, then roll about the nose:
              ;; each rotatef applies in the frame the previous ones left behind.
              (rl/rl-rotatef yaw 0.0 1.0 0.0)
              (rl/rl-rotatef pitch 1.0 0.0 0.0)
              (rl/rl-rotatef roll 0.0 0.0 1.0)
              (draw-plane!)
              (rl/rl-pop-matrix)))
          (rl/text! "yaw, pitch and roll" {:x 24
                                           :y 20
                                           :size 22
                                           :color rl/RAYWHITE})
          ;; The gauges sit over the 3D scene, so they get their own ground.
          (rl/rect! {:x 0
                     :y 288
                     :width W
                     :height (- H 288)
                     :color (rl/rgba 10 12 20 220)})
          (gauge! 24 300 "yaw   A / D" yaw)
          (gauge! 24 348 "pitch W / S" pitch)
          (gauge! 24 396 "roll  Q / E" roll)
          (rl/text! "let go and each axis eases back to level"
                    {:x 430
                     :y 396
                     :size 14
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) yaw pitch roll)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
