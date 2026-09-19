(ns net.b12n.raylib-jlt.texture-tiling
  "raylib [textures] example - texture tiling (`jolt -M:texture-tiling`).

  One small procedural tile covers the whole window by asking for texture
  coordinates well past 1.0 and letting the GPU's REPEAT wrap mode do the
  repeating, so the number of tiles on screen costs nothing extra to draw. UP/DOWN
  change the tile density and the whole field scrolls diagonally.

  Scrolling is just an offset added to both texcoords, which is why the seam never
  shows: a REPEAT sampler treats 4.25 and 0.25 identically."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TEX 64)

(defn- tile
  "A tile that has to line up with itself on all four edges: a diagonal weave over
  a dark ground, with a dot in the middle so the repeat is easy to see."
  [x y]
  (let [d (mod (+ x y) 16)
        e (mod (- x y) 16)
        c (/ TEX 2.0)
        r (Math/sqrt (+ (* (- x c) (- x c)) (* (- y c) (- y c))))]
    (cond
      (< r 5) (rl/rgba 255 203 0 255)
      (< d 3) (rl/rgba 0 121 241 255)
      (< e 3) (rl/rgba 102 191 255 255)
      :else (rl/rgba 20 24 34 255))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - texture tiling"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        id (rl/texture-from-fn TEX TEX tile)]
    ;; REPEAT is what makes texcoords past 1.0 tile instead of smearing the edge
    ;; texel; texture-from-fn already sets it, but the example depends on it, so
    ;; say so out loud.
    (rl/texture-wrap! id rl/RL-TEXTURE-WRAP-REPEAT)
    (loop [frame 0
           tiles 6.0
           scroll 0.0]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! id)
        (let [tiles (cond
                      (rl/key-down? rl/KEY-UP) (min 24.0 (+ tiles 0.08))
                      (rl/key-down? rl/KEY-DOWN) (max 1.0 (- tiles 0.08))
                      :else tiles)
              scroll (+ scroll 0.004)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/texture! id
                       {:x 0
                        :y 0
                        :width W
                        :height H
                        :u0 scroll
                        :v0 scroll
                        :u1 (+ scroll tiles)
                        :v1 (+ scroll (* tiles (/ (double H) W)))})
          (rl/rect! {:x 0
                     :y 0
                     :width W
                     :height 60
                     :color (rl/rgba 0 0 0 150)})
          ;; v spans a shorter range than u so the tiles stay square, so the
          ;; count on screen is tiles wide by tiles*(H/W) tall, not tiles squared.
          (rl/text! (str "texture tiling - " (int (* tiles tiles (/ (double H) W)))
                         " tiles from one " TEX "x" TEX " texture, one quad")
                    {:x 16
                     :y 14
                     :size 18
                     :color rl/RAYWHITE})
          (rl/text! "UP/DOWN change density" {:x 16
                                              :y 36
                                              :size 14
                                              :color rl/LIGHTGRAY})
          (app/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) tiles scroll)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
