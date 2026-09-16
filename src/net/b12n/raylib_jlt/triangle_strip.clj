(ns net.b12n.raylib-jlt.triangle-strip
  "raylib [shapes] example - triangle strip (`joltc -M:triangle-strip`).

  A rainbow band across the window built vertex by vertex via rlgl immediate mode
  (rlBegin RL_TRIANGLES + rlColor4ub + rlVertex2f), the scalar path around
  raylib's by-value Vector2 shape APIs."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SEGMENTS 16)
(def ^:const PI 3.141592653589793)

(defn- band-color
  [i]
  (let [t (/ (double i) SEGMENTS)
        r (int (* 255 (max 0.0 (Math/sin (* PI t)))))
        g (int (* 255 (max 0.0 (Math/sin (* PI (+ t 0.33))))))
        b (int (* 255 (max 0.0 (Math/sin (* PI (+ t 0.66))))))]
    (rl/rgba r g b 255)))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shapes] example - triangle strip")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        top 130.0 bot 320.0 step (/ (double W) SEGMENTS)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/rl-begin rl/RL-TRIANGLES)
        (doseq [i (range SEGMENTS)]
          (let [x0 (* i step) x1 (* (inc i) step)]
            (rl/rl-color! (band-color i))
            (rl/rl-vertex-2f x0 top) (rl/rl-vertex-2f x0 bot) (rl/rl-vertex-2f x1 top)
            (rl/rl-vertex-2f x1 top) (rl/rl-vertex-2f x0 bot) (rl/rl-vertex-2f x1 bot)))
        (rl/rl-end)
        (rl/text! "a rainbow strip via rlgl immediate mode" :x 10 :y 10 :size 20 :color rl/DARKGRAY)
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
