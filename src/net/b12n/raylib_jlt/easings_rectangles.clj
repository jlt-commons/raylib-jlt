(ns net.b12n.raylib-jlt.easings-rectangles
  "raylib [shapes] example - easings rectangles (`jolt -M:easings-rectangles`).

  Port of raylib's examples/shapes/shapes_easings_rectangles.c. A grid of
  rectangles collapses to nothing while the whole field spins, then SPACE plays
  it again.

  Both the shrink and the spin are the same easing curve, EaseCircOut, run over
  the same frame counter. That is what makes the animation read as one movement
  rather than two: the rectangles are smallest exactly when the rotation settles.
  A circular ease-out starts fast and decelerates hard, which is why the grid
  seems to snap away and then drift the last few degrees.

  raylib's easing signature is (t, b, c, d): current time, begin value, total
  change, duration. It is worth keeping rather than normalising to [0,1], because
  passing a negative change is how the C shrinks rather than grows, and the call
  sites read the same as the original.

  Each rectangle rotates about its own centre, which needs DrawRectanglePro.
  That takes a Rectangle and a Vector2 by value, so it is not bindable; rect-pro!
  draws the same thing as an rlgl quad.

  See easings for the curve family compared side by side."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const RECS-X 16)
(def ^:const RECS-Y 9)
(def ^:const REC-W (/ (double W) RECS-X))
(def ^:const REC-H (/ (double H) RECS-Y))
(def ^:const PLAY-FRAMES 240.0)

(defn- ease-circ-out
  "raylib's EaseCircOut, kept in its (t, b, c, d) shape."
  [t b c d]
  (let [t (min 1.0 (/ t d))]
    (+ (* c (Math/sqrt (- 1.0 (* (dec t) (dec t))))) b)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - easings rectangles"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           counter 0.0
           playing? true]
      (when (app/keep-running? deadline)
        (let [counter  (if playing? (inc counter) counter)
              done?    (>= counter PLAY-FRAMES)
              playing? (cond
                         (and playing? done?)               false
                         (rl/key-pressed? rl/KEY-SPACE)     true
                         :else                              playing?)
              counter  (if (and (not playing?) (rl/key-pressed? rl/KEY-SPACE)) 0.0 counter)
              ;; One curve, two uses: the rectangles shrink to nothing and the
              ;; field turns 360 degrees over the same frames.
              w        (max 0.0 (ease-circ-out counter REC-W (- REC-W) PLAY-FRAMES))
              h        (max 0.0 (ease-circ-out counter REC-H (- REC-H) PLAY-FRAMES))
              rotation (ease-circ-out counter 0.0 360.0 PLAY-FRAMES)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (dotimes [gy RECS-Y]
            (dotimes [gx RECS-X]
              (let [cx (+ (/ REC-W 2.0) (* REC-W gx))
                    cy (+ (/ REC-H 2.0) (* REC-H gy))]
                ;; Origin at the rectangle's own centre, so each spins in place.
                (rl/rect-pro! {:x cx
                               :y cy
                               :width w
                               :height h
                               :origin-x (/ w 2.0)
                               :origin-y (/ h 2.0)
                               :rotation rotation
                               :color rl/MAROON}))))
          (rl/text! (if playing?
                      "easing out, both size and rotation"
                      "[SPACE] play again")
                    {:x 10
                     :y (- H 30)
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) counter playing?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
