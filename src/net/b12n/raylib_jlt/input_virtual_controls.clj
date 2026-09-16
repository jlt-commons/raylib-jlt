(ns net.b12n.raylib-jlt.input-virtual-controls
  "raylib [core] example - input virtual controls (`jolt -M:input-virtual-controls`).

  An on-screen D-pad and action button, the control scheme a touch device needs
  when there is no keyboard. Press a pad segment with the mouse to move the
  square; the A button makes it hop. The keyboard arrows drive the same state, so
  the two input paths stay interchangeable.

  There is nothing raylib-specific about the hit testing: each segment is a circle
  and a wedge of angle around the pad's centre, both plain arithmetic, which is
  what keeps the control layout independent of the drawing API."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PAD-X 130)
(def ^:const PAD-Y 320)
(def ^:const PAD-R 82)
(def ^:const BTN-X 680)
(def ^:const BTN-Y 320)
(def ^:const BTN-R 46)

(def ^:private segments
  ;; [direction dx dy centre-angle-degrees]
  [[:up 0 -1 270] [:right 1 0 0] [:down 0 1 90] [:left -1 0 180]])

(defn- pressed-segment
  "Which D-pad quadrant the pointer is in, or nil. A press counts when it is
  inside the pad's ring and outside the dead centre, then the angle picks the
  quadrant: each direction owns the 90 degrees centred on its own axis."
  [mx my down?]
  (when down?
    (let [dx (- mx PAD-X)
          dy (- my PAD-Y)
          d (Math/sqrt (+ (* dx dx) (* dy dy)))]
      (when (< 22 d PAD-R)
        (let [deg (mod (+ 360 (Math/toDegrees (Math/atan2 dy dx))) 360)]
          (first (for [[dir _ _ centre] segments
                       :when (< (Math/abs (- 180 (Math/abs (- 180 (Math/abs (- deg centre)))))) 45)]
                   dir)))))))

(defn- segment!
  [dir dx dy active?]
  (let [x (+ PAD-X (* dx 44))
        y (+ PAD-Y (* dy 44))]
    (rl/circle! {:x x
                 :y y
                 :radius 26
                 :color (if active? rl/BLUE (rl/rgba 0 0 0 25))})
    (rl/text! (case dir :up "^" :down "v" :left "<" :right ">")
              {:x (- x 4)
               :y (- y 9)
               :size 20
               :color (if active? rl/RAYWHITE rl/GRAY)})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - input virtual controls"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           px 400.0
           py 160.0
           hop 0.0]
      (when (rl/keep-running? deadline)
        (let [mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              down? (rl/mouse-down? rl/MOUSE-LEFT)
              seg (pressed-segment mx my down?)
              btn? (and down?
                        (let [dx (- mx BTN-X) dy (- my BTN-Y)]
                          (< (+ (* dx dx) (* dy dy)) (* BTN-R BTN-R))))
              ;; The two input paths feed one movement vector, so nothing
              ;; downstream needs to know which was used.
              vx (+ (if (or (= seg :left) (rl/key-down? rl/KEY-LEFT)) -1 0)
                    (if (or (= seg :right) (rl/key-down? rl/KEY-RIGHT)) 1 0))
              vy (+ (if (or (= seg :up) (rl/key-down? rl/KEY-UP)) -1 0)
                    (if (or (= seg :down) (rl/key-down? rl/KEY-DOWN)) 1 0))
              speed (* 190 (rl/get-frame-time))
              px (max 30.0 (min (- W 30.0) (+ px (* vx speed))))
              py (max 30.0 (min 230.0 (+ py (* vy speed))))
              hop (cond
                    (or btn? (rl/key-pressed? rl/KEY-SPACE)) 1.0
                    (pos? hop) (max 0.0 (- hop (* 2.4 (rl/get-frame-time))))
                    :else 0.0)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "virtual controls" {:x 30
                                        :y 26
                                        :size 24
                                        :color rl/DARKGRAY})
          (rl/text! "press the pad with the mouse, or use the arrow keys"
                    {:x 30
                     :y 58
                     :size 14
                     :color rl/GRAY})
          ;; The hop is a sine arc over the button's decay, so the square lifts
          ;; and settles rather than teleporting.
          (rl/rect! {:x (int (- px 22))
                     :y (int (- py 22 (* 60 (Math/sin (* Math/PI hop)))))
                     :width 44
                     :height 44
                     :color rl/MAROON})
          (rl/circle-lines! {:x PAD-X
                             :y PAD-Y
                             :radius PAD-R
                             :color rl/LIGHTGRAY})
          (doseq [[dir dx dy _] segments]
            (segment! dir dx dy (or (= seg dir)
                                    (rl/key-down? (case dir
                                                    :up rl/KEY-UP
                                                    :down rl/KEY-DOWN
                                                    :left rl/KEY-LEFT
                                                    :right rl/KEY-RIGHT)))))
          (rl/circle! {:x BTN-X
                       :y BTN-Y
                       :radius BTN-R
                       :color (if btn? rl/MAROON (rl/rgba 190 33 55 60))})
          (rl/text! "A" {:x (- BTN-X 8)
                         :y (- BTN-Y 12)
                         :size 26
                         :color (if btn? rl/RAYWHITE rl/MAROON)})
          (rl/text! "jump" {:x (- BTN-X 20)
                            :y (+ BTN-Y BTN-R 10)
                            :size 14
                            :color rl/GRAY})
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) px py hop)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
