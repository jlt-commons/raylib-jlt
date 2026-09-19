(ns net.b12n.raylib-jlt.background-scrolling
  "raylib [textures] example - background scrolling.

  Parallax scrolling: three procedurally generated skyline layers
  (background, midground, foreground) scroll left at different speeds,
  each drawn twice at 2x scale so the seam wraps.

  No new FFI: each layer is rl/texture-from-fn (a silhouette skyline,
  same technique blend-modes.clj's sky-pixel uses, one hash seed per
  layer so the three don't repeat the same shape), and the 2x scale is
  just rl/texture!'s own :width/:height, since raylib's DrawTextureEx
  scale factor is nothing more than that.
  Loosely based on raylib/examples/textures/textures_background_scrolling.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TEX-W 400)
(def ^:const TEX-H 115)

(defn- hash01
  [n]
  (let [h (bit-and (* (+ n 12345) 2654435761) 0xffffffff)]
    (/ (double (bit-and h 0xffff)) 65536.0)))

(defn- skyline-pixel
  "A silhouette skyline on a transparent sky, so a layer behind shows
  through everywhere this one has no building: `seed` picks a different
  building pattern per layer, `horizon-frac` how much of the texture
  (from the bottom) the buildings can reach -- bigger for a closer
  layer -- `col-w` the width of each building (wider + sparser for a
  closer layer, so the layers behind still peek through the gaps), and
  `building` the flat silhouette color."
  [seed horizon-frac col-w building]
  (fn [x y]
    (let [horizon (int (* TEX-H (- 1.0 horizon-frac)))]
      (if (< y horizon)
        (rl/rgba 0 0 0 0)
        (let [col (quot x col-w)
              gap? (< (mod x col-w) (int (* col-w 0.15)))
              span (- TEX-H horizon)
              top (int (+ horizon (* span 0.6 (hash01 (+ seed col)))))]
          (if (or gap? (< y top)) (rl/rgba 0 0 0 0) building))))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - background scrolling"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        background (rl/texture-from-fn TEX-W TEX-H
                                       (skyline-pixel 1 0.2 16 (rl/rgba 20 52 78 255)))
        midground (rl/texture-from-fn TEX-W TEX-H
                                      (skyline-pixel 7 0.35 26 (rl/rgba 12 34 50 255)))
        foreground (rl/texture-from-fn TEX-W TEX-H
                                       (skyline-pixel 23 0.5 44 (rl/rgba 4 12 20 255)))
        bw (* 2 TEX-W)]
    (try
      (loop [frame 0 back 0.0 mid 0.0 fore 0.0]
        (when (app/keep-running? deadline)
          (let [back (let [b (- back 0.1)] (if (<= b (- bw)) 0.0 b))
                mid (let [m (- mid 0.5)] (if (<= m (- bw)) 0.0 m))
                fore (let [f (- fore 1.0)] (if (<= f (- bw)) 0.0 f))]
            (rl/begin-drawing)
            (rl/clear-background (rl/rgba 5 44 70 255))

            (doseq [x [back (+ bw back)]]
              (rl/texture! background {:x (int x)
                                       :y (- H (* 2 TEX-H))
                                       :width bw
                                       :height (* 2 TEX-H)}))
            (doseq [x [mid (+ bw mid)]]
              (rl/texture! midground {:x (int x)
                                      :y (- H (* 2 TEX-H))
                                      :width bw
                                      :height (* 2 TEX-H)}))
            (doseq [x [fore (+ bw fore)]]
              (rl/texture! foreground {:x (int x)
                                       :y (- H (* 2 TEX-H))
                                       :width bw
                                       :height (* 2 TEX-H)}))

            (rl/text! "BACKGROUND SCROLLING & PARALLAX"
                      {:x 10
                       :y 10
                       :size 20
                       :color rl/RED})

            (app/maybe-screenshot! frame 30)
            (rl/end-drawing)
            (recur (inc frame) back mid fore))))
      (finally
        (rl/unload-texture! background)
        (rl/unload-texture! midground)
        (rl/unload-texture! foreground))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
