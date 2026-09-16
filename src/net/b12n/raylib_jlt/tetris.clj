(ns net.b12n.raylib-jlt.tetris
  "Tetris (`joltc -M:tetris`).

  A full Tetris in a 10x20 well: the seven tetrominoes fall, LEFT/RIGHT move,
  UP rotates, DOWN soft-drops, SPACE hard-drops. Full rows clear and score; speed
  rises with level; ENTER restarts after GAME OVER. (Moves are one cell per key
  press, tap to nudge.)

  The whole game is one immutable state map threaded through the loop; `step` reads
  input and returns the next state. The board is a vector of ROWS rows, each a
  vector of COLS cells (nil = empty, else a packed Color)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const COLS 10)
(def ^:const ROWS 20)
(def ^:const CELL 18)
(def ^:const WELL-X 200)
(def ^:const WELL-Y 40)
(def ^:const KEY-ENTER 257)

;; Each piece: a color + its rotation states. A state is four [col row] cells in a
;; small local grid; pieces with fewer distinct rotations just list what they have.
(def ^:private PIECES
  {:i {:color rl/SKYBLUE
       :rots [[[0 1] [1 1] [2 1] [3 1]] [[2 0] [2 1] [2 2] [2 3]]]}
   :o {:color rl/GOLD
       :rots [[[1 0] [2 0] [1 1] [2 1]]]}
   :t {:color rl/PURPLE
       :rots [[[1 0] [0 1] [1 1] [2 1]] [[1 0] [1 1] [2 1] [1 2]]
              [[0 1] [1 1] [2 1] [1 2]] [[1 0] [0 1] [1 1] [1 2]]]}
   :s {:color rl/LIME
       :rots [[[1 0] [2 0] [0 1] [1 1]] [[1 0] [1 1] [2 1] [2 2]]]}
   :z {:color rl/RED
       :rots [[[0 0] [1 0] [1 1] [2 1]] [[2 0] [1 1] [2 1] [1 2]]]}
   :j {:color rl/BLUE
       :rots [[[0 0] [0 1] [1 1] [2 1]] [[1 0] [2 0] [1 1] [1 2]]
              [[0 1] [1 1] [2 1] [2 2]] [[1 0] [1 1] [0 2] [1 2]]]}
   :l {:color rl/ORANGE
       :rots [[[2 0] [0 1] [1 1] [2 1]] [[1 0] [1 1] [1 2] [2 2]]
              [[0 1] [1 1] [2 1] [0 2]] [[0 0] [1 0] [1 1] [1 2]]]}})

(def ^:private PIECE-TYPES [:i :o :t :s :z :j :l])

;; --- pure model --------------------------------------------------------------
(defn- rand-type
  []
  (nth PIECE-TYPES (rl/get-random-value 0 6)))

(defn- spawn
  [type]
  {:type type
   :rot 0
   :x 3
   :y 0})

(defn- interval
  [level]
  (max 6 (- 48 (* 4 level))))

(defn- piece-cells
  [{:keys [type rot x y]}]
  (let [rots  (:rots (PIECES type))
        state (nth rots (mod rot (count rots)))]
    (map (fn [[cx cy]] [(+ x cx) (+ y cy)]) state)))

(defn- valid?
  "True when every cell of `piece` is on the board and lands on an empty cell."
  [board piece]
  (every? (fn [[c r]]
            (and (>= c 0) (< c COLS) (>= r 0) (< r ROWS) (nil? (get-in board [r c]))))
          (piece-cells piece)))

(defn- lock
  [board piece]
  (let [color (:color (PIECES (:type piece)))]
    (reduce (fn [b [c r]] (assoc-in b [r c] color)) board (piece-cells piece))))

(defn- clear-lines
  "Drop full rows, refill from the top. Returns [board cleared-count]."
  [board]
  (let [kept    (filterv (fn [row] (some nil? row)) board)
        cleared (- ROWS (count kept))]
    [(into (vec (repeat cleared (vec (repeat COLS nil)))) kept) cleared]))

(defn- hard-drop
  [board piece]
  (loop [p piece]
    (let [pd (update p :y inc)]
      (if (valid? board pd) (recur pd) p))))

(defn- initial-state
  []
  {:board (vec (repeat ROWS (vec (repeat COLS nil))))
   :piece (spawn (rand-type))
   :next (rand-type)
   :score 0
   :lines 0
   :level 0
   :tick 0
   :over? false})

(defn- lock-and-next
  "Lock `piece`, clear lines, score, and bring up the next piece (game over if it
  can't spawn)."
  [s board piece]
  (let [[board' cleared] (clear-lines (lock board piece))
        lines   (+ (:lines s) cleared)
        next-p  (spawn (:next s))]
    (assoc s :board board' :piece next-p :next (rand-type) :tick 0
           :lines lines :level (quot lines 10)
           :score (+ (:score s) (nth [0 40 100 300 1200] cleared))
           :over? (not (valid? board' next-p)))))

(defn- step
  [s]
  (if (:over? s)
    (if (rl/key-pressed? KEY-ENTER) (initial-state) s)
    (let [board (:board s)
          dx    (cond (rl/key-pressed? rl/KEY-LEFT) -1 (rl/key-pressed? rl/KEY-RIGHT) 1 :else 0)
          p1    (let [p (update (:piece s) :x + dx)] (if (valid? board p) p (:piece s)))
          p2    (if (rl/key-pressed? rl/KEY-UP)
                  (let [p (update p1 :rot inc)] (if (valid? board p) p p1))
                  p1)
          soft? (rl/key-down? rl/KEY-DOWN)
          tick  (inc (:tick s))
          drop? (or soft? (>= tick (interval (:level s))))]
      (cond
        (rl/key-pressed? rl/KEY-SPACE) (lock-and-next s board (hard-drop board p2))
        drop? (let [pd (update p2 :y inc)]
                (if (valid? board pd) (assoc s :piece pd :tick 0) (lock-and-next s board p2)))
        :else (assoc s :piece p2 :tick tick)))))

;; --- draw --------------------------------------------------------------------
(defn- cell!
  [c r color]
  (rl/rect! {:x (+ WELL-X (* c CELL))
             :y (+ WELL-Y (* r CELL))
             :width (dec CELL)
             :height (dec CELL)
             :color color}))

(defn- draw-state
  [s]
  (rl/clear-background (rl/rgba 18 18 28 255))
  (rl/rect-lines! {:x (- WELL-X 2)
                   :y (- WELL-Y 2)
                   :width (+ (* COLS CELL) 4)
                   :height (+ (* ROWS CELL) 4)
                   :color rl/GRAY})
  (doseq [r (range ROWS) c (range COLS)]
    (when-let [color (get-in (:board s) [r c])] (cell! c r color)))
  (let [color (:color (PIECES (:type (:piece s))))]
    (doseq [[c r] (piece-cells (:piece s))]
      (when (>= r 0) (cell! c r color))))
  ;; side panel
  (rl/text! "TETRIS" {:x 470
                      :y 40
                      :size 34
                      :color rl/RAYWHITE})
  (rl/text! (str "SCORE " (:score s)) {:x 470
                                       :y 110
                                       :size 20
                                       :color rl/RAYWHITE})
  (rl/text! (str "LINES " (:lines s)) {:x 470
                                       :y 140
                                       :size 20
                                       :color rl/RAYWHITE})
  (rl/text! (str "LEVEL " (:level s)) {:x 470
                                       :y 170
                                       :size 20
                                       :color rl/RAYWHITE})
  (rl/text! "NEXT" {:x 470
                    :y 220
                    :size 20
                    :color rl/GRAY})
  (let [color (:color (PIECES (:next s)))]
    (doseq [[cx cy] (first (:rots (PIECES (:next s))))]
      (rl/rect! {:x (+ 480 (* cx CELL))
                 :y (+ 250 (* cy CELL))
                 :width (dec CELL)
                 :height (dec CELL)
                 :color color})))
  (rl/text! "<> move   ^ rotate   v soft   SPACE hard drop"
            {:x 30
             :y (- H 26)
             :size 16
             :color rl/GRAY})
  (when (:over? s)
    (rl/text! "GAME OVER" {:x 130
                           :y 180
                           :size 34
                           :color rl/RED})
    (rl/text! "ENTER to restart" {:x 120
                                  :y 230
                                  :size 18
                                  :color rl/RAYWHITE})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "tetris"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 s (initial-state)]
      (when (rl/keep-running? deadline)
        (let [s' (step s)]
          (rl/begin-drawing)
          (draw-state s')
          (rl/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame) s')))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
