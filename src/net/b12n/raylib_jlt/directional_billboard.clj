(ns net.b12n.raylib-jlt.directional-billboard
  "raylib [models] example - directional billboard.

  A camera-facing sprite billboard whose row in a procedural sheet is chosen
  from the orbiting camera's own angle, so the character appears to turn as
  the camera circles it, and whose column cycles through a 4-frame walk
  animation. Combines two techniques from earlier in this suite: the
  camera-facing quad math from `billboard-rendering` (right/up from the
  camera's forward vector, no spin here) and the UV sub-rect slicing from
  `textured-cube`'s DrawCubeTextureRec, together the shape of raylib's
  DrawBillboardPro (a by-value Texture2D + Rectangle, not bound here).

  No new FFI. The sheet is 8 direction rows x 4 walk-frame columns, each cell
  a small hue-by-direction figure with a two-frame leg wiggle, generated once
  via rl/texture-from-fn rather than the upstream C example's skillbot.png.
  Based on raylib/examples/models/models_directional_billboard.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CELL 24)
(def ^:const COLS 4)
(def ^:const ROWS 8)
(def ^:const SHEET-W (* CELL COLS))
(def ^:const SHEET-H (* CELL ROWS))
(def ^:const RXZ 2.8284271247461903) ;; sqrt(2^2 + 2^2): XZ orbit radius

(defn- hsv->color
  [h s v]
  (let [h' (/ (mod h 360.0) 60.0)
        i (int (Math/floor h'))
        f (- h' i)
        p (* v (- 1.0 s))
        q (* v (- 1.0 (* s f)))
        t (* v (- 1.0 (* s (- 1.0 f))))
        [r g b] (case (mod i 6)
                  0 [v t p]
                  1 [q v p]
                  2 [p v t]
                  3 [p q v]
                  4 [t p v]
                  5 [v p q])]
    (rl/rgba (int (* 255 r)) (int (* 255 g)) (int (* 255 b)) 255)))

(def ^:private LEG-OFFSETS [0 2 0 -2])

(defn- sheet-pixel
  "One cell of the sprite sheet: col picks the walk frame, row the facing
  direction. A round head + rect body (hue by row) plus two legs that swap
  position by column, transparent everywhere else."
  [x y]
  (let [col (quot x CELL)
        row (quot y CELL)
        lx (mod x CELL)
        ly (mod y CELL)
        hue (* row 45.0)
        offset (nth LEG-OFFSETS (mod col (count LEG-OFFSETS)))
        head-dx (- lx 12) head-dy (- ly 8)
        head-r (Math/sqrt (+ (* head-dx head-dx) (* head-dy head-dy)))]
    (cond
      (< head-r 5) (hsv->color hue 0.7 1.0)
      (and (<= 9 lx) (< lx 15) (<= 13 ly) (< ly 20)) (hsv->color hue 0.7 0.7)
      (and (<= (+ 10 offset) lx) (< lx (+ 12 offset)) (<= 20 ly) (< ly 23))
      (hsv->color hue 0.7 0.5)
      (and (<= (- 12 offset) lx) (< lx (- 14 offset)) (<= 20 ly) (< ly 23))
      (hsv->color hue 0.7 0.5)
      :else (rl/rgba 0 0 0 0))))

(defn- vsub [[ax ay az] [bx by bz]] [(- ax bx) (- ay by) (- az bz)])
(defn- vadd [[ax ay az] [bx by bz]] [(+ ax bx) (+ ay by) (+ az bz)])
(defn- vscale [[x y z] s] [(* x s) (* y s) (* z s)])
(defn- vcross [[ax ay az] [bx by bz]]
  [(- (* ay bz) (* az by)) (- (* az bx) (* ax bz)) (- (* ax by) (* ay bx))])
(defn- vdot [[ax ay az] [bx by bz]] (+ (* ax bx) (* ay by) (* az bz)))
(defn- vnorm [v] (let [l (Math/sqrt (vdot v v))] (if (zero? l) v (vscale v (/ 1.0 l)))))

(defn- tex-vert
  [u v [x y z]]
  (rl/rl-tex-coord-2f u v)
  (rl/rl-vertex-3f x y z))

(defn- draw-billboard-frame
  "A camera-facing quad at `center`, showing the [su0..su1] x [sv0..sv1] cell
  of `tex-id`'s sheet (already normalized to 0..1)."
  [{:keys [tex-id center cam-pos cam-target size su0 su1 sv0 sv1]}]
  (let [forward (vnorm (vsub cam-target cam-pos))
        right (vnorm (vcross [0.0 1.0 0.0] forward))
        up (vcross forward right)
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
    (tex-vert su0 sv0 tl) (tex-vert su1 sv0 tr) (tex-vert su1 sv1 br) (tex-vert su0 sv1 bl)
    (rl/rl-end)
    (rl/rl-set-texture 0)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - directional billboard"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        sheet (rl/texture-from-fn SHEET-W SHEET-H sheet-pixel)]
    (loop [frame 0
           theta (/ Math/PI 4.0)
           anim 0
           anim-timer 0.0]
      (if-not (rl/keep-running? deadline)
        (rl/unload-texture! sheet)
        (let [dt (rl/get-frame-time)
              theta (+ theta (* 0.5 dt))
              t2 (+ anim-timer dt)
              tick? (> t2 0.5)
              anim (if tick? (mod (inc anim) COLS) anim)
              anim-timer (if tick? 0.0 t2)
              px (* RXZ (Math/cos theta))
              pz (* RXZ (Math/sin theta))
              dir0 (Math/floor (+ (* (/ (Math/atan2 pz px) Math/PI) 4.0) 0.25))
              dir (int (if (< dir0 0.0) (+ 8.0 dir0) dir0))
              cam-pos [px 1.0 pz]
              cam-target [0.0 0.5 0.0]]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x px
                              :pos-y 1.0
                              :pos-z pz
                              :target-y 0.5
                              :fovy 45.0}
            (fn []
              (rl/draw-grid 10 1.0)
              (draw-billboard-frame {:tex-id sheet
                                     :center [0.0 0.5 0.0]
                                     :cam-pos cam-pos
                                     :cam-target cam-target
                                     :size 1.0
                                     :su0 (/ (double anim) COLS)
                                     :su1 (/ (double (inc anim)) COLS)
                                     :sv0 (/ (double dir) ROWS)
                                     :sv1 (/ (double (inc dir)) ROWS)})))
          (rl/text! (str "animation: " anim) {:x 10
                                              :y 10
                                              :size 20
                                              :color rl/DARKGRAY})
          (rl/text! (str "direction frame: " dir) {:x 10
                                                   :y 40
                                                   :size 20
                                                   :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) theta anim anim-timer)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
