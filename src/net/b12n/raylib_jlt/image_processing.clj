(ns net.b12n.raylib-jlt.image-processing
  "raylib [textures] example - image processing (`jolt -M:image-processing`).

  Port of raylib's examples/textures/textures_image_processing.c. Nine
  operations over the same picture, picked from the list on the left with the
  mouse or the arrow keys: grayscale, tint, invert, contrast, brightness, a
  Gaussian blur, and both flips. Each one is raylib's own, running over CPU
  pixels rather than in a shader.

  Note how differently these bind from the generators next to them in
  net.b12n.raylib.images. Every processor takes `Image *` and works IN PLACE, so it is a
  plain pointer argument and the 24-byte by-value dance never arises; only
  `ImageCopy` and `LoadImageFromTexture` move whole Images across the boundary.

  `LoadImageFromTexture` is the one that makes this example possible at all. The
  C opens `resources/parrots.png`, and this suite ships no image files, so the
  source here is drawn pixel by pixel with `texture-from-fn`, pulled BACK off the
  GPU into CPU memory, and handed to raylib from there. The pattern is
  deliberately lopsided, with the bright lobe off-centre, so that a flip is
  obvious rather than something you have to take on trust.

  The processed image is rebuilt only when the selection changes. A blur over a
  256x256 image every frame would be visible in the frame time for no reason,
  and the result cannot change while the mode is held."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SRC 256)
(def ^:const LIST-X 40)
(def ^:const LIST-Y 60)
(def ^:const ROW-H 32)

(def ^:private modes
  ["original" "grayscale" "tint" "invert" "contrast"
   "brightness" "gaussian blur" "flip vertical" "flip horizontal"])

(defn- source-pixel
  "A lopsided pattern: hue sweeping along the diagonal, a bright lobe pushed
  into one corner, and a darker ring around it. Asymmetric on purpose, so both
  flips show."
  [x y]
  (let [fx (/ (double x) SRC)
        fy (/ (double y) SRC)
        dx (- fx 0.32)
        dy (- fy 0.28)
        d (Math/sqrt (+ (* dx dx) (* dy dy)))
        lobe (Math/max 0.0 (- 1.0 (* 3.2 d)))
        ring (if (and (> d 0.34) (< d 0.40)) 0.45 0.0)
        r (+ (* 235 fx) (* 20 lobe))
        g (+ (* 90 (- 1.0 fy)) (* 165 lobe) (* 120 ring))
        b (+ (* 215 fy) (* 40 lobe))]
    (rl/rgba (min 255 (int r)) (min 255 (int g)) (min 255 (int b)) 255)))

(defn- apply-mode!
  "Run one operation over a copy of `img` and upload the result. Returns the new
  texture id; the copy is released before returning, since the GPU has the
  pixels by then."
  [img mode]
  (let [c (rl/image-copy! img)]
    (case mode
      "original" nil
      "grayscale" (rl/image-color-grayscale! c)
      "tint" (rl/image-color-tint! c rl/GREEN)
      "invert" (rl/image-color-invert! c)
      "contrast" (rl/image-color-contrast! c -40)
      "brightness" (rl/image-color-brightness! c -80)
      "gaussian blur" (rl/image-blur-gaussian! c 8)
      "flip vertical" (rl/image-flip-vertical! c)
      "flip horizontal" (rl/image-flip-horizontal! c)
      nil)
    ;; Grayscale and contrast can leave a narrower pixel format behind, and the
    ;; upload wants RGBA8 either way.
    (rl/image-format! c rl/PIXELFORMAT-R8G8B8A8)
    (let [id (rl/image->texture c)]
      (rl/unload-image! c)
      id)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - image processing"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        ;; The source, authored here and then pulled back off the GPU so
        ;; raylib's CPU-side operations have something to chew on.
        seed-tex (rl/texture-from-fn SRC SRC source-pixel)
        img (rl/image-from-texture! seed-tex SRC SRC)
        n (count modes)]
    (loop [frame 0
           sel 0
           shown (apply-mode! img (nth modes 0))
           steered? false]
      (if-not (app/keep-running? deadline)
        (do (rl/unload-texture! shown)
            (rl/unload-texture! seed-tex)
            (rl/unload-image! img))
        (let [my (rl/get-mouse-y)
              row (when (and (>= my LIST-Y) (< my (+ LIST-Y (* ROW-H n))))
                    (quot (- my LIST-Y) ROW-H))
              picked (cond
                       (and row (rl/mouse-pressed? rl/MOUSE-LEFT)) row
                       (rl/key-pressed? rl/KEY-DOWN) (mod (inc sel) n)
                       (rl/key-pressed? rl/KEY-UP) (mod (dec sel) n)
                       :else nil)
              steered? (or steered? (some? picked))
              ;; Left alone it walks the list, so the example shows what it does
              ;; without anyone driving it.
              auto (when (and (not steered?) (pos? frame) (zero? (mod frame 90)))
                     (mod (inc sel) n))
              next-sel (or picked auto sel)
              changed? (not= next-sel sel)
              shown (if changed?
                      (do (rl/unload-texture! shown)
                          (apply-mode! img (nth modes next-sel)))
                      shown)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [[i label] (map-indexed vector modes)]
            (let [y (+ LIST-Y (* ROW-H i))
                  on? (= i next-sel)]
              (rl/rect! {:x LIST-X
                         :y y
                         :width 200
                         :height (- ROW-H 4)
                         :color (if on? rl/SKYBLUE (rl/rgba 230 232 236 255))})
              (rl/rect-lines! {:x LIST-X
                               :y y
                               :width 200
                               :height (- ROW-H 4)
                               :color (if on? rl/BLUE rl/LIGHTGRAY)})
              (rl/text! label {:x (+ LIST-X 12)
                               :y (+ y 8)
                               :size 10
                               :color (if on? rl/DARKBLUE rl/GRAY)})))
          (rl/texture! shown {:x 460
                              :y 90
                              :width SRC
                              :height SRC})
          (rl/rect-lines! {:x 460
                           :y 90
                           :width SRC
                           :height SRC
                           :color rl/DARKGRAY})
          (rl/text! "raylib's own CPU-side image operations" {:x LIST-X
                                                              :y 20
                                                              :size 20
                                                              :color rl/DARKGRAY})
          (rl/text! (if steered?
                      "click a row, or UP/DOWN"
                      "walking the list until you pick one")
                    {:x LIST-X
                     :y (- H 30)
                     :size 10
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) next-sel shown steered?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
