(ns net.b12n.raylib-jlt.blend-modes
  "raylib [textures] example - blend modes.

  A glowing cluster of colored blobs drawn over a night skyline through
  the four 2D blend modes (ALPHA / ADDITIVE / MULTIPLIED / ADD_COLORS).
  SPACE cycles the mode.

  No new FFI: rl/begin-blend-mode, rl/end-blend-mode and the BLEND-*
  constants already exist (see particles-blending.clj); this is the
  second example to use them, and the first to show all four side by
  side. The skyline and the glow blobs are both procedurally generated
  with rl/texture-from-fn rather than the C example's
  cyberpunk_street_{background,foreground}.png, matching this suite's
  no-external-assets convention. The glow blobs sit on fully transparent
  black (alpha 0, rgb 0), which is why MULTIPLIED looks so different
  from the other three here: it ignores alpha and multiplies by that
  black everywhere the blobs don't reach, the real (if sometimes
  surprising) behavior of that mode.
  Loosely based on raylib/examples/textures/textures_blend_modes.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TEX-W 400)
(def ^:const TEX-H 225)
(def ^:const MODES [[rl/BLEND-ALPHA "BLEND_ALPHA"]
                    [rl/BLEND-ADDITIVE "BLEND_ADDITIVE"]
                    [rl/BLEND-MULTIPLIED "BLEND_MULTIPLIED"]
                    [rl/BLEND-ADD-COLORS "BLEND_ADD_COLORS"]])

(defn- lerp
  [a b t]
  (+ a (* (- b a) t)))

(defn- hash01
  "Deterministic pseudo-random in [0,1) from an int seed, so the skyline is
  the same texture every run."
  [n]
  (let [h (bit-and (* (+ n 12345) 2654435761) 0xffffffff)]
    (/ (double (bit-and h 0xffff)) 65536.0)))

(defn- sky-pixel
  "A vertical gradient sky, a row of buildings of varying height below the
  horizon, and a scatter of lit windows inside each one."
  [x y]
  (let [t (/ y (double TEX-H))
        r (int (lerp 18 58 t))
        g (int (lerp 12 24 t))
        b (int (lerp 42 74 t))
        horizon (int (* TEX-H 0.68))]
    (if (< y horizon)
      (rl/rgba r g b 255)
      (let [col (quot x 22)
            span (- TEX-H horizon)
            top (int (+ horizon (* span 0.5 (hash01 col))))]
        (if (< y top)
          (rl/rgba r g b 255)
          (let [lx (mod x 22)
                window? (and (> lx 4) (< lx 18)
                             (zero? (mod (+ (quot (- y top) 10) (quot lx 6)) 3))
                             (< (hash01 (+ (* col 977) (quot y 10))) 0.4))]
            (if window?
              (rl/rgba 255 214 120 255)
              (rl/rgba 14 10 22 255))))))))

(def ^:private BLOBS
  [[120.0 110.0 70.0 [0 255 255]]
   [220.0 150.0 60.0 [255 0 255]]
   [170.0 170.0 55.0 [255 230 0]]])

(defn- glow-pixel
  "Three soft-edged colored glows, summed so their overlaps read brighter.
  Fully transparent (alpha 0) and black outside every blob's radius."
  [x y]
  (loop [i 0 r 0 g 0 b 0 a 0]
    (if (>= i (count BLOBS))
      (rl/rgba (min 255 r) (min 255 g) (min 255 b) (min 255 a))
      (let [[cx cy radius [br bg bb]] (nth BLOBS i)
            d (Math/sqrt (+ (Math/pow (- x cx) 2) (Math/pow (- y cy) 2)))
            k (max 0.0 (- 1.0 (/ d radius)))]
        (recur (inc i)
               (+ r (int (* k br)))
               (+ g (int (* k bg)))
               (+ b (int (* k bb)))
               (+ a (int (* k 255))))))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - blend modes"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        bg (rl/texture-from-fn TEX-W TEX-H sky-pixel)
        fg (rl/texture-from-fn TEX-W TEX-H glow-pixel)]
    (loop [frame 0 mode-idx 0]
      (when (rl/keep-running? deadline)
        (let [mode-idx (if (rl/key-pressed? rl/KEY-SPACE)
                         (mod (inc mode-idx) (count MODES))
                         mode-idx)
              [mode mode-name] (nth MODES mode-idx)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/texture! bg {:x 0
                           :y 0
                           :width W
                           :height H})
          (rl/begin-blend-mode mode)
          (rl/texture! fg {:x 0
                           :y 0
                           :width W
                           :height H})
          (rl/end-blend-mode)
          (rl/text! "Press SPACE to change blend modes"
                    {:x 300
                     :y 350
                     :size 10
                     :color rl/GRAY})
          (rl/text! (str "Current: " mode-name)
                    {:x (- (quot W 2) 60)
                     :y 370
                     :size 10
                     :color rl/GRAY})
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) mode-idx))))
    (rl/unload-texture! bg)
    (rl/unload-texture! fg))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
