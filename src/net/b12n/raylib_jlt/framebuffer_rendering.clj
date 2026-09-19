(ns net.b12n.raylib-jlt.framebuffer-rendering
  "raylib [textures] example - framebuffer rendering (`jolt -M:framebuffer-rendering`).

  Port of raylib's examples/textures/textures_framebuffer_rendering.c. The same
  scene twice, side by side, each rendered into its own framebuffer before
  anything reaches the window. On the left an observer watches the scene AND the
  other camera, drawn as its own view frustum; on the right is what that second
  camera actually sees. The green square in the middle of the right view is
  copied out again as the inset in its corner, which is the same texture sampled
  through a different source rectangle.

  Zero new FFI. `rl/render-texture` and `rl/with-render-texture` are the scalar
  rlgl spelling of BeginTextureMode that the suite has had since `render-texture`,
  and the inset is `rl/texture!` with `:u0`/`:v0`/`:u1`/`:v1` narrowed to the
  middle 128 pixels rather than a second render pass. Note both views are drawn
  with `:v0 1.0 :v1 0.0`, since a framebuffer texture is stored bottom-up.

  The frustum is worth the few lines it costs. It is built from the subject
  camera's own position, target and field of view, so the wireframe on the left
  is not a drawing OF the right-hand view, it is the geometry that produced it,
  and the two move together because they are the same numbers.

  Deviation: the C drives the observer with CAMERA_FREE on a captured pointer.
  Both cameras orbit on their own here, at different rates, since no synthetic
  input actuates a raylib window (see scripts/demo_manifest.edn) and the C's
  subject camera is on CAMERA_ORBITAL anyway, which takes no input either."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const HALF-W 400)
(def ^:const CROP 128)
(def ^:const FOVY 45.0)
(def ^:const FRUSTUM-DEPTH 3.0)

(defn- v-
  [[ax ay az] [bx by bz]]
  [(- ax bx) (- ay by) (- az bz)])

(defn- v+
  [[ax ay az] [bx by bz]]
  [(+ ax bx) (+ ay by) (+ az bz)])

(defn- v*
  [[x y z] k]
  [(* x k) (* y k) (* z k)])

(defn- cross
  [[ax ay az] [bx by bz]]
  [(- (* ay bz) (* az by))
   (- (* az bx) (* ax bz))
   (- (* ax by) (* ay bx))])

(defn- normalize
  [[x y z :as v]]
  (let [len (Math/sqrt (+ (* x x) (* y y) (* z z)))]
    (if (zero? len) v (v* v (/ 1.0 len)))))

(defn- orbit
  "A camera position circling `target` at `radius` and `height`, one revolution
  every 2*pi/`speed` frames."
  [[tx ty tz] radius height speed n]
  (let [a (* n speed)]
    [(+ tx (* radius (Math/cos a)))
     (+ ty height)
     (+ tz (* radius (Math/sin a)))]))

(defn- frustum-corners
  "The four far-plane corners of `camera`'s view volume at FRUSTUM-DEPTH, in the
  order that walks the rectangle."
  [pos target aspect]
  (let [forward (normalize (v- target pos))
        right (normalize (cross forward [0.0 1.0 0.0]))
        up (cross right forward)
        half-h (* FRUSTUM-DEPTH (Math/tan (/ (* FOVY Math/PI) 360.0)))
        half-w (* half-h aspect)
        centre (v+ pos (v* forward FRUSTUM-DEPTH))]
    [(v+ centre (v+ (v* right (- half-w)) (v* up half-h)))
     (v+ centre (v+ (v* right half-w) (v* up half-h)))
     (v+ centre (v+ (v* right half-w) (v* up (- half-h))))
     (v+ centre (v+ (v* right (- half-w)) (v* up (- half-h))))]))

(defn- draw-prism!
  "The subject camera drawn as what it is: an apex at its position and four edges
  out to the corners of what it can see."
  [pos target aspect color]
  (let [corners (frustum-corners pos target aspect)]
    (rl/rl-begin rl/RL-LINES)
    (rl/rl-color! color)
    (doseq [[x y z] corners]
      (rl/rl-vertex-3f (nth pos 0) (nth pos 1) (nth pos 2))
      (rl/rl-vertex-3f x y z))
    (dotimes [i 4]
      (let [[ax ay az] (nth corners i)
            [bx by bz] (nth corners (mod (inc i) 4))]
        (rl/rl-vertex-3f ax ay az)
        (rl/rl-vertex-3f bx by bz)))
    (rl/rl-end)))

(defn- draw-scene!
  []
  (rl/draw-cube! {:pos [0.0 0.0 0.0]
                  :width 2.0
                  :height 2.0
                  :length 2.0
                  :color rl/GOLD})
  (rl/draw-cube-wires! {:pos [0.0 0.0 0.0]
                        :width 2.0
                        :height 2.0
                        :length 2.0
                        :color rl/PINK})
  (rl/draw-grid 10 1.0))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - framebuffer rendering"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        observer-rt (rl/render-texture HALF-W H)
        subject-rt (rl/render-texture HALF-W H)
        aspect (/ (double HALF-W) H)]
    (if-not (and observer-rt subject-rt)
      (binding [*out* *err*]
        (println "framebuffer-rendering: the driver reported an incomplete framebuffer"))
      (do
        (loop [frame 0]
          (when (app/keep-running? deadline)
            (let [origin [0.0 0.0 0.0]
                  subject-pos (orbit origin 5.0 2.0 0.012 frame)
                  observer-pos (orbit origin 14.0 10.0 0.004 frame)
                  subject-cam {:pos-x (nth subject-pos 0)
                               :pos-y (nth subject-pos 1)
                               :pos-z (nth subject-pos 2)
                               :target-x 0.0
                               :target-y 0.0
                               :target-z 0.0
                               :fovy FOVY
                               :projection 0}
                  observer-cam {:pos-x (nth observer-pos 0)
                                :pos-y (nth observer-pos 1)
                                :pos-z (nth observer-pos 2)
                                :target-x 0.0
                                :target-y 0.0
                                :target-z 0.0
                                :fovy FOVY
                                :projection 0}]
              (rl/with-render-texture observer-rt
                (fn []
                  (rl/clear-background rl/RAYWHITE)
                  (rl/with-camera-3d observer-cam
                    (fn []
                      (draw-scene!)
                      (draw-prism! subject-pos origin aspect rl/GREEN)))
                  (rl/text! "observer view" {:x 10
                                             :y (- H 30)
                                             :size 20
                                             :color rl/BLACK})
                  (rl/text! "both cameras orbit on their own" {:x 10
                                                               :y 10
                                                               :size 10
                                                               :color rl/DARKGRAY})))
              (rl/with-render-texture subject-rt
                (fn []
                  (rl/clear-background rl/RAYWHITE)
                  (rl/with-camera-3d subject-cam (fn [] (draw-scene!)))
                  (rl/rect-lines! {:x (/ (- HALF-W CROP) 2)
                                   :y (/ (- H CROP) 2)
                                   :width CROP
                                   :height CROP
                                   :color rl/GREEN})
                  (rl/text! "subject view" {:x 10
                                            :y (- H 30)
                                            :size 20
                                            :color rl/BLACK})))
              (rl/begin-drawing)
              (rl/clear-background rl/BLACK)
              (rl/texture! (:texture observer-rt) {:x 0
                                                   :y 0
                                                   :width HALF-W
                                                   :height H
                                                   :v0 1.0
                                                   :v1 0.0})
              (rl/texture! (:texture subject-rt) {:x HALF-W
                                                  :y 0
                                                  :width HALF-W
                                                  :height H
                                                  :v0 1.0
                                                  :v1 0.0})
              ;; The inset is the SAME texture, sampled through a narrower
              ;; source rectangle: the middle 128 pixels of the subject view,
              ;; still flipped, drawn over the corner it came from.
              (let [u0 (/ (- HALF-W CROP) 2.0 HALF-W)
                    u1 (/ (+ HALF-W CROP) 2.0 HALF-W)
                    v0 (/ (+ H CROP) 2.0 H)
                    v1 (/ (- H CROP) 2.0 H)]
                (rl/texture! (:texture subject-rt) {:x (+ HALF-W 20)
                                                    :y 20
                                                    :width CROP
                                                    :height CROP
                                                    :u0 u0
                                                    :v0 v0
                                                    :u1 u1
                                                    :v1 v1}))
              (rl/rect-lines! {:x (+ HALF-W 20)
                               :y 20
                               :width CROP
                               :height CROP
                               :color rl/GREEN})
              (rl/line! {:x1 HALF-W
                         :y1 0
                         :x2 HALF-W
                         :y2 H
                         :color rl/DARKGRAY})
              (rl/fps! {:x 10
                        :y 10})
              (app/maybe-screenshot! frame 60)
              (rl/end-drawing)
              (recur (inc frame)))))
        (rl/unload-render-texture! observer-rt)
        (rl/unload-render-texture! subject-rt))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
