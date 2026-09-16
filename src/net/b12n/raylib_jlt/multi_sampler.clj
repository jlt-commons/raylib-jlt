(ns net.b12n.raylib-jlt.multi-sampler
  "raylib [shaders] example - two samplers (`jolt -M:multi-sampler`).

  raylib binds the texture you draw to sampler slot 0 and names it `texture0`.
  Everything past that is the caller's job: a second sampler is a uniform you
  fill with SetShaderValueTexture. This example blends a checkerboard against a
  radial gradient, with the mix driven by the mouse.

  The call is the reason this example exists. SetShaderValueTexture takes a
  Shader AND a Texture2D, both by value, and on arm64 they travel by different
  routes - Shader is 16 bytes and goes in general-purpose registers, Texture2D is
  20 and is therefore passed indirectly, through a pointer the caller supplies.
  One signature, both ABI paths, which is the case worth having an example of.

  LEFT/RIGHT nudge the mix when the pointer is off-window."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TEX 128)

(def ^:private fragment-shader "
#version 330
in vec2 fragTexCoord;
in vec4 fragColor;
out vec4 finalColor;

uniform sampler2D texture0;    // raylib binds the drawn texture here
uniform sampler2D uOther;      // ours, via SetShaderValueTexture
uniform float     uMix;

void main() {
  vec4 a = texture(texture0, fragTexCoord);
  vec4 b = texture(uOther,   fragTexCoord);
  // Mixing in a slight contrast curve keeps the midpoint from washing out to
  // flat grey, which is what a straight lerp between these two produces.
  vec3 m = mix(a.rgb, b.rgb, uMix);
  finalColor = vec4(pow(m, vec3(0.9)), 1.0) * fragColor;
}")

(defn- checker
  "A checkerboard, 16px cells."
  [x y]
  (if (zero? (mod (+ (quot x 16) (quot y 16)) 2))
    (rl/rgba 240 240 245 255)
    (rl/rgba 40 45 70 255)))

(defn- gradient
  "A radial gradient from warm centre to cool edge."
  [x y]
  (let [cx (- (/ (double x) TEX) 0.5)
        cy (- (/ (double y) TEX) 0.5)
        d (min 1.0 (* 2.0 (Math/sqrt (+ (* cx cx) (* cy cy)))))]
    (rl/rgba (int (+ 40 (* 215 (- 1.0 d))))
             (int (+ 60 (* 120 (- 1.0 d))))
             (int (+ 90 (* 150 d)))
             255)))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shaders] example - two samplers")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "multi-sampler: the fragment shader did not link (log above)"))
      (let [tex-a (rl/texture-from-fn TEX TEX checker)
            tex-b (rl/texture-from-fn TEX TEX gradient)
            loc-other (rl/uniform-loc sh "uOther")
            loc-mix (rl/uniform-loc sh "uMix")]
        (try
          (loop [frame 0
                 keyed 0.5
                 touched? false]
            (when (rl/keep-running? deadline)
              ;; The pointer can sit outside the window and GetMouseX reports that
              ;; faithfully. Neither texture animates, so an off-window pointer with
              ;; a held value would render a completely static frame - and an
              ;; unattended demo capture is exactly that case. So off-window sweeps
              ;; the mix with time instead, unless a key has been used, which is
              ;; what makes the recorded GIF show the blend rather than one frame
              ;; of it.
              (let [mx (rl/get-mouse-x)
                    over? (<= 0 mx W)
                    touched? (or touched?
                                 (rl/key-down? rl/KEY-RIGHT)
                                 (rl/key-down? rl/KEY-LEFT))
                    keyed (cond
                            (rl/key-down? rl/KEY-RIGHT) (min 1.0 (+ keyed 0.01))
                            (rl/key-down? rl/KEY-LEFT) (max 0.0 (- keyed 0.01))
                            :else keyed)
                    swept (* 0.5 (+ 1.0 (Math/sin (* 0.9 (rl/get-time)))))
                    mix (cond over? (/ (double mx) W)
                              touched? keyed
                              :else swept)]
                (rl/set-uniform-float! sh loc-mix mix)
                (rl/begin-drawing)
                (rl/clear-background rl/BLACK)
                ;; texture0 is tex-a because it is the one being drawn; tex-b
                ;; arrives only through the sampler uniform below.
                (rl/with-shader
                  sh
                  (fn []
                    ;; A sampler uniform goes INSIDE the shader mode, unlike the
                    ;; value uniforms above. rlSetUniformSampler parks the id in
                    ;; rlgl's activeTextureId table for the next batch draw, and
                    ;; EndShaderMode forces that draw and clears the table - so a
                    ;; sampler registered before BeginShaderMode is wiped by the
                    ;; flush that BeginShaderMode itself performs, leaving uOther
                    ;; on unit 0 and sampling texture0. The failure is silent: it
                    ;; renders the first texture twice and blends it with itself.
                    (rl/set-uniform-texture! sh loc-other tex-b TEX TEX)
                    (rl/texture! tex-a :x 0 :y 0 :width W :height H)))
                ;; The two sources, unblended, so the mix has something to read
                ;; against.
                (rl/texture! tex-a :x 14 :y 74 :width 96 :height 96)
                (rl/texture! tex-b :x 122 :y 74 :width 96 :height 96)
                (rl/rect! :x 0 :y 0 :width W :height 62 :color (rl/rgba 0 0 0 150))
                (rl/text! (format "two samplers   ·   mix %.2f   ·   texture0 = checker, uOther = gradient"
                                  mix)
                          :x 14 :y 12 :size 18 :color rl/RAYWHITE)
                (rl/text! (if over?
                            "move the mouse to blend   ·   SetShaderValueTexture feeds the second sampler"
                            (if touched?
                              "pointer is off-window - LEFT/RIGHT blend"
                              "pointer is off-window - sweeping the mix   ·   LEFT/RIGHT to take over"))
                          :x 14 :y 38 :size 14 :color rl/LIGHTGRAY)
                (rl/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) keyed touched?))))
          (finally
            (rl/unload-texture! tex-a)
            (rl/unload-texture! tex-b)
            (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
