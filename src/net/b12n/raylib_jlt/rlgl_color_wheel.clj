(ns net.b12n.raylib-jlt.rlgl-color-wheel
  "raylib [shapes] example - rlgl color wheel (`jolt -M:rlgl-color-wheel`).

  Port of raylib's examples/shapes/shapes_rlgl_color_wheel.c. A hue wheel built
  as a triangle fan through rlgl. The wheel is a ring of triangles sharing the
  centre, each rim vertex carrying its own hue, so the GPU interpolates every
  shade between them and a few dozen triangles cover the whole spectrum.

  The mouse wheel changes how many triangles there are, which is the point: at
  eight the interpolation is visible as flat wedges, and by sixty-four the wheel
  looks continuous. UP and DOWN resize it, SPACE switches between the filled fan
  and its wireframe, and the centre brightness follows the arrow keys.

  ColorFromHSV is not bound, so hue-to-RGB is done here. It is the standard
  piecewise conversion, and at full saturation and value it reduces to walking
  the six edges of the RGB cube.

  See color-wheel for the same subject drawn with filled sectors instead, and
  rlgl-triangle for per-vertex colour on a single triangle."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MIN-TRIS 3)
(def ^:const MAX-TRIS 128)
(def ^:const TWO-PI (* 2.0 Math/PI))

(defn- hue->rgb
  "ColorFromHSV at saturation and value 1. Hue in degrees, wrapped."
  [deg]
  (let [h (/ (mod deg 360.0) 60.0)
        x (int (* 255 (- 1.0 (Math/abs (- (mod h 2.0) 1.0)))))
        s (int h)]
    (case s
      0 [255 x 0]
      1 [x 255 0]
      2 [0 255 x]
      3 [0 x 255]
      4 [x 0 255]
      [255 0 x])))

(defn- clamp [v lo hi] (max lo (min hi v)))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [shapes] example - rlgl color wheel")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cx (/ (double W) 2.0)
        cy (/ (double H) 2.0)]
    (loop [frame 0
           tris 32
           scale 150.0
           value 1.0
           lines? false]
      (when (rl/keep-running? deadline)
        (let [tris   (clamp (+ tris (int (rl/get-mouse-wheel))) MIN-TRIS MAX-TRIS)
              scale  (cond-> scale
                       (rl/key-down? rl/KEY-UP)   (* 1.025)
                       (rl/key-down? rl/KEY-DOWN) (* 0.975))
              scale  (clamp scale 32.0 (/ (double H) 2.0))
              value  (cond-> value
                       (rl/key-down? rl/KEY-RIGHT) (+ 0.02)
                       (rl/key-down? rl/KEY-LEFT)  (- 0.02))
              value  (clamp value 0.0 1.0)
              lines? (if (rl/key-pressed? rl/KEY-SPACE) (not lines?) lines?)
              grey   (int (* 255 value))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rl-begin (if lines? rl/RL-LINES rl/RL-TRIANGLES))
          (dotimes [i tris]
            (let [step (/ TWO-PI tris)
                  a0   (* step i)
                  a1   (* step (inc i))
                  x0   (+ cx (* scale (Math/sin a0)))
                  y0   (- cy (* scale (Math/cos a0)))
                  x1   (+ cx (* scale (Math/sin a1)))
                  y1   (- cy (* scale (Math/cos a1)))
                  [r0 g0 b0] (hue->rgb (* 360.0 (/ a0 TWO-PI)))
                  [r1 g1 b1] (hue->rgb (* 360.0 (/ a1 TWO-PI)))]
              (if lines?
                (do
                  ;; The wedge outline: rim edge, then both spokes.
                  (rl/rl-color! (rl/rgba r0 g0 b0 255)) (rl/rl-vertex-2f x0 y0)
                  (rl/rl-color! (rl/rgba r1 g1 b1 255)) (rl/rl-vertex-2f x1 y1)
                  (rl/rl-color! (rl/rgba r0 g0 b0 255)) (rl/rl-vertex-2f x0 y0)
                  (rl/rl-color! (rl/rgba grey grey grey 255)) (rl/rl-vertex-2f cx cy))
                (do
                  ;; One triangle per wedge: two rim vertices and the centre.
                  (rl/rl-color! (rl/rgba r0 g0 b0 255)) (rl/rl-vertex-2f x0 y0)
                  (rl/rl-color! (rl/rgba grey grey grey 255)) (rl/rl-vertex-2f cx cy)
                  (rl/rl-color! (rl/rgba r1 g1 b1 255)) (rl/rl-vertex-2f x1 y1)))))
          (rl/rl-end)
          (rl/text! "wheel: triangle count - [UP]/[DOWN] size - [LEFT]/[RIGHT] centre - [SPACE] wire"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/text! (str tris " triangles") :x 10 :y 36 :size 20 :color rl/MAROON)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) tris scale value lines?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
