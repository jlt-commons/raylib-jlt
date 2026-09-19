(ns net.b12n.raylib-jlt.rectangle-advanced
  "raylib [shapes] example - rectangle advanced (`jolt -M:rectangle-advanced`).

  Port of raylib's examples/shapes/shapes_rectangle_advanced.c. Five bars, each
  rounded by a different amount on the left and right, each filled with a
  horizontal gradient.

  raylib has no call for this. The C example builds it out of rlgl by hand, for
  exactly the reason the rest of this suite reaches for rlgl: DrawRectangleRounded
  takes a Rectangle by value and only accepts one roundness and one colour. So
  both the C and this port draw the outline vertex by vertex.

  The shape is a triangle fan from the centre. Walking the outline gives the four
  corner arcs, each swept over `segments/4` steps at whatever radius its own side
  asked for, joined by the straight runs between them. A corner with roundness 0
  collapses to a right angle because its arc radius is zero, which is why the
  same loop draws a square and a lozenge without a special case.

  The gradient is free once the fan exists. Colour is interpolated per vertex
  from each point's own x, so the GPU fills between them and the bar shades
  smoothly however many segments it has.

  See rounded-rectangle for the single-roundness version, and triangle-strip for
  the same immediate-mode path used more simply."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SEGMENTS 36)

(defn- lerp [a b t] (+ a (* (- b a) t)))

(defn- outline
  "The rounded rectangle's perimeter as [x y] points, counter-clockwise from the
  top-left. roundness is 0 to 1 per side, scaled against the shorter dimension so
  a fully rounded short bar becomes a lozenge rather than overshooting."
  [x y w h round-left round-right]
  (let [half (/ (min w h) 2.0)
        rl* (* half (max 0.0 (min 1.0 round-left)))
        rr  (* half (max 0.0 (min 1.0 round-right)))
        per (max 1 (quot SEGMENTS 4))
        arc (fn [cx cy r from-deg]
              ;; A quarter turn from from-deg, clockwise in screen space.
              (for [i (range (inc per))]
                (let [a (Math/toRadians (+ from-deg (* 90.0 (/ (double i) per))))]
                  [(+ cx (* r (Math/cos a))) (+ cy (* r (Math/sin a)))])))]
    (vec (concat
          (arc (+ x rl*) (+ y rl*) rl* 180.0)              ; top-left
          (arc (- (+ x w) rr) (+ y rr) rr 270.0)           ; top-right
          (arc (- (+ x w) rr) (- (+ y h) rr) rr 0.0)       ; bottom-right
          (arc (+ x rl*) (- (+ y h) rl*) rl* 90.0)))))     ; bottom-left

(defn- rounded-gradient!
  "A triangle fan over the outline, each vertex coloured by its own x."
  [x y w h round-left round-right [lr lg lb] [rr* rg rb]]
  (let [pts (outline x y w h round-left round-right)
        cx  (+ x (/ w 2.0))
        cy  (+ y (/ h 2.0))
        at  (fn [px] (let [t (max 0.0 (min 1.0 (/ (- px x) w)))]
                       (rl/rgba (int (lerp lr rr* t)) (int (lerp lg rg t)) (int (lerp lb rb t)) 255)))
        mid (at cx)]
    (rl/rl-begin rl/RL-TRIANGLES)
    (dotimes [i (count pts)]
      (let [[x0 y0] (nth pts i)
            [x1 y1] (nth pts (mod (inc i) (count pts)))]
        ;; rim, centre, rim: raylib's front-facing winding, same as sector!.
        (rl/rl-color! (at x0)) (rl/rl-vertex-2f x0 y0)
        (rl/rl-color! mid)     (rl/rl-vertex-2f cx cy)
        (rl/rl-color! (at x1)) (rl/rl-vertex-2f x1 y1)))
    (rl/rl-end)))

;; [left-roundness right-roundness left-colour right-colour], as the C has them.
(def ^:private bars
  [[0.8 0.8 [0 121 241] [230 41 55]]
   [0.5 1.0 [230 41 55] [255 109 194]]
   [1.0 0.5 [230 41 55] [0 121 241]]
   [0.0 1.0 [0 121 241] [0 0 0]]
   [1.0 0.0 [0 121 241] [255 109 194]]])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - rectangle advanced"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [w (- W 100.0)
              h (/ (- H 100.0) (count bars))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (dotimes [i (count bars)]
            (let [[rl-round rr-round left right] (nth bars i)]
              (rounded-gradient! 50.0 (+ 50.0 (* i (inc h))) w (dec h)
                                 rl-round rr-round left right)))
          (rl/text! "per-side roundness, horizontal gradient, one triangle fan each"
                    {:x 50
                     :y 16
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
