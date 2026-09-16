(ns net.b12n.raylib-jlt.texture-waves
  "raylib [shaders] example - texture waves.

  A procedurally generated starfield texture rippled by a fragment shader
  that displaces the sample UV with a sine/cosine pair, one frequency,
  amplitude and speed per axis -- the same displace-then-sample technique
  raylib's C example runs over a loaded space.png, simplified from its
  eight uniforms to four (a vec2 each for frequency, amplitude and speed,
  plus elapsed seconds) since this suite draws its own square-ish texture
  rather than a fixed asset, so the C's separate pixelWidth/pixelHeight
  aspect correction isn't needed. No new FFI: rl/texture-from-fn already
  builds a texture pixel by pixel (see texture-procedural.clj), and the
  uniform setters are the same ones every other shader example uses.
  Two copies are drawn side by side, same as the C, so the seam where the
  waves wrap doesn't show at the window edge.
  Loosely based on raylib/examples/shaders/shaders_texture_waves.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TEX 400)

(def ^:private fragment-shader "
#version 330
in vec2 fragTexCoord;
in vec4 fragColor;
out vec4 finalColor;

uniform sampler2D texture0;
uniform float uSeconds;
uniform vec2 uFreq;
uniform vec2 uAmp;
uniform vec2 uSpeed;

void main() {
  vec2 uv = fragTexCoord;
  uv.x += sin(fragTexCoord.y * uFreq.x + uSeconds * uSpeed.x) * uAmp.x;
  uv.y += cos(fragTexCoord.x * uFreq.y + uSeconds * uSpeed.y) * uAmp.y;
  finalColor = texture(texture0, uv) * fragColor;
}")

(defn- starfield-pixel
  [_ y]
  (let [t (/ y (double TEX))
        base (rl/rgba (int (+ 8 (* 26 t))) (int (+ 4 (* 10 t))) (int (+ 28 (* 48 t))) 255)]
    (if (< (rl/get-random-value 0 999) 4)
      rl/WHITE
      base)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - texture waves"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        tex (rl/texture-from-fn TEX TEX starfield-pixel)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "texture-waves: the fragment shader did not link (log above)"))
      (let [loc-seconds (rl/uniform-loc sh "uSeconds")
            loc-freq (rl/uniform-loc sh "uFreq")
            loc-amp (rl/uniform-loc sh "uAmp")
            loc-speed (rl/uniform-loc sh "uSpeed")]
        (rl/set-uniform-vec2! sh loc-freq 18.0 18.0)
        (rl/set-uniform-vec2! sh loc-amp 0.03 0.03)
        (rl/set-uniform-vec2! sh loc-speed 3.0 3.0)
        (try
          (loop [frame 0 seconds 0.0]
            (when (rl/keep-running? deadline)
              (let [seconds (+ seconds (rl/get-frame-time))]
                (rl/set-uniform-float! sh loc-seconds seconds)
                (rl/begin-drawing)
                (rl/clear-background rl/RAYWHITE)
                (rl/with-shader
                  sh
                  (fn []
                    (rl/texture! tex {:x 0
                                      :y 0
                                      :width TEX
                                      :height H})
                    (rl/texture! tex {:x TEX
                                      :y 0
                                      :width TEX
                                      :height H})))
                (rl/maybe-screenshot! frame 30)
                (rl/end-drawing)
                (recur (inc frame) seconds))))
          (finally (rl/unload-shader! sh)))))
    (rl/unload-texture! tex)
    (rl/close-window)))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
