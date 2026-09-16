(ns net.b12n.raylib-jlt.helitorus
  "A helix wound around a torus, swept into a tube (`jolt -M:helitorus`).

  Drag to turn, wheel to zoom, LEFT/RIGHT change the number of windings,
  UP/DOWN the resolution along the spine.

  The surface is generated rather than modelled: walk the spine of a helix that
  itself follows a torus, build a local frame (tangent, normal, binormal) at each
  point, sweep a circle around that frame, and you have a tube. Projection,
  lighting and hidden-surface removal are all done here rather than by raylib —
  which is the interesting part next to the Camera3D examples, because it shows
  what the rlgl layer alone can carry.

  Two things make it fast enough to animate at this vertex count. `compute!`
  writes into PRIMITIVE ARRAYS reused every frame, so a frame of ~10k vertices
  allocates nothing. And hidden surfaces are handled by the painter's algorithm
  over an `order` array that SURVIVES THE FRAME: consecutive frames differ by a
  small rotation, so the array starts almost sorted and the insertion sort over
  it is nearly linear.

  The HUD separates compute time from draw time, which is the honest way to read
  them: one is jolt arithmetic and the other is the FFI submitting vertices.

  Backface culling is switched off (`rl/rl-disable-backface-culling`) because
  this example decides visibility itself, by the sign of a 2D cross product in
  screen space. That is not a winding rule, and raylib's cull would drop exactly
  the faces the test keeps — see the note in raylib.clj."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 1000)
(def ^:const H 560)
(def CX (/ W 2.0))
(def CY (/ H 2.0))

(def ^:const NV 12)       ; points around the tube's cross-section
(def ^:const MAX-NU 900)  ; most points along the spine
(def ^:const R 1.0)       ; major radius of the torus
(def ^:const r2 0.30)     ; minor radius of the torus
(def ^:const r3 0.11)     ; radius of the swept tube
(def ^:const DIST 4.6)    ; camera distance
(def ^:const FOCAL 3.2)
(def TAU (* 2.0 Math/PI))

;; --- geometry buffers --------------------------------------------------------
;; Allocated once, rewritten every frame. Type hints on all of them, and
;; aset-double / aset-int rather than plain aset, so the writes compile to array
;; stores rather than to a generic path.

;; The phi grid around the cross-section is fixed; only its phase moves.
(def ^double/1 cos-phi (double-array NV))
(def ^double/1 sin-phi (double-array NV))
(dotimes [j NV]
  (let [a (/ (* TAU j) NV)]
    (aset-double cos-phi j (Math/cos a))
    (aset-double sin-phi j (Math/sin a))))

(def ^double/1 sx (double-array (* MAX-NU NV)))
(def ^double/1 sy (double-array (* MAX-NU NV)))
(def ^int/1 shade (int-array (* MAX-NU NV)))
(def ^double/1 ring-z (double-array MAX-NU))
;; Kept between frames: see the note in the namespace docstring.
(def ^int/1 order (int-array MAX-NU))
(def ^int/1 order-nu (int-array 1))

(defn reset-order!
  [nu]
  (dotimes [i nu] (aset-int order i i))
  (aset-int order-nu 0 nu))

;; --- palette -----------------------------------------------------------------
;; hsl(337..349, 100..78%, 17..72%), precomputed into three int arrays so the
;; draw loop never converts a color.

(defn hsl->rgb
  [h s l]
  (let [c (* (- 1.0 (Math/abs (- (* 2.0 l) 1.0))) s)
        h' (/ h 60.0)
        x (* c (- 1.0 (Math/abs (- (mod h' 2.0) 1.0))))
        m (- l (/ c 2.0))
        [r g b] (cond
                  (< h' 1) [c x 0.0]
                  (< h' 2) [x c 0.0]
                  (< h' 3) [0.0 c x]
                  (< h' 4) [0.0 x c]
                  (< h' 5) [x 0.0 c]
                  :else [c 0.0 x])]
    [(int (* 255 (+ r m))) (int (* 255 (+ g m))) (int (* 255 (+ b m)))]))

(def ^int/1 palette-r (int-array 64))
(def ^int/1 palette-g (int-array 64))
(def ^int/1 palette-b (int-array 64))
(dotimes [i 64]
  (let [t (/ i 63.0)
        [r g b] (hsl->rgb (+ 337.0 (* 12.0 t))
                          (/ (- 100.0 (* 22.0 t)) 100.0)
                          (/ (+ 17.0 (* 55.0 t)) 100.0))]
    (aset-int palette-r i r)
    (aset-int palette-g i g)
    (aset-int palette-b i b)))

;; --- one frame's worth of geometry ------------------------------------------

(defn sort-rings!
  "Insertion sort of `order` by ring depth, far rings first."
  [nu]
  (loop [i 1]
    (when (< i nu)
      (let [v (aget order i)
            vz (aget ring-z v)]
        (loop [k (dec i)]
          (if (and (>= k 0) (< (aget ring-z (aget order k)) vz))
            (do (aset-int order (inc k) (aget order k))
                (recur (dec k)))
            (aset-int order (inc k) v))))
      (recur (inc i)))))

(defn compute!
  "Project every vertex of the surface into `sx`/`sy`, shade it into `shade`,
  and sort the rings back to front. Pure arithmetic and array writes; nothing
  here touches raylib."
  [{:keys [nu twists rot-x rot-y zoom clock]}]
  (let [nu (long nu)
        n (long twists)
        r (+ r2 r3)
        zm (double zoom)
        ay (double rot-y)
        ax (double rot-x)
        cay (Math/cos ay) say (Math/sin ay)
        cax (Math/cos ax) sax (Math/sin ax)
        po (* clock 1.1)
        cpo (Math/cos po)
        spo (Math/sin po)
        dtheta (/ TAU nu)]
    (when (not= nu (aget order-nu 0)) (reset-order! nu))
    (loop [i 0]
      (when (< i nu)
        (let [th (* i dtheta)
              nth (* n th)
              cn (Math/cos nth) sn (Math/sin nth)
              ct (Math/cos th) st (Math/sin th)
              xr (+ R (* r cn))
              ;; the point on the spine
              px (* xr ct) py (* xr st) pz (* r sn)
              ;; its tangent: the theta derivative, written out
              tx (- (* (- xr) st) (* n r ct sn))
              ty (- (* xr ct) (* n r st sn))
              tz (* n r cn)
              ;; the tangent of the flat circle under the spine
              bx (- st) by ct
              ;; tube normal: t cross b, normalised
              nx (- (* ty 0.0) (* tz by))
              ny (- (* tz bx) (* tx 0.0))
              nz (- (* tx by) (* ty bx))
              nl (Math/sqrt (+ (* nx nx) (* ny ny) (* nz nz)))
              nx (/ nx nl) ny (/ ny nl) nz (/ nz nl)
              ;; third axis: n cross t, normalised
              ux (- (* ny tz) (* nz ty))
              uy (- (* nz tx) (* nx tz))
              uz (- (* nx ty) (* ny tx))
              ul (Math/sqrt (+ (* ux ux) (* uy uy) (* uz uz)))
              ux (/ ux ul) uy (/ uy ul) uz (/ uz ul)
              base (* i NV)]
          ;; ring depth, for the painter's ordering
          (let [rz (+ (* px say) (* pz cay))]
            (aset-double ring-z i (- (* rz cax) (* py sax))))
          (loop [j 0]
            (when (< j NV)
              (let [c0 (aget cos-phi j)
                    s0 (aget sin-phi j)
                    cp (- (* c0 cpo) (* s0 spo))
                    sp (+ (* s0 cpo) (* c0 spo))
                    ;; the surface normal, then the point on the surface
                    vx (+ (* ux cp) (* nx sp))
                    vy (+ (* uy cp) (* ny sp))
                    vz (+ (* uz cp) (* nz sp))
                    wx (+ px (* r3 vx))
                    wy (+ py (* r3 vy))
                    wz (+ pz (* r3 vz))
                    ;; rotate about Y, then about X
                    x1 (- (* wx cay) (* wz say))
                    z1 (+ (* wx say) (* wz cay))
                    y2 (+ (* wy cax) (* z1 sax))
                    z2 (- (* z1 cax) (* wy sax))
                    ;; the same rotation on the normal
                    m1 (- (* vx cay) (* vz say))
                    q1 (+ (* vx say) (* vz cay))
                    m2 (+ (* vy cax) (* q1 sax))
                    q2 (- (* q1 cax) (* vy sax))
                    ;; perspective divide
                    k (/ (* zm FOCAL) (+ FOCAL DIST z2))
                    ;; diffuse, plus a rim term
                    lum (+ (* 0.60 (max 0.0 (+ (* m1 -0.40) (* m2 -0.62) (* q2 -0.68))))
                           (* 0.28 (max 0.0 (- q2)))
                           0.12)
                    o (+ base j)]
                (aset-double sx o (+ CX (* x1 k)))
                (aset-double sy o (- CY (* y2 k)))
                (aset-int shade o (min 63 (max 0 (int (* 63.0 lum))))))
              (recur (inc j))))
          (recur (inc i)))))
    (sort-rings! nu)))

;; --- submitting it ----------------------------------------------------------

(defn draw-ring!
  "One ring of the tube as flat-shaded quads, two rlgl triangles each. A quad is
  skipped when the 2D cross product of its first two edges is negative, which is
  the back of the tube."
  [i nu]
  (let [i2 (let [x (inc i)] (if (= x nu) 0 x))
        b1 (* i NV)
        b2 (* i2 NV)]
    (loop [j 0]
      (when (< j NV)
        (let [j2 (let [x (inc j)] (if (= x NV) 0 x))
              a (+ b1 j) b (+ b1 j2)
              c (+ b2 j2) d (+ b2 j)
              xa (aget sx a) ya (aget sy a)
              xb (aget sx b) yb (aget sy b)
              xc (aget sx c) yc (aget sy c)]
          (when (pos? (- (* (- xb xa) (- yc ya))
                         (* (- yb ya) (- xc xa))))
            (let [s (aget shade a)]
              (rl/rl-color-4ub (aget palette-r s) (aget palette-g s)
                               (aget palette-b s) 255))
            (rl/rl-vertex-2f xa ya)
            (rl/rl-vertex-2f xb yb)
            (rl/rl-vertex-2f xc yc)
            (rl/rl-vertex-2f xa ya)
            (rl/rl-vertex-2f xc yc)
            (rl/rl-vertex-2f (aget sx d) (aget sy d))))
        (recur (inc j))))))

(defn draw-surface!
  [nu]
  (loop [oi 0]
    (when (< oi nu)
      ;; One rlBegin/rlEnd batch per ring: rlgl cannot flush inside an open
      ;; batch, and the whole surface at once overflows its vertex buffer.
      (rl/rl-begin rl/RL-TRIANGLES)
      (draw-ring! (aget order oi) nu)
      (rl/rl-end)
      (recur (inc oi)))))

;; --- input ------------------------------------------------------------------

(defn drag
  "While the button is held, the pointer turns the surface and its motion is
  remembered as a velocity."
  [s]
  (let [x (rl/get-mouse-x)
        y (rl/get-mouse-y)
        dx (- x (:last-px s))
        dy (- y (:last-py s))]
    (cond-> (assoc s :dragging? true :last-px x :last-py y)
      (:dragging? s)
      (-> (update :rot-y + (* dx 0.008))
          (update :rot-x #(max -1.45 (min 1.45 (+ % (* dy 0.008)))))
          (assoc :vel-y (* dx 0.35) :vel-x (* dy 0.35))))))

(defn coast
  "Released, the remembered spin decays to a slow idle turn."
  [s dt]
  (let [decay (Math/pow 0.94 (/ dt 0.016))]
    (assoc s
           :dragging? false
           :vel-y (+ (* (:vel-y s) decay) (* 0.16 (- 1.0 decay)))
           :vel-x (* (:vel-x s) decay))))

(defn advance-camera
  [s dt]
  (let [s (if (rl/mouse-down? rl/MOUSE-LEFT) (drag s) (coast s dt))]
    (-> s
        (update :rot-y + (* (:vel-y s) dt))
        (update :rot-x #(max -1.45 (min 1.45 (+ % (* (:vel-x s) dt))))))))

(defn read-input
  [s]
  (let [wheel (rl/get-mouse-wheel)]
    (cond-> s
      (not (zero? wheel))
      (update :zoom #(max 110.0 (min 520.0 (* % (Math/exp (* 0.09 wheel))))))

      (rl/key-pressed? rl/KEY-RIGHT) (update :twists #(min 24 (inc %)))
      (rl/key-pressed? rl/KEY-LEFT) (update :twists #(max 3 (dec %)))
      (rl/key-down? rl/KEY-UP) (update :nu #(min MAX-NU (+ % 4)))
      (rl/key-down? rl/KEY-DOWN) (update :nu #(max 60 (- % 4))))))

;; --- the loop ---------------------------------------------------------------

(def HUD-COLOR (rl/rgba 55 65 81 255))
(def HELP-COLOR (rl/rgba 156 163 175 255))
(def HELP-TEXT
  "drag to turn, wheel to zoom, LEFT/RIGHT windings, UP/DOWN resolution")

(defn initial-state
  []
  {:nu 260 :twists 14
   :rot-x 0.55 :rot-y 0.0
   :vel-x 0.0 :vel-y 0.45
   :zoom 250.0
   :dragging? false :last-px 0 :last-py 0
   :clock 0.0
   ;; rolling HUD counters: frames and seconds since it last refreshed, plus
   ;; accumulated compute and draw milliseconds
   :frames 0 :elapsed 0.0 :compute-ms 0.0 :draw-ms 0.0
   :hud "computing..."})

(defn refresh-hud
  "Every 0.4s, average the accumulated timings into one line and reset them."
  [s]
  (if-not (>= (:elapsed s) 0.4)
    s
    (let [{:keys [frames elapsed compute-ms draw-ms nu twists]} s
          fps (/ frames elapsed)]
      (assoc s
             :hud (format (str "fps %.0f | compute %.1f ms | draw %.1f ms "
                               "| %dk verts/s | windings %d | res %d")
                          fps
                          (/ compute-ms frames)
                          (/ draw-ms frames)
                          (int (/ (* fps nu NV) 1000))
                          twists
                          nu)
             :frames 0 :elapsed 0.0 :compute-ms 0.0 :draw-ms 0.0))))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "helitorus")
  (rl/set-target-fps 120)
  ;; See the namespace docstring: this example culls for itself.
  (rl/rl-disable-backface-culling)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           s (initial-state)]
      (when (rl/keep-running? deadline)
        (let [dt (rl/get-frame-time)
              s (-> s (update :clock + dt) read-input (advance-camera dt))
              c0 (System/nanoTime)
              _ (compute! s)
              c1 (System/nanoTime)]
          (rl/begin-drawing)
          (rl/clear-background rl/WHITE)
          (let [d0 (System/nanoTime)
                _ (draw-surface! (:nu s))
                d1 (System/nanoTime)
                s (-> s
                      (update :frames inc)
                      (update :elapsed + dt)
                      (update :compute-ms + (/ (- c1 c0) 1e6))
                      (update :draw-ms + (/ (- d1 d0) 1e6))
                      refresh-hud)]
            (rl/text! (:hud s) :x 12 :y 12 :size 20 :color HUD-COLOR)
            (rl/text! HELP-TEXT :x 12 :y (- H 28) :size 16 :color HELP-COLOR)
            (rl/maybe-screenshot! frame 30)
            (rl/end-drawing)
            (recur (inc frame) s))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
