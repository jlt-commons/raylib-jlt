(ns net.b12n.raylib-jlt.penrose-tiling
  "raylib [shapes] example - Penrose (P3 rhombus) tiling by deflation of Robinson
  triangles. A 10-triangle 'sun' seed is subdivided N times using golden-ratio lerps
  (Preshing's rules); triangles are filled (two colors by kind) as an rlgl batch and
  their edges stroked. Each fill triangle is winding-normalized to the front face so
  none are backface-culled. In the spirit of shapes_penrose_tile."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:private phi (/ (+ 1.0 (Math/sqrt 5.0)) 2.0))
(def ^:private inv (/ 1.0 phi))

(defn- lerp
  [[ax ay] [bx by] s]
  [(+ ax (* (- bx ax) s)) (+ ay (* (- by ay) s))])

(defn- wheel
  "Seed: 10 Robinson triangles (kind 0) around (cx,cy) forming a decagon."
  [cx cy radius]
  (vec (for [i (range 10)]
         (let [ba (/ (* (- (* 2 i) 1) Math/PI) 10.0)
               ca (/ (* (+ (* 2 i) 1) Math/PI) 10.0)
               b [(+ cx (* radius (Math/cos ba))) (+ cy (* radius (Math/sin ba)))]
               c [(+ cx (* radius (Math/cos ca))) (+ cy (* radius (Math/sin ca)))]
               a [cx cy]]
           (if (even? i) [0 a c b] [0 a b c])))))

(defn- subdivide
  [tris]
  (vec (mapcat (fn [[k a b c]]
                 (if (zero? k)
                   (let [p (lerp a b inv)]
                     [[0 c p b] [1 p c a]])
                   (let [q (lerp b a inv)
                         r (lerp b c inv)]
                     [[1 r c a] [1 q r b] [0 r q a]])))
               tris)))

(defn- deflate
  [seed n]
  (loop [tris seed i 0]
    (if (< i n) (recur (subdivide tris) (inc i)) tris)))

(defn- front
  "Normalize a triangle's winding to the front face (negative signed area in screen
  coords), swapping b and c if needed, so the rlgl fill isn't backface-culled."
  [[k a b c]]
  (let [[ax ay] a [bx by] b [cx cy] c
        area (- (* (- bx ax) (- cy ay)) (* (- cx ax) (- by ay)))]
    (if (> area 0.0) [k a c b] [k a b c])))

(def ^:private col0 (rl/rgba 235 130 60 255))   ; thin rhombus halves
(def ^:private col1 (rl/rgba 70 130 200 255))    ; thick rhombus halves
(def ^:private edge (rl/rgba 30 30 40 130))

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - penrose tiling"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        tris (mapv front (deflate (wheel 400 230 235.0) 5))]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background (rl/rgba 18 18 24 255))
        ;; fills (one rlgl batch)
        (rl/rl-begin rl/RL-TRIANGLES)
        (doseq [[k [ax ay] [bx by] [cx cy]] tris]
          (rl/rl-color! (if (zero? k) col0 col1))
          (rl/rl-vertex-2f (double ax) (double ay))
          (rl/rl-vertex-2f (double bx) (double by))
          (rl/rl-vertex-2f (double cx) (double cy)))
        (rl/rl-end)
        ;; edges
        (doseq [[_ [ax ay] [bx by] [cx cy]] tris]
          (rl/line! {:x1 (int ax)
                     :y1 (int ay)
                     :x2 (int bx)
                     :y2 (int by)
                     :color edge})
          (rl/line! {:x1 (int bx)
                     :y1 (int by)
                     :x2 (int cx)
                     :y2 (int cy)
                     :color edge})
          (rl/line! {:x1 (int cx)
                     :y1 (int cy)
                     :x2 (int ax)
                     :y2 (int ay)
                     :color edge}))
        (rl/text! "Penrose P3 tiling (deflation)" {:x 10
                                                   :y 10
                                                   :size 20
                                                   :color rl/RAYWHITE})
        (rl/maybe-screenshot! frame 12)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
