(ns net.b12n.raylib-jlt.sprite-stacking
  "raylib [textures] example - sprite stacking (`jolt -M:sprite-stacking`).

  Port of raylib's examples/textures/textures_sprite_stacking.c. The C loads
  booth.png, a fairground booth sliced into 122 horizontal layers, and this
  suite ships no image files, so the sheet here is generated at startup: 40
  top-down slices of a small car, painted into one tall image with the
  ImageDraw* family and uploaded once.

  Sprite stacking fakes a 3D object out of 2D art. Each slice is drawn at the
  same rotation, offset a little further up the screen than the one below it,
  and the eye reads the pile as a solid body turning in place. Row 0 of the
  sheet is the roof and row 39 is the wheels, so the loop walks from the last
  row to the first and the roof lands on top.

  This is the example that needed rl/texture! to learn about rotation. Until
  now it only emitted axis-aligned quads, which is all DrawTexturePro's
  by-value Rectangle/Vector2 arguments let the rlgl stand-in do. It now takes
  :rotation with :origin-x/:origin-y, rotates the four corners about that
  pivot before emitting them, and leaves the default-origin unrotated case
  producing byte-identical frames.

  A and D, or the arrow keys, change the spin speed. The mouse wheel changes
  the spacing between layers, which is what sells the illusion or breaks it."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

;; One slice is a top-down footprint: 48 along the car, 32 across it. Forty of
;; them stacked is enough to read as solid without making the sheet enormous.
(def ^:const FRAME-W 56)
(def ^:const FRAME-H 28)
(def ^:const LAYERS 40)
(def ^:const SHEET-H (* FRAME-H LAYERS))

(def ^:const STACK-SCALE 3.0)
(def ^:const SPEED-STEP 0.35)
(def ^:const MIN-SPACING 0.0)
(def ^:const MAX-SPACING 5.0)

(defn- slab!
  "A rounded rectangle in layer-local coordinates, painted into row `y0` of the
  sheet. Corner radius `r` is drawn as four circles plus a cross of two
  rectangles, which is cheaper to read than a real rounded-rect routine and
  good enough at this size."
  [img y0 x y w h r color]
  (rl/image-draw-rectangle! img (+ x r) (+ y0 y) (- w (* 2 r)) h color)
  (rl/image-draw-rectangle! img x (+ y0 y r) w (- h (* 2 r)) color)
  (rl/image-draw-circle! img (+ x r) (+ y0 y r) r color)
  (rl/image-draw-circle! img (+ x w (- r)) (+ y0 y r) r color)
  (rl/image-draw-circle! img (+ x r) (+ y0 y h (- r)) r color)
  (rl/image-draw-circle! img (+ x w (- r)) (+ y0 y h (- r)) r color))

(defn- draw-layer!
  "Paint slice `i` into the sheet. `t` runs 0.0 at the roof to 1.0 at the road,
  so the bands below read top-down: cabin, then body, then the wheels the body
  overhangs."
  [img i]
  (let [y0 (* i FRAME-H)
        t (/ (double i) (dec LAYERS))]
    (cond
      ;; wheels, inset from the sides so the body above visibly overhangs them
      (>= t 0.88)
      (do (rl/image-draw-rectangle! img 8 (+ y0 0) 11 28 rl/BLACK)
          (rl/image-draw-rectangle! img 38 (+ y0 0) 11 28 rl/BLACK))

      ;; the full-width body, widest just above the wheels
      (>= t 0.55)
      (slab! img y0 2 1 52 26 3 rl/RED)

      ;; shoulders, tucked in a little
      (>= t 0.30)
      (slab! img y0 5 2 46 24 3 rl/RED)

      ;; cabin: shorter than the body and pushed back, which is what makes the
      ;; rotation legible. A symmetric stack would spin without looking like it
      :else
      (do (slab! img y0 10 3 26 22 3 rl/MAROON)
          (when (< t 0.15)
            (slab! img y0 13 6 20 16 2 rl/SKYBLUE))))))

(defn- build-sheet!
  "Generate the stacking sheet and upload it, answering its texture id. The
  only way to a blank Image here is to make a one-colour texture and pull it
  back off the GPU, the same round trip net.b12n.raylib-jlt.image-drawing uses."
  []
  (let [seed (rl/image-color FRAME-W SHEET-H (rl/rgba 0 0 0 0))
        img (rl/image-from-texture! seed FRAME-W SHEET-H)]
    (rl/unload-texture! seed)
    (dotimes [i LAYERS]
      (draw-layer! img i))
    (let [tex (rl/image->texture img)]
      (rl/unload-image! img)
      tex)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - sprite stacking"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        tex (build-sheet!)
        slab-w (* FRAME-W STACK-SCALE)
        slab-h (* FRAME-H STACK-SCALE)]
    (loop [frame 0
           rotation 0.0
           speed 30.0
           spacing 3.2]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! tex)
        (let [spacing' (-> (+ spacing (* (rl/get-mouse-wheel) 0.1))
                           (max MIN-SPACING)
                           (min MAX-SPACING))
              speed' (cond-> speed
                       (or (rl/key-down? rl/KEY-LEFT) (rl/key-down? rl/KEY-A))
                       (- SPEED-STEP)
                       (or (rl/key-down? rl/KEY-RIGHT) (rl/key-down? rl/KEY-D))
                       (+ SPEED-STEP))
              rotation' (+ rotation (* speed' (rl/get-frame-time)))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; last row first, so row 0 (the roof) is drawn last and sits on top
          (doseq [i (range (dec LAYERS) -1 -1)]
            (rl/texture! tex {:x (/ W 2.0)
                              :y (+ (/ H 2.0)
                                    (* i spacing')
                                    (- (/ (* spacing' LAYERS) 2.0)))
                              :width slab-w
                              :height slab-h
                              :origin-x (/ slab-w 2.0)
                              :origin-y (/ slab-h 2.0)
                              :rotation rotation'
                              :v0 (/ (double i) LAYERS)
                              :v1 (/ (double (inc i)) LAYERS)}))
          (rl/text! "A/D or arrows to spin, mouse wheel to change separation"
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! (str "spacing " (format "%.1f" spacing'))
                    {:x 10
                     :y 40
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! (str "speed " (format "%.2f" speed'))
                    {:x 10
                     :y 62
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! "40 slices, generated at startup: no image files ship here"
                    {:x 10
                     :y (- H 30)
                     :size 18
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame) rotation' speed' spacing'))))))
