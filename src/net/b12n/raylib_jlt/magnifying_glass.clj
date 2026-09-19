(ns net.b12n.raylib-jlt.magnifying-glass
  "raylib [textures] example - magnifying glass (`jolt -M:magnifying-glass`).

  Port of raylib's examples/textures/textures_magnifying_glass.c. Move the
  pointer and a round lens follows it, showing the scene at 3x. Some markers
  are drawn only into the lens, so the scene has things in it that cannot be
  found without moving the glass over them.

  Two departures from the C, both forced by what this suite binds.

  The C cuts the lens out of a square render target with
  BLEND_CUSTOM_SEPARATE and rlSetBlendFactorsSeparate, multiplying a circular
  mask into the target's alpha. rlSetBlendFactorsSeparate is not bound here,
  and a six-argument blend-factor call is a lot of surface to add for one
  example. The lens is drawn as a textured triangle fan instead: the disc is
  built out of the target's own texels, so there is no mask and no square to
  hide. rl/texture! cannot do this, because it emits exactly one quad.

  The C also uses a Camera2D for the zoom, which goes by pointer here and is
  wrong on x86-64 anyway. The magnified pass just takes an offset and a scale
  instead, chosen so the point under the pointer lands in the middle of the
  target: ox = size/2 - mouse-x * zoom.

  A framebuffer texture is stored bottom-up, so the fan's v coordinates are
  flipped, the same wrinkle net.b12n.raylib-jlt.render-texture documents for
  the axis-aligned case."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const LENS 220)
(def ^:const ZOOM 3.0)
(def ^:const SEGMENTS 64)

;; markers the naked eye never sees: drawn only on the magnified pass
(def hidden-spots
  [[170 150] [560 120] [300 330] [660 300] [430 210]])

(defn- draw-scene!
  "The scene, at `zoom` with its origin moved to (`ox`,`oy`). The unmagnified
  pass calls this with 0 0 1.0, the lens with an offset that puts the pointer
  in the middle of the target. `reveal?` adds the hidden markers."
  [backdrop ox oy zoom reveal?]
  (let [sx (fn [x] (+ ox (* x zoom)))
        sy (fn [y] (+ oy (* y zoom)))]
    (rl/texture! backdrop {:x ox
                           :y oy
                           :width (* W zoom)
                           :height (* H zoom)})
    (dotimes [i 7]
      (let [x (+ 60 (* i 100))]
        (rl/rect! {:x (sx x)
                   :y (sy 90)
                   :width (* 56 zoom)
                   :height (* 56 zoom)
                   :color (rl/rgba (+ 40 (* i 25)) 90 (- 210 (* i 20)) 255)})
        (rl/circle! {:x (sx (+ x 28))
                     :y (sy 300)
                     :radius (* 22 zoom)
                     :color (rl/rgba 230 (+ 60 (* i 20)) 70 255)})))
    (when reveal?
      (doseq [[x y] hidden-spots]
        (rl/circle! {:x (sx x)
                     :y (sy y)
                     :radius (* 11 zoom)
                     :color rl/LIME})
        (rl/circle! {:x (sx x)
                     :y (sy y)
                     :radius (* 6 zoom)
                     :color rl/DARKGREEN})))))

(defn- textured-disc!
  "Draw `tex` as a disc of radius `r` at (`cx`,`cy`), as a fan of triangles whose
  texcoords come off the unit circle. Vertices go in the order the second point
  of each wedge before the first, matching the winding rl/texture! uses; the
  other order renders nothing when backface culling is on. `flip-v?` is for a
  framebuffer texture, which is stored bottom-up."
  [tex cx cy r segments flip-v?]
  (let [v (fn [t] (if flip-v? (- 1.0 t) t))]
    (rl/rl-set-texture tex)
    (rl/rl-begin rl/RL-TRIANGLES)
    (rl/rl-color! rl/WHITE)
    (dotimes [i segments]
      (let [a0 (* 2.0 Math/PI (/ (double i) segments))
            a1 (* 2.0 Math/PI (/ (double (inc i)) segments))
            c0 (Math/cos a0) s0 (Math/sin a0)
            c1 (Math/cos a1) s1 (Math/sin a1)]
        (rl/rl-tex-coord-2f 0.5 (v 0.5))
        (rl/rl-vertex-2f (double cx) (double cy))
        (rl/rl-tex-coord-2f (+ 0.5 (* 0.5 c1)) (v (+ 0.5 (* 0.5 s1))))
        (rl/rl-vertex-2f (+ cx (* r c1)) (+ cy (* r s1)))
        (rl/rl-tex-coord-2f (+ 0.5 (* 0.5 c0)) (v (+ 0.5 (* 0.5 s0))))
        (rl/rl-vertex-2f (+ cx (* r c0)) (+ cy (* r s0)))))
    (rl/rl-end)
    (rl/rl-set-texture 0)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - magnifying glass"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        backdrop (rl/image-perlin-noise W H 0 0 6.0)
        rt (rl/render-texture LENS LENS)]
    (loop [frame 0
           start-x (rl/get-mouse-x)
           start-y (rl/get-mouse-y)
           moved-before? false]
      (if-not (app/keep-running? deadline)
        (do (rl/unload-texture! backdrop)
            (when rt (rl/unload-render-texture! rt)))
        (let [raw-x (rl/get-mouse-x)
              raw-y (rl/get-mouse-y)
              ;; Until the pointer actually moves, walk the lens along a slow
              ;; path of its own. raylib reports the last known position, which
              ;; is (0,0) before the pointer has ever entered the window, and a
              ;; lens parked in the corner shows neither the scene nor whether
              ;; the fan renders at all.
              moved? (or moved-before? (not= [raw-x raw-y] [start-x start-y]))
              t (* frame 0.02)
              mx (if moved? raw-x (+ (/ W 2.0) (* 250.0 (Math/sin t))))
              my (if moved? raw-y (+ (/ H 2.0) (* 120.0 (Math/sin (* t 1.7)))))
              half (/ LENS 2.0)]
          (rl/begin-drawing)
          ;; the magnified pass first, before anything touches the window
          (when rt
            (rl/with-render-texture
              rt
              (fn []
                (rl/clear-background rl/RAYWHITE)
                (draw-scene! backdrop
                             (- half (* mx ZOOM))
                             (- half (* my ZOOM))
                             ZOOM
                             true))))
          (rl/clear-background rl/RAYWHITE)
          (draw-scene! backdrop 0 0 1.0 false)
          (when rt
            (textured-disc! (:texture rt) mx my half SEGMENTS true)
            (rl/ring! {:cx mx
                       :cy my
                       :inner (- half 4)
                       :outer half
                       :start-deg 0
                       :end-deg 360
                       :segments SEGMENTS
                       :color rl/BLACK}))
          (rl/text! (if moved?
                      "five markers are drawn only inside the lens"
                      "move the pointer to take over the lens")
                    {:x 20
                     :y 16
                     :size 18
                     :color rl/BLACK})
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) start-x start-y moved?))))))
