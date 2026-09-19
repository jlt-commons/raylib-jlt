(ns net.b12n.raylib-jlt.postprocessing
  "raylib [shaders] example - post-processing (`jolt -M:postprocessing`).

  The two-pass shape every post-process effect has: draw the scene into an
  off-screen framebuffer, then draw that framebuffer's texture back to the
  window with a fragment shader in the way. The scene itself knows nothing about
  the effect, which is the whole point - the same pass drives all six here.

  SPACE (or LEFT/RIGHT) cycles the effect. Six of them, each one uniform or two:
  none, grayscale, posterize, pixelate, sobel edges, and a chromatic-aberration
  split.

  The shader runs on the FBO's texture, so it samples with fragTexCoord rather
  than gl_FragCoord and the resolution arrives as a uniform - that keeps the
  kernel effects (pixelate, sobel) independent of the window size.

  Framebuffer textures are bottom-up in GL's convention, so the FBO is drawn
  back with :v0 1.0 :v1 0.0."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private fragment-shader "
#version 330
in vec2 fragTexCoord;
in vec4 fragColor;
out vec4 finalColor;

uniform sampler2D texture0;
uniform vec2 uResolution;
uniform int  uEffect;

// Luma weights are the usual Rec.601 set; a flat average makes reds and blues
// read far brighter than the eye sees them.
float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

void main() {
  vec2 uv = fragTexCoord;
  vec2 px = 1.0 / uResolution;
  vec3 c  = texture(texture0, uv).rgb;

  if (uEffect == 1) {
    c = vec3(luma(c));
  } else if (uEffect == 2) {
    c = floor(c * 5.0) / 5.0;                     // posterize to 5 levels
  } else if (uEffect == 3) {
    vec2 cell = 12.0 * px;
    c = texture(texture0, floor(uv / cell) * cell + 0.5 * cell).rgb;
  } else if (uEffect == 4) {
    // Sobel over luma. Nine taps, so it is written out rather than looped -
    // the unrolled form is what the GLSL compiler would produce anyway.
    float tl = luma(texture(texture0, uv + px * vec2(-1, -1)).rgb);
    float t  = luma(texture(texture0, uv + px * vec2( 0, -1)).rgb);
    float tr = luma(texture(texture0, uv + px * vec2( 1, -1)).rgb);
    float l  = luma(texture(texture0, uv + px * vec2(-1,  0)).rgb);
    float r  = luma(texture(texture0, uv + px * vec2( 1,  0)).rgb);
    float bl = luma(texture(texture0, uv + px * vec2(-1,  1)).rgb);
    float b  = luma(texture(texture0, uv + px * vec2( 0,  1)).rgb);
    float br = luma(texture(texture0, uv + px * vec2( 1,  1)).rgb);
    float gx = (tr + 2.0*r + br) - (tl + 2.0*l + bl);
    float gy = (bl + 2.0*b + br) - (tl + 2.0*t + tr);
    c = vec3(clamp(length(vec2(gx, gy)), 0.0, 1.0));
  } else if (uEffect == 5) {
    // Offset the channels away from the centre, so the split grows toward the
    // edges the way a real lens misbehaves.
    vec2 d = (uv - 0.5) * 0.02;
    c = vec3(texture(texture0, uv + d).r,
             c.g,
             texture(texture0, uv - d).b);
  }

  finalColor = vec4(c, 1.0) * fragColor;
}")

(def ^:private effect-names
  ["none" "grayscale" "posterize" "pixelate" "sobel edges" "chromatic split"])

(defn- draw-scene!
  "The thing being post-processed: orbiting discs over a dark ground, plus a
  band of bars. Deliberately high-contrast, since sobel and posterize show
  nothing interesting on a flat image."
  [t]
  (rl/clear-background (rl/rgba 18 20 32 255))
  (dotimes [i 7]
    (let [a (+ (* t 0.6) (* i (/ (* 2 Math/PI) 7)))
          x (+ (/ W 2.0) (* 190 (Math/cos a)))
          y (+ (/ H 2.0) (* 120 (Math/sin (* 1.3 a))))
          hue (mod (+ (* i 36) (* t 20)) 360)]
      (rl/circle! {:x (int x)
                   :y (int y)
                   :radius (+ 26 (* 10 (Math/sin (+ t i))))
                   :color (rl/rgba (int (+ 128 (* 127 (Math/cos (Math/toRadians hue)))))
                                   (int (+ 128 (* 127 (Math/sin (Math/toRadians hue)))))
                                   220 255)})))
  (dotimes [i 16]
    (rl/rect! {:x (* i 50)
               :y (- H 70)
               :width 30
               :height (int (+ 20 (* 40 (Math/abs (Math/sin (+ (* 0.4 i) t))))))
               :color (rl/rgba 250 240 200 255)})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - post-processing"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader fragment-shader)
        rt (rl/render-texture W H)]
    (cond
      (nil? sh) (binding [*out* *err*]
                  (println "postprocessing: the fragment shader did not link (log above)"))
      (nil? rt) (binding [*out* *err*]
                  (println "postprocessing: the framebuffer is incomplete"))
      :else
      (let [loc-res (rl/uniform-loc sh "uResolution")
            loc-effect (rl/uniform-loc sh "uEffect")]
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          (loop [frame 0
                 effect 4]
            (when (app/keep-running? deadline)
              (let [effect (cond
                             (or (rl/key-pressed? rl/KEY-SPACE)
                                 (rl/key-pressed? rl/KEY-RIGHT)) (mod (inc effect) 6)
                             (rl/key-pressed? rl/KEY-LEFT) (mod (+ effect 5) 6)
                             :else effect)
                    ;; rl/local-time is wall-clock [h m s], not an animation
                    ;; clock - GetTime is the seconds-since-init one.
                    t (rl/get-time)]
                ;; Pass one: the scene, into the FBO. No shader here - the effect
                ;; belongs to the pass that reads this texture back, not to the
                ;; drawing that fills it.
                (rl/with-render-texture rt (fn [] (draw-scene! t)))
                (rl/set-uniform-int! sh loc-effect effect)
                (rl/begin-drawing)
                (rl/clear-background rl/BLACK)
                ;; Pass two: the FBO's texture through the shader. v0/v1 are
                ;; swapped because a framebuffer texture is bottom-up.
                (rl/with-shader
                  sh
                  (fn []
                    (rl/texture! (:texture rt) {:x 0
                                                :y 0
                                                :width W
                                                :height H
                                                :v0 1.0
                                                :v1 0.0})))
                (rl/rect! {:x 0
                           :y 0
                           :width W
                           :height 62
                           :color (rl/rgba 0 0 0 150)})
                (rl/text! (format "post-processing   ·   %d/6  %s"
                                  (inc effect) (nth effect-names effect))
                          {:x 14
                           :y 12
                           :size 18
                           :color rl/RAYWHITE})
                (rl/text! "SPACE or LEFT/RIGHT cycles the effect   ·   the scene never changes"
                          {:x 14
                           :y 38
                           :size 14
                           :color rl/LIGHTGRAY})
                (app/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) effect))))
          (finally
            (rl/unload-render-texture! rt)
            (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
