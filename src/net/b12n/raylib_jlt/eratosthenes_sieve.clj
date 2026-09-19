(ns net.b12n.raylib-jlt.eratosthenes-sieve
  "raylib [shaders] example - Sieve of Eratosthenes.

  The Sieve of Eratosthenes computed per pixel in a fragment shader: each
  pixel maps to an integer, primes light up, composites are dimmed by their
  smallest factor. No uniforms beyond the window resolution and no input --
  the whole example is the shader, following the same full-window rl/rect!
  + rl/with-shader pattern as julia-set.clj (no render texture needed)."
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

uniform vec2 uResolution;

void main() {
  // gl_FragCoord is bottom-left origin; flip y to match raylib's 2D pixels.
  vec2 p = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y);
  int cols = int(uResolution.x);
  int n = int(p.y) * cols + int(p.x) + 2;

  int smallestFactor = 0;
  for (int f = 2; f * f <= n; f++) {
    if (n % f == 0) {
      smallestFactor = f;
      break;
    }
  }

  if (smallestFactor == 0) {
    finalColor = vec4(1.0, 0.85, 0.2, 1.0) * fragColor;
  } else {
    float t = clamp(float(smallestFactor) / 20.0, 0.0, 1.0);
    finalColor = vec4(0.05, 0.05, 0.08 + 0.3 * t, 1.0) * fragColor;
  }
}")

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - Sieve of Eratosthenes"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "eratosthenes-sieve: the fragment shader did not link (log above)"))
      (let [loc-res (rl/uniform-loc sh "uResolution")]
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          (loop [frame 0]
            (when (app/keep-running? deadline)
              (rl/begin-drawing)
              (rl/clear-background rl/BLACK)
              (rl/with-shader
                sh
                (fn []
                  (rl/rect! {:x 0
                             :y 0
                             :width W
                             :height H
                             :color rl/WHITE})))
              (rl/text! "each pixel is a number; gold = prime, blue = composite"
                        {:x 10
                         :y 10
                         :size 18
                         :color rl/RAYWHITE})
              (app/maybe-screenshot! frame 5)
              (rl/end-drawing)
              (recur (inc frame))))
          (finally (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
