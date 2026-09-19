(ns net.b12n.raylib-jlt.image-channel
  "raylib [textures] example - image channel (`jolt -M:image-channel`).

  Port of raylib's examples/textures/textures_image_channel.c, adapted
  rather than faithfully ported: the C loads a picture from disk, and this
  suite ships no image files, so the source here is three overlapping
  shapes (ImageDrawCircle x2, ImageDrawRectangle, the family
  net.b12n.raylib-jlt.image-drawing introduced) painted in pure red,
  green and blue, so each of the R/G/B channels below owns a different
  region rather than three copies of the same gradient.

  A flat, opaque source has nothing in its alpha channel: alpha is 255
  everywhere, so ImageFromChannel on channel 3 would come back uniformly
  white and teach nothing. This applies ImageAlphaMask first, with a
  GenImageGradientRadial as the mask, so the alpha panel carries the same
  radial falloff the mask does and genuinely differs from the other
  three.

  ImageFromChannel returns a new Image by value for every call, four of
  them here on top of the source and the mask: all six get an explicit
  UnloadImage, since a leak here fails no gate."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SRC 150)
(def ^:const SLOT 170)
(def ^:const GAP 20)
(def ^:const Y0 130)
(def ^:const N-PANELS 4)
(def ^:const ROW-WIDTH (+ (* N-PANELS SLOT) (* (dec N-PANELS) GAP)))
(def ^:const X0 (quot (- W ROW-WIDTH) 2))

(def ^:private channel-specs
  ;; label, ImageFromChannel's selected-channel index (0=red 1=green
  ;; 2=blue 3=alpha).
  [["R" 0]
   ["G" 1]
   ["B" 2]
   ["A (masked)" 3]])

(defn- paint-source!
  "Three overlapping pure-colour shapes on `img`, in place: a red circle
  top-left, a green rectangle across the middle, a blue circle
  bottom-right. Each ends up owning a different region of the R/G/B
  channels below, with the overlaps mixing two of the three."
  [img]
  (rl/image-draw-circle! img (quot SRC 3) (quot SRC 3) (quot SRC 3)
                         (rl/rgba 255 0 0 255))
  (rl/image-draw-rectangle! img (quot SRC 4) (quot SRC 3) (quot SRC 2) (quot SRC 3)
                            (rl/rgba 0 255 0 255))
  (rl/image-draw-circle! img (- SRC (quot SRC 3)) (- SRC (quot SRC 3)) (quot SRC 3)
                         (rl/rgba 0 0 255 255)))

(defn- channel-texture!
  "ImageFromChannel for `channel` off `img`, uploaded once and freed -- the
  Image ImageFromChannel hands back is as real as the source and the mask
  and needs the same UnloadImage."
  [img channel]
  (let [chan-img (rl/image-from-channel img channel)
        tex (rl/image->texture chan-img)]
    (rl/unload-image! chan-img)
    tex))

(defn- draw-panel!
  [i label tex]
  (let [sx (+ X0 (* i (+ SLOT GAP)))]
    (rl/texture! tex {:x sx
                      :y Y0
                      :width SRC
                      :height SRC})
    (rl/rect-lines! {:x sx
                     :y Y0
                     :width SRC
                     :height SRC
                     :color rl/DARKGRAY})
    (rl/text! label {:x sx
                     :y (+ Y0 SRC 10)
                     :size 15
                     :color rl/DARKGRAY})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - image channel"})
  (rl/set-target-fps 60)
  ;; One source image with three painted-in shapes, masked with a radial
  ;; gradient so the alpha split has structure too, then split four ways.
  (let [deadline (app/auto-quit-deadline)
        seed-tex (rl/image-color SRC SRC rl/BLACK)
        base-img (rl/image-from-texture! seed-tex SRC SRC)
        _ (rl/unload-texture! seed-tex)
        _ (paint-source! base-img)
        mask-tex (rl/image-gradient-radial SRC SRC 0.0 rl/WHITE rl/BLACK)
        mask-img (rl/image-from-texture! mask-tex SRC SRC)
        _ (rl/unload-texture! mask-tex)
        _ (rl/image-alpha-mask! base-img mask-img)
        _ (rl/unload-image! mask-img)
        panels (mapv (fn [[label channel]]
                       {:label label
                        :tex (channel-texture! base-img channel)})
                     channel-specs)
        _ (rl/unload-image! base-img)]
    (loop [frame 0]
      (if-not (app/keep-running? deadline)
        (doseq [{:keys [tex]} panels] (rl/unload-texture! tex))
        (do
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "raylib [textures] example - image channel"
                    {:x 40
                     :y 16
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! "no image files: the source is three overlapping ImageDraw shapes"
                    {:x 40
                     :y 44
                     :size 13
                     :color rl/GRAY})
          (rl/text! "alpha panel is masked with a radial gradient, or it would be flat white"
                    {:x 40
                     :y 64
                     :size 13
                     :color rl/GRAY})
          (doseq [[i {:keys [label tex]}] (map-indexed vector panels)]
            (draw-panel! i label tex))
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
