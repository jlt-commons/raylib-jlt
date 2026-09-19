(ns net.b12n.raylib-jlt.eyes
  "raylib [shapes] example - following eyes (`joltc -M:eyes`).

  Ported from examples/shapes/shapes_following_eyes.c: two eyes whose pupils track
  the mouse cursor, each pupil clamped to stay inside its eye. Uses scalar
  GetMouseX / GetMouseY + DrawCircle and a little trig for the pupil offset."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn- pupil
  "Pupil position for an eye centred at (ex,ey) radius er, pupil radius pr, given
  the mouse at (mx,my): the eye centre plus the vector toward the mouse, clamped
  so the pupil stays within the eye."
  [ex ey er pr mx my]
  (let [dx   (- mx ex)
        dy   (- my ey)
        d    (Math/sqrt (+ (* dx dx) (* dy dy)))
        maxd (- er pr)
        s    (if (> d maxd) (/ maxd d) 1.0)]   ; d=0 falls through to s=1, dx=0 → no NaN
    [(+ ex (* dx s)) (+ ey (* dy s))]))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - following eyes"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        er 60.0 pr 22.0
        lx 300.0 rx 500.0 cy 225.0]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              [lpx lpy] (pupil lx cy er pr mx my)
              [rpx rpy] (pupil rx cy er pr mx my)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/circle! {:x (int lx)
                       :y (int cy)
                       :radius er
                       :color rl/LIGHTGRAY})
          (rl/circle! {:x (int rx)
                       :y (int cy)
                       :radius er
                       :color rl/LIGHTGRAY})
          (rl/circle! {:x (int lpx)
                       :y (int lpy)
                       :radius pr
                       :color rl/DARKGRAY})
          (rl/circle! {:x (int rpx)
                       :y (int rpy)
                       :radius pr
                       :color rl/DARKGRAY})
          (rl/text! "the eyes follow the mouse" {:x 10
                                                 :y 10
                                                 :size 20
                                                 :color rl/GRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
