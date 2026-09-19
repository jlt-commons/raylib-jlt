(ns net.b12n.raylib-jlt.rlgl-triangle
  "raylib [shapes] example - rlgl triangle (`jolt -M:rlgl-triangle`).

  Port of raylib's examples/shapes/shapes_rlgl_triangle.c. One Gouraud-shaded
  triangle whose three corners you can drag. SPACE switches between the filled
  triangle and its outline, R puts the corners back.

  The colour across the face is the point. rlgl carries a colour per vertex, so
  setting red, green and blue at the three corners lets the GPU interpolate the
  whole gradient with no work here. That is the same rlColor4ub/rlVertex2f path
  the rest of the suite uses to get around raylib's by-value Vector2 calls, used
  for what it is actually good at rather than as a workaround.

  Winding matters in filled mode. raylib's default culls back faces, so dragging
  a corner past the other two flips the triangle away and it vanishes. The C
  offers rlEnableBackfaceCulling and rlDisableBackfaceCulling on the arrow keys
  to show that off; neither is bound here, so instead the corners are sorted into
  a consistent winding before drawing and the triangle simply never disappears.
  The trade is deliberate: the example keeps working, and the culling lesson
  moves to this docstring."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const HANDLE 8.0)

(def ^:private start-positions
  [[400.0 150.0] [300.0 300.0] [500.0 300.0]])

(def ^:private corner-colors
  [[255 0 0 255] [0 255 0 255] [0 0 255 255]])

(defn- near?
  [[px py] mx my]
  (let [dx (- mx px) dy (- my py)]
    (<= (+ (* dx dx) (* dy dy)) (* HANDLE HANDLE))))

(defn- counter-clockwise?
  "Sign of the cross product of two edges. Positive means one winding, negative
  the other; which is which depends on the y axis pointing down."
  [[ax ay] [bx by] [cx cy]]
  (neg? (- (* (- bx ax) (- cy ay))
           (* (- by ay) (- cx ax)))))

(defn- wound
  "The three corners in a consistent winding, with their colours carried along so
  a corner keeps its colour when the order swaps."
  [pts]
  (let [[a b c] pts]
    (if (counter-clockwise? a b c)
      [[a 0] [b 1] [c 2]]
      [[a 0] [c 2] [b 1]])))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - rlgl triangle"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           pts start-positions
           dragging nil
           lines? false]
      (when (app/keep-running? deadline)
        (let [mx      (double (rl/get-mouse-x))
              my      (double (rl/get-mouse-y))
              down?   (rl/mouse-down? rl/MOUSE-LEFT)
              lines?  (if (rl/key-pressed? rl/KEY-SPACE) (not lines?) lines?)
              reset?  (rl/key-pressed? rl/KEY-R)
              ;; Grab the first handle under the cursor, and keep it until release.
              dragging (cond
                         (not down?) nil
                         (some? dragging) dragging
                         :else (first (keep-indexed (fn [i p] (when (near? p mx my) i)) pts)))
              pts     (cond
                        reset?           start-positions
                        (some? dragging) (assoc pts dragging [mx my])
                        :else            pts)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (if lines?
            (do
              (rl/rl-begin rl/RL-LINES)
              ;; Three edges, each drawn as its own pair so both ends can carry
              ;; their own colour.
              (dotimes [i 3]
                (let [[x0 y0] (nth pts i)
                      [x1 y1] (nth pts (mod (inc i) 3))
                      [r0 g0 b0 a0] (nth corner-colors i)
                      [r1 g1 b1 a1] (nth corner-colors (mod (inc i) 3))]
                  (rl/rl-color! (rl/rgba r0 g0 b0 a0))
                  (rl/rl-vertex-2f x0 y0)
                  (rl/rl-color! (rl/rgba r1 g1 b1 a1))
                  (rl/rl-vertex-2f x1 y1)))
              (rl/rl-end))
            (do
              (rl/rl-begin rl/RL-TRIANGLES)
              (doseq [[[x y] colour-idx] (wound pts)]
                (let [[r g b a] (nth corner-colors colour-idx)]
                  (rl/rl-color! (rl/rgba r g b a))
                  (rl/rl-vertex-2f x y)))
              (rl/rl-end)))
          ;; Handles last, so they sit over the triangle.
          (dotimes [i 3]
            (let [[x y] (nth pts i)]
              (rl/circle! {:x (int x)
                           :y (int y)
                           :radius HANDLE
                           :color (if (= dragging i) rl/DARKGRAY rl/GRAY)})))
          (rl/text! "drag a corner - [SPACE] lines/filled - [R] reset"
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) pts dragging lines?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
