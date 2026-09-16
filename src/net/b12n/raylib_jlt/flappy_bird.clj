(ns net.b12n.raylib-jlt.flappy-bird
  "raylib [games] example - flappy bird. SPACE to flap; fly through the pipe gaps.
  Gravity + scrolling pipes + AABB/circle collision, all in Clojure."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def width 800)
(def height 450)
(def bird-x 150)
(def bird-r 14)
(def gravity 0.4)
(def flap -7.0)
(def pipe-w 70)
(def gap-h 140)
(def scroll 3.0)
(def spacing 300)

(defn- rand-gap
  []
  (rl/get-random-value 80 (- height 80 gap-h)))

(defn- new-game
  []
  {:y 225.0
   :vy 0.0
   :score 0
   :over? false
   :pipes (mapv (fn [i] {:x (+ 500 (* i spacing))
                         :gap (rand-gap)
                         :scored false})
                (range 3))})

(defn- step
  [{:keys [y vy pipes score]
    :as st}]
  (let [vy (+ vy gravity)
        vy (if (rl/key-pressed? rl/KEY-SPACE) flap vy)
        ny (+ y vy)
        pipes (mapv (fn [p]
                      (let [nx (- (:x p) scroll)]
                        (if (< nx (- pipe-w))
                          {:x (+ nx (* 3 spacing))
                           :gap (rand-gap)
                           :scored false}
                          (assoc p :x nx))))
                    pipes)
        passed (count (filter (fn [p] (and (not (:scored p)) (< (+ (:x p) pipe-w) bird-x))) pipes))
        pipes (mapv (fn [p]
                      (if (and (not (:scored p)) (< (+ (:x p) pipe-w) bird-x))
                        (assoc p :scored true) p)) pipes)
        hit-pipe? (some (fn [p]
                          (and (< (- bird-x bird-r) (+ (:x p) pipe-w))
                               (> (+ bird-x bird-r) (:x p))
                               (or (< (- ny bird-r) (:gap p))
                                   (> (+ ny bird-r) (+ (:gap p) gap-h)))))
                        pipes)
        oob? (or (< (- ny bird-r) 0) (> (+ ny bird-r) height))]
    (if (or hit-pipe? oob?)
      (assoc st :over? true :y ny :vy vy)
      (assoc st :y ny :vy vy :pipes pipes :score (+ score passed)))))

(defn -main
  [& _]
  (rl/window! :width width :height height :title "raylib [games] example - flappy bird")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [st (if (:over? st)
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (step st))]
          (rl/begin-drawing)
          (rl/clear-background rl/SKYBLUE)
          (doseq [p (:pipes st)]
            (rl/rect! :x (int (:x p)) :y 0 :width pipe-w :height (int (:gap p)) :color rl/DARKGREEN)
            (rl/rect! :x (int (:x p)) :y (int (+ (:gap p) gap-h)) :width pipe-w
                      :height (- height (int (+ (:gap p) gap-h))) :color rl/DARKGREEN))
          (rl/circle! :x bird-x :y (int (:y st)) :radius bird-r :color rl/GOLD)
          (rl/text! (str "score " (:score st)) :x 8 :y 8 :size 20 :color rl/DARKBLUE)
          (when (:over? st) (rl/text! "GAME OVER - SPACE" :x 280 :y 200 :size 28 :color rl/MAROON))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
