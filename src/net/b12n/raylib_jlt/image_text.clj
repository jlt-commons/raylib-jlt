(ns net.b12n.raylib-jlt.image-text
  "raylib [textures] example - image text (`jolt -M:image-text`).

  Port of raylib's examples/textures/textures_image_text.c, adapted rather
  than faithfully ported. Two divergences, both forced by what this library
  binds today rather than by choice, worth stating rather than hiding.

  The C loads a picture from disk to draw the label onto; this suite ships
  no image files, so the canvas here is a flat GenImageColor field, pulled
  back off the GPU with image-from-texture! the same way
  net.b12n.raylib-jlt.image-drawing and image-processing do.

  Upstream calls ImageDrawTextEx, which takes a Font. No Font binding exists
  yet, so this uses the default-font ImageDrawText instead. The point still
  lands: once text is baked into an Image's pixels, it IS pixels, and
  behaves like any other pixel from then on.

  The label is drawn into a small image at font size 26, uploaded, set to
  nearest-neighbour filtering so the scale-up doesn't blur it, then shown at
  4x its native size. The same word, drawn fresh with rl/text! at the
  matching on-screen size, stays crisp: DrawText rasterises glyphs at
  whatever size it is asked for each frame; ImageDrawText rasterised once at
  26, and everything past that is the GPU stretching four texels into one."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SRC-W 170)
(def ^:const SRC-H 50)
(def ^:const FONT-SIZE 26)
(def ^:const SCALE 4)
(def ^:const LABEL "PIXELS")
(def ^:const DISP-W (* SRC-W SCALE))
(def ^:const DISP-H (* SRC-H SCALE))
(def ^:const PANEL-X (quot (- W DISP-W) 2))
(def ^:const PANEL-Y 70)
(def ^:const CMP-SIZE (* FONT-SIZE SCALE))
(def ^:const CMP-Y 306)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - image text"})
  (rl/set-target-fps 60)
  ;; A small canvas, one label baked into it with ImageDrawText, uploaded
  ;; once and shown 4x native size. Nearest-neighbour filtering keeps the
  ;; scale-up blocky rather than blurred, so the pixelation is legible.
  (let [deadline (app/auto-quit-deadline)
        label-color (rl/rgba 30 30 30 255)
        seed-tex (rl/image-color SRC-W SRC-H rl/RAYWHITE)
        img (rl/image-from-texture! seed-tex SRC-W SRC-H)
        _ (rl/image-draw-text! img LABEL 12 8 FONT-SIZE label-color)
        baked-tex (rl/image->texture img)
        _ (rl/unload-image! img)
        _ (rl/unload-texture! seed-tex)
        _ (rl/texture-filter! baked-tex rl/RL-TEXTURE-FILTER-NEAREST)
        cmp-w (rl/text-width LABEL {:size CMP-SIZE})
        cmp-x (quot (- W cmp-w) 2)]
    (loop [frame 0]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! baked-tex)
        (do
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "raylib [textures] example - image text"
                    {:x 40
                     :y 16
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! "no image files: the canvas is GenImageColor"
                    {:x 40
                     :y 44
                     :size 13
                     :color rl/GRAY})
          (rl/texture! baked-tex {:x PANEL-X
                                  :y PANEL-Y
                                  :width DISP-W
                                  :height DISP-H})
          (rl/rect-lines! {:x PANEL-X
                           :y PANEL-Y
                           :width DISP-W
                           :height DISP-H
                           :color rl/DARKGRAY})
          (rl/text! "baked at size 26 (ImageDrawText), scaled up 4x"
                    {:x PANEL-X
                     :y (+ PANEL-Y DISP-H 8)
                     :size 13
                     :color rl/DARKGRAY})
          (rl/text! LABEL {:x cmp-x
                           :y CMP-Y
                           :size CMP-SIZE
                           :color label-color})
          (rl/text! "same word, rl/text! at the matching size, stays crisp"
                    {:x 40
                     :y (+ CMP-Y CMP-SIZE 12)
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
