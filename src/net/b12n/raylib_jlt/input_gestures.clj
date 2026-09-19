(ns net.b12n.raylib-jlt.input-gestures
  "raylib [core] example - input gestures (`jolt -M:input-gestures`).

  Port of raylib's examples/core/core_input_gestures.c. Gestures made inside the
  right-hand box are named and pushed onto a log down the left. On a desktop the
  mouse stands in for a finger, so tap, double-tap, hold and drag all work; the
  swipes and pinches need a trackpad or a touchscreen.

  raylib reports one gesture at a time from GetGestureDetected, and reports it on
  every frame the gesture persists. Logging that directly would fill the list
  with one entry per frame, so an entry is recorded only when the gesture differs
  from the previous frame's, which is what the C does with lastGesture.

  See input-multitouch for the raw touch points underneath these."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MAX-LOG 20)

(def ^:private gesture-names
  {rl/GESTURE-TAP         "GESTURE TAP"
   rl/GESTURE-DOUBLETAP   "GESTURE DOUBLETAP"
   rl/GESTURE-HOLD        "GESTURE HOLD"
   rl/GESTURE-DRAG        "GESTURE DRAG"
   rl/GESTURE-SWIPE-RIGHT "GESTURE SWIPE RIGHT"
   rl/GESTURE-SWIPE-LEFT  "GESTURE SWIPE LEFT"
   rl/GESTURE-SWIPE-UP    "GESTURE SWIPE UP"
   rl/GESTURE-SWIPE-DOWN  "GESTURE SWIPE DOWN"
   rl/GESTURE-PINCH-IN    "GESTURE PINCH IN"
   rl/GESTURE-PINCH-OUT   "GESTURE PINCH OUT"})

(def ^:const AREA-X 220)
(def ^:const AREA-Y 10)
(def ^:const AREA-W (- W 230))
(def ^:const AREA-H (- H 20))

(defn- in-area?
  [x y]
  (and (>= x AREA-X) (< x (+ AREA-X AREA-W))
       (>= y AREA-Y) (< y (+ AREA-Y AREA-H))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - input gestures"})
  (rl/set-gestures-enabled 0x0fff)   ; every gesture flag raylib knows
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           last-gesture rl/GESTURE-NONE
           log []]
      (when (app/keep-running? deadline)
        (let [gesture (rl/get-gesture-detected)
              tx      (rl/get-touch-x)
              ty      (rl/get-touch-y)
              ;; Only on the frame the gesture changes, and only inside the box.
              new?    (and (not= gesture rl/GESTURE-NONE)
                           (not= gesture last-gesture)
                           (in-area? tx ty))
              log     (if new?
                        (let [entry (get gesture-names gesture (str "GESTURE " gesture))]
                          (vec (take-last MAX-LOG (conj log entry))))
                        log)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect! {:x AREA-X
                     :y AREA-Y
                     :width AREA-W
                     :height AREA-H
                     :color rl/GRAY})
          (rl/text! "GESTURES TEST AREA" {:x (- (+ AREA-X AREA-W) 210)
                                          :y (- (+ AREA-Y AREA-H) 40)
                                          :size 20
                                          :color (rl/rgba 200 200 200 255)})
          ;; The log, newest at the bottom, alternating rows for legibility.
          (dotimes [i (count log)]
            (let [y (+ 15 (* i 20))]
              (when (odd? i)
                (rl/rect! {:x 0
                           :y y
                           :width 200
                           :height 20
                           :color (rl/rgba 200 200 200 128)}))
              (rl/text! (nth log i) {:x 35
                                     :y (+ y 3)
                                     :size 14
                                     :color (if (= i (dec (count log))) rl/MAROON rl/DARKGRAY)})))
          (rl/rect-lines! {:x 0
                           :y 0
                           :width 200
                           :height H
                           :color rl/GRAY})
          (rl/text! "DETECTED GESTURES" {:x 25
                                         :y (- H 30)
                                         :size 14
                                         :color rl/GRAY})
          ;; Where raylib thinks the finger is, when it thinks there is one.
          (when (not= gesture rl/GESTURE-NONE)
            (rl/circle! {:x tx
                         :y ty
                         :radius 30
                         :color rl/MAROON}))
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) gesture log)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
