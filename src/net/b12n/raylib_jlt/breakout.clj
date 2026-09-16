(ns net.b12n.raylib-jlt.breakout
  "raylib [games] example - breakout. The paddle follows the mouse; bounce the ball
  to clear every brick. Ball/wall/paddle/brick collisions computed in Clojure."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def width 800)
(def height 450)
(def cols 10)
(def brk-rows 6)
(def brick-w 80)
(def brick-h 24)
(def top 50)
(def paddle-w 100)
(def paddle-h 14)
(def paddle-y 420)
(def ball-r 8)
(def row-colors [rl/RED rl/ORANGE rl/GOLD rl/GREEN rl/SKYBLUE rl/VIOLET])

(defn- fabs
  [n]
  (if (neg? n) (- n) n))

(defn- all-bricks
  []
  (set (for [c (range cols) r (range brk-rows)] [c r])))

(defn- new-ball
  []
  {:x 400.0
   :y 300.0
   :vx 3.0
   :vy -3.0})

(defn- new-game
  []
  {:bricks (all-bricks)
   :ball (new-ball)
   :lives 3
   :over? false
   :won? false})

(defn- brick-at
  [x y]
  (let [c (quot (int x) brick-w)
        r (quot (- (int y) top) brick-h)]
    (when (and (>= (- (int y) top) 0) (< r brk-rows) (>= c 0) (< c cols)) [c r])))

(defn- step
  [{:keys [ball bricks lives]
    :as st} paddle-x]
  (let [{:keys [x y vx vy]} ball
        nx (+ x vx) ny (+ y vy)
        [nx vx] (cond (< nx ball-r) [ball-r (- vx)]
                      (> nx (- width ball-r)) [(- width ball-r) (- vx)]
                      :else [nx vx])
        [ny vy] (if (< ny ball-r) [ball-r (- vy)] [ny vy])
        on-paddle? (and (> vy 0)
                        (>= (+ ny ball-r) paddle-y)
                        (<= (+ ny ball-r) (+ paddle-y paddle-h 8))
                        (>= nx paddle-x) (<= nx (+ paddle-x paddle-w)))
        vx (if on-paddle? (+ vx (* 0.08 (- nx (+ paddle-x (/ paddle-w 2.0))))) vx)
        vy (if on-paddle? (- (fabs vy)) vy)
        hit (brick-at nx ny)
        hit? (and hit (contains? bricks hit))
        bricks (if hit? (disj bricks hit) bricks)
        vy (if hit? (- vy) vy)
        lost? (> ny (+ height 20))]
    (cond
      (empty? bricks) (assoc st :won? true)
      lost? (if (<= lives 1)
              (assoc st :over? true :lives 0)
              (assoc st :lives (dec lives) :ball (new-ball)))
      :else (assoc st :bricks bricks :ball {:x nx
                                            :y ny
                                            :vx vx
                                            :vy vy}))))

(defn -main
  [& _]
  (rl/window! :width width :height height :title "raylib [games] example - breakout")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [mx (rl/get-mouse-x)
              paddle-x (double (max 0 (min (- width paddle-w) (- mx (quot paddle-w 2)))))
              st (if (or (:over? st) (:won? st))
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (step st paddle-x))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [[c r] (:bricks st)]
            (rl/rect! :x (+ 1 (* c brick-w)) :y (+ 1 top (* r brick-h))
                      :width (- brick-w 2) :height (- brick-h 2) :color (nth row-colors r)))
          (rl/rect! :x (int paddle-x) :y paddle-y :width paddle-w :height paddle-h :color rl/DARKGRAY)
          (let [{:keys [x y]} (:ball st)]
            (rl/circle! :x (int x) :y (int y) :radius ball-r :color rl/MAROON))
          (rl/text! (str "lives " (:lives st)) :x 8 :y 8 :size 20 :color rl/DARKGRAY)
          (when (:over? st) (rl/text! "GAME OVER - SPACE" :x 280 :y 210 :size 28 :color rl/MAROON))
          (when (:won? st) (rl/text! "YOU WIN! - SPACE" :x 290 :y 210 :size 28 :color rl/DARKGREEN))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
