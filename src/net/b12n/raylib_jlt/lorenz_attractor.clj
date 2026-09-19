(ns net.b12n.raylib-jlt.lorenz-attractor
  "raylib [models] example - Lorenz attractor (`jolt -M:lorenz-attractor`).

  The Lorenz system integrated one step per frame and drawn as a 3D ribbon, which
  traces the butterfly the equations are famous for: two lobes the trajectory
  swaps between at intervals no amount of precision will let you predict.

    dx/dt = sigma (y - x)
    dy/dt = x (rho - z) - y
    dz/dt = x y - beta z

  UP/DOWN change rho (the Rayleigh number, which is what decides whether the
  system settles or stays chaotic), SPACE pauses, R restarts from a fresh seed.
  The trail is a fixed-length window of the last few thousand points, coloured by
  age, drawn as rlgl RL_LINES segments under an orbiting camera."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TRAIL 4000)
(def ^:const DT 0.006)
(def ^:const SIGMA 10.0)
(def ^:const BETA (/ 8.0 3.0))
(def ^:const SCALE 0.42)

(defn- step
  "One Euler step of the Lorenz system. The step is small enough that Euler holds
  up here: the attractor's shape is robust to integration error, which is part of
  what makes it a good demonstration."
  [[x y z] rho]
  [(+ x (* DT SIGMA (- y x)))
   (+ y (* DT (- (* x (- rho z)) y)))
   (+ z (* DT (- (* x y) (* BETA z))))])

(defn- seed
  []
  [(/ (rl/get-random-value -20 20) 10.0) 0.0 (/ (rl/get-random-value 5 25) 1.0)])

(def ^:const WARMUP 900)

(defn- warm
  "Run the system forward from a fresh seed before the first frame. The opening
  transient is a single long swing into one lobe and says nothing about the
  attractor; starting on the orbit means the butterfly is there to see straight
  away, and R gets the same treatment."
  [rho]
  (vec (take-last TRAIL
                  (reduce (fn [p _] (conj p (step (peek p) rho)))
                          [(seed)]
                          (range WARMUP)))))

(defn- draw-trail!
  [points]
  (let [n (count points)]
    (rl/rl-begin rl/RL-LINES)
    (dotimes [i (dec n)]
      (let [[x1 y1 z1] (nth points i)
            [x2 y2 z2] (nth points (inc i))
            ;; Age drives the colour, so the head of the trail reads as the
            ;; current position without needing a separate marker.
            age (/ (double i) n)]
        ;; A bright cyan-to-pink ramp rather than a fade to black: on a dark
        ;; ground a single-pixel line at low luminance simply disappears.
        (rl/rl-color! (rl/rgba (int (+ 90 (* 165 age)))
                               (int (- 200 (* 130 age)))
                               (int (- 255 (* 105 age)))
                               255))
        (rl/rl-vertex-3f (* SCALE x1) (* SCALE (- z1 25.0)) (* SCALE y1))
        (rl/rl-vertex-3f (* SCALE x2) (* SCALE (- z2 25.0)) (* SCALE y2))))
    (rl/rl-end)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - Lorenz attractor"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           points (warm 28.0)
           rho 28.0
           running? true]
      (when (app/keep-running? deadline)
        (let [running? (if (rl/key-pressed? rl/KEY-SPACE) (not running?) running?)
              rho (cond
                    (rl/key-down? rl/KEY-UP) (min 60.0 (+ rho 0.15))
                    (rl/key-down? rl/KEY-DOWN) (max 1.0 (- rho 0.15))
                    :else rho)
              points (cond
                       (rl/key-pressed? rl/KEY-R) (warm rho)
                       ;; Six integration steps a frame: one is too slow to watch
                       ;; the trajectory develop, and the fixed trail window keeps
                       ;; the per-frame cost flat however long it runs.
                       running? (vec (take-last TRAIL
                                                (reduce (fn [p _] (conj p (step (peek p) rho)))
                                                        points
                                                        (range 6))))
                       :else points)
              t (* 0.25 (rl/get-time))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 12 14 22 255))
          (rl/with-camera-3d
            {:pos-x (* 26 (Math/sin t))
             :pos-y 8.0
             :pos-z (* 26 (Math/cos t))
             :target-x 0.0
             :target-y 0.0
             :target-z 0.0
             :fovy 45}
            (fn [] (draw-trail! points)))
          (rl/text! "Lorenz attractor" {:x 20
                                        :y 18
                                        :size 22
                                        :color rl/RAYWHITE})
          (rl/text! (format "rho %.2f   sigma %.1f   beta %.2f" rho SIGMA BETA)
                    {:x 20
                     :y 46
                     :size 16
                     :color rl/SKYBLUE})
          (rl/text! (str (count points) " points" (when-not running? "   (paused)"))
                    {:x 20
                     :y 68
                     :size 14
                     :color rl/GRAY})
          (rl/text! "UP/DOWN rho   SPACE pause   R reseed"
                    {:x 20
                     :y (- H 30)
                     :size 14
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame) points rho running?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
