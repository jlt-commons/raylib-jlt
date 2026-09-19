(ns net.b12n.raylib-jlt.texture-painting
  "raylib [shaders] example - painting a texture (`jolt -M:texture-painting`).

  The other render-texture examples here treat the framebuffer as scratch: fill
  it, read it back, refill it next frame. This one treats it as a canvas. It is
  never cleared, so what accumulates in it IS the state - there is no stroke
  list, no history, nothing on this side but the last brush position.

  Each frame stamps one quad into the canvas and a fragment shader turns that
  quad into a soft dab: a radial falloff, a hue that walks with time, and
  additive-ish buildup where strokes overlap. So the brush is a shader and the
  canvas is a texture, which is as close to `painting with a shader` as this
  suite gets without compute.

  With the pointer over the window it paints under the cursor. Off-window it
  walks a Lissajous path instead, so an unattended run still paints something.
  C clears the canvas.

  Framebuffer textures are bottom-up in GL's convention, so the canvas is drawn
  back with :v0 1.0 :v1 0.0 - and the brush's y is mirrored on the way in for
  the same reason."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const BRUSH 128)

(def ^:private fragment-shader "
#version 330
in vec2 fragTexCoord;
in vec4 fragColor;
out vec4 finalColor;

uniform sampler2D texture0;
uniform vec3  uInk;
uniform float uFlow;

void main() {
  // The quad is a unit square; the dab is the disc inscribed in it.
  float d = length(fragTexCoord - vec2(0.5)) * 2.0;
  float a = 1.0 - smoothstep(0.55, 1.0, d);
  // Squaring the falloff keeps a hot core with a soft edge - a linear ramp
  // reads as a flat disc with a fuzzy rim rather than as a brush.
  a = a * a * uFlow;
  finalColor = vec4(uInk, a) * fragColor;
}")

(defn- ink
  "A hue walk in RGB, so consecutive strokes differ without a palette."
  [t]
  [(+ 0.5 (* 0.5 (Math/sin t)))
   (+ 0.5 (* 0.5 (Math/sin (+ t 2.09))))
   (+ 0.5 (* 0.5 (Math/sin (+ t 4.19))))])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - painting a texture"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader fragment-shader)
        rt (rl/render-texture W H)]
    (cond
      (nil? sh) (binding [*out* *err*]
                  (println "texture-painting: the fragment shader did not link (log above)"))
      (nil? rt) (binding [*out* *err*]
                  (println "texture-painting: the framebuffer is incomplete"))
      :else
      (let [;; A white quad the shader shapes into the dab. The brush is entirely
            ;; in the shader; this only supplies geometry and texcoords.
            white (rl/texture-from-fn 8 8 (fn [_ _] (rl/rgba 255 255 255 255)))
            loc-ink (rl/uniform-loc sh "uInk")
            loc-flow (rl/uniform-loc sh "uFlow")]
        ;; Start from a known ground rather than whatever the allocation held.
        (rl/with-render-texture rt (fn [] (rl/clear-background (rl/rgba 12 12 18 255))))
        (try
          (loop [frame 0]
            (when (app/keep-running? deadline)
              (let [t (rl/get-time)
                    mx (rl/get-mouse-x)
                    my (rl/get-mouse-y)
                    over? (and (<= 0 mx W) (<= 0 my H))
                    ;; Off-window, walk a Lissajous path so an unattended capture
                    ;; still shows paint rather than one dab in a corner.
                    px (if over? mx (int (+ (/ W 2.0) (* 300 (Math/sin (* 0.9 t))))))
                    py (if over? my (int (+ (/ H 2.0) (* 160 (Math/sin (* 1.4 t))))))
                    [r g b] (ink (* 0.7 t))]
                (when (rl/key-pressed? rl/KEY-C)
                  (rl/with-render-texture rt (fn [] (rl/clear-background (rl/rgba 12 12 18 255)))))
                (rl/set-uniform-vec3! sh loc-ink r g b)
                (rl/set-uniform-float! sh loc-flow 0.55)
                ;; The stamp: one quad, into the canvas, with the brush shader on.
                ;; Nothing clears the canvas, so this is what accumulates.
                (rl/with-render-texture
                  rt
                  (fn []
                    (rl/with-shader
                      sh
                      (fn []
                        (rl/texture! white
                                     {:x (- px (/ BRUSH 2))
                                      :y (- (- H py) (/ BRUSH 2))
                                      :width BRUSH
                                      :height BRUSH})))))
                (rl/begin-drawing)
                (rl/clear-background rl/BLACK)
                (rl/texture! (:texture rt) {:x 0
                                            :y 0
                                            :width W
                                            :height H
                                            :v0 1.0
                                            :v1 0.0})
                (rl/rect! {:x 0
                           :y 0
                           :width W
                           :height 62
                           :color (rl/rgba 0 0 0 150)})
                (rl/text! "painting a texture   ·   the canvas is never cleared"
                          {:x 14
                           :y 12
                           :size 18
                           :color rl/RAYWHITE})
                (rl/text! (if over?
                            "paint under the cursor   ·   C clears"
                            "pointer is off-window - painting a Lissajous path   ·   C clears")
                          {:x 14
                           :y 38
                           :size 14
                           :color rl/LIGHTGRAY})
                ;; Frame 110 rather than the suite's usual 5: this example has
                ;; nothing to show until the canvas has accumulated a stroke, and
                ;; frame 5 captures a single dab.
                (app/maybe-screenshot! frame 110)
                (rl/end-drawing)
                (recur (inc frame)))))
          (finally
            (rl/unload-texture! white)
            (rl/unload-render-texture! rt)
            (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
