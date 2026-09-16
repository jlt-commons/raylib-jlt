(ns net.b12n.raylib-jlt.input-gamepad
  "raylib [core] example - input gamepad (`jolt -M:input-gamepad`).

  A live view of gamepad 0: both sticks as dots inside their deadzone circles,
  the triggers as bars, and the face and shoulder buttons lighting up as they are
  pressed. With no pad connected the panel says so and the layout stays put, so
  it is obvious the example is working and the hardware is simply absent.

  All of raylib's gamepad queries are scalar (an int pad index, an int button or
  axis, a bool or float back), so they bind one for one with no struct work."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PAD 0)

(def ^:private face-buttons
  ;; [label button dx dy] laid out as a diamond around (0,0)
  [["Y" rl/PAD-Y 0 -40] ["A" rl/PAD-A 0 40]
   ["X" rl/PAD-X -40 0] ["B" rl/PAD-B 40 0]])

(def ^:private dpad-buttons
  [["^" rl/PAD-UP 0 -34] ["v" rl/PAD-DOWN 0 34]
   ["<" rl/PAD-LEFT -34 0] [">" rl/PAD-RIGHT 34 0]])

(defn- stick!
  "A stick's deadzone circle with a dot at its current position."
  [cx cy ax ay label]
  (let [x (rl/get-gamepad-axis-movement PAD ax)
        y (rl/get-gamepad-axis-movement PAD ay)]
    (rl/circle-lines! {:x cx
                       :y cy
                       :radius 48
                       :color rl/LIGHTGRAY})
    (rl/circle! {:x (int (+ cx (* 44 x)))
                 :y (int (+ cy (* 44 y)))
                 :radius 12
                 :color rl/BLUE})
    (rl/text! label {:x (- cx 20)
                     :y (+ cy 58)
                     :size 14
                     :color rl/GRAY})
    (rl/text! (format "%.2f %.2f" x y) {:x (- cx 34)
                                        :y (+ cy 76)
                                        :size 14
                                        :color rl/DARKGRAY})))

(defn- button!
  [cx cy dx dy label button]
  (let [on? (rl/gamepad-down? PAD button)]
    (rl/circle! {:x (+ cx dx)
                 :y (+ cy dy)
                 :radius 16
                 :color (if on? rl/GOLD (rl/rgba 0 0 0 20))})
    (rl/text! label {:x (- (+ cx dx) 4)
                     :y (- (+ cy dy) 8)
                     :size 16
                     :color (if on? rl/DARKBROWN rl/GRAY)})))

(defn- trigger!
  [x y axis label]
  ;; Triggers rest at -1 and go to +1, so remap to 0..1 for the bar.
  (let [v (/ (+ 1.0 (rl/get-gamepad-axis-movement PAD axis)) 2.0)]
    (rl/rect! {:x x
               :y y
               :width 120
               :height 16
               :color (rl/rgba 0 0 0 20)})
    (rl/rect! {:x x
               :y y
               :width (int (* 120 v))
               :height 16
               :color rl/SKYBLUE})
    (rl/text! label {:x x
                     :y (- y 20)
                     :size 14
                     :color rl/GRAY})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - input gamepad"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [there? (rl/gamepad-available? PAD)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! (if there?
                      (str "gamepad 0: " (rl/get-gamepad-name PAD))
                      "no gamepad detected on port 0")
                    {:x 40
                     :y 30
                     :size 20
                     :color (if there? rl/DARKGREEN rl/MAROON)})
          (when there?
            (rl/text! (str (rl/get-gamepad-axis-count PAD) " axes")
                      {:x 40
                       :y 58
                       :size 14
                       :color rl/GRAY}))
          (stick! 170 210 rl/AXIS-LEFT-X rl/AXIS-LEFT-Y "left stick")
          (stick! 630 210 rl/AXIS-RIGHT-X rl/AXIS-RIGHT-Y "right stick")
          (doseq [[label b dx dy] face-buttons] (button! 500 200 dx dy label b))
          (doseq [[label b dx dy] dpad-buttons] (button! 300 200 dx dy label b))
          (trigger! 300 360 4 "L2")
          (trigger! 460 360 5 "R2")
          (doseq [[label b x] [["L1" rl/PAD-L1 300] ["R1" rl/PAD-R1 460]]]
            (let [on? (rl/gamepad-down? PAD b)]
              (rl/rect! {:x x
                         :y 300
                         :width 120
                         :height 22
                         :color (if on? rl/GOLD (rl/rgba 0 0 0 20))})
              (rl/text! label {:x (+ x 50)
                               :y 303
                               :size 16
                               :color (if on? rl/DARKBROWN rl/GRAY)})))
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing))
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
