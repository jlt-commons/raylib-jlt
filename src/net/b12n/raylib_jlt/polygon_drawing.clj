(ns net.b12n.raylib-jlt.polygon-drawing
  "raylib [textures] example - polygon drawing.

  A hue-wheel texture mapped onto a spinning 10-sided polygon, drawn as a
  triangle fan via rlSetTexture/rlBegin/rlTexCoord2f/rlVertex2f, the same
  reimplementation of DrawTexturePoly raylib's own C example uses. Q quits.

  No new FFI: the low-level rlgl calls this needs (rl-set-texture, rl-begin,
  rl-tex-coord-2f, rl-vertex-2f, rl-end) are already public, the same ones
  rl/texture! is built from. The texture is a procedurally generated hue
  wheel via rl/texture-from-fn rather than the C example's cat.png,
  matching this suite's no-external-assets convention (see bunnymark,
  particles, doom).
  Ported from raylib's examples/textures/textures_polygon_drawing.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CX 400.0)
(def ^:const CY 225.0)
(def ^:const TEX 256)
(def ^:const DEG2RAD 0.0174532925)

;; texture coords mapping the texture onto the poly, last closes the loop
(def ^:const TEXCOORDS
  [[0.75 0.0] [0.25 0.0] [0.0 0.5] [0.0 0.75] [0.25 1.0]
   [0.375 0.875] [0.625 0.875] [0.75 1.0] [1.0 0.75] [1.0 0.5] [0.75 0.0]])

;; base poly vertices derived from the UVs: (uv - 0.5) * 256
(def ^:const POINTS
  (mapv (fn [[u v]] [(* (- u 0.5) 256.0) (* (- v 0.5) 256.0)]) TEXCOORDS))

(defn- hsv->color
  "HSV -> packed Color for s=1, v=1. h in degrees (wrapped)."
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

(defn- wheel-pixel
  "Angle around the texture's centre picked out as hue, so the rotation of
  the finished polygon is unmistakable."
  [x y]
  (let [cx (/ TEX 2.0)
        cy (/ TEX 2.0)
        dx (- x cx)
        dy (- y cy)]
    (hsv->color (Math/toDegrees (Math/atan2 dy dx)))))

(defn- rotate
  [x y rad]
  [(- (* x (Math/cos rad)) (* y (Math/sin rad)))
   (+ (* x (Math/sin rad)) (* y (Math/cos rad)))])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - polygon drawing"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        tex (rl/texture-from-fn TEX TEX wheel-pixel)]
    (loop [frame 0 angle 0.0]
      (when (rl/keep-running? deadline)
        (let [angle (+ angle 1.0)
              rad (* angle DEG2RAD)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "textured polygon" {:x 20
                                        :y 20
                                        :size 20
                                        :color rl/DARKGRAY})
          (rl/rl-set-texture tex)
          (rl/rl-begin rl/RL-TRIANGLES)
          (rl/rl-color! rl/WHITE)
          (dotimes [i 10]
            (let [[p0x p0y] (nth POINTS i)
                  [p1x p1y] (nth POINTS (inc i))
                  [t0u t0v] (nth TEXCOORDS i)
                  [t1u t1v] (nth TEXCOORDS (inc i))
                  [r0x r0y] (rotate p0x p0y rad)
                  [r1x r1y] (rotate p1x p1y rad)]
              (rl/rl-tex-coord-2f 0.5 0.5)
              (rl/rl-vertex-2f CX CY)
              (rl/rl-tex-coord-2f t0u t0v)
              (rl/rl-vertex-2f (+ r0x CX) (+ r0y CY))
              (rl/rl-tex-coord-2f t1u t1v)
              (rl/rl-vertex-2f (+ r1x CX) (+ r1y CY))))
          (rl/rl-end)
          (rl/rl-set-texture 0)
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) angle))))
    (rl/unload-texture! tex))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
