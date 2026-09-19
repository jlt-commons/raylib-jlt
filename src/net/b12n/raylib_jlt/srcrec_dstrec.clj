(ns net.b12n.raylib-jlt.srcrec-dstrec
  "raylib [textures] example - srcrec dstrec.

  One frame of a procedurally generated sprite sheet drawn like
  DrawTexturePro: a source rectangle picks the frame, a destination
  rectangle scales it 2x at screen center, and an origin offset makes it
  spin in place (rotation increments every frame). Q quits.

  No binding for DrawTexturePro exists (Rectangle/Vector2 by-value args,
  see net.b12n.raylib.textures), so this reimplements it directly: rotated-quad-corners
  mirrors raylib's own rtextures.c algorithm (rotate the four destination
  corners around the origin offset, before translating to screen
  position), and the quad is emitted through the same low-level rlgl
  calls rl/texture! and polygon-drawing.clj already use
  (rlSetTexture/rlBegin/rlTexCoord2f/rlVertex2f) -- no new FFI. The sheet
  is six colored, ringed frames built with rl/texture-from-fn rather than
  the C example's scarfy.png, matching this suite's no-external-assets
  convention.
  Loosely based on raylib/examples/textures/textures_srcrec_dstrec.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const FRAME-W 64)
(def ^:const FRAME-H 64)
(def ^:const FRAMES 6)
(def ^:const SHEET-W (* FRAME-W FRAMES))
(def ^:const FRAME-SHOWN 3)

(defn- sheet-pixel
  "Frame `(quot x FRAME-W)` gets a distinct ring color; the ring itself
  reads as a simple icon rather than a flat swatch."
  [x y]
  (let [frame (quot x FRAME-W)
        lx (- x (* frame FRAME-W))
        cx (/ FRAME-W 2.0)
        cy (/ FRAME-H 2.0)
        d (Math/sqrt (+ (Math/pow (- lx cx) 2) (Math/pow (- y cy) 2)))
        bg (nth [rl/RED rl/ORANGE rl/GOLD rl/GREEN rl/SKYBLUE rl/VIOLET] frame)]
    (if (< d (* FRAME-W 0.32)) rl/RAYWHITE bg)))

(defn- rotated-quad-corners
  "The four corners of a `dw` x `dh` rect anchored at [x y], rotated by
  `rot-deg` around the [ox oy] origin offset -- raylib's own
  DrawTexturePro algorithm (rtextures.c), reimplemented directly since
  there is no by-value binding for it."
  [x y dw dh ox oy rot-deg]
  (let [rad (Math/toRadians rot-deg)
        s (Math/sin rad)
        c (Math/cos rad)
        dx (- ox)
        dy (- oy)]
    ;; A point (px, py) in origin-relative space rotates to
    ;; (x + px*c - py*s, y + px*s + py*c).
    {:tl [(+ x (* dx c) (- (* dy s))) (+ y (* dx s) (* dy c))]
     :tr [(+ x (* (+ dx dw) c) (- (* dy s))) (+ y (* (+ dx dw) s) (* dy c))]
     :bl [(+ x (* dx c) (- (* (+ dy dh) s))) (+ y (* dx s) (* (+ dy dh) c))]
     :br [(+ x (* (+ dx dw) c) (- (* (+ dy dh) s))) (+ y (* (+ dx dw) s) (* (+ dy dh) c))]}))

(defn- draw-texture-pro!
  "DrawTexturePro, reimplemented over rlgl immediate mode: src is
  [sx sy sw sh] in texture pixels, dst is [dx dy dw dh] in screen pixels,
  origin is [ox oy] within dst, rotation in degrees."
  [tex sheet-w sheet-h [sx sy sw sh] [dx dy dw dh] [ox oy] rot-deg]
  (let [{:keys [tl tr bl br]} (rotated-quad-corners dx dy dw dh ox oy rot-deg)
        u0 (/ sx (double sheet-w))
        v0 (/ sy (double sheet-h))
        u1 (/ (+ sx sw) (double sheet-w))
        v1 (/ (+ sy sh) (double sheet-h))]
    (rl/rl-set-texture tex)
    (rl/rl-begin rl/RL-QUADS)
    (rl/rl-color! rl/WHITE)
    (rl/rl-tex-coord-2f u0 v0) (apply rl/rl-vertex-2f tl)
    (rl/rl-tex-coord-2f u0 v1) (apply rl/rl-vertex-2f bl)
    (rl/rl-tex-coord-2f u1 v1) (apply rl/rl-vertex-2f br)
    (rl/rl-tex-coord-2f u1 v0) (apply rl/rl-vertex-2f tr)
    (rl/rl-end)
    (rl/rl-set-texture 0)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - srcrec dstrec"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sheet (rl/texture-from-fn SHEET-W FRAME-H sheet-pixel)
        cx (/ W 2.0)
        cy (/ H 2.0)]
    (loop [frame 0 rotation 0.0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (draw-texture-pro!
         sheet SHEET-W FRAME-H
         [(* FRAME-SHOWN FRAME-W) 0.0 (double FRAME-W) (double FRAME-H)]
         [cx cy (* 2.0 FRAME-W) (* 2.0 FRAME-H)]
         [FRAME-W FRAME-H]
         rotation)
        (rl/line! {:x1 (int cx)
                   :y1 0
                   :x2 (int cx)
                   :y2 H
                   :color rl/GRAY})
        (rl/line! {:x1 0
                   :y1 (int cy)
                   :x2 W
                   :y2 (int cy)
                   :color rl/GRAY})
        (rl/text! "srcrec picks the frame, dstrec scales it, origin spins it"
                  {:x 10
                   :y 10
                   :size 10
                   :color rl/GRAY})
        (app/maybe-screenshot! frame 30)
        (rl/end-drawing)
        (recur (inc frame) (+ rotation 1.0))))
    (rl/unload-texture! sheet))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
