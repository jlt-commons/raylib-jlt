(ns net.b12n.raylib-jlt.analog-clock
  "raylib [shapes] example - analog clock. A live clock face: a ring! bezel, 60 tick
  marks (12 long), and hour/minute/second hands drawn with line-ex!. Time comes from
  rl/local-time (libc); the second hand sweeps smoothly via sub-second millis. Port of
  shapes_clock_of_clocks reduced to a single face."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:private d->r (/ Math/PI 180.0))

(defn- polar
  [cx cy r deg]
  (let [t (* deg d->r)]
    [(+ cx (* r (Math/sin t))) (- cy (* r (Math/cos t)))]))

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - analog clock"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        cx 400 cy 235 r 175]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [[h m s] (rl/local-time)
              frac (/ (double (mod (System/currentTimeMillis) 1000)) 1000.0)
              sec (+ s frac)
              sec-ang (* 6.0 sec)
              min-ang (* 6.0 (+ m (/ sec 60.0)))
              hr-ang  (* 30.0 (+ (mod h 12) (/ m 60.0)))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 24 26 33 255))
          (rl/ring! {:cx cx
                     :cy cy
                     :inner (- r 9)
                     :outer r
                     :start-deg 0
                     :end-deg 360
                     :segments 120
                     :color (rl/rgba 210 212 222 255)})
          ;; tick marks
          (dotimes [i 60]
            (let [long? (zero? (mod i 5))
                  [x0 y0] (polar cx cy (if long? (- r 26) (- r 16)) (* 6.0 i))
                  [x1 y1] (polar cx cy (- r 10) (* 6.0 i))]
              (rl/line-ex! {:x1 x0
                            :y1 y0
                            :x2 x1
                            :y2 y1
                            :thick (if long? 3 1)
                            :color (rl/rgba 150 155 165 255)})))
          ;; hands
          (let [hand (fn [ang len thick color]
                       (let [[x y] (polar cx cy len ang)]
                         (rl/line-ex! {:x1 cx
                                       :y1 cy
                                       :x2 x
                                       :y2 y
                                       :thick thick
                                       :color color})))]
            (hand hr-ang  (* r 0.5)  8 (rl/rgba 235 235 245 255))
            (hand min-ang (* r 0.72) 5 (rl/rgba 235 235 245 255))
            (hand sec-ang (* r 0.84) 2 (rl/rgba 235 90 90 255)))
          (rl/circle! {:x cx
                       :y cy
                       :radius 8
                       :color (rl/rgba 235 90 90 255)})
          (rl/text! (format "%02d:%02d:%02d (local)" h m s)
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/RAYWHITE})
          (app/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
