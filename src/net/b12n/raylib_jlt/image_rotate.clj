(ns net.b12n.raylib-jlt.image-rotate
  "raylib [textures] example - image rotate (`jolt -M:image-rotate`).

  Port of raylib's examples/textures/textures_image_rotate.c, adapted
  rather than faithfully ported: the C loads a picture from disk, and this
  suite ships no image files, so the source here is a linear gradient from
  GenImageGradientLinear with a small marker rectangle baked into one
  corner (ImageDrawRectangle, the family net.b12n.raylib-jlt.image-drawing
  introduced), pulled back off the GPU with image-from-texture! the same
  way the rest of this batch does. The marker is what makes rotation
  visible; a plain gradient rotated 180 degrees looks almost like itself.

  Five panels: the source untouched, then rotated 90 (ImageRotateCW), 180
  (ImageRotateCW twice), 270 (ImageRotateCCW, the cheaper single call for
  that angle) and one arbitrary angle through the general ImageRotate. The
  first four are square rotations of a square source, so their width and
  height never move. The arbitrary angle does: raylib's ImageRotate grows
  the buffer to fit the rotated bounds, so that panel comes back wider and
  taller than it went in.

  Each panel's dimensions are read back with net.b12n.raylib.images'
  image-width/image-height rather than computed from the angle or assumed
  unchanged. What ends up on screen is measured, not predicted."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SRC 90)
(def ^:const SLOT 140)
(def ^:const GAP 10)
(def ^:const Y0 130)
(def ^:const N-PANELS 5)
(def ^:const ROW-WIDTH (+ (* N-PANELS SLOT) (* (dec N-PANELS) GAP)))
(def ^:const X0 (quot (- W ROW-WIDTH) 2))
(def ^:const ARB-ANGLE 40)
(def ^:const MARKER-SIZE 18)

(def ^:private panel-specs
  ;; label, rotate! (a fn of one Image pointer mutating it in place, or nil
  ;; for "leave this copy untouched"). CW/CCW are the cheap exact
  ;; 90-degree paths; the last entry goes through the general ImageRotate,
  ;; the one call here that can change the buffer's own dimensions.
  [["0" nil]
   ["90 CW" (fn [img] (rl/image-rotate-cw! img))]
   ["180 CW+CW" (fn [img] (rl/image-rotate-cw! img) (rl/image-rotate-cw! img))]
   ["270 CCW" (fn [img] (rl/image-rotate-ccw! img))]
   [(str ARB-ANGLE " arbitrary") (fn [img] (rl/image-rotate! img ARB-ANGLE))]])

(defn- build-panel
  "Copy `base`, run `rotate!` on the copy (or leave it be), read its
  dimensions back, upload once and free the CPU-side copy. `label` plus
  the measured [w h] is everything the frame loop needs to draw and
  caption this panel."
  [base [label rotate!]]
  (let [img (rl/image-copy! base)
        _ (when rotate! (rotate! img))
        w (rl/image-width img)
        h (rl/image-height img)
        tex (rl/image->texture img)]
    (rl/unload-image! img)
    {:label label
     :tex tex
     :w w
     :h h}))

(defn- draw-panel!
  "One slot: a fixed SLOTxSLOT frame outline, the texture centred inside it
  at its own measured size, and a two-line caption underneath."
  [i {:keys [label tex w h]}]
  (let [sx (+ X0 (* i (+ SLOT GAP)))
        tx (+ sx (quot (- SLOT w) 2))
        ty (+ Y0 (quot (- SLOT h) 2))]
    (rl/rect-lines! {:x sx
                     :y Y0
                     :width SLOT
                     :height SLOT
                     :color rl/LIGHTGRAY})
    (rl/texture! tex {:x tx
                      :y ty
                      :width w
                      :height h})
    (rl/text! label {:x sx
                     :y (+ Y0 SLOT 8)
                     :size 15
                     :color rl/DARKGRAY})
    (rl/text! (str w "x" h) {:x sx
                             :y (+ Y0 SLOT 26)
                             :size 13
                             :color rl/GRAY})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - image rotate"})
  (rl/set-target-fps 60)
  ;; One source image, generated and marked once, then copied five times:
  ;; the ImageRotate* family works in place, so each panel needs its own
  ;; copy to rotate independently of the others.
  (let [deadline (app/auto-quit-deadline)
        bg-color (rl/rgba 70 95 145 255)
        seed-tex (rl/image-gradient-linear SRC SRC 0 rl/RAYWHITE bg-color)
        base-img (rl/image-from-texture! seed-tex SRC SRC)
        _ (rl/image-draw-rectangle! base-img 6 6 MARKER-SIZE MARKER-SIZE rl/RED)
        _ (rl/unload-texture! seed-tex)
        panels (mapv (partial build-panel base-img) panel-specs)
        _ (rl/unload-image! base-img)]
    (loop [frame 0]
      (if-not (app/keep-running? deadline)
        (doseq [{:keys [tex]} panels] (rl/unload-texture! tex))
        (do
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "raylib [textures] example - image rotate"
                    {:x 40
                     :y 16
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! "no image files: the source is GenImageGradientLinear + a marker"
                    {:x 40
                     :y 44
                     :size 13
                     :color rl/GRAY})
          (rl/text! (str ARB-ANGLE " is the only angle here that changes the image's own size")
                    {:x 40
                     :y 64
                     :size 13
                     :color rl/GRAY})
          (doseq [[i panel] (map-indexed vector panels)]
            (draw-panel! i panel))
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
