(ns net.b12n.raylib-jlt.doom
  "A textured raycaster (`jolt -M:doom`).

  WASD or the arrow keys move, the mouse or LEFT/RIGHT turn, left click or SPACE
  shoots, ESC quits.

  The 2.5D rendering technique Wolfenstein and Doom made famous, and the reason
  it is worth having next to `first-person-maze`: that example walks a grid of
  real 3D cubes under a Camera3D, while this one casts ONE RAY PER SCREEN COLUMN
  and draws each hit as a single textured vertical strip. No 3D geometry, no
  camera matrix, no depth buffer — just a DDA walk over a grid of characters.

  `cast-column` is the whole renderer: step cell by cell along the ray until a
  solid one, and the distance you stepped decides the strip's height (closer
  means taller) while the fraction of the wall you hit decides its texture u
  coordinate. That distance is also kept per column in `zbuf`, which is what the
  sprite pass depth-tests against: each imp is cut into vertical strips, and a
  strip is drawn only where it is nearer than the wall column behind it. That is
  how a sprite gets occluded by geometry without a per-pixel depth buffer.

  Everything visible goes through the shared layer. The texture atlas — four
  wall styles and one sprite, 64x64 each, stacked vertically — is
  `rl/texture-from-fn`, generated procedurally so the example ships no assets,
  and the strips are rlgl quads wound the way `rl/texture!` winds its own."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 900)
(def ^:const H 560)
;; One raycast per column: the knob that trades detail for frame rate.
(def ^:const COLS 450)
(def COLW (/ (double W) COLS))

;; --- the level ---------------------------------------------------------------
;; Digits are wall styles (which atlas row to sample), '.' is open floor.

(def level
  ["1111111111111111"
   "1..............1"
   "1..2222...44...1"
   "1..2......4....1"
   "1..2..33..4....1"
   "1.....3........1"
   "1.....3...222..1"
   "1..............1"
   "1...44444......1"
   "1.......4...33.1"
   "1.......4......1"
   "1..333..4..22..1"
   "1....3.....2...1"
   "1....3.........1"
   "1..............1"
   "1111111111111111"])

(def MAP-W (count (first level)))
(def MAP-H (count level))

(def ^int/1 grid (int-array (* MAP-W MAP-H)))
(dotimes [y MAP-H]
  (dotimes [x MAP-W]
    (let [c (nth (nth level y) x)]
      (aset-int grid (+ (* y MAP-W) x)
                (if (= \. c) 0 (- (int c) (int \0)))))))

(defn wall-at
  "The wall style at grid cell x,y — 0 for open floor, and 1 outside the map so
  a ray that escapes still terminates."
  [x y]
  (if (or (< x 0) (< y 0) (>= x MAP-W) (>= y MAP-H))
    1
    (aget grid (+ (* y MAP-W) x))))

;; --- the texture atlas -------------------------------------------------------
;; Five 64x64 tiles stacked vertically: brick, stone, metal, tech green, and an
;; imp sprite with a transparent surround. Row t covers v in [t/TILES,
;; (t+1)/TILES], which is how a strip picks its wall style.

(def ^:const TILE 64)
(def ^:const TILES 5)

(defn tile-pixel
  "The packed Color of pixel x,y within tile t. Procedural, so nothing is
  loaded from disk — which is just as well, since raylib's image loaders return
  an Image by value and do not cross this FFI boundary (see net.b12n.raylib.images)."
  [t x y]
  (case (long t)
    ;; brick, offset every other course
    0 (let [row (quot y 16)
            bx (mod (+ x (if (even? row) 0 32)) 32)
            mortar? (or (< (mod y 16) 2) (< bx 2))
            n (mod (* (+ x (* y 7)) 31) 24)]
        (if mortar?
          (rl/rgba 126 122 118 255)
          (rl/rgba (+ 120 n) (+ 46 (quot n 2)) (+ 38 (quot n 3)) 255)))
    ;; stone, with hairline cracks
    1 (let [n (mod (* (+ (* x 13) (* y 29)) 17) 40)
            v (+ 96 n)]
        (if (< (mod (+ (* x 3) y) 61) 2)
          (rl/rgba 58 58 62 255)
          (rl/rgba v v (+ v 6) 255)))
    ;; metal panel with rivets
    2 (let [px (mod x 32)
            py (mod y 32)
            n (mod (* (+ x y) 11) 14)]
        (cond
          (and (< 5 px 11) (< 5 py 11)) (rl/rgba 176 182 196 255)
          (or (< px 3) (< py 3) (> px 28) (> py 28)) (rl/rgba 70 78 96 255)
          :else (rl/rgba (+ 92 n) (+ 100 n) (+ 124 n) 255)))
    ;; tech green
    3 (let [line? (or (< (mod y 12) 2)
                      (and (< (mod x 24) 2) (> (mod y 24) 10)))
            n (mod (* (+ (* x 7) y) 13) 18)]
        (if line?
          (rl/rgba 92 232 140 255)
          (rl/rgba (+ 20 n) (+ 54 n) (+ 40 n) 255)))
    ;; the sprite: a squat imp. Alpha 0 around it, so the quad is see-through
    ;; wherever the imp is not.
    4 (let [cx (- x 32)
            cy (- y 40)
            body (+ (* cx cx) (* (/ cy 1.3) (/ cy 1.3)))
            head (+ (* cx cx) (* (- y 18) (- y 18)))
            eye-l (+ (* (- x 24) (- x 24)) (* (- y 16) (- y 16)))
            eye-r (+ (* (- x 40) (- x 40)) (* (- y 16) (- y 16)))]
        (cond
          (or (< eye-l 9) (< eye-r 9)) (rl/rgba 255 226 92 255)
          (< head 150) (rl/rgba 150 74 52 255)
          (< body 420) (rl/rgba 112 52 40 255)
          :else (rl/rgba 0 0 0 0)))))

(defn atlas-pixel
  "The atlas as one w x h surface: y selects the tile, and the remainder is the
  pixel within it."
  [x y]
  (tile-pixel (quot y TILE) x (mod y TILE)))

;; --- state -------------------------------------------------------------------

(def imp-spawns [[8.5 2.5] [12.5 6.5] [5.5 12.5] [11.5 11.5] [13.5 3.5] [4.5 8.5]])

(defn initial-state
  []
  ;; dir is the heading; plane is the camera plane perpendicular to it, and its
  ;; length is the field of view.
  {:pos-x 2.5
   :pos-y 7.5
   :dir-x 1.0
   :dir-y 0.0
   :plane-x 0.0
   :plane-y 0.66
   :imps (mapv (fn [[x y]] {:x x
                            :y y
                            :alive true}) imp-spawns)
   :health 100
   :kills 0
   :shots 0
   :flash 0.0
   :fps 0.0
   :fps-frames 0
   :fps-elapsed 0.0})

;; Per-frame scratch, not game state: the wall distance for each column, written
;; by the wall pass and read by the sprite pass.
(def ^double/1 zbuf (double-array COLS))

;; --- raycasting --------------------------------------------------------------

(defn cast-column
  "DDA for screen column `i`: step cell by cell along the ray until a solid one.

  Returns {:dist :tile :side :wall-x}, where :side is 0 for a vertical face and
  1 for a horizontal one (which is all the shading needs to make corners
  readable), and :wall-x is where along the wall the ray hit — the texture u."
  [i {:keys [pos-x pos-y dir-x dir-y plane-x plane-y]}]
  (let [camera (- (/ (* 2.0 i) COLS) 1.0)
        rdx (+ dir-x (* plane-x camera))
        rdy (+ dir-y (* plane-y camera))
        ;; How far along the ray one full cell of x, and of y, costs.
        ddx (if (zero? rdx) 1e30 (Math/abs (/ 1.0 rdx)))
        ddy (if (zero? rdy) 1e30 (Math/abs (/ 1.0 rdy)))
        stepx (if (neg? rdx) -1 1)
        stepy (if (neg? rdy) -1 1)]
    (loop [mx (int pos-x)
           my (int pos-y)
           sdx (if (neg? rdx)
                 (* (- pos-x (int pos-x)) ddx)
                 (* (- (+ (int pos-x) 1.0) pos-x) ddx))
           sdy (if (neg? rdy)
                 (* (- pos-y (int pos-y)) ddy)
                 (* (- (+ (int pos-y) 1.0) pos-y) ddy))
           side 0
           n 0]
      (let [tile (wall-at mx my)]
        (if (or (pos? tile) (> n 64))
          (let [dist (max 0.0001 (if (zero? side) (- sdx ddx) (- sdy ddy)))
                wx (if (zero? side)
                     (+ pos-y (* dist rdy))
                     (+ pos-x (* dist rdx)))]
            {:dist dist
             :tile (max 1 tile)
             :side side
             :wall-x (- wx (Math/floor wx))})
          ;; Always advance whichever axis is nearer along the ray.
          (if (< sdx sdy)
            (recur (+ mx stepx) my (+ sdx ddx) sdy 0 (inc n))
            (recur mx (+ my stepy) sdx (+ sdy ddy) 1 (inc n))))))))

(defn shade
  "Distance and side shading, as the 0-255 factor multiplied into the texel."
  [dist side]
  (let [f (/ 1.0 (+ 1.0 (* 0.11 dist dist)))
        f (if (zero? side) f (* f 0.72))]
    (max 30 (min 255 (long (* 255 (+ 0.12 (* 0.95 f))))))))

;; --- the wall pass -----------------------------------------------------------
;; rlgl cannot flush inside an open rlBegin/rlEnd, so the columns are submitted
;; in batches small enough to fit its vertex buffer.

(def ^:const BATCH 128)

(defn draw-wall-column!
  [i {:keys [dist tile side wall-x]}]
  (let [half (/ H 2.0)
        line (/ H dist)
        x0 (* i COLW)
        s (shade dist side)
        ;; Which atlas row this wall style samples.
        tile-i (min (dec TILES) (dec tile))
        v0 (/ (double tile-i) TILES)
        v1 (/ (+ tile-i 1.0) TILES)
        u (if (zero? side) wall-x (- 1.0 wall-x))]
    (aset-double zbuf i dist)
    (rl/rl-color-4ub s s s 255)
    ;; topLeft -> bottomLeft -> bottomRight -> topRight, the winding rl/texture!
    ;; uses, so these quads are front-facing and batch with everything else.
    (rl/rl-tex-coord-2f u v0) (rl/rl-vertex-2f x0 (- half (/ line 2.0)))
    (rl/rl-tex-coord-2f u v1) (rl/rl-vertex-2f x0 (+ half (/ line 2.0)))
    (rl/rl-tex-coord-2f u v1) (rl/rl-vertex-2f (+ x0 COLW) (+ half (/ line 2.0)))
    (rl/rl-tex-coord-2f u v0) (rl/rl-vertex-2f (+ x0 COLW) (- half (/ line 2.0)))))

(defn draw-walls!
  [atlas-id s]
  (rl/rl-set-texture atlas-id)
  (loop [start 0]
    (when (< start COLS)
      (rl/rl-begin rl/RL-QUADS)
      (let [batch-end (min COLS (+ start BATCH))]
        (loop [i start]
          (when (< i batch-end)
            (draw-wall-column! i (cast-column i s))
            (recur (inc i)))))
      (rl/rl-end)
      (recur (+ start BATCH))))
  (rl/rl-set-texture 0))

;; --- the sprite pass ---------------------------------------------------------

(def ^:const SPRITE-STRIPS 12)

(defn visible-imps
  "Living imps in camera space, furthest first, as {:tx :ty}. :ty is depth along
  the heading; :tx is sideways offset."
  [{:keys [pos-x pos-y dir-x dir-y plane-x plane-y imps]}]
  (let [inv-det (/ 1.0 (- (* plane-x dir-y) (* dir-x plane-y)))]
    (->> imps
         (filter :alive)
         (map (fn [{:keys [x y]}]
                (let [sx (- x pos-x)
                      sy (- y pos-y)]
                  {:tx (* inv-det (- (* dir-y sx) (* dir-x sy)))
                   :ty (* inv-det (+ (* (- plane-y) sx) (* plane-x sy)))})))
         (filter #(> (:ty %) 0.25))
         (sort-by :ty >))))

(defn draw-imp!
  "One billboard, cut into vertical strips so each can be depth-tested against
  the wall column behind it."
  [{:keys [tx ty]}]
  (let [half (/ H 2.0)
        screen-x (* (/ COLS 2.0) (+ 1.0 (/ tx ty)))
        size (/ COLS ty)
        w2 (/ size 2.0)
        h-px (/ H ty)
        y0 (- half (/ h-px 2.0))
        y1 (+ half (/ h-px 2.0))
        s (shade ty 0)]
    (when (< (max 0 (long (- screen-x w2))) (min COLS (long (+ screen-x w2))))
      (rl/rl-begin rl/RL-QUADS)
      (dotimes [k SPRITE-STRIPS]
        (let [a (+ (- screen-x w2) (* size (/ (double k) SPRITE-STRIPS)))
              b (+ (- screen-x w2) (* size (/ (+ k 1.0) SPRITE-STRIPS)))
              mid (long (/ (+ a b) 2.0))]
          (when (and (>= mid 0) (< mid COLS) (< ty (aget zbuf mid)))
            (let [u0 (/ (double k) SPRITE-STRIPS)
                  u1 (/ (+ k 1.0) SPRITE-STRIPS)
                  ;; the sprite is the last atlas row
                  v0 (/ (double (dec TILES)) TILES)]
              (rl/rl-color-4ub s s s 255)
              (rl/rl-tex-coord-2f u0 v0) (rl/rl-vertex-2f (* a COLW) y0)
              (rl/rl-tex-coord-2f u0 1.0) (rl/rl-vertex-2f (* a COLW) y1)
              (rl/rl-tex-coord-2f u1 1.0) (rl/rl-vertex-2f (* b COLW) y1)
              (rl/rl-tex-coord-2f u1 v0) (rl/rl-vertex-2f (* b COLW) y0)))))
      (rl/rl-end))))

(defn draw-imps!
  [atlas-id s]
  (let [visible (visible-imps s)]
    (when (seq visible)
      (rl/rl-set-texture atlas-id)
      (doseq [imp visible] (draw-imp! imp))
      (rl/rl-set-texture 0))))

;; --- input and game rules ----------------------------------------------------

(defn rotate
  "Turn the heading and the camera plane together by `a` radians."
  [s a]
  (let [c (Math/cos a)
        sn (Math/sin a)
        {:keys [dir-x dir-y plane-x plane-y]} s]
    (assoc s
           :dir-x (- (* dir-x c) (* dir-y sn))
           :dir-y (+ (* dir-x sn) (* dir-y c))
           :plane-x (- (* plane-x c) (* plane-y sn))
           :plane-y (+ (* plane-x sn) (* plane-y c)))))

(def ^:const MOVE-SPEED 3.4)

(defn move
  [s {:keys [forward strafe dt]}]
  (let [{:keys [pos-x pos-y dir-x dir-y plane-x plane-y]} s
        sp (* MOVE-SPEED dt)
        nx (+ pos-x (* sp (+ (* forward dir-x) (* strafe plane-x))))
        ny (+ pos-y (* sp (+ (* forward dir-y) (* strafe plane-y))))]
    ;; Slide along walls by testing each axis on its own.
    (cond-> s
      (zero? (wall-at (int nx) (int pos-y))) (assoc :pos-x nx)
      (zero? (wall-at (int pos-x) (int ny))) (assoc :pos-y ny))))

(defn shoot
  "Hit the nearest living imp inside a narrow cone of the heading."
  [s]
  (let [{:keys [pos-x pos-y dir-x dir-y imps]} s
        s (-> s (update :shots inc) (assoc :flash 0.06))
        hit (->> imps
                 (keep-indexed
                  (fn [i imp]
                    (when (:alive imp)
                      (let [ex (- (:x imp) pos-x)
                            ey (- (:y imp) pos-y)
                            dist (Math/sqrt (+ (* ex ex) (* ey ey)))
                            dot (/ (+ (* ex dir-x) (* ey dir-y)) (max 0.001 dist))]
                        (when (> dot 0.985) [i dist])))))
                 (sort-by second)
                 first)]
    (if hit
      (-> s
          (assoc-in [:imps (first hit) :alive] false)
          (update :kills inc))
      s)))

(def ^:const IMP-SPEED 0.9)

(defn- imp-distance
  [{:keys [pos-x pos-y]} imp]
  (let [ex (- pos-x (:x imp))
        ey (- pos-y (:y imp))]
    (Math/sqrt (+ (* ex ex) (* ey ey)))))

(defn- walk-imp
  "One imp step straight at the player, sliding along walls."
  [s imp dt]
  (let [d (imp-distance s imp)
        sp (* IMP-SPEED dt)
        nx (+ (:x imp) (* sp (/ (- (:pos-x s) (:x imp)) d)))
        ny (+ (:y imp) (* sp (/ (- (:pos-y s) (:y imp)) d)))]
    (cond-> imp
      (zero? (wall-at (int nx) (int (:y imp)))) (assoc :x nx)
      (zero? (wall-at (int (:x imp)) (int ny))) (assoc :y ny))))

(defn advance-imps
  "Imps walk at the player; one already close enough stops and bites instead,
  costing a point of health per frame per imp."
  [s dt]
  (let [biting? (fn [imp] (and (:alive imp) (< (imp-distance s imp) 0.9)))]
    (-> s
        (update :imps
                (fn [imps]
                  (mapv #(if (or (not (:alive %)) (biting? %)) % (walk-imp s % dt))
                        imps)))
        (update :health - (count (filter biting? (:imps s))))
        (update :health #(max 0 %)))))

(defn read-input
  [s dt]
  (let [;; Mouse look: read the offset from the window centre, turn by it, then
        ;; warp the pointer back, so it can turn forever without leaving.
        dxm (- (rl/get-mouse-x) (quot W 2))
        s (cond-> s
            (not (zero? dxm)) (rotate (* dxm 0.0022))
            (rl/key-down? rl/KEY-LEFT) (rotate (* -1.8 dt))
            (rl/key-down? rl/KEY-RIGHT) (rotate (* 1.8 dt)))
        forward (+ (if (or (rl/key-down? rl/KEY-W) (rl/key-down? rl/KEY-UP)) 1 0)
                   (if (or (rl/key-down? rl/KEY-S) (rl/key-down? rl/KEY-DOWN)) -1 0))
        strafe (+ (if (rl/key-down? rl/KEY-D) 1 0)
                  (if (rl/key-down? rl/KEY-A) -1 0))]
    (rl/set-mouse-position (quot W 2) (quot H 2))
    (cond-> s
      (or (not= 0 forward) (not= 0 strafe))
      (move {:forward forward
             :strafe strafe
             :dt dt})

      (or (rl/mouse-pressed? rl/MOUSE-LEFT) (rl/key-pressed? rl/KEY-SPACE))
      shoot)))

(defn track-fps
  "A frame rate averaged over roughly 0.4s, so the HUD number is readable."
  [s dt]
  (let [frames (inc (:fps-frames s))
        elapsed (+ (:fps-elapsed s) dt)]
    (if (> elapsed 0.4)
      (assoc s :fps (/ frames elapsed) :fps-frames 0 :fps-elapsed 0.0)
      (assoc s :fps-frames frames :fps-elapsed elapsed))))

(defn step
  "One frame of the world. dt is clamped: a long stall must not teleport the
  player through a wall."
  [s]
  (let [dt (min 0.05 (rl/get-frame-time))
        s (read-input s dt)
        s (if (pos? (:health s)) (advance-imps s dt) s)]
    (-> s
        (update :flash #(max 0.0 (- % dt)))
        (track-fps dt))))

;; --- minimap and HUD ---------------------------------------------------------

(def MINIMAP-BG (rl/rgba 12 12 16 210))
(def PLAYER (rl/rgba 245 235 120 255))
(def IMP (rl/rgba 230 80 60 255))
(def CROSSHAIR (rl/rgba 240 240 240 200))
(def HUD-BG (rl/rgba 16 14 18 235))
(def CEILING (rl/rgba 28 26 32 255))
(def FLOOR (rl/rgba 48 42 38 255))
(def MUZZLE-FLASH (rl/rgba 255 220 140 40))

(defn wall-color
  [t]
  (case t
    1 (rl/rgba 150 70 55 255)
    2 (rl/rgba 120 120 130 255)
    3 (rl/rgba 90 110 150 255)
    (rl/rgba 70 180 110 255)))

(defn draw-minimap!
  [{:keys [pos-x pos-y dir-x dir-y imps]}]
  (let [s 7 ox 12 oy 12]
    (rl/rect! {:x (- ox 4)
               :y (- oy 4)
               :width (+ (* MAP-W s) 8)
               :height (+ (* MAP-H s) 8)
               :color MINIMAP-BG})
    (dotimes [y MAP-H]
      (dotimes [x MAP-W]
        (let [t (wall-at x y)]
          (when (pos? t)
            (rl/rect! {:x (+ ox (* x s))
                       :y (+ oy (* y s))
                       :width (dec s)
                       :height (dec s)
                       :color (wall-color t)})))))
    (doseq [imp imps :when (:alive imp)]
      (rl/circle! {:x (+ ox (* (:x imp) s))
                   :y (+ oy (* (:y imp) s))
                   :radius 2.0
                   :color IMP}))
    (rl/circle! {:x (+ ox (* pos-x s))
                 :y (+ oy (* pos-y s))
                 :radius 2.5
                 :color PLAYER})
    (rl/line! {:x1 (+ ox (* pos-x s))
               :y1 (+ oy (* pos-y s))
               :x2 (+ ox (* (+ pos-x (* 2 dir-x)) s))
               :y2 (+ oy (* (+ pos-y (* 2 dir-y)) s))
               :color PLAYER})))

(defn draw-crosshair!
  []
  (let [cx (quot W 2)
        cy (quot H 2)]
    (rl/line! {:x1 (- cx 9)
               :y1 cy
               :x2 (- cx 3)
               :y2 cy
               :color CROSSHAIR})
    (rl/line! {:x1 (+ cx 3)
               :y1 cy
               :x2 (+ cx 9)
               :y2 cy
               :color CROSSHAIR})
    (rl/line! {:x1 cx
               :y1 (- cy 9)
               :x2 cx
               :y2 (- cy 3)
               :color CROSSHAIR})
    (rl/line! {:x1 cx
               :y1 (+ cy 3)
               :x2 cx
               :y2 (+ cy 9)
               :color CROSSHAIR})))

(defn draw-hud!
  [{:keys [health kills shots imps fps]}]
  (let [alive (count (filter :alive imps))]
    (rl/rect! {:x 0
               :y (- H 42)
               :width W
               :height 42
               :color HUD-BG})
    (rl/text! (str "HEALTH " health) {:x 16
                                      :y (- H 32)
                                      :size 22
                                      :color (if (< health 40)
                                               (rl/rgba 235 70 60 255)
                                               (rl/rgba 220 220 210 255))})
    (rl/text! (str "KILLS " kills) {:x 190
                                    :y (- H 32)
                                    :size 22
                                    :color (rl/rgba 220 220 210 255)})
    (rl/text! (str "IMPS " alive) {:x 330
                                   :y (- H 32)
                                   :size 22
                                   :color (rl/rgba 220 180 120 255)})
    (rl/text! (str "SHOTS " shots) {:x 460
                                    :y (- H 32)
                                    :size 22
                                    :color (rl/rgba 150 150 160 255)})
    (rl/text! (format "%d fps  %d cols" (long fps) COLS)
              {:x 620
               :y (- H 32)
               :size 22
               :color (rl/rgba 120 130 140 255)})
    (draw-crosshair!)
    (when (zero? health)
      (rl/text! "YOU DIED" {:x (- (quot W 2) 120)
                            :y (- (quot H 2) 40)
                            :size 54
                            :color (rl/rgba 220 40 40 255)}))))

(defn draw-state!
  [atlas-id s]
  ;; The ceiling is the clear color; the floor is one rectangle over its half.
  (rl/clear-background CEILING)
  (rl/rect! {:x 0
             :y (quot H 2)
             :width W
             :height (quot H 2)
             :color FLOOR})
  (draw-walls! atlas-id s)
  (draw-imps! atlas-id s)
  (when (pos? (:flash s))
    (rl/rect! {:x 0
               :y 0
               :width W
               :height H
               :color MUZZLE-FLASH}))
  (draw-minimap! s)
  (draw-hud! s))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "doom-like raycaster"})
  (rl/set-target-fps 60)
  (rl/hide-cursor)
  ;; Only now is there a GL context to upload a texture into.
  (let [atlas-id (rl/texture-from-fn TILE (* TILE TILES) atlas-pixel)
        deadline (app/auto-quit-deadline)]
    (rl/set-mouse-position (quot W 2) (quot H 2))
    (let [final (loop [frame 0
                       s (initial-state)]
                  (if-not (app/keep-running? deadline)
                    s
                    (let [s' (step s)]
                      (rl/begin-drawing)
                      (draw-state! atlas-id s')
                      (app/maybe-screenshot! frame 60)
                      (rl/end-drawing)
                      (recur (inc frame) s'))))]
      (rl/unload-texture! atlas-id)
      (rl/show-cursor)
      (rl/close-window)
      (println "kills:" (:kills final) "health:" (:health final)))))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
