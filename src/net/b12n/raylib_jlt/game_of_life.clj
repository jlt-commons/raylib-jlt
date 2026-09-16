(ns net.b12n.raylib-jlt.game-of-life
  "raylib [generative] example - Conway's Game of Life. A random soup evolves by the
  classic B3/S23 rules on a toroidal grid; SPACE reseeds. State is the set of live
  cells."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def cols 80)
(def rows 45)
(def cell 10)
(def tick 6)

(defn- seed
  []
  (set (for [c (range cols) r (range rows)
             :when (< (rl/get-random-value 0 99) 28)]
         [c r])))

(defn- neighbors
  [[c r]]
  (for [dc [-1 0 1] dr [-1 0 1] :when (not (and (zero? dc) (zero? dr)))]
    [(mod (+ c dc) cols) (mod (+ r dr) rows)]))

(defn- next-gen
  [live]
  (let [candidates (into live (mapcat neighbors live))]
    (set (filter (fn [cell]
                   (let [n (count (filter live (neighbors cell)))]
                     (if (live cell) (or (= n 2) (= n 3)) (= n 3))))
                 candidates))))

(defn -main
  [& _]
  (rl/window! {:width (* cols cell)
               :height (* rows cell)
               :title "raylib [generative] example - game of life"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           live (seed)]
      (when (rl/keep-running? deadline)
        (let [live (cond (rl/key-pressed? rl/KEY-SPACE) (seed)
                         (zero? (mod frame tick)) (next-gen live)
                         :else live)]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [[c r] live]
            (rl/rect! {:x (* c cell)
                       :y (* r cell)
                       :width (- cell 1)
                       :height (- cell 1)
                       :color rl/LIME}))
          (rl/text! (str (count live) " cells - SPACE reseeds") {:x 8
                                                                 :y 6
                                                                 :size 18
                                                                 :color rl/RAYWHITE})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) live)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
