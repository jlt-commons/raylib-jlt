(ns net.b12n.raylib-jlt.particles-blending
  "raylib [textures] example - particles blending.

  A pool of 200 spark particles trails the mouse: one activates per frame at
  the cursor, falls under gravity and fades out over ~3 seconds, then is
  recycled. SPACE toggles ALPHA vs ADDITIVE blending. No bundled sprite --
  the spark is a procedurally generated soft radial gradient via
  rl/texture-from-fn, the same convention texture_procedural.clj already
  uses, instead of raylib's loaded spark_flame.png. Simplified from the
  original: no per-particle rotation, since rl/texture! draws an
  axis-aligned quad and adding a rotated variant is its own separate change.
  Ported from raylib's examples/textures/textures_particles_blending.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SPARK-SIZE 32)
(def ^:const MAX-PARTICLES 200)
(def ^:const GRAVITY 3.0)

(defn- spark-pixel
  "A soft white dot fading to transparent at the edge -- (f x y) for
  rl/texture-from-fn."
  [x y]
  (let [cx (/ SPARK-SIZE 2.0)
        cy (/ SPARK-SIZE 2.0)
        dx (- x cx)
        dy (- y cy)
        dist (/ (Math/sqrt (+ (* dx dx) (* dy dy))) cx)
        a (max 0.0 (- 1.0 dist))]
    (rl/rgba 255 255 255 (int (* 255 a a)))))

(defn- fresh-particle
  []
  {:x 0.0
   :y 0.0
   :r (rl/get-random-value 0 255)
   :g (rl/get-random-value 0 255)
   :b (rl/get-random-value 0 255)
   :alpha 1.0
   :size (/ (rl/get-random-value 1 30) 20.0)
   :active? false})

(defn- activate-first-inactive
  [particles mx my]
  (loop [i 0]
    (if (>= i MAX-PARTICLES)
      particles
      (if (:active? (nth particles i))
        (recur (inc i))
        (assoc particles i (assoc (nth particles i)
                                  :active? true :alpha 1.0
                                  :x (double mx) :y (double my)))))))

(defn- update-particle
  [p]
  (if (:active? p)
    (let [alpha (- (:alpha p) 0.005)]
      (if (<= alpha 0.0)
        (assoc p :active? false)
        (assoc p :y (+ (:y p) (/ GRAVITY 2.0)) :alpha alpha)))
    p))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - particles blending"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        spark (rl/texture-from-fn SPARK-SIZE SPARK-SIZE spark-pixel)]
    (loop [frame 0 particles (vec (repeatedly MAX-PARTICLES fresh-particle)) blending 0]
      (when (app/keep-running? deadline)
        (let [particles (activate-first-inactive particles (rl/get-mouse-x) (rl/get-mouse-y))
              particles (mapv update-particle particles)
              blending (if (rl/key-pressed? rl/KEY-SPACE) (if (= blending 0) 1 0) blending)]
          (rl/begin-drawing)
          (rl/clear-background rl/DARKGRAY)
          (rl/begin-blend-mode blending)
          (doseq [{:keys [active? x y size r g b alpha]} particles]
            (when active?
              (let [side (* SPARK-SIZE size)]
                (rl/texture! spark {:x (- x (/ side 2.0))
                                    :y (- y (/ side 2.0))
                                    :width side
                                    :height side
                                    :tint (rl/rgba r g b (int (* 255 alpha)))}))))
          (rl/end-blend-mode)
          (rl/text! "PRESS SPACE to CHANGE BLENDING MODE" {:x 150
                                                           :y 20
                                                           :size 20
                                                           :color rl/BLACK})
          (if (= blending 0)
            (rl/text! "ALPHA BLENDING" {:x 290
                                        :y (- H 40)
                                        :size 20
                                        :color rl/BLACK})
            (rl/text! "ADDITIVE BLENDING" {:x 280
                                           :y (- H 40)
                                           :size 20
                                           :color rl/RAYWHITE}))
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) particles blending))))
    (rl/unload-texture! spark))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
