(ns net.b12n.raylib-jlt.cellular-automata
  "raylib [shapes] example - elementary cellular automata (`jolt -M:cellular-automata`).

  Wolfram's one-dimensional automata, one generation per row down the screen. Each
  cell's next state is decided by itself and its two neighbours, and the eight
  possible neighbourhoods are read off the eight bits of the rule number: rule 30
  is chaotic, rule 90 draws a Sierpinski triangle, rule 110 is complicated enough
  to be Turing complete.

  LEFT/RIGHT step through the rules, UP/DOWN jump by ten, SPACE toggles between a
  single live cell in the middle and a random first row. The panel underneath the
  title spells the current rule out as the eight neighbourhood transitions it
  stands for."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PX 3)                 ; cell size in pixels
(def ^:const TOP 110)              ; where the automaton starts, under the header
(def ^:const COLS (quot W PX))
(def ^:const GENS (quot (- H TOP) PX))

(defn- next-row
  [row rule]
  (let [n (count row)]
    (mapv (fn [i]
            ;; The neighbourhood (left, self, right) read as a 3-bit number picks
            ;; which bit of the rule to use. Edges wrap, so the strip is a ring.
            (let [l (nth row (mod (dec i) n))
                  c (nth row i)
                  r (nth row (mod (inc i) n))
                  idx (+ (* 4 l) (* 2 c) r)]
              (bit-and (bit-shift-right rule idx) 1)))
          (range n))))

(defn- seed-row
  [random?]
  (if random?
    (mapv (fn [_] (rl/get-random-value 0 1)) (range COLS))
    (mapv (fn [i] (if (= i (quot COLS 2)) 1 0)) (range COLS))))

(defn- generations
  "The whole triangle, computed once per rule change rather than per frame: the
  automaton is deterministic, so there is nothing to animate between changes."
  [rule random?]
  (loop [rows [(seed-row random?)] k 1]
    (if (>= k GENS)
      rows
      (recur (conj rows (next-row (peek rows) rule)) (inc k)))))

(defn- rule-key!
  "The rule's eight bits drawn as the neighbourhoods they encode: three cells in,
  one cell out, most significant on the left."
  [rule x y]
  (dotimes [k 8]
    (let [idx (- 7 k)
          out (bit-and (bit-shift-right rule idx) 1)
          gx (+ x (* k 62))]
      (dotimes [b 3]
        (let [on (bit-and (bit-shift-right idx (- 2 b)) 1)]
          (rl/rect! {:x (+ gx (* b 13))
                     :y y
                     :width 11
                     :height 11
                     :color (if (= 1 on) rl/DARKGRAY rl/LIGHTGRAY)})))
      (rl/rect! {:x (+ gx 13)
                 :y (+ y 14)
                 :width 11
                 :height 11
                 :color (if (= 1 out) rl/MAROON rl/LIGHTGRAY)}))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - cellular automata"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           rule 30
           random? false
           rows (generations 30 false)]
      (when (app/keep-running? deadline)
        (let [d (cond
                  (rl/key-pressed? rl/KEY-RIGHT) 1
                  (rl/key-pressed? rl/KEY-LEFT) -1
                  (rl/key-pressed? rl/KEY-UP) 10
                  (rl/key-pressed? rl/KEY-DOWN) -10
                  :else 0)
              flip? (rl/key-pressed? rl/KEY-SPACE)
              new-rule (mod (+ rule d) 256)
              random? (if flip? (not random?) random?)
              rows (if (or (not= new-rule rule) flip?)
                     (generations new-rule random?)
                     rows)
              rule new-rule]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (dotimes [g (count rows)]
            (let [row (nth rows g)
                  y (+ TOP (* g PX))]
              (dotimes [i COLS]
                (when (= 1 (nth row i))
                  (rl/rect! {:x (* i PX)
                             :y y
                             :width PX
                             :height PX
                             :color (rl/rgba 20 30 60 255)})))))
          (rl/rect! {:x 0
                     :y 0
                     :width W
                     :height TOP
                     :color rl/RAYWHITE})
          (rl/text! (str "rule " rule) {:x 20
                                        :y 16
                                        :size 24
                                        :color rl/DARKGRAY})
          (rl/text! (if random? "random first row" "one live cell")
                    {:x 150
                     :y 22
                     :size 16
                     :color rl/GRAY})
          (rule-key! rule 20 52)
          (rl/text! "LEFT/RIGHT rule   UP/DOWN by ten   SPACE seed"
                    {:x 20
                     :y 88
                     :size 13
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) rule random? rows)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
