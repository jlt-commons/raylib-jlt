(ns net.b12n.raylib-jlt.snake
  "raylib [games] example - the classic snake. Arrow keys steer; eat food to grow;
  hitting a wall or yourself ends it (SPACE restarts). Grid + frame-tick movement,
  all state threaded through the loop."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def cols 32)
(def rows 18)
(def cell 25)
(def tick 6)   ; advance one cell every N frames

(defn- rand-food
  [snake]
  (loop []
    (let [f [(rl/get-random-value 0 (dec cols)) (rl/get-random-value 0 (dec rows))]]
      (if (some (fn [s] (= s f)) snake) (recur) f))))

(defn- new-game
  []
  (let [snake [[16 9] [15 9] [14 9]]]
    {:snake snake
     :dir [1 0]
     :food (rand-food snake)
     :dead? false}))

(defn- turn
  [dir]
  (let [want (cond (rl/key-pressed? rl/KEY-UP)    [0 -1]
                   (rl/key-pressed? rl/KEY-DOWN)  [0 1]
                   (rl/key-pressed? rl/KEY-LEFT)  [-1 0]
                   (rl/key-pressed? rl/KEY-RIGHT) [1 0]
                   :else nil)]
    (if (and want (not= want [(- (first dir)) (- (second dir))])) want dir)))

(defn- step
  [{:keys [snake dir food]
    :as st}]
  (let [[hc hr] (first snake)
        [dc dr] dir
        head [(+ hc dc) (+ hr dr)]
        [nc nr] head]
    (if (or (< nc 0) (>= nc cols) (< nr 0) (>= nr rows) (some (fn [s] (= s head)) snake))
      (assoc st :dead? true)
      (if (= head food)
        (let [ns (into [head] snake)]
          (assoc st :snake ns :food (rand-food ns)))
        (assoc st :snake (into [head] (pop (vec snake))))))))

(defn -main
  [& _]
  (rl/window! :width (* cols cell) :height (* rows cell) :title "raylib [games] example - snake")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [st (if (:dead? st)
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (let [st (assoc st :dir (turn (:dir st)))]
                     (if (zero? (mod frame tick)) (step st) st)))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (let [[fc fr] (:food st)]
            (rl/rect! :x (* fc cell) :y (* fr cell) :width cell :height cell :color rl/RED))
          (doseq [[c r] (:snake st)]
            (rl/rect! :x (+ 1 (* c cell)) :y (+ 1 (* r cell))
                      :width (- cell 2) :height (- cell 2) :color rl/LIME))
          (rl/text! (str "len " (count (:snake st))) :x 8 :y 6 :size 20 :color rl/RAYWHITE)
          (when (:dead? st)
            (rl/text! "GAME OVER - SPACE to restart" :x 150 :y 210 :size 24 :color rl/RAYWHITE))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
