(ns net.b12n.raylib-jlt.easings-box
  "raylib [shapes] example - easings box anim (`jolt -M:easings-box`).

  Port of raylib's examples/shapes/shapes_easings_box.c. A square drops in,
  flattens into a bar, spins, grows to fill the window, then fades. SPACE
  restarts.

  Five stages, a different curve on each, chosen to contrast rather than to look
  uniform:

  - Drop on EaseElasticOut, which overshoots and springs back.
  - Flatten on EaseBounceOut, so the bar settles in decreasing hops.
  - Spin 270 degrees on EaseQuadOut, a plain decelerate against the two springy
    stages either side of it.
  - Grow on EaseCircOut, which starts fast and brakes hard.
  - Fade on EaseSineOut, the gentlest of them, so the end is unhurried.

  The square rotates about its own centre, which needs DrawRectanglePro. That
  takes a Rectangle and a Vector2 by value and is not bindable, so rect-pro!
  draws it as an rlgl quad.

  Curves come from reasings, keeping raylib's (t, b, c, d) signature. See
  easings-ball for the same idea on three stages, and easings for the whole
  family side by side."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]
   [net.b12n.raylib-jlt.reasings :as ez]))

(def ^:const W 800)
(def ^:const H 450)

;; Frames per stage, as the C has them.
(def ^:const DROP 120.0)
(def ^:const FLATTEN 120.0)
(def ^:const SPIN 240.0)
(def ^:const GROW 120.0)
(def ^:const FADE 160.0)

(defn- stage-length
  [stage]
  (case stage :drop DROP :flatten FLATTEN :spin SPIN :grow GROW :fade FADE 1.0))

(def ^:private next-stage
  {:drop :flatten
   :flatten :spin
   :spin :grow
   :grow :fade
   :fade :done})

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - easings box anim"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           stage :drop
           counter 0.0]
      (when (app/keep-running? deadline)
        (let [restart? (rl/key-pressed? rl/KEY-SPACE)
              stage    (if restart? :drop stage)
              counter  (if restart? 0.0 (inc counter))
              done?    (and (not= stage :done) (>= counter (stage-length stage)))
              [stage counter] (if done? [(next-stage stage) 0.0] [stage counter])
              ;; Each property holds its finished value once its stage is past,
              ;; so later stages animate on top of what earlier ones left.
              y      (if (= stage :drop)
                       (ez/elastic-out counter -100.0 (+ (/ (double H) 2.0) 100.0) DROP)
                       (/ (double H) 2.0))
              height (case stage
                       :drop    100.0
                       :flatten (ez/bounce-out counter 100.0 -90.0 FLATTEN)
                       :grow    (ez/circ-out counter 10.0 (double W) GROW)
                       (:fade :done) (double W)
                       10.0)
              width  (case stage
                       :drop    100.0
                       :flatten (ez/bounce-out counter 100.0 (double W) FLATTEN)
                       (double W))
              rot    (case stage
                       (:drop :flatten) 0.0
                       :spin (ez/quad-out counter 0.0 270.0 SPIN)
                       270.0)
              alpha  (case stage
                       :fade (ez/sine-out counter 1.0 -1.0 FADE)
                       :done 0.0
                       1.0)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect-pro! {:x (/ (double W) 2.0)
                         :y y
                         :width width
                         :height height
                         :origin-x (/ width 2.0)
                         :origin-y (/ height 2.0)
                         :rotation rot
                         :color (rl/rgba 0 82 172 (int (* 255 (max 0.0 (min 1.0 alpha)))))})
          (rl/text! (str "stage: " (name stage) "   [SPACE] restart")
                    {:x 20
                     :y (- H 40)
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 90)
          (rl/end-drawing)
          (recur (inc frame) stage counter)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
