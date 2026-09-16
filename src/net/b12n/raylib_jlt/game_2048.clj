(ns net.b12n.raylib-jlt.game-2048
  "raylib [games] example - 2048. Arrow keys slide + merge tiles on a 4x4 board;
  reach 2048. Slide/merge is one pure function reused for all four directions via
  row reversal / transpose. (Handle is game-2048; bb can't name a task '2048'.)"
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(defn- compress
  [row]
  (vec (remove zero? row)))

(defn- merge-row
  [row]
  ;; row is compressed (no zeros)
  (loop [in row out [] score 0]
    (cond
      (empty? in) [out score]
      (= (count in) 1) [(conj out (first in)) score]
      (= (first in) (second in)) (recur (drop 2 in) (conj out (* 2 (first in)))
                                        (+ score (* 2 (first in))))
      :else (recur (rest in) (conj out (first in)) score))))

(defn- slide-left
  [row]
  (let [[merged score] (merge-row (compress row))]
    [(vec (take 4 (concat merged (repeat 0)))) score]))

(defn- rows
  [board]
  (mapv vec (partition 4 board)))

(defn- from-rows
  [rs]
  (vec (apply concat rs)))

(defn- transpose
  [rs]
  (apply mapv vector rs))

(defn- revrows
  [rs]
  (mapv (fn [r] (vec (reverse r))) rs))

(defn- slide-board
  [rs]
  (let [results (mapv slide-left rs)]
    [(mapv first results) (reduce + (mapv second results))]))

(defn- move
  [board dir]
  (let [rs (rows board)]
    (cond
      (= dir :left)  (let [[nr g] (slide-board rs)] [(from-rows nr) g])
      (= dir :right) (let [[nr g] (slide-board (revrows rs))] [(from-rows (revrows nr)) g])
      (= dir :up)    (let [[nr g] (slide-board (transpose rs))] [(from-rows (transpose nr)) g])
      (= dir :down)  (let [[nr g] (slide-board (revrows (transpose rs)))]
                       [(from-rows (transpose (revrows nr))) g]))))

(defn- spawn
  [board]
  (let [empties (filterv (fn [i] (zero? (nth board i))) (range 16))]
    (if (empty? empties)
      board
      (let [i (nth empties (rl/get-random-value 0 (dec (count empties))))
            v (if (< (rl/get-random-value 0 9) 9) 2 4)]
        (assoc board i v)))))

(defn- new-board
  []
  (spawn (spawn (vec (repeat 16 0)))))

(defn- try-move
  [{:keys [board score]
    :as st} dir]
  (let [[nb g] (move board dir)]
    (if (= nb board) st (assoc st :board (spawn nb) :score (+ score g)))))

(defn- stuck?
  [board]
  (every? (fn [dir] (= board (first (move board dir)))) [:left :right :up :down]))

(def tile-colors
  {0 (rl/rgba 205 193 180 255)
   2 (rl/rgba 238 228 218 255)
   4 (rl/rgba 237 224 200 255)
   8 (rl/rgba 242 177 121 255)
   16 (rl/rgba 245 149 99 255)
   32 (rl/rgba 246 124 95 255)
   64 (rl/rgba 246 94 59 255)
   128 (rl/rgba 237 207 114 255)
   256 (rl/rgba 237 204 97 255)
   512 (rl/rgba 237 200 80 255)
   1024 (rl/rgba 237 197 63 255)
   2048 (rl/rgba 237 194 46 255)})

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [games] example - 2048"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st {:board (new-board)
               :score 0}]
      (when (rl/keep-running? deadline)
        (let [over? (stuck? (:board st))
              st (cond
                   over? (if (rl/key-pressed? rl/KEY-SPACE) {:board (new-board)
                                                             :score 0} st)
                   (rl/key-pressed? rl/KEY-LEFT)  (try-move st :left)
                   (rl/key-pressed? rl/KEY-RIGHT) (try-move st :right)
                   (rl/key-pressed? rl/KEY-UP)    (try-move st :up)
                   (rl/key-pressed? rl/KEY-DOWN)  (try-move st :down)
                   :else st)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 187 173 160 255))
          (dotimes [i 16]
            (let [c (mod i 4) r (quot i 4)
                  v (nth (:board st) i)
                  x (+ 210 (* c 95)) y (+ 40 (* r 95))]
              (rl/rect! {:x x
                         :y y
                         :width 88
                         :height 88
                         :color (get tile-colors v (get tile-colors 2048))})
              (when (pos? v)
                (rl/text! (str v) {:x (+ x 8)
                                   :y (+ y 28)
                                   :size 32
                                   :color rl/DARKGRAY}))))
          (rl/text! (str "score " (:score st)) {:x 20
                                                :y 20
                                                :size 24
                                                :color rl/RAYWHITE})
          (when over? (rl/text! "GAME OVER - SPACE" {:x 250
                                                     :y 415
                                                     :size 24
                                                     :color rl/RAYWHITE}))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
