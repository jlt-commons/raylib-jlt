(ns net.b12n.raylib-jlt.palette-switch
  "raylib [shaders] example - palette switching (`jolt -M:palette-switch`).

  The image is indices, not colours. A plasma function gives every pixel a number
  0-7, and an `ivec3 palette[8]` uniform turns that number into a colour. Changing
  the whole picture is uploading 24 ints; the pattern itself is never recomputed
  and never re-uploaded.

  This is how paletted hardware worked, and the reason palette animation was free
  on machines that could not afford to touch a framebuffer twice: cycling the
  entries scrolls colour through a static image. LEFT/RIGHT switch palettes,
  SPACE animates by rotating the entries, and the strip along the bottom shows the
  eight entries currently in force.

  It is also the one example here that sends an ARRAY uniform:
  rl/set-uniform-ivec3-array! stages 8 x 3 ints and hands them to SetShaderValueV
  with a count, where the other setters send a single value."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SLOTS 8)

(def ^:private fragment-shader "
#version 330
in vec4 fragColor;
out vec4 finalColor;

uniform vec2  uResolution;
uniform ivec3 uPalette[8];
uniform float uTime;

void main() {
  vec2 p = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y) / uResolution.y;

  // A plasma: sums of sines, which is what the effect was built from originally.
  float v = sin(p.x * 9.0 + uTime)
          + sin((p.y * 7.0 + uTime * 0.7))
          + sin((p.x + p.y) * 6.0 - uTime * 0.5)
          + sin(length(p - vec2(0.9, 0.5)) * 12.0 - uTime);

  // -4..4 -> a slot index. floor, not round: every slot gets an equal band.
  int idx = int(floor((v + 4.0) / 8.0 * float(uPalette.length())));
  idx = clamp(idx, 0, uPalette.length() - 1);

  finalColor = vec4(vec3(uPalette[idx]) / 255.0, 1.0) * fragColor;
}")

;; Eight-entry palettes, flat [r g b r g b ...]. The names are the machines or
;; formats the ramps come from, which is most of why the effect is recognisable.
(def ^:private palettes
  [["ember"    [20 12 28  60 16 32  110 26 30  160 48 24
                205 88 22  235 140 40  248 196 96  255 246 210]]
   ["ice"      [8 12 32  16 32 66  24 60 104  36 96 140
                60 136 172  104 178 202  164 214 228  226 244 250]]
   ["viridis"  [68 1 84  72 40 120  62 74 137  49 104 142
                38 130 142  53 183 121  109 205 89  253 231 37]]
   ["gameboy"  [15 56 15  27 76 32  48 98 48  72 120 60
                100 140 72  139 172 15  170 195 60  202 220 159]]
   ["grayscale" [16 16 16  48 48 48  80 80 80  112 112 112
                 144 144 144  176 176 176  208 208 208  240 240 240]]])

(defn- rotate-entries
  "Cycle the palette by n slots. Rotating the ENTRIES rather than redrawing is the
  whole trick: the indices in the image never change, so the colour appears to
  flow through a picture nothing is touching."
  [flat n]
  (let [triples (vec (partition 3 flat))
        k (mod n SLOTS)]
    (vec (mapcat identity (concat (subvec triples k) (subvec triples 0 k))))))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shaders] example - palette switching")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "palette-switch: the fragment shader did not link (log above)"))
      (let [loc-res (rl/uniform-loc sh "uResolution")
            loc-pal (rl/uniform-loc sh "uPalette")
            loc-time (rl/uniform-loc sh "uTime")]
        (rl/set-uniform-vec2! sh loc-res (double W) (double H))
        (try
          (loop [frame 0
                 pick 0
                 cycling? true]
            (when (rl/keep-running? deadline)
              (let [pick (cond
                           (rl/key-pressed? rl/KEY-RIGHT) (mod (inc pick) (count palettes))
                           (rl/key-pressed? rl/KEY-LEFT) (mod (dec pick) (count palettes))
                           :else pick)
                    cycling? (if (rl/key-pressed? rl/KEY-SPACE) (not cycling?) cycling?)
                    [pal-name base] (nth palettes pick)
                    shift (if cycling? (int (* 3.0 (rl/get-time))) 0)
                    entries (rotate-entries base shift)]
                ;; The array uniform: 8 entries of 3 ints, sent in one call.
                (rl/set-uniform-ivec3-array! sh loc-pal entries SLOTS)
                (rl/set-uniform-float! sh loc-time (rl/get-time))
                (rl/begin-drawing)
                (rl/clear-background rl/BLACK)
                (rl/with-shader sh (fn [] (rl/rect! :x 0 :y 0 :width W :height H :color rl/WHITE)))
                (rl/rect! :x 0 :y 0 :width W :height 62 :color (rl/rgba 0 0 0 160))
                (rl/text! (str "palette: " pal-name
                               (when cycling? (str "   cycling +" (mod shift SLOTS))))
                          :x 14 :y 12 :size 18 :color rl/RAYWHITE)
                (rl/text! "LEFT/RIGHT switch palette   ·   SPACE cycles the entries"
                          :x 14 :y 38 :size 14 :color rl/LIGHTGRAY)
                ;; The eight entries currently in force, so the indirection is
                ;; visible rather than implied.
                (let [sw (quot W SLOTS)]
                  (dotimes [i SLOTS]
                    (let [r (nth entries (* 3 i))
                          g (nth entries (+ 1 (* 3 i)))
                          b (nth entries (+ 2 (* 3 i)))]
                      (rl/rect! :x (* i sw) :y (- H 40) :width sw :height 40
                                :color (rl/rgba r g b 255))
                      (rl/text! (str i) :x (+ 6 (* i sw)) :y (- H 30) :size 16
                                :color (if (> (+ r g b) 380) rl/BLACK rl/RAYWHITE)))))
                (rl/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) pick cycling?))))
          (finally (rl/unload-shader! sh))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
