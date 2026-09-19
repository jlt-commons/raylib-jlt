(ns net.b12n.raylib-jlt.to-image
  "raylib [textures] example - to image (`jolt -M:to-image`).

  Port of raylib's examples/textures/textures_to_image.c. The C walks one
  picture around the loop RAM to VRAM to RAM to VRAM and draws the result,
  which looks exactly like the picture it started with. That is the point,
  and it is also why the C on its own shows you nothing: a correct round
  trip and a no-op are indistinguishable on screen.

  So this port makes the middle of the trip visible. Both panels start from
  the same generated image. The left one is uploaded once and never comes
  back down. The right one is uploaded, pulled off the GPU with
  LoadImageFromTexture, stamped on the CPU with ImageDrawText while it sits
  in RAM, and uploaded again. The stamp is the evidence: those pixels can
  only have been written by a CPU-side draw into a real buffer, so if it
  shows up, the read-back genuinely returned the image rather than a handle.

  No image files ship in this suite, so the source is GenImageGradientRadial
  with a few shapes baked in, the same way net.b12n.raylib-jlt.image-drawing
  builds its source."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PANEL 260)
(def ^:const XA 90)
(def ^:const XB 450)
(def ^:const Y0 120)

(defn- bake-source!
  "A few shapes baked into `img` so the round trip has something with edges and
  corners to preserve. A flat gradient would survive a broken read-back just as
  convincingly as a working one."
  [img]
  (rl/image-draw-rectangle! img 30 30 90 60 rl/RED)
  (rl/image-draw-circle! img 190 80 38 rl/GOLD)
  (rl/image-draw-line! img 20 200 240 230 rl/BLACK)
  (rl/image-draw-rectangle! img 40 150 50 50 rl/LIME))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - to image"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        seed (rl/image-gradient-radial PANEL PANEL 0.0 rl/RAYWHITE (rl/rgba 60 85 130 255))
        src (rl/image-from-texture! seed PANEL PANEL)
        _ (rl/unload-texture! seed)
        _ (bake-source! src)

        ;; left: one hop, RAM to VRAM, and it stays there
        direct-tex (rl/image->texture src)

        ;; right: the full loop. up, back down, stamped while it is down,
        ;; then up again. round-img is a genuinely new CPU buffer, not the
        ;; one we uploaded from
        up-tex (rl/image->texture src)
        round-img (rl/image-from-texture! up-tex PANEL PANEL)
        _ (rl/unload-texture! up-tex)
        ;; a bar first, so the stamp reads against the bright middle of the
        ;; gradient as well as the dark edges. Two CPU-side draws, not one
        _ (rl/image-draw-rectangle! round-img 18 110 224 32 (rl/rgba 20 20 30 235))
        _ (rl/image-draw-text! round-img "stamped in RAM" 28 118 20 rl/RAYWHITE)
        round-tex (rl/image->texture round-img)
        _ (rl/unload-image! round-img)
        _ (rl/unload-image! src)]
    (loop [frame 0]
      (if-not (app/keep-running? deadline)
        (do (rl/unload-texture! direct-tex)
            (rl/unload-texture! round-tex))
        (do
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "raylib [textures] example - to image"
                    {:x 40
                     :y 20
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! "the same pixels, one of them by way of a trip through RAM"
                    {:x 40
                     :y 48
                     :size 13
                     :color rl/GRAY})

          (rl/texture! direct-tex {:x XA
                                   :y Y0
                                   :width PANEL
                                   :height PANEL})
          (rl/rect-lines! {:x XA
                           :y Y0
                           :width PANEL
                           :height PANEL
                           :color rl/DARKGRAY})
          (rl/text! "uploaded once, never read back"
                    {:x XA
                     :y (- Y0 22)
                     :size 13
                     :color rl/DARKGRAY})

          (rl/texture! round-tex {:x XB
                                  :y Y0
                                  :width PANEL
                                  :height PANEL})
          (rl/rect-lines! {:x XB
                           :y Y0
                           :width PANEL
                           :height PANEL
                           :color rl/DARKGRAY})
          (rl/text! "VRAM to RAM to VRAM, stamped on the way"
                    {:x XB
                     :y (- Y0 22)
                     :size 13
                     :color rl/DARKGRAY})

          (rl/text! "if the read-back were broken the right panel would be blank or garbage"
                    {:x 40
                     :y (- H 40)
                     :size 13
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame)))))))
