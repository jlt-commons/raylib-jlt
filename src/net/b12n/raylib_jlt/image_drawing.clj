(ns net.b12n.raylib-jlt.image-drawing
  "raylib [textures] example - image drawing (`jolt -M:image-drawing`).

  Port of raylib's examples/textures/textures_image_drawing.c, adapted rather
  than faithfully ported: the C loads a picture from disk, and this suite
  ships no image files, so the source here is a radial gradient from
  GenImageGradientRadial, pulled back off the GPU with image-from-texture!
  the same way net.b12n.raylib-jlt.image-processing does.

  New FFI, all in place on an Image* like the processing family next to them
  in net.b12n.raylib.images: ImageDrawRectangle, ImageDrawLine,
  ImageDrawCircle and ImageDrawPixel. The left panel bakes a rectangle, a
  line, a circle and a handful of pixels into a copy of the source with
  those four calls, then uploads once. The right panel draws the identical
  rl/rect!/line!/circle!/pixel! calls over an untouched copy of the same
  source, every frame.

  At rest the two panels would look identical, which would prove nothing
  worth looking at, so the right panel's circle pulses on a sine wave while
  the left one stays exactly as it was baked. What moves is what gets
  redrawn every frame; what stays still already had its pixels committed."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PANEL 300)
(def ^:const XA 50)
(def ^:const XB 450)
(def ^:const Y0 100)

;; Shapes, in panel-local coordinates. Both the baked copy and the live
;; overlay draw these same numbers, offset by whichever panel's origin
;; applies, so a mismatch here would show up as the two panels genuinely
;; disagreeing rather than as a caption that happens to be wrong.
(def ^:const RECT-X 30) (def ^:const RECT-Y 30)
(def ^:const RECT-W 130) (def ^:const RECT-H 80)
(def ^:const LINE-X1 20) (def ^:const LINE-Y1 220)
(def ^:const LINE-X2 280) (def ^:const LINE-Y2 260)
(def ^:const CIRC-X 200) (def ^:const CIRC-Y 90)
(def ^:const CIRC-R 50.0)
(def ^:const PIXEL-N 5) (def ^:const PIXEL-STEP 4)
(def ^:const PIXEL-X0 15) (def ^:const PIXEL-Y0 15)

(defn- bake-shapes!
  "Draw the panel's four shapes into `img`, in place, with the ImageDraw*
  family. Runs once, before the loop starts: whatever lands here is what
  panel A shows for the rest of the run."
  [img]
  (rl/image-draw-rectangle! img RECT-X RECT-Y RECT-W RECT-H rl/RED)
  (rl/image-draw-line! img LINE-X1 LINE-Y1 LINE-X2 LINE-Y2 rl/BLACK)
  (rl/image-draw-circle! img CIRC-X CIRC-Y CIRC-R rl/BLUE)
  (dotimes [i PIXEL-N]
    (rl/image-draw-pixel! img (+ PIXEL-X0 (* i PIXEL-STEP))
                          (+ PIXEL-Y0 (* i PIXEL-STEP)) rl/ORANGE)))

(defn- draw-live-shapes!
  "The same four shapes as bake-shapes!, drawn over panel B with the ordinary
  rlgl drawing calls instead of ImageDraw*. Runs every frame at panel origin
  (`ox`,`oy`), and the circle's radius is the one number that moves, so a
  viewer can see it is being redrawn rather than trusting a caption."
  [ox oy frame]
  (rl/rect! {:x (+ ox RECT-X)
             :y (+ oy RECT-Y)
             :width RECT-W
             :height RECT-H
             :color rl/RED})
  (rl/line! {:x1 (+ ox LINE-X1)
             :y1 (+ oy LINE-Y1)
             :x2 (+ ox LINE-X2)
             :y2 (+ oy LINE-Y2)
             :color rl/BLACK})
  (let [r (+ CIRC-R (* 15.0 (Math/sin (* frame 0.05))))]
    (rl/circle! {:x (+ ox CIRC-X)
                 :y (+ oy CIRC-Y)
                 :radius r
                 :color rl/BLUE}))
  (dotimes [i PIXEL-N]
    (rl/pixel! {:x (+ ox PIXEL-X0 (* i PIXEL-STEP))
                :y (+ oy PIXEL-Y0 (* i PIXEL-STEP))
                :color rl/ORANGE})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - image drawing"})
  (rl/set-target-fps 60)
  ;; One source image, generated (no assets ship in this suite) and pulled
  ;; back to CPU once. A copy gets the ImageDraw* treatment and is uploaded
  ;; as panel A; the untouched original is uploaded as panel B's backdrop,
  ;; which the live shapes then paint over every frame.
  (let [deadline (app/auto-quit-deadline)
        bg-color (rl/rgba 70 95 145 255)
        seed-tex (rl/image-gradient-radial PANEL PANEL 0.0 rl/RAYWHITE bg-color)
        base-img (rl/image-from-texture! seed-tex PANEL PANEL)
        baked-img (rl/image-copy! base-img)
        _ (bake-shapes! baked-img)
        baked-tex (rl/image->texture baked-img)
        _ (rl/unload-image! baked-img)
        plain-tex (rl/image->texture base-img)
        _ (rl/unload-image! base-img)
        _ (rl/unload-texture! seed-tex)]
    (loop [frame 0]
      (if-not (app/keep-running? deadline)
        (do (rl/unload-texture! baked-tex)
            (rl/unload-texture! plain-tex))
        (do
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "raylib [textures] example - image drawing"
                    {:x 40
                     :y 16
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! "no image files: the source is GenImageGradientRadial"
                    {:x 40
                     :y 44
                     :size 13
                     :color rl/GRAY})
          (rl/texture! baked-tex {:x XA
                                  :y Y0
                                  :width PANEL
                                  :height PANEL})
          (rl/rect-lines! {:x XA
                           :y Y0
                           :width PANEL
                           :height PANEL
                           :color rl/DARKGRAY})
          (rl/text! "baked into the pixels, once"
                    {:x XA
                     :y 76
                     :size 13
                     :color rl/DARKGRAY})
          (rl/texture! plain-tex {:x XB
                                  :y Y0
                                  :width PANEL
                                  :height PANEL})
          (draw-live-shapes! XB Y0 frame)
          (rl/rect-lines! {:x XB
                           :y Y0
                           :width PANEL
                           :height PANEL
                           :color rl/DARKGRAY})
          (rl/text! "drawn live over it, circle pulses"
                    {:x XB
                     :y 76
                     :size 13
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
