(ns net.b12n.raylib-jlt.bunnymark
  "raylib [textures] example - bunnymark (`jolt -M:bunnymark`).

  The traditional sprite-count benchmark: hold the left mouse button to spawn
  bunnies, each bouncing off the window edges with its own velocity and tint.
  The readout tracks how many are on screen and what that does to the frame rate.
  SPACE clears them.

  raylib's version loads wabbit_alpha.png; there is no image loader here (see
  raylib.clj on why LoadTexture has no binding), so the sprite is drawn into an
  RGBA buffer by hand and uploaded with rlLoadTexture. Every bunny is then one
  rl/texture! quad, which rlgl batches into the same draw call as long as they
  all share the texture."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SPRITE 32)
(def ^:const BATCH 60)             ; bunnies added per frame while held
(def ^:const MAX-BUNNIES 40000)

(defn- bunny-texel
  "A blocky white rabbit on a transparent ground: two ears, a head, an eye. Alpha
  0 outside the silhouette is what lets the tint show one bunny over another."
  [x y]
  (let [cx (/ SPRITE 2.0)
        head-y 20.0
        dx (- x cx)
        dy (- y head-y)
        sy (* 1.15 dy)
        head? (< (+ (* dx dx) (* sy sy)) 81)
        ear? (and (< 4 y 18)
                  (or (< 8 x 13) (< 19 x 24)))
        eye? (and (< 17 y 21) (or (< 11 x 14) (< 18 x 21)))]
    (cond
      eye? (rl/rgba 20 20 30 255)
      (or head? ear?) (rl/rgba 255 255 255 255)
      :else (rl/rgba 0 0 0 0))))

(defn- spawn
  "n bunnies at (mx, my). Pass :scattered to seed them across the whole window
  instead, which is what the opening batch wants - a pile in the middle takes a
  few seconds to disperse into anything worth looking at."
  [n mx my]
  (mapv (fn [_]
          {:x (if (= mx :scattered) (double (rl/get-random-value 0 (- W SPRITE))) (double mx))
           :y (if (= mx :scattered) (double (rl/get-random-value 0 (- H SPRITE))) (double my))
           :vx (/ (rl/get-random-value -250 250) 60.0)
           :vy (/ (rl/get-random-value -250 250) 60.0)
           :color (rl/rgba (rl/get-random-value 90 255)
                           (rl/get-random-value 90 255)
                           (rl/get-random-value 90 255)
                           255)})
        (range n)))

(defn- step
  [{:keys [x y vx vy]
    :as b}]
  (let [x (+ x vx)
        y (+ y vy)
        vx (if (or (< x 0) (> (+ x SPRITE) W)) (- vx) vx)
        vy (if (or (< y 0) (> (+ y SPRITE) H)) (- vy) vy)]
    (assoc b :x x :y y :vx vx :vy vy)))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [textures] example - bunnymark")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        id (rl/texture-from-fn SPRITE SPRITE bunny-texel)]
    (loop [frame 0
           bunnies (spawn 200 :scattered :scattered)]
      (if-not (rl/keep-running? deadline)
        (rl/unload-texture! id)
        (let [bunnies (cond
                        (rl/key-pressed? rl/KEY-SPACE) []
                        (and (rl/mouse-down? rl/MOUSE-LEFT)
                             (< (count bunnies) MAX-BUNNIES))
                        (into bunnies (spawn BATCH (rl/get-mouse-x) (rl/get-mouse-y)))
                        :else bunnies)
              bunnies (mapv step bunnies)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [{:keys [x y color]} bunnies]
            (rl/texture! id :x (int x) :y (int y)
                         :width SPRITE :height SPRITE :tint color))
          (rl/rect! :x 0 :y 0 :width W :height 46 :color (rl/rgba 20 24 34 220))
          (rl/text! (str (count bunnies) " bunnies") :x 12 :y 8 :size 20 :color rl/RAYWHITE)
          (rl/fps! :x 12 :y 28)
          (rl/text! "hold the left mouse button to add - SPACE clears"
                    :x 170 :y 14 :size 16 :color rl/LIGHTGRAY)
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) bunnies)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
