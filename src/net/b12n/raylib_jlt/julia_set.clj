(ns net.b12n.raylib-jlt.julia-set
  "raylib [shaders] example - Julia set (`jolt -M:julia-set`).

  The Julia set for c = (cRe, cIm), evaluated per pixel in a fragment shader.
  Moving the mouse steers c around the unit circle, which is what makes the shape
  breathe between a connected blob and scattered dust: the set is connected
  exactly when c lies inside the Mandelbrot set. SPACE freezes c so a shape can be
  examined, the wheel zooms, UP/DOWN change the iteration cap.

  The whole image is one full-window rectangle drawn inside rl/with-shader; there
  is no per-pixel work on this side at all. Uniforms exercised: vec2 (uC,
  uResolution), float (uZoom), int (uIter).

  gl_FragCoord has its origin at the BOTTOM left, unlike raylib's 2D coordinates,
  so the y flip happens in the shader rather than in the mouse maths here."
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

uniform vec2  uResolution;
uniform vec2  uC;
uniform float uZoom;
uniform int   uIter;

// A smooth escape-time colouring: the fractional part of the iteration count
// removes the banding a bare integer count produces.
vec3 palette(float t) {
  return 0.5 + 0.5 * cos(6.28318 * (vec3(0.00, 0.33, 0.67) + t));
}

void main() {
  // gl_FragCoord is bottom-left origin; flip y so the image matches the mouse.
  vec2 p = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y);
  vec2 z = (p - 0.5 * uResolution) / (0.5 * uResolution.y) / uZoom;

  int i = 0;
  for (; i < uIter; i++) {
    z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + uC;
    if (dot(z, z) > 16.0) break;
  }

  if (i >= uIter) {
    finalColor = vec4(0.02, 0.02, 0.05, 1.0);      // inside the set
  } else {
    float smoothed = float(i) - log2(max(log2(length(z)), 1.0)) + 4.0;
    finalColor = vec4(palette(smoothed * 0.02), 1.0) * fragColor;
  }
}")

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - Julia set"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "julia-set: the fragment shader did not link (log above)"))
      ;; Uniform locations are GL queries, so they are looked up once here rather
      ;; than every frame.
      (let [loc-res (rl/uniform-loc sh "uResolution")
            loc-c (rl/uniform-loc sh "uC")
            loc-zoom (rl/uniform-loc sh "uZoom")
            loc-iter (rl/uniform-loc sh "uIter")]
        ;; uResolution never changes, so it is set once rather than re-uploaded
        ;; every frame like the three that do.
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          (loop [frame 0
                 zoom 1.0
                 iter 160
                 frozen nil]
            (when (app/keep-running? deadline)
              (let [frozen (if (rl/key-pressed? rl/KEY-SPACE)
                             (if frozen nil [(rl/get-mouse-x) (rl/get-mouse-y)])
                             frozen)
                    ;; The pointer can sit outside the window - on another monitor, or just
                    ;; past the edge - and GetMouseX/Y report that faithfully. Extrapolating c
                    ;; from an off-window position lands far outside the interesting disc and
                    ;; draws a flat wash. That is also what an unattended screenshot or demo
                    ;; capture gets whenever the cursor happens to be elsewhere, so an
                    ;; off-window pointer falls back to a classic constant instead.
                    over? (and (<= 0 (rl/get-mouse-x) W) (<= 0 (rl/get-mouse-y) H))
                    [mx my] (or frozen (when over? [(rl/get-mouse-x) (rl/get-mouse-y)]))
                    ;; c tracks the pointer across a window on the complex plane that keeps
                    ;; the interesting region reachable by hand.
                    c-re (if mx (- (* 1.6 (/ (double mx) W)) 0.8) -0.79)
                    c-im (if my (- (* 1.2 (/ (double my) H)) 0.6) 0.15)
                    zoom (max 0.25 (min 40.0 (+ zoom (* zoom 0.1 (rl/get-mouse-wheel)))))
                    iter (cond
                           (rl/key-down? rl/KEY-UP) (min 900 (+ iter 4))
                           (rl/key-down? rl/KEY-DOWN) (max 24 (- iter 4))
                           :else iter)]
              ;; Uniforms are set BEFORE the shader mode, as raylib's own
              ;; examples do. SetShaderValue enables the program itself, so it
              ;; does not need the mode active - and keeping them out here avoids
              ;; modelling a trap: geometry inside one with-shader block is
              ;; batched and flushed once, so two draws with different uniform
              ;; values between them would both silently use the last value.
                (rl/set-uniform-vec2! sh loc-c c-re c-im)
                (rl/set-uniform-float! sh loc-zoom zoom)
                (rl/set-uniform-int! sh loc-iter iter)
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
                (rl/rect! {:x 0
                           :y 0
                           :width W
                           :height 62
                           :color (rl/rgba 0 0 0 150)})
              ;; The sign is built by hand: jolt's format has no '+' flag, and a
              ;; complex number wants the sign attached to the imaginary part.
                (rl/text! (format "c = %.4f %s%.4fi   zoom %.2f   %d iterations"
                                  c-re (if (neg? c-im) "-" "+") (Math/abs c-im) zoom iter)
                          {:x 14
                           :y 12
                           :size 18
                           :color rl/RAYWHITE})
                (rl/text! (if frozen
                            "SPACE releases c   ·   wheel zooms   ·   UP/DOWN iterations"
                            (if over?
                              "move the mouse to steer c   ·   SPACE freezes it"
                              "pointer is off-window - showing c = -0.79 + 0.15i"))
                          {:x 14
                           :y 38
                           :size 14
                           :color rl/LIGHTGRAY})
                (app/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) zoom iter frozen))))
          (finally (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
