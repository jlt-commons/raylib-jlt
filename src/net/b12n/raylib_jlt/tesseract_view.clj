(ns net.b12n.raylib-jlt.tesseract-view
  "Tesseract (`joltc -M:tesseract-view`).

  A rotating 4D hypercube: 16 vertices (every coordinate ±1) and 32 edges (a pair
  is joined when it differs in exactly one coordinate). It spins in two 4D planes,
  then projects 4D→3D→2D by perspective, so the inner cube appears to turn
  inside-out through the outer one. Pure math + 2D lines, no camera. Inner cube is
  red, outer cube blue, the connecting edges green."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const D4 3.0)       ; 4D viewer distance
(def ^:const D3 4.0)       ; 3D viewer distance
(def ^:const SCALE 820.0)

(def ^:private verts4
  (vec (for [x [-1.0 1.0] y [-1.0 1.0] z [-1.0 1.0] w [-1.0 1.0]] [x y z w])))

(def ^:private edges
  (vec (for [i (range 16) j (range (inc i) 16)
             :when (= 1 (reduce + (map (fn [p q] (if (== p q) 0 1))
                                       (nth verts4 i) (nth verts4 j))))]
         [i j])))

(defn- rot4
  "Rotate a 4D point: the x,w plane by angle a and the y,z plane by angle b."
  [[x y z w] a b]
  (let [ca (Math/cos a) sa (Math/sin a) cb (Math/cos b) sb (Math/sin b)]
    [(- (* x ca) (* w sa))
     (- (* y cb) (* z sb))
     (+ (* y sb) (* z cb))
     (+ (* x sa) (* w ca))]))

(defn- project
  "Perspective-project a 4D point to a 2D screen point (4D→3D, then 3D→2D)."
  [[x y z w]]
  (let [k4 (/ 1.0 (- D4 w))
        x3 (* x k4) y3 (* y k4) z3 (* z k4)
        k3 (/ SCALE (- D3 z3))]
    [(+ (/ W 2.0) (* x3 k3)) (+ (/ H 2.0) (* y3 k3))]))

(defn- edge-color
  [i j]
  (let [wi (nth (nth verts4 i) 3) wj (nth (nth verts4 j) 3)]
    (cond (and (neg? wi) (neg? wj)) (rl/rgba 255 90 90 255)     ; inner cube (w = -1)
          (and (pos? wi) (pos? wj)) (rl/rgba 90 170 255 255)    ; outer cube (w = +1)
          :else                     (rl/rgba 110 240 110 255)))) ; connecting edges

(defn -main
  [& _]
  (rl/window! :width W :height H :title "tesseract")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [a   (* 0.02 frame)
              pts (mapv (fn [v] (project (rot4 v a (* a 0.6)))) verts4)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 8 8 16 255))
          (doseq [[i j] edges]
            (let [[x1 y1] (nth pts i)
                  [x2 y2] (nth pts j)]
              (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2)
                        :color (edge-color i j))))
          (rl/text! "a rotating tesseract (4D hypercube)" :x 12 :y 12 :size 20 :color rl/RAYWHITE)
          (rl/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
