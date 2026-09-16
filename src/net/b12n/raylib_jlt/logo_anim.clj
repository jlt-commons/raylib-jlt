(ns net.b12n.raylib-jlt.logo-anim
  "raylib [shapes] example - raylib logo animation (`jolt -M:logo-anim`).

  Port of raylib's examples/shapes/shapes_logo_raylib_anim.c. The raylib logo
  assembles itself: a blinking square, then the top and left bars grow, then the
  bottom and right close the frame, then the letters arrive one at a time and the
  whole thing fades. R replays it.

  Nothing here is eased. Every stage advances by a fixed step per frame, and the
  stage changes when a counter hits an exact value, which is how the original
  reads and why it feels mechanical rather than smooth. That is worth preserving:
  it is the counterpoint to easings-ball and easings-box sitting beside it in the
  same group.

  The letters appear by drawing a growing prefix of \"raylib\", one more every
  twelve frames. raylib does that with TextSubtext; subs does it here.

  See logo-raylib for the finished logo drawn in one pass."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const LOGO 256)       ; the logo is 256x256
(def ^:const BORDER 16)      ; bar thickness
(def ^:const LX (- (/ W 2) (/ LOGO 2)))
(def ^:const LY (- (/ H 2) (/ LOGO 2)))

(defn- fade
  [alpha]
  (rl/rgba 0 0 0 (int (* 255 (max 0.0 (min 1.0 alpha))))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - raylib logo animation"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           state 0
           counter 0
           top-w 16 left-h 16 bottom-w 16 right-h 16
           letters 0
           alpha 1.0]
      (when (rl/keep-running? deadline)
        (let [replay? (and (= state 4) (rl/key-pressed? rl/KEY-R))
              ;; Fixed steps, exact thresholds: no easing anywhere.
              [state counter top-w left-h bottom-w right-h letters alpha]
              (cond
                replay? [0 0 16 16 16 16 0 1.0]

                (= state 0)
                (if (>= counter 120)
                  [1 0 top-w left-h bottom-w right-h letters alpha]
                  [0 (inc counter) top-w left-h bottom-w right-h letters alpha])

                (= state 1)
                (let [tw (+ top-w 4) lh (+ left-h 4)]
                  [(if (>= tw LOGO) 2 1) counter tw lh bottom-w right-h letters alpha])

                (= state 2)
                (let [bw (+ bottom-w 4) rh (+ right-h 4)]
                  [(if (>= bw LOGO) 3 2) counter top-w left-h bw rh letters alpha])

                (= state 3)
                (let [c (inc counter)
                      [c letters] (if (>= c 12) [0 (inc letters)] [c letters])
                      a (if (>= letters 10) (- alpha 0.02) alpha)]
                  (if (<= a 0.0)
                    [4 c top-w left-h bottom-w right-h letters 0.0]
                    [3 c top-w left-h bottom-w right-h letters a]))

                :else [state counter top-w left-h bottom-w right-h letters alpha])]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (cond
            ;; The blink: on for half of each 30-frame cycle.
            (= state 0)
            (when (< (mod (quot counter 15) 2) 1)
              (rl/rect! {:x LX
                         :y LY
                         :width BORDER
                         :height BORDER
                         :color rl/BLACK}))

            (#{1 2} state)
            (do
              (rl/rect! {:x LX
                         :y LY
                         :width top-w
                         :height BORDER
                         :color rl/BLACK})
              (rl/rect! {:x LX
                         :y LY
                         :width BORDER
                         :height left-h
                         :color rl/BLACK})
              (when (= state 2)
                (rl/rect! {:x (+ LX LOGO (- BORDER))
                           :y LY
                           :width BORDER
                           :height right-h
                           :color rl/BLACK})
                (rl/rect! {:x (- (+ LX LOGO) bottom-w)
                           :y (+ LY LOGO (- BORDER))
                           :width bottom-w
                           :height BORDER
                           :color rl/BLACK})))

            (#{3 4} state)
            (let [c (fade alpha)]
              ;; The finished frame, then a growing prefix of the word inside it.
              (rl/rect! {:x LX
                         :y LY
                         :width LOGO
                         :height BORDER
                         :color c})
              (rl/rect! {:x LX
                         :y LY
                         :width BORDER
                         :height LOGO
                         :color c})
              (rl/rect! {:x (+ LX LOGO (- BORDER))
                         :y LY
                         :width BORDER
                         :height LOGO
                         :color c})
              (rl/rect! {:x LX
                         :y (+ LY LOGO (- BORDER))
                         :width LOGO
                         :height BORDER
                         :color c})
              (rl/text! (subs "raylib" 0 (min 6 letters))
                        {:x (- (+ LX LOGO) 138)
                         :y (- (+ LY LOGO) 100)
                         :size 50
                         :color c})))
          (when (= state 4)
            (rl/text! "[R] REPLAY" {:x 340
                                    :y 200
                                    :size 20
                                    :color rl/GRAY}))
          (rl/maybe-screenshot! frame 150)
          (rl/end-drawing)
          (recur (inc frame) state counter top-w left-h bottom-w right-h letters alpha)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
