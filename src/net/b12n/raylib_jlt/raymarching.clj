(ns net.b12n.raylib-jlt.raymarching
  "raylib [shaders] example - raymarching (`jolt -M:raymarching`).

  A 3D scene with no geometry at all. Every pixel walks a ray forward until it is
  close enough to a surface, where \"close enough\" comes from a signed distance
  function: a function that returns how far the nearest surface is from any point
  in space. Take that many steps and you cannot overshoot, so the walk converges
  on the surface without ever intersecting a triangle.

  The scene is a plane, a sphere, a box and a torus, combined with min() for union
  and a smooth minimum for the blend between the sphere and the box. Normals come
  from sampling the distance field either side of the hit point, which is why
  lighting works without a single vertex normal being stored.

  W/S/A/D fly the camera, the mouse looks around, SPACE toggles a heat map of the
  step count - the bright regions are where rays travel nearly parallel to a
  surface and the marcher has to creep.

  Contrast the 3D examples elsewhere in this suite: those push vertices through
  rlgl. Here the GPU is handed four numbers and derives the whole image."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private fragment-shader "
#version 330
in vec4 fragColor;
out vec4 finalColor;

uniform vec2  uResolution;
uniform vec3  uCamPos;
uniform vec2  uCamAngle;   // yaw, pitch
uniform float uTime;
uniform int   uHeat;

const int   MAX_STEPS = 96;
const float MAX_DIST  = 60.0;
const float SURF_DIST = 0.0015;

float sdSphere(vec3 p, float r) { return length(p) - r; }
float sdBox(vec3 p, vec3 b) {
  vec3 q = abs(p) - b;
  return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}
float sdTorus(vec3 p, vec2 t) {
  return length(vec2(length(p.xz) - t.x, p.y)) - t.y;
}
// Polynomial smooth min: blends two surfaces instead of creasing them.
float smin(float a, float b, float k) {
  float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
  return mix(b, a, h) - k * h * (1.0 - h);
}

float scene(vec3 p) {
  float plane  = p.y + 1.0;
  float sphere = sdSphere(p - vec3(-1.6, sin(uTime) * 0.35, 0.0), 1.0);
  float box    = sdBox(p - vec3(0.6, 0.0, 0.0), vec3(0.8));
  float torus  = sdTorus(p - vec3(3.4, 0.0, 0.0), vec2(1.1, 0.35));
  return min(plane, min(smin(sphere, box, 0.6), torus));
}

vec3 normalAt(vec3 p) {
  // Central differences on the distance field - no stored normals anywhere.
  vec2 e = vec2(0.001, 0.0);
  return normalize(vec3(scene(p + e.xyy) - scene(p - e.xyy),
                        scene(p + e.yxy) - scene(p - e.yxy),
                        scene(p + e.yyx) - scene(p - e.yyx)));
}

void main() {
  vec2 uv = (vec2(gl_FragCoord.x, gl_FragCoord.y) - 0.5 * uResolution) / uResolution.y;

  float cy = cos(uCamAngle.x), sy = sin(uCamAngle.x);
  float cp = cos(uCamAngle.y), sp = sin(uCamAngle.y);
  vec3 fwd   = normalize(vec3(sy * cp, sp, cy * cp));
  vec3 right = normalize(cross(fwd, vec3(0.0, 1.0, 0.0)));
  vec3 up    = cross(right, fwd);
  vec3 rd    = normalize(uv.x * right + uv.y * up + 1.2 * fwd);

  float d = 0.0;
  int steps = 0;
  for (; steps < MAX_STEPS; steps++) {
    float dist = scene(uCamPos + rd * d);
    if (dist < SURF_DIST || d > MAX_DIST) break;
    d += dist;                       // safe to advance by the distance itself
  }

  if (uHeat == 1) {
    float t = float(steps) / float(MAX_STEPS);
    finalColor = vec4(t, t * t, 1.0 - t, 1.0);
    return;
  }

  if (d > MAX_DIST) {
    vec3 sky = mix(vec3(0.05, 0.07, 0.12), vec3(0.22, 0.30, 0.45), rd.y * 0.5 + 0.5);
    finalColor = vec4(sky, 1.0) * fragColor;
    return;
  }

  vec3 p = uCamPos + rd * d;
  vec3 n = normalAt(p);
  vec3 lightDir = normalize(vec3(0.6, 0.8, -0.4));
  float diff = max(dot(n, lightDir), 0.0);

  // One extra march toward the light: if it hits something first, p is shadowed.
  float sh = 1.0;
  float t = 0.05;
  for (int i = 0; i < 40; i++) {
    float ds = scene(p + lightDir * t);
    if (ds < SURF_DIST) { sh = 0.25; break; }
    t += ds;
    if (t > 12.0) break;
  }

  vec3 base = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + p.x * 0.35 + p.z * 0.2);
  vec3 col  = base * (0.15 + diff * sh);
  col = mix(col, vec3(0.08, 0.10, 0.16), 1.0 - exp(-0.020 * d * d));  // distance fog
  finalColor = vec4(pow(col, vec3(0.4545)), 1.0) * fragColor;         // to sRGB
}")

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - raymarching"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "raymarching: the fragment shader did not link (log above)"))
      (let [loc-res (rl/uniform-loc sh "uResolution")
            loc-pos (rl/uniform-loc sh "uCamPos")
            loc-ang (rl/uniform-loc sh "uCamAngle")
            loc-time (rl/uniform-loc sh "uTime")
            loc-heat (rl/uniform-loc sh "uHeat")]
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          (loop [frame 0
                 px 0.9 py 1.1 pz -6.5
                 yaw 0.0 pitch -0.09
                 heat? false
                 last-mouse nil]
            (when (rl/keep-running? deadline)
              (let [mx (rl/get-mouse-x)
                    my (rl/get-mouse-y)
                    over? (and (<= 0 mx W) (<= 0 my H))
                    ;; Look with the mouse only while it is over the window and
                    ;; held: a bare position would swing the camera wildly the
                    ;; moment the cursor crossed the frame, and an unattended
                    ;; capture would point somewhere arbitrary.
                    looking? (and over? (rl/mouse-down? rl/MOUSE-LEFT))
                    [dx dy] (if (and looking? last-mouse)
                              [(- mx (first last-mouse)) (- my (second last-mouse))]
                              [0 0])
                    yaw (- yaw (* dx 0.005))
                    pitch (max -1.4 (min 1.4 (- pitch (* dy 0.005))))
                    last-mouse (when looking? [mx my])
                    heat? (if (rl/key-pressed? rl/KEY-SPACE) (not heat?) heat?)
                    speed (* 4.5 (rl/get-frame-time))
                    fwd-x (* (Math/sin yaw) (Math/cos pitch))
                    fwd-y (Math/sin pitch)
                    fwd-z (* (Math/cos yaw) (Math/cos pitch))
                    ;; right = normalize(cross(fwd, up)) with up = +y
                    rt-x (Math/cos yaw)
                    rt-z (- (Math/sin yaw))
                    f (+ (if (rl/key-down? rl/KEY-W) 1 0) (if (rl/key-down? rl/KEY-S) -1 0))
                    s (+ (if (rl/key-down? rl/KEY-D) 1 0) (if (rl/key-down? rl/KEY-A) -1 0))
                    px (+ px (* speed (+ (* f fwd-x) (* s rt-x))))
                    py (+ py (* speed (* f fwd-y)))
                    pz (+ pz (* speed (+ (* f fwd-z) (* s rt-z))))]
                (rl/set-uniform-vec3! sh loc-pos px py pz)
                (rl/set-uniform-vec2! sh loc-ang yaw pitch)
                (rl/set-uniform-float! sh loc-time (rl/get-time))
                (rl/set-uniform-int! sh loc-heat (if heat? 1 0))
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
                (rl/text! (format "camera %.2f %.2f %.2f   %s"
                                  px py pz (if heat? "step-count heat map" "shaded"))
                          {:x 14
                           :y 12
                           :size 17
                           :color rl/RAYWHITE})
                (rl/text! "W/A/S/D fly   ·   drag to look   ·   SPACE toggles the heat map"
                          {:x 14
                           :y 38
                           :size 14
                           :color rl/LIGHTGRAY})
                (rl/fps! {:x 14
                          :y (- H 28)})
                (rl/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) px py pz yaw pitch heat? last-mouse))))
          (finally (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
