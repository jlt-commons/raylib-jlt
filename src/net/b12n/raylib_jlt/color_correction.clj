(ns net.b12n.raylib-jlt.color-correction
  "raylib [shaders] example - color correction.

  A post-process fragment shader adjusts contrast, saturation and
  brightness of a picture in real time. Press 1-4 to switch picture; hold
  Q/W contrast, A/S saturation, Z/X brightness; R resets.

  No new FFI: the same uniform-loc / set-uniform-float! / with-shader
  every other shader example uses. The shader's grading math (contrast
  around the midpoint, additive brightness, luminance-weighted
  saturation) is the standard technique, the same formula raylib's own
  color_correction.fs uses, since there isn't really another way to
  define what those three sliders mean. The four pictures are
  procedurally generated with rl/texture-from-fn rather than the C
  example's parrots/cat/mandrill/fudesumi PNGs, matching this suite's
  no-external-assets convention.
  Loosely based on raylib/examples/shaders/shaders_color_correction.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PIC 300)
(def ^:const PIC-X (- 290 (quot PIC 2)))
(def ^:const PIC-Y (- 225 (quot PIC 2)))

(def ^:private fragment-shader "
#version 330
in vec2 fragTexCoord;
in vec4 fragColor;
out vec4 finalColor;

uniform sampler2D texture0;
uniform float contrast;
uniform float saturation;
uniform float brightness;

void main() {
  vec4 texel = texture(texture0, fragTexCoord);

  texel.rgb = (texel.rgb - 0.5) * (contrast / 100.0 + 1.0) + 0.5;
  texel.rgb = texel.rgb + brightness / 100.0;

  float intensity = dot(texel.rgb, vec3(0.299, 0.587, 0.114));
  texel.rgb = (texel.rgb - intensity) * (saturation / 100.0) + texel.rgb;

  finalColor = texel * fragColor;
}")

(defn- hue->rgb
  [h]
  (let [h' (/ (mod h 360.0) 60.0)
        i (int (Math/floor h'))
        f (- h' i)
        q (- 1.0 f)
        [r g b] (cond
                  (= i 0) [1.0 f 0.0]
                  (= i 1) [q 1.0 0.0]
                  (= i 2) [0.0 1.0 f]
                  (= i 3) [0.0 q 1.0]
                  (= i 4) [f 0.0 1.0]
                  :else [1.0 0.0 q])]
    (rl/rgba (int (* 255 r)) (int (* 255 g)) (int (* 255 b)) 255)))

(defn- hash-rgb
  [n]
  (let [h (bit-and (* (+ n 12345) 2654435761) 0xffffffff)]
    (rl/rgba (bit-and h 0xff) (bit-and (bit-shift-right h 8) 0xff)
             (bit-and (bit-shift-right h 16) 0xff) 255)))

(defn- pic-hue-wheel
  [x y]
  (let [c (/ PIC 2.0)]
    (hue->rgb (Math/toDegrees (Math/atan2 (- y c) (- x c))))))

(defn- pic-checker
  [x y]
  (let [cs 30
        cxi (quot x cs)
        cyi (quot y cs)]
    (if (even? (+ cxi cyi))
      (hue->rgb (mod (* (+ cxi cyi) 47.0) 360.0))
      (rl/rgba 20 20 30 255))))

(defn- pic-rings
  [x y]
  (let [c (/ PIC 2.0)
        d (Math/sqrt (+ (Math/pow (- x c) 2) (Math/pow (- y c) 2)))
        v (int (* 255 (mod (/ d 14.0) 1.0)))]
    (rl/rgba v (int (* 0.6 v)) (- 255 v) 255)))

(defn- pic-mosaic
  [x y]
  (let [cs 18]
    (hash-rgb (+ (* (quot x cs) 928371) (* (quot y cs) 123457)))))

(def ^:private PICTURES [pic-hue-wheel pic-checker pic-rings pic-mosaic])

(defn- adj
  [v dn up step lo hi]
  (let [v (if dn (- v step) v)
        v (if up (+ v step) v)]
    (max lo (min hi v))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - color correction"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        pictures (mapv (fn [f] (rl/texture-from-fn PIC PIC f)) PICTURES)
        sh (rl/shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "color-correction: the fragment shader did not link (log above)"))
      (let [loc-contrast (rl/uniform-loc sh "contrast")
            loc-saturation (rl/uniform-loc sh "saturation")
            loc-brightness (rl/uniform-loc sh "brightness")]
        (try
          (loop [frame 0 idx 0 contrast 0.0 saturation 0.0 brightness 0.0]
            (when (app/keep-running? deadline)
              (let [idx (cond (rl/key-pressed? rl/KEY-ONE) 0
                              (rl/key-pressed? rl/KEY-TWO) 1
                              (rl/key-pressed? rl/KEY-THREE) 2
                              (rl/key-pressed? rl/KEY-FOUR) 3
                              :else idx)
                    reset? (rl/key-pressed? rl/KEY-R)
                    contrast (if reset? 0.0
                                 (adj contrast (rl/key-down? rl/KEY-Q) (rl/key-down? rl/KEY-W)
                                      1.5 -100.0 100.0))
                    saturation (if reset? 0.0
                                   (adj saturation (rl/key-down? rl/KEY-A) (rl/key-down? rl/KEY-S)
                                        1.5 -100.0 100.0))
                    brightness (if reset? 0.0
                                   (adj brightness (rl/key-down? rl/KEY-Z) (rl/key-down? rl/KEY-X)
                                        1.5 -100.0 100.0))]
                (rl/set-uniform-float! sh loc-contrast contrast)
                (rl/set-uniform-float! sh loc-saturation saturation)
                (rl/set-uniform-float! sh loc-brightness brightness)

                (rl/begin-drawing)
                (rl/clear-background rl/RAYWHITE)
                (rl/with-shader
                  sh
                  (fn []
                    (rl/texture! (nth pictures idx) {:x PIC-X
                                                     :y PIC-Y
                                                     :width PIC
                                                     :height PIC})))

                (rl/line! {:x1 580
                           :y1 0
                           :x2 580
                           :y2 H
                           :color (rl/rgba 218 218 218 255)})
                (rl/rect! {:x 580
                           :y 0
                           :width (- W 580)
                           :height H
                           :color (rl/rgba 232 232 232 255)})

                (rl/text! "Color Correction" {:x 585
                                              :y 40
                                              :size 20
                                              :color rl/GRAY})
                (rl/text! "Press [1] - [4] to change picture" {:x 588
                                                               :y 75
                                                               :size 8
                                                               :color rl/GRAY})
                (rl/text! (str "Contrast [Q/W]:   " (int contrast))
                          {:x 588
                           :y 110
                           :size 10
                           :color rl/DARKGRAY})
                (rl/text! (str "Saturation [A/S]: " (int saturation))
                          {:x 588
                           :y 135
                           :size 10
                           :color rl/DARKGRAY})
                (rl/text! (str "Brightness [Z/X]: " (int brightness))
                          {:x 588
                           :y 160
                           :size 10
                           :color rl/DARKGRAY})
                (rl/text! "Press [R] to reset values" {:x 588
                                                       :y 195
                                                       :size 8
                                                       :color rl/GRAY})

                (app/maybe-screenshot! frame 5)
                (rl/end-drawing)
                (recur (inc frame) idx contrast saturation brightness))))
          (finally (rl/unload-shader! sh)))))
    (doseq [id pictures] (rl/unload-texture! id)))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
