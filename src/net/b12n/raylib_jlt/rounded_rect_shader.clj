(ns net.b12n.raylib-jlt.rounded-rect-shader
  "raylib [shaders] example - SDF rounded rectangles (`jolt -M:rounded-rect-shader`).

  Fill, border and drop shadow from one signed distance function, evaluated per
  pixel. The distance to a rounded rectangle is a two-line closed form, and every
  visual feature falls out of thresholding it: inside is fill, a band either side
  of zero is the border, and the same distance blurred and offset is the shadow.

  That is the point of the technique. A rounded rectangle built from geometry
  needs corner tessellation, and its border and shadow need separate passes and
  separate meshes; here changing the radius is changing one number, and antialias
  is free because the distance is continuous rather than sampled.

  Compare `rounded-rectangle` elsewhere in this suite, which builds the same shape
  out of rl/sector! corners and rectangles - the honest way to do it without a
  shader, and visibly more code for a harder-edged result.

  Drag to move the card, wheel changes the corner radius, UP/DOWN the border
  width, SPACE cycles what is drawn: everything, then the raw distance field."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private fragment-shader "
#version 330
in vec4 fragColor;
out vec4 finalColor;

uniform vec2  uResolution;
uniform vec4  uRect;      // centre.xy, half-size.xy
uniform float uRadius;
uniform float uBorder;
uniform int   uMode;      // 0 = composed, 1 = raw distance field

// Signed distance to a rounded box: negative inside, zero on the edge, positive
// outside, and the magnitude is the actual distance in pixels.
// `half` would be the natural name for the half-size, and is a RESERVED WORD in
// GLSL - reserved for a future half-precision type. It compiles nowhere and the
// error (half : syntax error) points at the parameter list rather than at
// the name, so it is easy to misread as a malformed signature.
float sdRoundedBox(vec2 p, vec2 halfSize, float r) {
  vec2 q = abs(p) - halfSize + r;
  return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
  vec2 p = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y);
  vec2 rel = p - uRect.xy;
  float d = sdRoundedBox(rel, uRect.zw, uRadius);

  if (uMode == 1) {
    // The field itself: banded so the iso-lines are visible, warm inside.
    float bands = 0.5 + 0.5 * cos(d * 0.20);
    vec3 col = mix(vec3(0.10, 0.14, 0.22), vec3(0.95, 0.75, 0.35), bands);
    col *= (d < 0.0) ? 1.0 : 0.55;
    finalColor = vec4(col, 1.0) * fragColor;
    return;
  }

  // A shadow is the same distance, offset and softened - no second shape.
  float ds = sdRoundedBox(rel - vec2(6.0, 10.0), uRect.zw, uRadius);
  float shadow = smoothstep(18.0, 0.0, ds) * 0.55;

  // fwidth gives the pixel footprint of d, so the same expression antialiases
  // correctly at any scale without a magic epsilon.
  float aa   = fwidth(d);
  float fill = 1.0 - smoothstep(-aa, aa, d);
  float ring = 1.0 - smoothstep(-aa, aa, abs(d + uBorder * 0.5) - uBorder * 0.5);

  vec3 bg     = mix(vec3(0.07, 0.09, 0.14), vec3(0.13, 0.16, 0.24), p.y / uResolution.y);
  vec3 col    = mix(bg, vec3(0.02, 0.02, 0.04), shadow);
  vec3 face   = mix(vec3(0.20, 0.55, 0.95), vec3(0.55, 0.25, 0.85), rel.y / uRect.w * 0.5 + 0.5);
  col         = mix(col, face, fill);
  col         = mix(col, vec3(0.98), ring);

  finalColor = vec4(col, 1.0) * fragColor;
}")

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - SDF rounded rectangles"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "rounded-rect-shader: the fragment shader did not link (log above)"))
      (let [loc-res (rl/uniform-loc sh "uResolution")
            loc-rect (rl/uniform-loc sh "uRect")
            loc-radius (rl/uniform-loc sh "uRadius")
            loc-border (rl/uniform-loc sh "uBorder")
            loc-mode (rl/uniform-loc sh "uMode")]
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          (loop [frame 0
                 cx (* 0.5 W)
                 cy (* 0.5 H)
                 radius 48.0
                 border 5.0
                 mode 0]
            (when (rl/keep-running? deadline)
              (let [mx (rl/get-mouse-x)
                    my (rl/get-mouse-y)
                    over? (and (<= 0 mx W) (<= 0 my H))
                    ;; The card follows the pointer only while dragging, and
                    ;; starts centred. Parking it AT the pointer looks livelier
                    ;; in person and is worse everywhere else: the card sits
                    ;; wherever the cursor happens to be, so an unattended
                    ;; screenshot or demo capture catches it half off-screen,
                    ;; and you cannot study the distance field without the card
                    ;; fleeing the cursor you are reading it with.
                    dragging? (and over? (rl/mouse-down? rl/MOUSE-LEFT))
                    cx (if dragging? (double mx) cx)
                    cy (if dragging? (double my) cy)
                    half-w 190.0
                    half-h 110.0
                    ;; The radius cannot exceed half the shorter side, or the
                    ;; corners overlap and the field folds in on itself.
                    max-r (min half-w half-h)
                    radius (max 0.0 (min max-r (+ radius (* 4.0 (rl/get-mouse-wheel)))))
                    border (cond
                             (rl/key-down? rl/KEY-UP) (min 40.0 (+ border 0.4))
                             (rl/key-down? rl/KEY-DOWN) (max 0.0 (- border 0.4))
                             :else border)
                    mode (if (rl/key-pressed? rl/KEY-SPACE) (mod (inc mode) 2) mode)]
                (rl/set-uniform-vec4! sh loc-rect cx cy half-w half-h)
                (rl/set-uniform-float! sh loc-radius radius)
                (rl/set-uniform-float! sh loc-border border)
                (rl/set-uniform-int! sh loc-mode mode)
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
                (rl/text! (format "radius %.0f   border %.1f   %s"
                                  radius border
                                  (if (zero? mode) "fill + border + shadow" "raw distance field"))
                          {:x 14
                           :y 12
                           :size 17
                           :color rl/RAYWHITE})
                (rl/text! "drag to move   ·   wheel changes the radius   ·   UP/DOWN border   ·   SPACE"
                          {:x 14
                           :y 38
                           :size 14
                           :color rl/LIGHTGRAY})
                (rl/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) cx cy radius border mode))))
          (finally (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
