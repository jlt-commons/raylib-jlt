(ns net.b12n.raylib-jlt.wireframe-shapes
  "Wireframe shapes (`joltc -M:wireframe-shapes`).

  Four wireframe solids, a pyramid, an octahedron, a torus and a helix, each
  tumbling under a 3D camera. Each shape is a list of 3D edges drawn with rlgl
  immediate mode in RL_LINES mode (rl-vertex-3f pairs); rotation/position come from
  the rlgl matrix stack, the same 3D path as camera-3d and rlgl-solar-system."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TAU 6.283185307179586)

;; Each shape is a vector of edges; an edge is [[x1 y1 z1] [x2 y2 z2]] in local space.

(def ^:private pyramid-edges
  (let [b    [[-1.3 -1.0 -1.3] [1.3 -1.0 -1.3] [1.3 -1.0 1.3] [-1.3 -1.0 1.3]]
        apex [0.0 1.6 0.0]]
    (vec (concat (map (fn [i] [(nth b i) (nth b (mod (inc i) 4))]) (range 4))  ; base
                 (map (fn [c] [c apex]) b)))))                                  ; sides

(def ^:private octa-edges
  (let [top [0.0 1.5 0.0] bot [0.0 -1.5 0.0]
        ring [[1.5 0.0 0.0] [0.0 0.0 1.5] [-1.5 0.0 0.0] [0.0 0.0 -1.5]]]
    (vec (concat (map (fn [v] [top v]) ring)
                 (map (fn [v] [bot v]) ring)
                 (map (fn [i] [(nth ring i) (nth ring (mod (inc i) 4))]) (range 4))))))

(def ^:private torus-edges
  (let [rr 1.2 r 0.42 nu 14 nv 7
        pt (fn [ui vi]
             (let [u (* TAU (/ ui (double nu))) v (* TAU (/ vi (double nv)))]
               [(* (+ rr (* r (Math/cos v))) (Math/cos u))
                (* r (Math/sin v))
                (* (+ rr (* r (Math/cos v))) (Math/sin u))]))]
    (vec (for [ui (range nu) vi (range nv)
               e [[(pt ui vi) (pt (mod (inc ui) nu) vi)]      ; around the major ring
                  [(pt ui vi) (pt ui (mod (inc vi) nv))]]]    ; around the minor ring
           e))))

(def ^:private spiral-edges
  (let [n 64 turns 3
        pt (fn [i]
             (let [t (* turns TAU (/ i (double n)))]
               [(* 1.2 (Math/cos t)) (- (* 2.6 (/ i (double n))) 1.3) (* 1.2 (Math/sin t))]))]
    (vec (map (fn [i] [(pt i) (pt (inc i))]) (range n)))))

(def ^:private shapes
  [{:x -6.0
    :edges pyramid-edges
    :color (rl/rgba 255 120 120 255)}
   {:x -2.0
    :edges octa-edges
    :color (rl/rgba 120 200 255 255)}
   {:x  2.0
    :edges torus-edges
    :color (rl/rgba 170 255 120 255)}
   {:x  6.0
    :edges spiral-edges
    :color (rl/rgba 255 220 120 255)}])

(defn- draw-edges!
  [edges color]
  (rl/rl-begin rl/RL-LINES)
  (rl/rl-color! color)
  (doseq [[[x1 y1 z1] [x2 y2 z2]] edges]
    (rl/rl-vertex-3f x1 y1 z1)
    (rl/rl-vertex-3f x2 y2 z2))
  (rl/rl-end))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "wireframe shapes"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [spin (* 0.9 frame)]                              ; degrees
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 12 12 20 255))
          (rl/with-camera-3d {:pos-x 0.0
                              :pos-y 3.2
                              :pos-z 12.0
                              :target-x 0.0
                              :target-y 0.0
                              :target-z 0.0
                              :fovy 45.0
                              :projection 0}
            (fn []
              (doseq [sh shapes]
                (rl/rl-push-matrix)
                (rl/rl-translatef (:x sh) 0.0 0.0)
                (rl/rl-rotatef spin 0.4 1.0 0.3)               ; tumble
                (draw-edges! (:edges sh) (:color sh))
                (rl/rl-pop-matrix))))
          (rl/text! "wireframe shapes via rlgl 3D lines: pyramid, octahedron, torus, helix"
                    {:x 20
                     :y 12
                     :size 18
                     :color rl/RAYWHITE})
          (app/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
