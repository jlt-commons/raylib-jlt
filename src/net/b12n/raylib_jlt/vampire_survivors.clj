(ns net.b12n.raylib-jlt.vampire-survivors
  "Vampire Survivors (`joltc -M:vampire-survivors`).

  A tiny survivors-like: move with WASD / arrows, and your hero auto-fires at the
  nearest enemy. Enemies stream in from the edges and chase you; shooting one drops
  an XP gem, and collecting gems levels you up (which speeds up your fire rate).
  Touching an enemy costs HP; survive as long as you can. ENTER restarts.

  One immutable state map threaded through the loop; `step` reads input and returns
  the next state."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const HERO-R 12)
(def ^:const HERO-SPEED 3.2)
(def ^:const HERO-HP 100)
(def ^:const ENEMY-R 10)
(def ^:const ENEMY-HP 2)
(def ^:const ESPEED 1.4)
(def ^:const BSPEED 6.0)
(def ^:const BULLET-R 4)
(def ^:const CONTACT-DMG 8)
(def ^:const PICKUP-R 44)
(def ^:const KEY-ENTER 257)

(defn- clamp
  [v lo hi]
  (max lo (min hi v)))

(defn- close?
  "True when a and b (each with :x :y) are within radius r."
  [a b r]
  (let [dx (- (:x a) (:x b)) dy (- (:y a) (:y b))]
    (< (+ (* dx dx) (* dy dy)) (* r r))))

(defn- nearest
  [x y enemies]
  (when (seq enemies)
    (apply min-key (fn [e] (let [dx (- (:x e) x) dy (- (:y e) y)] (+ (* dx dx) (* dy dy)))) enemies)))

(defn- spawn-enemy
  []
  (let [[x y] (case (rl/get-random-value 0 3)
                0 [-20.0 (double (rl/get-random-value 0 H))]
                1 [(+ W 20.0) (double (rl/get-random-value 0 H))]
                2 [(double (rl/get-random-value 0 W)) -20.0]
                [(double (rl/get-random-value 0 W)) (+ H 20.0)])]
    {:x x
     :y y
     :hp ENEMY-HP}))

(defn- toward
  "Velocity [vx vy] of magnitude `speed` pointing from point `from` [x y] to `to`."
  [[fx fy] [tx ty] speed]
  (let [dx (- tx fx) dy (- ty fy) d (max 1.0 (Math/sqrt (+ (* dx dx) (* dy dy))))]
    [(* speed (/ dx d)) (* speed (/ dy d))]))

(defn- initial-state
  []
  {:hero {:x (/ W 2.0)
          :y (/ H 2.0)
          :hp HERO-HP
          :level 1
          :xp 0
          :hurt-cd 0}
   :enemies []
   :bullets []
   :gems []
   :fire-cd 0
   :spawn-cd 30
   :time 0
   :kills 0
   :over? false})

(defn- resolve-hits
  "Fold bullets into enemies: a bullet within range takes 1 HP; a dead enemy drops
  an XP gem and bumps kills. Returns {:enemies :bullets :gems :kills}."
  [enemies bullets gems kills]
  (loop [es (seq enemies), bs (vec bullets), out-e [], out-g (vec gems), k kills]
    (if (empty? es)
      {:enemies out-e
       :bullets bs
       :gems out-g
       :kills k}
      (let [e       (first es)
            hit-idx (first (keep-indexed (fn [i b] (when (close? e b (+ ENEMY-R BULLET-R)) i)) bs))]
        (if hit-idx
          (let [bs' (into (subvec bs 0 hit-idx) (subvec bs (inc hit-idx)))
                e'  (update e :hp dec)]
            (if (<= (:hp e') 0)
              (recur (next es) bs' out-e (conj out-g {:x (:x e)
                                                      :y (:y e)}) (inc k))
              (recur (next es) bs' (conj out-e e') out-g k)))
          (recur (next es) bs (conj out-e e) out-g k))))))

(defn- step
  [s]
  (if (:over? s)
    (if (rl/key-pressed? KEY-ENTER) (initial-state) s)
    (let [h  (:hero s)
          dx (+ (if (rl/key-down? rl/KEY-D) 1.0 0.0) (if (rl/key-down? rl/KEY-A) -1.0 0.0)
                (if (rl/key-down? rl/KEY-RIGHT) 1.0 0.0) (if (rl/key-down? rl/KEY-LEFT) -1.0 0.0))
          dy (+ (if (rl/key-down? rl/KEY-S) 1.0 0.0) (if (rl/key-down? rl/KEY-W) -1.0 0.0)
                (if (rl/key-down? rl/KEY-DOWN) 1.0 0.0) (if (rl/key-down? rl/KEY-UP) -1.0 0.0))
          hx (clamp (+ (:x h) (* HERO-SPEED dx)) HERO-R (- W HERO-R))
          hy (clamp (+ (:y h) (* HERO-SPEED dy)) HERO-R (- H HERO-R))
          ;; spawn a wave when the timer elapses (waves get bigger + faster over time)
          spawn-cd (dec (:spawn-cd s))
          [enemies0 spawn-cd] (if (<= spawn-cd 0)
                                [(into (:enemies s) (repeatedly (+ 2 (quot (:time s) 600)) spawn-enemy))
                                 (max 12 (- 50 (quot (:time s) 120)))]
                                [(:enemies s) spawn-cd])
          ;; enemies chase the hero
          enemies1 (mapv (fn [e]
                           (let [[vx vy] (toward [(:x e) (:y e)] [hx hy] ESPEED)]
                             (assoc e :x (+ (:x e) vx) :y (+ (:y e) vy))))
                         enemies0)
          ;; auto-fire at the nearest enemy
          fire-cd (dec (:fire-cd s))
          target  (nearest hx hy enemies1)
          [bullets0 fire-cd] (if (and (<= fire-cd 0) target)
                               (let [[vx vy] (toward [hx hy] [(:x target) (:y target)] BSPEED)]
                                 [(conj (:bullets s) {:x hx
                                                      :y hy
                                                      :vx vx
                                                      :vy vy
                                                      :life 90})
                                  (max 5 (- 22 (* 2 (:level h))))])
                               [(:bullets s) (max 0 fire-cd)])
          bullets1 (->> bullets0
                        (map (fn [b]
                               (assoc b :x (+ (:x b) (:vx b)) :y (+ (:y b) (:vy b))
                                      :life (dec (:life b)))))
                        (filterv (fn [b] (pos? (:life b)))))
          {:keys [enemies bullets gems kills]} (resolve-hits enemies1 bullets1 (:gems s) (:kills s))
          ;; collect gems within pickup range
          [gems2 got] (reduce (fn [[keep n] g]
                                (if (close? {:x hx
                                             :y hy} g PICKUP-R) [keep (inc n)] [(conj keep g) n]))
                              [[] 0] gems)
          xp0 (+ (:xp h) got)
          need (* (:level h) 5)
          [level xp] (if (>= xp0 need) [(inc (:level h)) (- xp0 need)] [(:level h) xp0])
          ;; contact damage (with a short cooldown so you don't melt instantly)
          hurt-cd0 (max 0 (dec (:hurt-cd h)))
          touch?   (some (fn [e] (close? {:x hx
                                          :y hy} e (+ HERO-R ENEMY-R))) enemies)
          [hp hurt-cd] (if (and touch? (zero? hurt-cd0)) [(- (:hp h) CONTACT-DMG) 25] [(:hp h) hurt-cd0])]
      {:hero {:x hx
              :y hy
              :hp hp
              :level level
              :xp xp
              :hurt-cd hurt-cd}
       :enemies enemies
       :bullets bullets
       :gems gems2
       :fire-cd fire-cd
       :spawn-cd spawn-cd
       :time (inc (:time s))
       :kills kills
       :over? (<= hp 0)})))

(defn- bar!
  "A background bar plus a fill of the given :frac (0..1). Keyword args :x :y :w :h
  :frac :color."
  [& {:keys [x y w h frac color]}]
  (rl/rect! :x x :y y :width w :height h :color rl/DARKGRAY)
  (rl/rect! :x x :y y :width (int (* w (clamp frac 0.0 1.0))) :height h :color color))

(defn- draw-state
  [s]
  (rl/clear-background (rl/rgba 20 18 28 255))
  (doseq [g (:gems s)]    (rl/rect! :x (int (:x g)) :y (int (:y g)) :width 6 :height 6 :color rl/LIME))
  (doseq [e (:enemies s)] (rl/circle! :x (int (:x e)) :y (int (:y e)) :radius ENEMY-R :color rl/RED))
  (doseq [b (:bullets s)] (rl/circle! :x (int (:x b)) :y (int (:y b)) :radius BULLET-R :color rl/GOLD))
  (let [h (:hero s)]
    (rl/circle! :x (int (:x h)) :y (int (:y h)) :radius HERO-R
                :color (if (pos? (:hurt-cd h)) rl/RAYWHITE rl/SKYBLUE))
    (bar! :x 12 :y 12 :w 200 :h 16 :frac (/ (max 0 (:hp h)) (double HERO-HP)) :color rl/RED)
    (bar! :x 12 :y 34 :w 200 :h 8  :frac (/ (:xp h) (double (* (:level h) 5))) :color rl/SKYBLUE)
    (rl/text! (str "LV " (:level h)) :x 220 :y 14 :size 20 :color rl/RAYWHITE))
  (rl/text! (str "KILLS " (:kills s)) :x (- W 160) :y 12 :size 20 :color rl/RAYWHITE)
  (rl/text! (str "TIME " (quot (:time s) 60) "s") :x (- W 160) :y 38 :size 18 :color rl/GRAY)
  (rl/text! "WASD / arrows to move - you auto-fire at the nearest enemy"
            :x 170 :y (- H 26) :size 16 :color rl/GRAY)
  (when (:over? s)
    (rl/text! "YOU DIED" :x 270 :y 175 :size 46 :color rl/RED)
    (rl/text! (str "survived " (quot (:time s) 60) "s  -  " (:kills s) " kills")
              :x 250 :y 235 :size 20 :color rl/RAYWHITE)
    (rl/text! "ENTER to restart" :x 300 :y 270 :size 18 :color rl/GRAY)))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "vampire survivors")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 s (initial-state)]
      (when (rl/keep-running? deadline)
        (let [s' (step s)]
          (rl/begin-drawing)
          (draw-state s')
          (rl/maybe-screenshot! frame 100)
          (rl/end-drawing)
          (recur (inc frame) s')))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
