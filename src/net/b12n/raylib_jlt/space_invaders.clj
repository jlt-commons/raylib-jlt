(ns net.b12n.raylib-jlt.space-invaders
  "raylib [games] example - space invaders. ←/→ move, SPACE shoots; clear the
  marching alien grid before it reaches you. Formation march + AABB hits, all in
  Clojure."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def width 800)
(def height 450)
(def acols 8)
(def arows 4)
(def sp 60)
(def alien-w 40)
(def alien-h 26)
(def ship-y 420)
(def ship-w 50)

(defn- new-game
  []
  {:ship-x 375.0
   :bullets []
   :aliens (set (for [c (range acols) r (range arows)] [c r]))
   :ax 40.0
   :ay 40.0
   :adir 1.0
   :cooldown 0
   :over? false
   :won? false
   :score 0})

(defn- alien-px
  [ax c]
  (+ ax (* c sp)))

(defn- alien-py
  [ay r]
  (+ ay (* r 45)))

(defn- march
  [{:keys [aliens ax ay adir]
    :as st}]
  (let [cs (map first aliens)
        minc (reduce min cs) maxc (reduce max cs)
        nax (+ ax (* adir 1.2))]
    (if (or (< (+ nax (* minc sp)) 6)
            (> (+ (alien-px nax maxc) alien-w) (- width 6)))
      (assoc st :adir (- adir) :ay (+ ay 18))
      (assoc st :ax nax))))

(defn- hit-alien
  [aliens bx by ax ay]
  (some (fn [[c r]]
          (let [px (alien-px ax c) py (alien-py ay r)]
            (when (and (>= bx px) (<= bx (+ px alien-w))
                       (>= by py) (<= by (+ py alien-h)))
              [c r])))
        aliens))

(defn- step
  [{:keys [ship-x bullets aliens ax ay cooldown score]
    :as st}]
  (let [ship-x (cond (rl/key-down? rl/KEY-LEFT)  (max 0.0 (- ship-x 5.0))
                     (rl/key-down? rl/KEY-RIGHT) (min (- width ship-w) (+ ship-x 5.0))
                     :else ship-x)
        shoot? (and (rl/key-pressed? rl/KEY-SPACE) (zero? cooldown))
        bullets (cond-> (mapv (fn [b] (update b :y - 8.0)) bullets)
                  shoot? (conj {:x (+ ship-x (/ ship-w 2.0))
                                :y ship-y}))
        bullets (filterv (fn [b] (> (:y b) -10)) bullets)
        cooldown (cond shoot? 15 (pos? cooldown) (dec cooldown) :else 0)
        result (reduce (fn [acc b]
                         (if-let [a (hit-alien (:aliens acc) (:x b) (:y b) ax ay)]
                           (-> acc (update :aliens disj a) (update :score + 10))
                           (update acc :bullets conj b)))
                       {:aliens aliens
                        :bullets []
                        :score score}
                       bullets)
        st (assoc st :ship-x ship-x :bullets (:bullets result)
                  :aliens (:aliens result) :cooldown cooldown :score (:score result))
        st (if (seq (:aliens st)) (march st) st)
        lowest (if (seq (:aliens st)) (reduce max (map second (:aliens st))) -1)
        reached? (and (>= lowest 0) (>= (+ (alien-py (:ay st) lowest) alien-h) ship-y))]
    (cond (empty? (:aliens st)) (assoc st :won? true)
          reached? (assoc st :over? true)
          :else st)))

(defn -main
  [& _]
  (rl/window! :width width :height height :title "raylib [games] example - space invaders")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-game)]
      (when (rl/keep-running? deadline)
        (let [st (if (or (:over? st) (:won? st))
                   (if (rl/key-pressed? rl/KEY-SPACE) (new-game) st)
                   (step st))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [[c r] (:aliens st)]
            (rl/rect! :x (int (alien-px (:ax st) c)) :y (int (alien-py (:ay st) r))
                      :width alien-w :height alien-h :color rl/LIME))
          (doseq [{:keys [x y]} (:bullets st)]
            (rl/rect! :x (int x) :y (int y) :width 4 :height 12 :color rl/GOLD))
          (rl/rect! :x (int (:ship-x st)) :y ship-y :width ship-w :height 16 :color rl/SKYBLUE)
          (rl/text! (str "score " (:score st)) :x 8 :y 8 :size 20 :color rl/RAYWHITE)
          (when (:over? st) (rl/text! "GAME OVER - SPACE" :x 280 :y 210 :size 28 :color rl/RED))
          (when (:won? st) (rl/text! "YOU WIN! - SPACE" :x 290 :y 210 :size 28 :color rl/LIME))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
