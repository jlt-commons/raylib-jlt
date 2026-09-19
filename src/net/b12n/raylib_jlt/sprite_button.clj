(ns net.b12n.raylib-jlt.sprite-button
  "raylib [textures] example - sprite button (`jolt -M:sprite-button`).

  Port of raylib's examples/textures/textures_sprite_button.c. One texture
  holding three stacked frames, normal, hover and pressed, and the button picks
  its frame by moving a window down the sheet rather than by swapping textures.
  Click it and the counter goes up.

  Zero new FFI. A sprite sheet is a source rectangle and nothing more, so the
  whole mechanism is `texture!` with `:v0` and `:v1` set to the third of the
  image the current state lives in, which is the same UV slicing `textured-cube`
  and `directional-billboard` use for their atlases. Dividing one texture rather
  than binding three is the point: the GPU keeps one upload and the batch never
  breaks to change texture between frames.

  Two deviations. The C loads `resources/button.png`, and this suite ships no
  image files, so the sheet is drawn with `texture-from-fn`, all three frames in
  one pass, with the lit edge moving and the label shifting down a pixel when
  pressed. And the C plays `buttonfx.wav` on release through `LoadSound`, which
  needs raylib's `Sound` bindings this suite does not have yet: the audio here
  is `audio-raw-stream` and `audio-stream-callback`, both of which generate their
  samples rather than loading them. The click is silent and counted instead."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const BTN-W 160)
(def ^:const BTN-H 48)
(def ^:const FRAMES 3)

(defn- sheet-pixel
  "All three frames in one image, stacked. `y` picks the frame, so the shading
  changes down the sheet: lit from above when idle, brighter on hover, and
  inverted with the face pushed down when pressed."
  [x y]
  (let [frame (quot y BTN-H)
        fy (mod y BTN-H)
        edge (min x fy (- BTN-W 1 x) (- BTN-H 1 fy))
        ;; The pressed frame lights from below instead of above.
        top? (< fy (/ BTN-H 2))
        base (case frame
               0 [70 110 190]
               1 [95 150 235]
               [55 85 150])
        [r g b] base
        lift (cond
               (< edge 2) -35
               (and (< edge 6) (if (= frame 2) (not top?) top?)) 55
               (< edge 6) -25
               :else 0)
        ;; A pale bar standing in for a label, nudged down in the pressed frame.
        label-y (if (= frame 2) (+ (/ BTN-H 2) 1) (/ BTN-H 2))
        label? (and (> x 40) (< x (- BTN-W 40))
                    (>= fy (- label-y 3)) (< fy (+ label-y 3)))]
    (if label?
      (rl/rgba 240 245 255 255)
      (rl/rgba (min 255 (max 0 (+ r lift)))
               (min 255 (max 0 (+ g lift)))
               (min 255 (max 0 (+ b lift)))
               255))))

(defn- state-of
  "0 normal, 1 hover, 2 pressed, from the pointer against the button's bounds."
  [inside?]
  (cond
    (and inside? (rl/mouse-down? rl/MOUSE-LEFT)) 2
    inside? 1
    :else 0))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - sprite button"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sheet (rl/texture-from-fn BTN-W (* BTN-H FRAMES) sheet-pixel)
        bx (- (/ W 2) (/ BTN-W 2))
        by (- (/ H 2) (/ BTN-H 2))]
    (rl/texture-wrap! sheet rl/RL-TEXTURE-WRAP-CLAMP)
    (loop [frame 0
           clicks 0]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! sheet)
        (let [mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              inside? (and (<= bx mx (+ bx BTN-W)) (<= by my (+ by BTN-H)))
              live? (or inside? (rl/mouse-down? rl/MOUSE-LEFT))
              ;; With nobody at the pointer the three states are cycled so the
              ;; sheet shows what it holds; a real hover takes over at once.
              ;; Fully derived from the pointer and the frame, so there is no
              ;; state to carry between frames.
              demo-state (if live? (state-of inside?) (quot (mod frame 180) 60))
              clicks (if (and inside? (rl/mouse-released? rl/MOUSE-LEFT))
                       (inc clicks)
                       clicks)
              v0 (/ (double demo-state) FRAMES)
              v1 (/ (double (inc demo-state)) FRAMES)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/texture! sheet {:x bx
                              :y by
                              :width BTN-W
                              :height BTN-H
                              :v0 v0
                              :v1 v1})
          (rl/text! (str "clicks: " clicks) {:x 20
                                             :y 20
                                             :size 20
                                             :color rl/DARKGRAY})
          (rl/text! (nth ["normal" "hover" "pressed"] demo-state)
                    {:x 20
                     :y 46
                     :size 10
                     :color rl/GRAY})
          (rl/text! (if live?
                      "one texture, three frames, sliced by v"
                      "cycling the three frames until you hover it")
                    {:x 20
                     :y (- H 30)
                     :size 10
                     :color rl/GRAY})
          ;; The whole sheet, so the three frames it holds are visible at once.
          (rl/texture! sheet {:x (- W BTN-W 20)
                              :y 20
                              :width BTN-W
                              :height (* BTN-H FRAMES)})
          (rl/rect-lines! {:x (- W BTN-W 20)
                           :y (+ 20 (* demo-state BTN-H))
                           :width BTN-W
                           :height BTN-H
                           :color rl/RED})
          (app/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame) clicks)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
