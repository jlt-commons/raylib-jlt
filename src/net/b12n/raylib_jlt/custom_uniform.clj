(ns net.b12n.raylib-jlt.custom-uniform
  "raylib [shaders] example - a custom uniform (`jolt -M:custom-uniform`).

  The same two-pass structure as `postprocessing`, but the point here is the
  uniform rather than the effect: a swirl whose centre and strength come from
  the mouse, uploaded every frame. One vec2 and two floats is the whole
  interface between the loop and the shader.

  The swirl rotates each sample around the centre by an angle that falls off
  with distance, so the distortion is strongest under the pointer and fades to
  nothing at the radius. Sampling is the inverse of the effect you want - to
  make the image appear rotated one way, you read from a point rotated the
  other - which is why the sign here looks backwards at first glance.

  The wheel changes the radius; UP/DOWN change the strength.

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
uniform vec2  uCenter;     // in texture space, 0..1
uniform float uRadius;
uniform float uStrength;
uniform float uAspect;     // so the swirl stays circular on a non-square target

void main() {
  vec2 uv = fragTexCoord;

  // Work in an aspect-corrected space, or the falloff is an ellipse.
  vec2 d = (uv - uCenter) * vec2(uAspect, 1.0);
  float dist = length(d);

  if (dist < uRadius) {
    // 1 at the centre, 0 at the rim, smooth in between.
    float fall = 1.0 - smoothstep(0.0, uRadius, dist);
    float ang = uStrength * fall * fall;
    float s = sin(ang), c = cos(ang);
    // Rotate the SAMPLE point, not the pixel - hence -s where +s would read.
    vec2 r = vec2(d.x * c - d.y * s, d.x * s + d.y * c);
    uv = uCenter + r / vec2(uAspect, 1.0);
  }

  finalColor = texture(texture0, uv) * fragColor;
}")

(defn- draw-scene!
  "A grid the distortion can be read against. A swirl over a smooth gradient is
  nearly invisible; straight lines make it obvious."
  [t]
  (rl/clear-background (rl/rgba 16 18 28 255))
  (dotimes [i 17]
    (rl/line! {:x1 (* i 50)
               :y1 0
               :x2 (* i 50)
               :y2 H
               :color (rl/rgba 60 70 110 255)}))
  (dotimes [i 10]
    (rl/line! {:x1 0
               :y1 (* i 50)
               :x2 W
               :y2 (* i 50)
               :color (rl/rgba 60 70 110 255)}))
  (dotimes [i 5]
    (let [a (+ (* t 0.4) (* i 1.26))
          x (int (+ (/ W 2.0) (* 220 (Math/cos a))))
          y (int (+ (/ H 2.0) (* 140 (Math/sin (* 1.7 a)))))]
      (rl/circle! {:x x
                   :y y
                   :radius 34
                   :color (rl/rgba (- 250 (* i 30)) (+ 90 (* i 30)) 210 255)})))
  (rl/text! "straight lines make the swirl legible"
            {:x 20
             :y (- H 40)
             :size 20
             :color (rl/rgba 230 230 240 255)}))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - a custom uniform"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader fragment-shader)
        rt (rl/render-texture W H)]
    (cond
      (nil? sh) (binding [*out* *err*]
                  (println "custom-uniform: the fragment shader did not link (log above)"))
      (nil? rt) (binding [*out* *err*]
                  (println "custom-uniform: the framebuffer is incomplete"))
      :else
      (let [loc-center (rl/uniform-loc sh "uCenter")
            loc-radius (rl/uniform-loc sh "uRadius")
            loc-strength (rl/uniform-loc sh "uStrength")
            loc-aspect (rl/uniform-loc sh "uAspect")]
        (rl/set-uniform-float! sh loc-aspect (/ (double W) H))
        (try
          (loop [frame 0
                 radius 0.32
                 strength 2.6]
            (when (app/keep-running? deadline)
              (let [mx (rl/get-mouse-x)
                    my (rl/get-mouse-y)
                    ;; An off-window pointer is reported faithfully by GetMouseX/Y,
                    ;; which would park the swirl in a corner for any unattended
                    ;; capture. Centre it instead, so the demo shows the effect.
                    over? (and (<= 0 mx W) (<= 0 my H))
                    cx (if over? (/ (double mx) W) 0.5)
                    ;; The FBO texture is sampled bottom-up, so the y the shader
                    ;; wants is the mirror of the y the mouse reports.
                    cy (if over? (- 1.0 (/ (double my) H)) 0.5)
                    radius (max 0.08 (min 0.9 (+ radius (* 0.03 (rl/get-mouse-wheel)))))
                    strength (cond
                               (rl/key-down? rl/KEY-UP) (min 8.0 (+ strength 0.06))
                               (rl/key-down? rl/KEY-DOWN) (max -8.0 (- strength 0.06))
                               :else strength)]
                (rl/with-render-texture rt (fn [] (draw-scene! (rl/get-time))))
                (rl/set-uniform-vec2! sh loc-center cx cy)
                (rl/set-uniform-float! sh loc-radius radius)
                (rl/set-uniform-float! sh loc-strength strength)
                (rl/begin-drawing)
                (rl/clear-background rl/BLACK)
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
                (rl/text! (format "custom uniform   ·   radius %.2f   strength %.2f" radius strength)
                          {:x 14
                           :y 12
                           :size 18
                           :color rl/RAYWHITE})
                (rl/text! (if over?
                            "the swirl follows the mouse   ·   wheel radius   ·   UP/DOWN strength"
                            "pointer is off-window - swirl centred   ·   wheel radius   ·   UP/DOWN strength")
                          {:x 14
                           :y 38
                           :size 14
                           :color rl/LIGHTGRAY})
                (app/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) radius strength))))
          (finally
            (rl/unload-render-texture! rt)
            (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
