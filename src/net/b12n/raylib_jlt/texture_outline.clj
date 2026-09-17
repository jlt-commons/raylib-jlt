(ns net.b12n.raylib-jlt.texture-outline
  "raylib [shaders] example - texture outline.

  A fragment shader draws a red outline around a sprite's alpha edge: it
  samples the four diagonal texels around each pixel, and where any of them
  is opaque but the pixel itself is transparent, that pixel is outline.
  MOUSE WHEEL grows/shrinks the outline in texel units, and a slow sine
  drift keeps it breathing even with no one at the wheel.

  No new FFI: the same shader/uniform-loc/set-uniform-*!/with-shader every
  other shader example here uses. The sprite is a small procedural blob via
  rl/texture-from-fn (alpha 0 outside its radius), standing in for the
  upstream C example's fudesumi.png.
  Based on raylib/examples/shaders/shaders_texture_outline.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SPRITE 80)
(def ^:const SCALE 4)

(def ^:private fragment-shader "
#version 330

in vec2 fragTexCoord;
in vec4 fragColor;

uniform sampler2D texture0;
uniform vec4 colDiffuse;

uniform vec2 textureSize;
uniform float outlineSize;
uniform vec4 outlineColor;

out vec4 finalColor;

void main() {
  vec4 texel = texture(texture0, fragTexCoord);
  vec2 texelScale = vec2(outlineSize / textureSize.x, outlineSize / textureSize.y);

  vec4 corners = vec4(0.0);
  corners.x = texture(texture0, fragTexCoord + vec2(texelScale.x, texelScale.y)).a;
  corners.y = texture(texture0, fragTexCoord + vec2(texelScale.x, -texelScale.y)).a;
  corners.z = texture(texture0, fragTexCoord + vec2(-texelScale.x, texelScale.y)).a;
  corners.w = texture(texture0, fragTexCoord + vec2(-texelScale.x, -texelScale.y)).a;

  float outline = min(dot(corners, vec4(1.0)), 1.0);
  vec4 color = mix(vec4(0.0), outlineColor, outline);
  finalColor = mix(color, texel, texel.a);
}")

(defn- sprite-pixel
  "A round blob with two eyes: opaque body, transparent everywhere else, so
  the outline shader has a clean alpha edge to trace."
  [x y]
  (let [cx (/ SPRITE 2.0) cy (/ SPRITE 2.0)
        dx (- x cx) dy (- y cy)
        r (Math/sqrt (+ (* dx dx) (* dy dy)))
        head-r (* SPRITE 0.4)
        eye-l (Math/sqrt (+ (Math/pow (- x (- cx 12)) 2) (Math/pow (- y (- cy 8)) 2)))
        eye-r (Math/sqrt (+ (Math/pow (- x (+ cx 12)) 2) (Math/pow (- y (- cy 8)) 2)))]
    (cond
      (> r head-r) (rl/rgba 0 0 0 0)
      (or (< eye-l 5) (< eye-r 5)) (rl/rgba 20 20 20 255)
      :else (rl/rgba 255 165 79 255))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - texture outline"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sprite (rl/texture-from-fn SPRITE SPRITE sprite-pixel)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "texture-outline: the fragment shader did not link (log above)"))
      (let [size-loc (rl/uniform-loc sh "outlineSize")
            color-loc (rl/uniform-loc sh "outlineColor")
            tsize-loc (rl/uniform-loc sh "textureSize")]
        (rl/set-uniform-vec4! sh color-loc 1.0 0.0 0.0 1.0)
        (rl/set-uniform-vec2! sh tsize-loc (double SPRITE) (double SPRITE))
        (try
          (loop [frame 0
                 base 2.0]
            (when (rl/keep-running? deadline)
              (let [base (max 1.0 (+ base (rl/get-mouse-wheel)))
                    ;; A slow breathing drift on top of the wheel-set base, so
                    ;; the effect keeps moving even with no one at the wheel
                    ;; (an unattended capture, or just watching it run).
                    osize (max 1.0 (+ base (* 2.0 (Math/sin (* frame 0.03)))))]
                (rl/set-uniform-float! sh size-loc osize)

                (rl/begin-drawing)
                (rl/clear-background rl/RAYWHITE)
                (rl/with-shader sh
                  (fn []
                    (rl/texture! sprite {:x (- 400 (/ (* SPRITE SCALE) 2))
                                         :y (- 225 (/ (* SPRITE SCALE) 2))
                                         :width (* SPRITE SCALE)
                                         :height (* SPRITE SCALE)})))

                (rl/text! "Shader-based texture outline" {:x 10
                                                          :y 10
                                                          :size 20
                                                          :color rl/GRAY})
                (rl/text! "Scroll mouse wheel to change outline size"
                          {:x 10
                           :y 40
                           :size 16
                           :color rl/GRAY})
                (rl/text! (str "Outline size: " (int osize) " px")
                          {:x 10
                           :y 70
                           :size 20
                           :color rl/MAROON})

                (rl/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) base))))
          (finally (rl/unload-shader! sh)))))
    (rl/unload-texture! sprite))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
