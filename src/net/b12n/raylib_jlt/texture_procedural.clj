(ns net.b12n.raylib-jlt.texture-procedural
  "raylib [textures] example - procedural textures (`jolt -M:texture-procedural`).

  Four textures generated pixel by pixel and uploaded to the GPU: a checkerboard,
  a two-axis gradient, white noise, and concentric rings. SPACE regenerates the
  noise, which re-uploads that one texture in place.

  This is the closest jolt gets to raylib's [textures] category. LoadTexture and
  LoadImage return structs by value and so have no binding (see raylib.clj), but
  rlgl's layer underneath them is entirely scalar: rl/texture-from-fn builds the
  RGBA8 buffer in native memory and hands the pointer to rlLoadTexture, and
  rl/texture! draws it as an rlgl quad. Nothing is read off disk, so every texel
  here comes from a Clojure function of (x, y)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TEX 128)              ; each texture is TEX x TEX texels
(def ^:const CELL 165)             ; drawn at CELL x CELL on screen

(defn- checker
  [x y]
  (if (even? (+ (quot x 16) (quot y 16)))
    (rl/rgba 40 44 52 255)
    (rl/rgba 230 232 238 255)))

(defn- gradient
  [x y]
  (rl/rgba (int (* 255 (/ x (double TEX))))
           (int (* 255 (/ y (double TEX))))
           140
           255))

(defn- noise
  [_ _]
  (let [v (rl/get-random-value 0 255)]
    (rl/rgba v v v 255)))

(defn- rings
  [x y]
  (let [c (/ TEX 2.0)
        d (Math/sqrt (+ (* (- x c) (- x c)) (* (- y c) (- y c))))
        t (Math/sin (/ d 6.0))
        v (int (* 127 (+ 1.0 t)))]
    (rl/rgba v (int (* 0.4 v)) (- 255 v) 255)))

(def ^:private panels
  [["checkerboard" checker]
   ["gradient" gradient]
   ["noise" noise]
   ["rings" rings]])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - procedural textures"})
  (rl/set-target-fps 60)
  ;; Textures need the GL context, so they can only be built after the window
  ;; exists, the same rule raylib documents for LoadTexture.
  (let [deadline (rl/auto-quit-deadline)
        ids (mapv (fn [[_ f]] (rl/texture-from-fn TEX TEX f)) panels)
        noise-id (nth ids 2)
        xs (mapv (fn [i] (+ 40 (* i (+ CELL 20)))) (range 4))
        top 120]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (when (rl/key-pressed? rl/KEY-SPACE)
          ;; Regenerating uploads a fresh buffer over the same id, so the quad
          ;; below keeps drawing without knowing anything changed.
          (rl/update-texture-from-fn! noise-id TEX TEX noise))
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/text! "procedural textures: every texel from a fn of (x, y)"
                  {:x 40
                   :y 40
                   :size 20
                   :color rl/DARKGRAY})
        (rl/text! (str TEX "x" TEX " RGBA8, uploaded with rlLoadTexture")
                  {:x 40
                   :y 70
                   :size 16
                   :color rl/GRAY})
        (dotimes [i 4]
          (let [x (nth xs i)]
            (rl/texture! (nth ids i) {:x x
                                      :y top
                                      :width CELL
                                      :height CELL})
            (rl/rect-lines! {:x x
                             :y top
                             :width CELL
                             :height CELL
                             :color rl/LIGHTGRAY})
            (rl/text! (first (nth panels i))
                      {:x x
                       :y (+ top CELL 8)
                       :size 16
                       :color rl/DARKGRAY})))
        (rl/text! "SPACE reseeds the noise" {:x 40
                                             :y (- H 34)
                                             :size 16
                                             :color rl/GRAY})
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame))))
    (doseq [id ids] (rl/unload-texture! id)))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
