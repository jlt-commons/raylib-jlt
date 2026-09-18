(ns net.b12n.raylib-jlt.image-generation
  "raylib [textures] example - image generation (`jolt -M:image-generation`).

  Port of raylib's examples/textures/textures_image_generation.c. Nine
  full-screen textures, none of them loaded from anywhere: click or press RIGHT
  to cycle through three linear gradients, a radial and a square one, a
  checkerboard, white noise, Perlin noise and a cellular field. Each one is
  generated once at startup and lives on the GPU from then on.

  This is the first example on the suite's new `Image` bindings, and the
  generators are exactly why they were worth having. `Image` is
  `{void *data; int width, height, mipmaps, format;}`, 24 bytes returned by
  value from every `GenImage*` call and taken by value by everything that
  consumes one. That has been expressible since jolt 0.7.23 and nothing here
  had used it for pixels yet.

  What `rl/image-*` hands back is an rlgl texture id rather than the `Image` or
  the `Texture2D`, which is what makes this a small addition rather than a new
  drawing path: `texture!`, `texture-filter!` and `unload-texture!` all speak ids
  already, so a generated image draws through the same code a `texture-from-fn`
  one does. The `Image` itself is freed inside the call, once its pixels have
  reached the GPU.

  Deviation: the view cycles on its own every couple of seconds until you click
  or press a key, since no synthetic input actuates a raylib window (see the note
  in scripts/demo_manifest.edn) and a single texture held still is one frame."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const AUTO-FRAMES 120)

(defn- generate
  "The nine procedural textures, in the C's order, each with the caption it
  carries. Generated once: every one of these walks every pixel."
  []
  [[(rl/image-gradient-linear W H 0 rl/RED rl/BLUE) "vertical gradient" rl/RAYWHITE]
   [(rl/image-gradient-linear W H 90 rl/RED rl/BLUE) "horizontal gradient" rl/RAYWHITE]
   [(rl/image-gradient-linear W H 45 rl/RED rl/BLUE) "diagonal gradient" rl/RAYWHITE]
   [(rl/image-gradient-radial W H 0.0 rl/WHITE rl/BLACK) "radial gradient" rl/LIGHTGRAY]
   [(rl/image-gradient-square W H 0.0 rl/WHITE rl/BLACK) "square gradient" rl/LIGHTGRAY]
   [(rl/image-checked W H 32 32 rl/RED rl/BLUE) "checked" rl/RAYWHITE]
   [(rl/image-white-noise W H 0.5) "white noise" rl/RED]
   [(rl/image-perlin-noise W H 50 50 4.0) "perlin noise" rl/RED]
   [(rl/image-cellular W H 32) "cellular" rl/RAYWHITE]])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - image generation"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        textures (generate)
        n (count textures)]
    (loop [frame 0
           current 0
           steered? false]
      (if-not (rl/keep-running? deadline)
        (doseq [[id _ _] textures]
          (rl/unload-texture! id))
        (let [clicked? (or (rl/mouse-pressed? rl/MOUSE-LEFT)
                           (rl/key-pressed? rl/KEY-RIGHT))
              steered? (or steered? clicked?)
              current (cond
                        clicked? (mod (inc current) n)
                        (and (not steered?) (zero? (mod frame AUTO-FRAMES)) (pos? frame))
                        (mod (inc current) n)
                        :else current)
              [id caption caption-color] (nth textures current)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/texture! id {:x 0
                           :y 0
                           :width W
                           :height H})
          (rl/rect! {:x 30
                     :y 400
                     :width 360
                     :height 30
                     :color (rl/rgba 102 191 255 128)})
          (rl/rect-lines! {:x 30
                           :y 400
                           :width 360
                           :height 30
                           :color (rl/rgba 255 255 255 128)})
          (rl/text! (if steered?
                      "CLICK or RIGHT to cycle the procedural textures"
                      "cycling on its own until you click")
                    {:x 40
                     :y 410
                     :size 10
                     :color rl/WHITE})
          (rl/text! caption {:x (- W (rl/text-width caption {:size 20}) 20)
                             :y 10
                             :size 20
                             :color caption-color})
          (rl/text! (str (inc current) " / " n) {:x 20
                                                 :y 10
                                                 :size 20
                                                 :color caption-color})
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) current steered?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
