(ns net.b12n.raylib-jlt.billboard-rendering
  "raylib [models] example - billboard rendering.

  Two camera-facing quads over a grid, textured from one shared atlas: one
  stays flat-on to the camera (DrawBillboard's idea), the other also spins
  around its own facing normal (DrawBillboardPro's rotation). The camera
  orbits the scene, so the billboards' facing direction changes every frame.

  raylib's DrawBillboard/DrawBillboardPro take a by-value Texture2D and
  aren't bound here yet, so both are reimplemented directly: a billboard's
  quad corners are `centre +/- right*halfsize +/- up*halfsize`, where `right`
  and `up` come from the camera's own forward vector (`cross(world-up,
  forward)` and `cross(forward, right)`). Spinning it is a Rodrigues
  rotation of `right`/`up` around `forward` by the animated angle. The
  farther billboard draws first so the nearer one composites over it, same
  as the upstream C example."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const ATLAS 64)
(def ^:const ORBIT-R 7.07)
(def ^:const STATIC-POS [0.0 2.0 0.0])
(def ^:const SPIN-POS [1.0 2.0 1.0])

(defn- atlas-pixel
  "A ring so the billboard's facing (and any rotation) is easy to read."
  [x y]
  (let [c (/ ATLAS 2.0)
        r (Math/sqrt (+ (Math/pow (- x c) 2) (Math/pow (- y c) 2)))]
    (cond
      (< r (* c 0.35)) (rl/rgba 253 249 0 255)
      (< r (* c 0.7)) (rl/rgba 230 41 55 255)
      :else (rl/rgba 0 121 241 255))))

(defn- vsub [[ax ay az] [bx by bz]] [(- ax bx) (- ay by) (- az bz)])
(defn- vadd [[ax ay az] [bx by bz]] [(+ ax bx) (+ ay by) (+ az bz)])
(defn- vscale [[x y z] s] [(* x s) (* y s) (* z s)])
(defn- vcross [[ax ay az] [bx by bz]]
  [(- (* ay bz) (* az by)) (- (* az bx) (* ax bz)) (- (* ax by) (* ay bx))])
(defn- vdot [[ax ay az] [bx by bz]] (+ (* ax bx) (* ay by) (* az bz)))
(defn- vlen [v] (Math/sqrt (vdot v v)))
(defn- vnorm [v] (let [l (vlen v)] (if (zero? l) v (vscale v (/ 1.0 l)))))

(defn- vrotate-axis
  "Rodrigues' rotation formula: v rotated by deg degrees around unit axis k."
  [v k deg]
  (let [rad (Math/toRadians deg)
        c (Math/cos rad) s (Math/sin rad)
        kdotv (vdot k v)]
    (vadd (vadd (vscale v c) (vscale (vcross k v) s))
          (vscale k (* kdotv (- 1.0 c))))))

(defn- tex-vert
  [u v [x y z]]
  (rl/rl-tex-coord-2f u v)
  (rl/rl-vertex-3f x y z))

(defn- draw-billboard
  "A camera-facing quad centred at `center`, textured from `tex-id`. `spin-deg`
  (if non-zero) rotates the quad's own right/up basis around the camera-facing
  normal, so the texture spins in the billboard's own plane."
  [{:keys [tex-id center cam-pos cam-target size spin-deg]}]
  (let [forward (vnorm (vsub cam-target cam-pos))
        right0 (vnorm (vcross [0.0 1.0 0.0] forward))
        up0 (vcross forward right0)
        [right up] (if (zero? spin-deg)
                     [right0 up0]
                     [(vrotate-axis right0 forward spin-deg) (vrotate-axis up0 forward spin-deg)])
        h (/ size 2.0)
        tl (vadd center (vadd (vscale right (- h)) (vscale up h)))
        tr (vadd center (vadd (vscale right h) (vscale up h)))
        br (vadd center (vadd (vscale right h) (vscale up (- h))))
        bl (vadd center (vadd (vscale right (- h)) (vscale up (- h))))
        [nx ny nz] (vscale forward -1.0)]
    (rl/rl-set-texture tex-id)
    (rl/rl-begin rl/RL-QUADS)
    (rl/rl-color-4ub 255 255 255 255)
    (rl/rl-normal-3f nx ny nz)
    ;; tl/tr/br/bl (not bl/br/tr/tl): that winding is the one whose
    ;; cross(edge1, edge2) points back at the camera, the side backface
    ;; culling keeps. The other order draws a quad that's there but
    ;; invisible from every angle, caught only by looking at the PNG.
    (tex-vert 0.0 0.0 tl) (tex-vert 1.0 0.0 tr) (tex-vert 1.0 1.0 br) (tex-vert 0.0 1.0 bl)
    (rl/rl-end)
    (rl/rl-set-texture 0)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - billboard rendering"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        atlas (rl/texture-from-fn ATLAS ATLAS atlas-pixel)]
    (loop [frame 0
           angle 0.8
           spin 0.0]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! atlas)
        (let [angle (+ angle (* 0.5 (rl/get-frame-time)))
              spin (+ spin 0.4)
              cam-pos [(* ORBIT-R (Math/cos angle)) 4.0 (* ORBIT-R (Math/sin angle))]
              cam-target [0.0 2.0 0.0]
              d-static (vdot (vsub cam-pos STATIC-POS) (vsub cam-pos STATIC-POS))
              d-spin (vdot (vsub cam-pos SPIN-POS) (vsub cam-pos SPIN-POS))
              draw-both (fn []
                          (rl/draw-grid 10 1.0)
                          (let [static-bb #(draw-billboard {:tex-id atlas
                                                            :center STATIC-POS
                                                            :cam-pos cam-pos
                                                            :cam-target cam-target
                                                            :size 2.0
                                                            :spin-deg 0.0})
                                spin-bb #(draw-billboard {:tex-id atlas
                                                          :center SPIN-POS
                                                          :cam-pos cam-pos
                                                          :cam-target cam-target
                                                          :size 2.0
                                                          :spin-deg spin})]
                            (if (> d-static d-spin)
                              (do (static-bb) (spin-bb))
                              (do (spin-bb) (static-bb)))))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x (nth cam-pos 0)
                              :pos-y (nth cam-pos 1)
                              :pos-z (nth cam-pos 2)
                              :target-y 2.0
                              :fovy 45.0}
            draw-both)
          (rl/text! "The camera orbits; the right billboard also spins"
                    {:x 10
                     :y 40
                     :size 16
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) angle spin)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
