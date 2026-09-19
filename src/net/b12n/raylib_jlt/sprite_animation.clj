(ns net.b12n.raylib-jlt.sprite-animation
  "raylib [textures] example - sprite animation (`jolt -M:sprite-animation`).

  Port of raylib's examples/textures/textures_sprite_animation.c. The C loads
  scarfy.png, a strip of six frames, and walks a source rectangle along it.
  No image files ship here, so the strip is generated at startup: six poses of
  a stick walker, drawn with the ImageDraw* family into one wide image.

  The animation itself is the part worth understanding, and it is entirely
  about the source rectangle. One texture goes to the GPU once and never
  changes. What changes is which sixth of it the quad samples, which
  rl/texture! expresses as :u0 and :u1. Nothing is uploaded per frame and
  nothing is redrawn into the image.

  The strip is shown at the top with the current frame boxed, so the
  relationship between the sheet and the figure below it stays visible.
  LEFT and RIGHT change the playback rate, which is counted in whole frames
  at 60 fps, so the speeds it steps through are coarse on purpose."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const FRAMES 6)
(def ^:const FRAME-W 96)
(def ^:const FRAME-H 120)
(def ^:const SHEET-W (* FRAME-W FRAMES))

(def ^:const MIN-SPEED 1)
(def ^:const MAX-SPEED 15)

(def ^:const SHEET-X 112)
(def ^:const SHEET-Y 60)

(defn- thick-line!
  "A limb: a run of circles from (x1,y1) to (x2,y2). image-draw-line! is one
  pixel wide, which disappears at this size, and there is no thick-line call in
  the ImageDraw* family to reach for instead."
  [img x1 y1 x2 y2 r color]
  (let [steps 14]
    (dotimes [i (inc steps)]
      (let [f (/ (double i) steps)]
        (rl/image-draw-circle! img
                               (int (+ x1 (* f (- x2 x1))))
                               (int (+ y1 (* f (- y2 y1))))
                               r color)))))

(defn- draw-pose!
  "Pose `i` of the walk, painted into its own frame of the strip. The legs
  scissor on a sine and the arms swing against them, which is the whole of
  what makes a walk read as a walk."
  [img i]
  (let [x0 (* i FRAME-W)
        phase (* 2.0 Math/PI (/ (double i) FRAMES))
        swing (Math/sin phase)
        ;; the body rises on the passing pose and drops on the contact pose
        bob (int (* 3.0 (Math/abs (Math/cos phase))))
        cx (+ x0 48)
        hip (+ 72 bob)
        shoulder (+ 46 bob)]
    ;; legs first, so the torso overlaps them at the hip
    (thick-line! img cx hip (+ cx (int (* 18 swing))) 106 6 (rl/rgba 45 60 95 255))
    (thick-line! img cx hip (- cx (int (* 18 swing))) 106 6 (rl/rgba 60 80 125 255))
    ;; torso
    (thick-line! img cx (+ 38 bob) cx hip 9 rl/RED)
    ;; arms counter-swing against the legs. They hang from shoulder points
    ;; either side of the spine rather than from it, because an arm that
    ;; starts at the centre line spends the whole cycle hidden behind a
    ;; torso of nearly the same colour
    (thick-line! img (- cx 8) shoulder (- cx 8 (int (* 20 swing))) (+ 76 bob) 5
                 (rl/rgba 120 25 35 255))
    (thick-line! img (+ cx 8) shoulder (+ cx 8 (int (* 20 swing))) (+ 76 bob) 5
                 (rl/rgba 245 130 120 255))
    ;; head last, on top of the shoulders
    (rl/image-draw-circle! img cx (+ 22 bob) 15 (rl/rgba 235 195 150 255))
    (rl/image-draw-circle! img (+ cx 6) (+ 19 bob) 3 rl/BLACK)))

(defn- build-strip!
  []
  (let [seed (rl/image-color SHEET-W FRAME-H (rl/rgba 0 0 0 0))
        img (rl/image-from-texture! seed SHEET-W FRAME-H)]
    (rl/unload-texture! seed)
    (dotimes [i FRAMES]
      (draw-pose! img i))
    (let [tex (rl/image->texture img)]
      (rl/unload-image! img)
      tex)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - sprite animation"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        tex (build-strip!)]
    (loop [frame 0
           counter 0
           current 0
           speed 8]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! tex)
        (let [speed' (cond-> speed
                       (rl/key-pressed? rl/KEY-RIGHT) inc
                       (rl/key-pressed? rl/KEY-LEFT) dec)
              speed' (-> speed' (max MIN-SPEED) (min MAX-SPEED))
              counter' (inc counter)
              tick? (>= counter' (quot 60 speed'))
              current' (if tick? (mod (inc current) FRAMES) current)
              counter' (if tick? 0 counter')
              u0 (/ (double current') FRAMES)
              u1 (/ (double (inc current')) FRAMES)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "raylib [textures] example - sprite animation"
                    {:x 40
                     :y 16
                     :size 20
                     :color rl/DARKGRAY})

          ;; the whole strip, with the frame currently on screen boxed
          (rl/texture! tex {:x SHEET-X
                            :y SHEET-Y
                            :width SHEET-W
                            :height FRAME-H})
          (rl/rect-lines! {:x SHEET-X
                           :y SHEET-Y
                           :width SHEET-W
                           :height FRAME-H
                           :color rl/LIME})
          (rl/rect-lines! {:x (+ SHEET-X (* current' FRAME-W))
                           :y SHEET-Y
                           :width FRAME-W
                           :height FRAME-H
                           :color rl/RED})

          (rl/text! "one texture, six frames: only :u0/:u1 change"
                    {:x SHEET-X
                     :y (- SHEET-Y 20)
                     :size 13
                     :color rl/GRAY})

          ;; the frame the source rectangle currently selects, drawn larger
          (rl/texture! tex {:x 336
                            :y 260
                            :width 128
                            :height 160
                            :u0 u0
                            :u1 u1})

          (rl/text! "FRAME SPEED:" {:x 150
                                    :y 228
                                    :size 13
                                    :color rl/DARKGRAY})
          (rl/text! (str speed' " fps") {:x 590
                                         :y 228
                                         :size 13
                                         :color rl/DARKGRAY})
          (dotimes [i MAX-SPEED]
            (when (< i speed')
              (rl/rect! {:x (+ 260 (* 21 i))
                         :y 224
                         :width 20
                         :height 20
                         :color rl/RED}))
            (rl/rect-lines! {:x (+ 260 (* 21 i))
                             :y 224
                             :width 20
                             :height 20
                             :color rl/MAROON}))
          (rl/text! "LEFT / RIGHT change the rate"
                    {:x 150
                     :y 252
                     :size 13
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) counter' current' speed'))))))
