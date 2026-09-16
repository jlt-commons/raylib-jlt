(ns net.b12n.raylib-jlt.easings-ball
  "raylib [shapes] example - easings ball anim (`jolt -M:easings-ball`).

  Port of raylib's examples/shapes/shapes_easings_ball.c. A ball slides in,
  swells until it fills the window, then fades out. SPACE restarts.

  Three stages, each one curve on one property, which is the point: the ball
  never moves and grows at the same time. Reading them in order shows what each
  curve feels like in isolation.

  - Slide in on EaseElasticOut. It overshoots and springs back, so the ball
    arrives past centre and settles.
  - Swell on EaseElasticIn. The mirror image: almost nothing happens for most of
    the duration, then it snaps.
  - Fade on EaseCubicOut, which is smooth and unremarkable on purpose, so the
    two elastic stages stand out against it.

  Curves come from reasings, the shared counterpart of raylib's reasings.h, and
  keep its (t, b, c, d) signature. See easings for all of them at once."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]
   [net.b12n.raylib-jlt.reasings :as ez]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const SLIDE-FRAMES 120.0)
(def ^:const SWELL-FRAMES 200.0)
(def ^:const FADE-FRAMES 200.0)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shapes] example - easings ball anim")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           stage :slide
           counter 0.0]
      (when (rl/keep-running? deadline)
        (let [restart? (rl/key-pressed? rl/KEY-SPACE)
              counter  (if restart? 0.0 (inc counter))
              stage    (if restart? :slide stage)
              ;; Each stage runs its own curve and hands over when its time is up.
              [stage counter] (case stage
                                :slide (if (>= counter SLIDE-FRAMES) [:swell 0.0] [:slide counter])
                                :swell (if (>= counter SWELL-FRAMES) [:fade 0.0] [:swell counter])
                                :fade  (if (>= counter FADE-FRAMES) [:done 0.0] [:fade counter])
                                [:done 0.0])
              x      (if (= stage :slide)
                       (ez/elastic-out counter -100.0 (+ (/ (double W) 2.0) 100.0) SLIDE-FRAMES)
                       (/ (double W) 2.0))
              radius (case stage
                       :slide 20.0
                       :swell (ez/elastic-in counter 20.0 500.0 SWELL-FRAMES)
                       520.0)
              alpha  (case stage
                       :fade (- 1.0 (ez/cubic-out counter 0.0 1.0 FADE-FRAMES))
                       :done 0.0
                       1.0)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/circle! :x x :y (/ H 2) :radius radius
                      :color (rl/rgba 190 33 55 (int (* 255 (max 0.0 (min 1.0 alpha))))))
          (rl/text! (str "stage: " (clojure.core/name stage) "   [SPACE] restart")
                    :x 20 :y (- H 40) :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame) stage counter)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
