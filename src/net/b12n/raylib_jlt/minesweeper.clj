(ns net.b12n.raylib-jlt.minesweeper
  "raylib [games] example - minesweeper. Left-click reveals (0-cells flood-fill),
  right-click flags; find every safe cell without hitting a mine (SPACE restarts).
  Exercises the mouse-pressed? / MOUSE-RIGHT toolkit binds."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def cols 16)
(def rows 12)
(def cell 30)
(def n-mines 30)
(def top 40)

(defn- place-mines
  []
  (loop [mines #{}]
    (if (>= (count mines) n-mines)
      mines
      (recur (conj mines [(rl/get-random-value 0 (dec cols))
                          (rl/get-random-value 0 (dec rows))])))))

(defn- new-game
  []
  {:mines (place-mines)
   :revealed #{}
   :flagged #{}
   :over? false
   :won? false})

(defn- neighbors
  [[c r]]
  (filterv (fn [[nc nr]] (and (>= nc 0) (< nc cols) (>= nr 0) (< nr rows)))
           (for [dc [-1 0 1] dr [-1 0 1] :when (not (and (zero? dc) (zero? dr)))]
             [(+ c dc) (+ r dr)])))

(defn- mine-count
  [mines cell]
  (count (filter mines (neighbors cell))))

(defn- reveal
  [{:keys [mines revealed]
    :as st} cell]
  (cond
    (revealed cell) st
    (contains? mines cell) (assoc st :over? true)
    :else
    (loop [stack [cell] rev revealed]
      (if (empty? stack)
        (assoc st :revealed rev)
        (let [c (peek stack) stack (pop stack)]
          (if (rev c)
            (recur stack rev)
            (let [rev (conj rev c)]
              (if (zero? (mine-count mines c))
                (recur (into stack (remove rev (neighbors c))) rev)
                (recur stack rev)))))))))

(defn- toggle-flag
  [{:keys [flagged]
    :as st} cell]
  (assoc st :flagged (if (flagged cell) (disj flagged cell) (conj flagged cell))))

(defn- won?
  [{:keys [mines revealed]}]
  (= (count revealed) (- (* cols rows) (count mines))))

(defn- cell-at
  [mx my]
  (let [c (quot mx cell) r (quot (- my top) cell)]
    (when (and (>= (- my top) 0) (>= c 0) (< c cols) (>= r 0) (< r rows)) [c r])))

(defn -main
  [& _]
  (rl/window! {:width (* cols cell)
               :height (+ top (* rows cell))
               :title "raylib [games] example - minesweeper"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [done? (or (:over? st) (:won? st))
              st (cond
                   done? (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (rl/mouse-pressed? rl/MOUSE-LEFT)
                   (if-let [cl (cell-at (rl/get-mouse-x) (rl/get-mouse-y))]
                     (let [st2 (reveal st cl)] (if (won? st2) (assoc st2 :won? true) st2))
                     st)
                   (rl/mouse-pressed? rl/MOUSE-RIGHT)
                   (if-let [cl (cell-at (rl/get-mouse-x) (rl/get-mouse-y))]
                     (toggle-flag st cl) st)
                   :else st)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (dotimes [i (* cols rows)]
            (let [c (mod i cols) r (quot i cols)
                  cl [c r]
                  x (* c cell) y (+ top (* r cell))
                  revealed? (contains? (:revealed st) cl)
                  flagged? (contains? (:flagged st) cl)
                  mine? (contains? (:mines st) cl)]
              (cond
                (and revealed? mine?)
                (rl/rect! {:x x
                           :y y
                           :width cell
                           :height cell
                           :color rl/RED})
                revealed?
                (let [n (mine-count (:mines st) cl)]
                  (rl/rect! {:x x
                             :y y
                             :width cell
                             :height cell
                             :color rl/LIGHTGRAY})
                  (when (pos? n)
                    (rl/text! (str n) {:x (+ x 9)
                                       :y (+ y 5)
                                       :size 22
                                       :color rl/DARKBLUE})))
                :else
                (rl/rect! {:x x
                           :y y
                           :width cell
                           :height cell
                           :color rl/GRAY}))
              (rl/rect-lines! {:x x
                               :y y
                               :width cell
                               :height cell
                               :color rl/DARKGRAY})
              (when (and flagged? (not revealed?))
                (rl/rect! {:x (+ x 8)
                           :y (+ y 8)
                           :width (- cell 16)
                           :height (- cell 16)
                           :color rl/ORANGE}))
              (when (and (:over? st) mine? (not revealed?))
                (rl/circle! {:x (+ x (quot cell 2))
                             :y (+ y (quot cell 2))
                             :radius 6
                             :color rl/BLACK}))))
          (rl/text! (cond (:won? st) "YOU WIN! - SPACE"
                          (:over? st) "BOOM! - SPACE"
                          :else "L: reveal   R: flag")
                    {:x 8
                     :y 10
                     :size 20
                     :color (if (:won? st) rl/DARKGREEN rl/MAROON)})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
