(ns net.b12n.raylib-jlt.image-kernel
  "raylib [textures] example - image kernel (`jolt -M:image-kernel`).

  Port of raylib's examples/textures/textures_image_kernel.c. The same picture
  four times: untouched, sharpened, run through a Sobel operator, and blurred by
  six passes of a Gaussian. Every one of them is `ImageKernelConvolution`, the
  same call with a different nine numbers.

  New FFI: `ImageKernelConvolution` and `ImageCrop`, both in place on an
  `Image *` like the colour operations, except that crop's `Rectangle` is by
  value, four floats in 16 bytes. Worth noting that the convolution takes the
  kernel's COUNT rather than its side length, so a 3x3 is passed as 9.

  Kernels are normalised first, dividing by the sum of their entries, which is
  what keeps the sharpen and Gaussian results at the same overall brightness as
  the source. Sobel's entries sum to zero, so it is left alone, and the C's own
  normaliser has the same guard.

  That zero sum has a consequence worth knowing about, because it is invisible
  until it bites. raylib convolves the ALPHA channel along with the colours and,
  unlike the colours, does not clamp the result, so a zero-sum kernel drives
  alpha to zero across every flat region: the Sobel edges are computed correctly
  and then drawn completely transparent. Measured here, not guessed, by looking
  at the panel and finding it blank. Dropping the image to a format with no
  alpha channel and converting back is the cheapest way to say opaque again.

  The C opens `resources/cat.png`. This suite ships no image files, so the
  source is a test card drawn with `texture-from-fn` and pulled back off the GPU
  with `image-from-texture!`. It is built for this job: concentric rings at the
  top for curved edges, a fine checker in the middle that only survives the
  sharpen, and a smooth gradient at the bottom that only the Sobel leaves blank."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PANEL 200)

(defn- normalize-kernel
  "Divide by the sum of the entries, unless that sum is zero. A zero-sum kernel
  like Sobel is a difference operator and scaling it would only dim the edges."
  [k]
  (let [s (reduce + 0.0 k)]
    (if (zero? s) k (mapv (fn [v] (/ v s)) k))))

(def ^:private gaussian (normalize-kernel [1.0 2.0 1.0 2.0 4.0 2.0 1.0 2.0 1.0]))
(def ^:private sobel (normalize-kernel [1.0 0.0 -1.0 2.0 0.0 -2.0 1.0 0.0 -1.0]))
(def ^:private sharpen (normalize-kernel [0.0 -1.0 0.0 -1.0 5.0 -1.0 0.0 -1.0 0.0]))

(defn- test-card
  "Three bands chosen to show what each kernel does: rings, then a fine checker,
  then a smooth ramp."
  [x y]
  (let [third (/ H 3.0)]
    (cond
      (< y third)
      (let [dx (- x (/ PANEL 2.0))
            dy (- y (/ third 2.0))
            d (Math/sqrt (+ (* dx dx) (* dy dy)))
            band (mod (int (/ d 9.0)) 2)]
        (if (zero? band)
          (rl/rgba 240 90 60 255)
          (rl/rgba 40 50 90 255)))

      (< y (* 2 third))
      (if (zero? (mod (+ (quot x 4) (quot (int (- y third)) 4)) 2))
        (rl/rgba 245 245 245 255)
        (rl/rgba 30 30 30 255))

      :else
      (let [t (/ (- y (* 2 third)) third)]
        (rl/rgba (int (* 250 t)) (int (* 200 (- 1.0 t))) 160 255)))))

(defn- convolved
  "A copy of `img` run through `kernel` `passes` times, cropped to one panel and
  uploaded. The copy is released once the GPU has the pixels."
  [img kernel passes]
  (let [c (rl/image-copy! img)]
    (dotimes [_ passes]
      (rl/image-convolve! c kernel))
    (rl/image-crop! c {:x 0
                       :y 0
                       :width PANEL
                       :height H})
    ;; raylib convolves the ALPHA channel along with the colours, and unlike the
    ;; colours it does not clamp the result. A zero-sum kernel therefore drives
    ;; alpha to zero everywhere the image is flat, which is every interior pixel
    ;; of an opaque picture: the Sobel edges are computed correctly and then
    ;; drawn completely transparent. Dropping to a format with no alpha channel
    ;; and coming back is the cheapest way to say "opaque again", and it costs
    ;; the other three panels nothing, since their kernels sum to one and their
    ;; alpha was already 255.
    (rl/image-format! c rl/PIXELFORMAT-R8G8B8)
    (rl/image-format! c rl/PIXELFORMAT-R8G8B8A8)
    (let [id (rl/image->texture c)]
      (rl/unload-image! c)
      id)))

(def ^:private captions ["source" "sharpen" "sobel" "gaussian x6"])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - image kernel"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        seed (rl/texture-from-fn PANEL H test-card)
        img (rl/image-from-texture! seed PANEL H)
        panels [(convolved img gaussian 0)
                (convolved img sharpen 1)
                (convolved img sobel 1)
                (convolved img gaussian 6)]]
    (loop [frame 0]
      (if-not (rl/keep-running? deadline)
        (do (doseq [id panels] (rl/unload-texture! id))
            (rl/unload-texture! seed)
            (rl/unload-image! img))
        (do
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [[i id] (map-indexed vector panels)]
            (rl/texture! id {:x (* i PANEL)
                             :y 0
                             :width PANEL
                             :height H})
            (rl/line! {:x1 (* i PANEL)
                       :y1 0
                       :x2 (* i PANEL)
                       :y2 H
                       :color rl/RAYWHITE})
            (rl/rect! {:x (* i PANEL)
                       :y (- H 24)
                       :width PANEL
                       :height 24
                       :color (rl/rgba 0 0 0 150)})
            (rl/text! (nth captions i) {:x (+ (* i PANEL) 10)
                                        :y (- H 18)
                                        :size 10
                                        :color rl/RAYWHITE}))
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
