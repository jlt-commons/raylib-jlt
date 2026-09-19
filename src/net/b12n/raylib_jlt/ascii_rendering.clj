(ns net.b12n.raylib-jlt.ascii-rendering
  "raylib [shaders] example - ASCII rendering.

  A post-process fragment shader re-renders the scene as ASCII glyphs: each
  cell samples one texel, turns it into a greyscale value, and picks one of
  eight 5x5 bitmap characters by brightness (the character's dots are just
  bits of an int, unpacked per-pixel in the shader). Two shapes are drawn
  into a render texture (one bouncing), then the whole texture is drawn back
  through the ascii shader. LEFT/RIGHT change the glyph cell size (9..15).

  Same postprocess-over-RenderTexture pipeline as `custom-uniform`: draw the
  2D scene into a render texture, then draw its texture back once through
  `with-shader`, y-flipped (`:v0 1.0 :v1 0.0`) for the FBO's bottom-up
  convention. Zero new FFI.
  Based on raylib/examples/shaders/shaders_ascii_rendering.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private fragment-shader "
#version 330

in vec2 fragTexCoord;
out vec4 finalColor;

uniform sampler2D texture0;
uniform vec2 resolution;
uniform float fontSize;

float GreyScale(in vec3 col) {
  return dot(col, vec3(0.2126, 0.7152, 0.0722));
}

float GetCharacter(int n, vec2 p) {
  p = floor(p * vec2(-4.0, 4.0) + 2.5);
  if (clamp(p.x, 0.0, 4.0) == p.x && clamp(p.y, 0.0, 4.0) == p.y) {
    int a = int(round(p.x) + 5.0 * round(p.y));
    if (((n >> a) & 1) == 1) {
      return 1.0;
    }
  }
  return 0.0;
}

void main() {
  vec2 charPixelSize = vec2(fontSize, fontSize);
  vec2 uvCellSize = charPixelSize / resolution;

  vec2 cellUV = floor(fragTexCoord / uvCellSize) * uvCellSize;
  vec3 cellColor = texture(texture0, cellUV).rgb;

  float gray = GreyScale(cellColor);

  int n = 4096;
  if (gray > 0.2) n = 65600;
  if (gray > 0.3) n = 18725316;
  if (gray > 0.4) n = 15255086;
  if (gray > 0.5) n = 13121101;
  if (gray > 0.6) n = 15252014;
  if (gray > 0.7) n = 13195790;
  if (gray > 0.8) n = 11512810;

  vec2 localUV = (fragTexCoord - cellUV) / uvCellSize;
  vec2 p = localUV * 2.0 - 1.0;

  vec3 color = cellColor * GetCharacter(n, p);
  finalColor = vec4(color, 1.0);
}")

(defn- draw-scene!
  "A fixed shape plus a bouncing one, bright enough on a white ground that the
  ascii pass has real greyscale range to work with."
  [cx]
  (rl/clear-background rl/WHITE)
  (rl/circle! {:x 640
               :y 120
               :radius 60
               :color (rl/rgba 40 44 60 255)})
  (rl/rect! {:x 590
             :y 150
             :width 100
             :height 140
             :color (rl/rgba 40 44 60 255)})
  (rl/circle! {:x (int cx)
               :y 260
               :radius 42
               :color (rl/rgba 200 60 60 255)})
  (rl/rect! {:x (int (- cx 30))
             :y 300
             :width 60
             :height 80
             :color (rl/rgba 200 60 60 255)}))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - ascii rendering"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader fragment-shader)
        rt (rl/render-texture W H)]
    (cond
      (nil? sh) (binding [*out* *err*]
                  (println "ascii-rendering: the fragment shader did not link (log above)"))
      (nil? rt) (binding [*out* *err*]
                  (println "ascii-rendering: the framebuffer is incomplete"))
      :else
      (let [loc-resolution (rl/uniform-loc sh "resolution")
            loc-fontsize (rl/uniform-loc sh "fontSize")]
        (rl/set-uniform-vec2! sh loc-resolution (double W) (double H))
        (try
          (loop [frame 0
                 cx 40.0
                 speed 1.0
                 font-size 9.0]
            (when (app/keep-running? deadline)
              (let [cx (+ cx speed)
                    speed (if (or (> cx 200.0) (< cx 40.0)) (* speed -1.0) speed)
                    font-size (cond
                                (and (rl/key-pressed? rl/KEY-LEFT) (> font-size 9.0)) (dec font-size)
                                (and (rl/key-pressed? rl/KEY-RIGHT) (< font-size 15.0)) (inc font-size)
                                :else font-size)]
                (rl/set-uniform-float! sh loc-fontsize font-size)
                (rl/with-render-texture rt (fn [] (draw-scene! cx)))

                (rl/begin-drawing)
                (rl/clear-background rl/RAYWHITE)
                (rl/with-shader sh
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
                           :height 40
                           :color rl/BLACK})
                (rl/text! (str "Ascii effect - FontSize:" (int font-size) " - [Left] -1 [Right] +1")
                          {:x 120
                           :y 10
                           :size 20
                           :color rl/LIGHTGRAY})

                (app/maybe-screenshot! frame 10)
                (rl/end-drawing)
                (recur (inc frame) cx speed font-size))))
          (finally
            (rl/unload-render-texture! rt)
            (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
