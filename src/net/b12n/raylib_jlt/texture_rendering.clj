(ns net.b12n.raylib-jlt.texture-rendering
  "raylib [shaders] example - texture rendering.

  A grid of squares panned, rotated and recolored entirely by a fragment
  shader: nothing is drawn on the Clojure side except one full-window
  rect, the same rl/rect! + rl/with-shader pattern eratosthenes-sieve.clj
  uses. raylib's own C example draws over a 1024x1024 BLANK texture to
  give the shader something to paint into GPU memory; here the full-quad
  rect stands in for that blank texture, so there's no texture at all.

  The shader itself is an original grid-of-squares animation (panning,
  per-cell rotation, size and color driven by a phase offset unique to
  each cell), not a port of raylib's own cubes_panning.fs.
  Loosely based on raylib's examples/shaders/shaders_texture_rendering.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private fragment-shader "
#version 330
in vec4 fragColor;
out vec4 finalColor;

uniform float uTime;
uniform vec2 uResolution;

void main() {
  // rl/rect! draws through the flat-color path, whose fragTexCoord is a
  // degenerate constant (the whole rect samples one pixel of the shared
  // white texture) rather than a 0..1 span -- gl_FragCoord is what
  // eratosthenes-sieve.clj uses for the same reason.
  vec2 uv = gl_FragCoord.xy / uResolution;
  uv.y = 1.0 - uv.y;
  uv.x *= uResolution.x / uResolution.y;

  float divisions = 10.0;
  vec2 grid = uv * divisions;
  grid.x += uTime * 0.6;
  vec2 id = floor(grid);
  vec2 cell = fract(grid);

  float phase = fract(uTime * 0.15 + id.x * 0.17 + id.y * 0.31);
  float wobble = sin(phase * 6.2831853);
  float angle = wobble * 0.78539816;

  vec2 p = cell - 0.5;
  float c = cos(angle), s = sin(angle);
  p = mat2(c, -s, s, c) * p;
  p += 0.5;

  float size = 0.55 + 0.15 * wobble;
  float inset = 0.5 - size * 0.5;
  float inside = step(inset, p.x) * step(inset, p.y) * step(p.x, 1.0 - inset) * step(p.y, 1.0 - inset);

  vec3 dim = vec3(0.08, 0.09, 0.14);
  vec3 lit = mix(vec3(0.95, 0.35, 0.15), vec3(0.15, 0.65, 0.95), 0.5 + 0.5 * wobble);
  finalColor = vec4(mix(dim, lit, inside), 1.0) * fragColor;
}")

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - texture rendering"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "texture-rendering: the fragment shader did not link (log above)"))
      (let [loc-time (rl/uniform-loc sh "uTime")
            loc-res (rl/uniform-loc sh "uResolution")]
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          (loop [frame 0]
            (when (rl/keep-running? deadline)
              (rl/set-uniform-float! sh loc-time (rl/get-time))
              (rl/begin-drawing)
              (rl/clear-background rl/RAYWHITE)
              (rl/with-shader
                sh
                (fn []
                  (rl/rect! {:x 0
                             :y 0
                             :width W
                             :height H
                             :color rl/WHITE})))
              (rl/text! "BACKGROUND is PAINTED and ANIMATED on SHADER!"
                        {:x 10
                         :y 10
                         :size 20
                         :color rl/MAROON})
              (rl/maybe-screenshot! frame 30)
              (rl/end-drawing)
              (recur (inc frame))))
          (finally (rl/unload-shader! sh)))))
    (rl/close-window)))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
