(ns net.b12n.raylib-jlt.mandelbrot-set
  "raylib [shaders] example - Mandelbrot set (`jolt -M:mandelbrot-set`).

  The Mandelbrot set, evaluated per pixel in a fragment shader. The wheel zooms
  toward the pointer rather than the window centre, which is what makes deep
  diving feel like navigation instead of arithmetic; dragging with the left button
  pans. UP/DOWN change the iteration cap, and deep zooms need it - detail vanishes
  into flat colour long before the shape runs out.

  The companion to julia-set: the same escape-time loop, but iterating from z = 0
  with c taken from the pixel, rather than from the pixel with c fixed. That one
  swap is the whole difference between the two sets, and it is why julia-set's
  shape is connected exactly when its c lies inside this one.

  Single-precision floats run out of resolution around zoom 1e5, where the image
  goes blocky. That is the shader's limit, not the maths, and it is left visible
  rather than clamped away."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private fragment-shader "
#version 330
in vec4 fragColor;
out vec4 finalColor;

uniform vec2  uResolution;
uniform vec2  uCentre;
uniform float uZoom;
uniform int   uIter;

vec3 palette(float t) {
  return 0.5 + 0.5 * cos(6.28318 * (vec3(0.00, 0.10, 0.20) + t * 1.5));
}

void main() {
  vec2 p = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y);
  vec2 c = (p - 0.5 * uResolution) / (0.5 * uResolution.y) / uZoom + uCentre;

  vec2 z = vec2(0.0);
  int i = 0;
  for (; i < uIter; i++) {
    z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
    if (dot(z, z) > 16.0) break;
  }

  if (i >= uIter) {
    finalColor = vec4(0.0, 0.0, 0.0, 1.0);
  } else {
    float smoothed = float(i) - log2(max(log2(length(z)), 1.0)) + 4.0;
    finalColor = vec4(palette(smoothed * 0.015), 1.0) * fragColor;
  }
}")

(defn- screen->plane
  "Pixel -> complex plane, the same transform the shader performs. Keeping it here
  as well is what lets the wheel zoom toward the pointer: the point under the
  cursor is held fixed by solving for the centre that keeps it there."
  [px py centre-x centre-y zoom]
  [(+ centre-x (/ (- px (* 0.5 W)) (* 0.5 H zoom)))
   (+ centre-y (/ (- py (* 0.5 H)) (* 0.5 H zoom)))])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - Mandelbrot set"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "mandelbrot-set: the fragment shader did not link (log above)"))
      (let [loc-res (rl/uniform-loc sh "uResolution")
            loc-centre (rl/uniform-loc sh "uCentre")
            loc-zoom (rl/uniform-loc sh "uZoom")
            loc-iter (rl/uniform-loc sh "uIter")]
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          ;; Opens on the seahorse valley rather than the whole set: the classic
          ;; full view is mostly empty, and an unattended capture of it says less.
          (loop [frame 0
                 cx -0.743
                 cy 0.1265
                 zoom 24.0
                 iter 260
                 drag nil]
            (when (rl/keep-running? deadline)
              (let [mx (rl/get-mouse-x)
                    my (rl/get-mouse-y)
                    over? (and (<= 0 mx W) (<= 0 my H))
                    wheel (if over? (rl/get-mouse-wheel) 0.0)
                    ;; Zoom toward the pointer: hold the point under the cursor
                    ;; fixed by shifting the centre to compensate.
                    [ax ay] (screen->plane mx my cx cy zoom)
                    nzoom (max 0.3 (min 1.0e6 (* zoom (Math/pow 1.2 wheel))))
                    [bx by] (screen->plane mx my cx cy nzoom)
                    cx (if (zero? wheel) cx (+ cx (- ax bx)))
                    cy (if (zero? wheel) cy (+ cy (- ay by)))
                    ;; Drag to pan, in plane units so it tracks the cursor at any
                    ;; zoom rather than drifting from it.
                    down? (and over? (rl/mouse-down? rl/MOUSE-LEFT))
                    drag (if down? (or drag [mx my]) nil)
                    [dx dy] (if (and down? drag)
                              [(- (first drag) mx) (- (second drag) my)]
                              [0 0])
                    cx (+ cx (/ dx (* 0.5 H nzoom)))
                    cy (+ cy (/ dy (* 0.5 H nzoom)))
                    drag (when down? [mx my])
                    iter (cond
                           (rl/key-down? rl/KEY-UP) (min 2000 (+ iter 8))
                           (rl/key-down? rl/KEY-DOWN) (max 32 (- iter 8))
                           :else iter)]
                (rl/set-uniform-vec2! sh loc-centre cx cy)
                (rl/set-uniform-float! sh loc-zoom nzoom)
                (rl/set-uniform-int! sh loc-iter iter)
                (rl/begin-drawing)
                (rl/clear-background rl/BLACK)
                (rl/with-shader sh (fn [] (rl/rect! {:x 0
                                                     :y 0
                                                     :width W
                                                     :height H
                                                     :color rl/WHITE})))
                (rl/rect! {:x 0
                           :y 0
                           :width W
                           :height 62
                           :color (rl/rgba 0 0 0 150)})
                (rl/text! (format "centre %.6f %s%.6fi   zoom %.0fx   %d iterations"
                                  cx (if (neg? cy) "-" "+") (Math/abs cy) nzoom iter)
                          {:x 14
                           :y 12
                           :size 17
                           :color rl/RAYWHITE})
                (rl/text! "wheel zooms toward the pointer   ·   drag to pan   ·   UP/DOWN iterations"
                          {:x 14
                           :y 38
                           :size 14
                           :color rl/LIGHTGRAY})
                (rl/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) cx cy nzoom iter drag))))
          (finally (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
