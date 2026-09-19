(ns net.b12n.raylib-jlt.asteroids
  "Asteroids (`joltc -M:asteroids`).

  The classic vector game, ported to jolt: rotate with LEFT/RIGHT, thrust with UP,
  fire with SPACE. Everything wraps around the screen edges; shooting an asteroid
  splits it into two smaller ones until it's gone. All 2D, the ship and asteroids
  are line outlines, bullets are dots.

  The game state is one immutable map threaded through the loop; `step` reads input
  and returns the next state (input FFI is the only side effect)."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PI 3.141592653589793)
(def ^:const TAU 6.283185307179586)
(def ^:const ROT 0.07)
(def ^:const THRUST 0.15)
(def ^:const FRICTION 0.99)
(def ^:const SHIP-R 14.0)
(def ^:const BSPEED 7.0)
(def ^:const BLIFE 55)
(def ^:const KEY-ENTER 257)

(def ^:private SIZES
  {3 {:r 40.0
      :score 20}
   2 {:r 22.0
      :score 50}
   1 {:r 11.0
      :score 100}})

;; --- helpers -----------------------------------------------------------------
(defn- wrap
  [v hi]
  (cond (< v 0) (+ v hi) (>= v hi) (- v hi) :else v))

(defn- rand-unit
  []
  ;; a random double in [-1, 1)
  (- (/ (rl/get-random-value 0 200) 100.0) 1.0))

(defn- close?
  "True when entities a and b (each with :x :y) are within radius r of each other."
  [a b r]
  (let [dx (- (:x a) (:x b)) dy (- (:y a) (:y b))]
    (< (+ (* dx dx) (* dy dy)) (* r r))))

(defn- make-asteroid
  [x y size]
  (let [r     (get-in SIZES [size :r])
        n     10
        verts (vec (for [i (range n)]
                     (let [a  (* TAU (/ (double i) n))
                           rr (* r (+ 0.65 (* 0.5 (/ (rl/get-random-value 0 100) 100.0))))]
                       [(* rr (Math/cos a)) (* rr (Math/sin a))])))]
    {:x x
     :y y
     :vx (* (rand-unit) (/ 2.2 size))
     :vy (* (rand-unit) (/ 2.2 size))
     :size size
     :r r
     :angle 0.0
     :spin (* 0.02 (rand-unit))
     :verts verts}))

(defn- spawn-wave
  [n]
  (vec (for [_ (range n)]
         (let [[x y] (case (rl/get-random-value 0 3)
                       0 [0.0 (double (rl/get-random-value 0 H))]
                       1 [(double W) (double (rl/get-random-value 0 H))]
                       2 [(double (rl/get-random-value 0 W)) 0.0]
                       [(double (rl/get-random-value 0 W)) (double H)])]
           (make-asteroid x y 3)))))

(defn- initial-state
  []
  {:ship {:x (/ W 2.0)
          :y (/ H 2.0)
          :angle (- (/ PI 2))
          :vx 0.0
          :vy 0.0}
   :bullets []
   :asteroids (spawn-wave 4)
   :score 0
   :lives 3
   :over? false
   :invuln 60})

;; --- update ------------------------------------------------------------------
(defn- resolve-hits
  "Fold bullets into asteroids: a bullet inside an asteroid destroys both, scores,
  and (if the asteroid isn't smallest) splits it into two. Returns
  {:asteroids [...] :bullets [...] :score n}."
  [asteroids bullets]
  (loop [as (seq asteroids), bs (vec bullets), out [], gained 0]
    (if (empty? as)
      {:asteroids out
       :bullets bs
       :score gained}
      (let [a       (first as)
            hit-idx (first (keep-indexed (fn [i b] (when (close? a b (:r a)) i)) bs))]
        (if hit-idx
          (let [bs'    (into (subvec bs 0 hit-idx) (subvec bs (inc hit-idx)))
                splits (if (> (:size a) 1)
                         [(make-asteroid (:x a) (:y a) (dec (:size a)))
                          (make-asteroid (:x a) (:y a) (dec (:size a)))]
                         [])]
            (recur (next as) bs' (into out splits) (+ gained (get-in SIZES [(:size a) :score]))))
          (recur (next as) bs (conj out a) gained))))))

(defn- step
  [s]
  (if (:over? s)
    (if (rl/key-pressed? KEY-ENTER) (initial-state) s)
    (let [ship  (:ship s)
          angle (cond-> (:angle ship)
                  (rl/key-down? rl/KEY-LEFT)  (- ROT)
                  (rl/key-down? rl/KEY-RIGHT) (+ ROT))
          thr?  (rl/key-down? rl/KEY-UP)
          vx    (* FRICTION (+ (:vx ship) (if thr? (* THRUST (Math/cos angle)) 0.0)))
          vy    (* FRICTION (+ (:vy ship) (if thr? (* THRUST (Math/sin angle)) 0.0)))
          x     (wrap (+ (:x ship) vx) W)
          y     (wrap (+ (:y ship) vy) H)
          ship' {:x x
                 :y y
                 :angle angle
                 :vx vx
                 :vy vy}
          fired (if (rl/key-pressed? rl/KEY-SPACE)
                  [{:x x
                    :y y
                    :vx (+ vx (* BSPEED (Math/cos angle)))
                    :vy (+ vy (* BSPEED (Math/sin angle)))
                    :life BLIFE}]
                  [])
          bullets (->> (into (:bullets s) fired)
                       (map (fn [b]
                              (assoc b :x (wrap (+ (:x b) (:vx b)) W)
                                     :y (wrap (+ (:y b) (:vy b)) H)
                                     :life (dec (:life b)))))
                       (filterv (fn [b] (pos? (:life b)))))
          asteroids (mapv (fn [a]
                            (assoc a :x (wrap (+ (:x a) (:vx a)) W)
                                   :y (wrap (+ (:y a) (:vy a)) H)
                                   :angle (+ (:angle a) (:spin a))))
                          (:asteroids s))
          {as' :asteroids
           bs' :bullets
           gained :score} (resolve-hits asteroids bullets)
          invuln  (max 0 (dec (:invuln s)))
          crash?  (and (zero? invuln) (some (fn [a] (close? ship' a (+ SHIP-R (:r a) -4.0))) as'))
          lives   (if crash? (dec (:lives s)) (:lives s))]
      {:ship    (if crash? (:ship (initial-state)) ship')
       :bullets bs'
       :asteroids (if (empty? as') (spawn-wave (+ 4 (quot (:score s) 500))) as')
       :score   (+ (:score s) gained)
       :lives   lives
       :over?   (and crash? (<= lives 0))
       :invuln  (if crash? 90 invuln)})))

;; --- draw --------------------------------------------------------------------
(defn- draw-asteroid
  [a]
  (let [{:keys [x y angle verts]} a
        ca  (Math/cos angle) sa (Math/sin angle)
        pts (mapv (fn [[dx dy]]
                    [(+ x (- (* dx ca) (* dy sa)))
                     (+ y (+ (* dx sa) (* dy ca)))]) verts)
        n   (count pts)]
    (doseq [i (range n)]
      (let [[ax ay] (nth pts i)
            [bx by] (nth pts (mod (inc i) n))]
        (rl/line! {:x1 (int ax)
                   :y1 (int ay)
                   :x2 (int bx)
                   :y2 (int by)
                   :color rl/LIGHTGRAY})))))

(defn- draw-ship
  [ship]
  (let [{:keys [x y angle]} ship
        pt (fn [da]
             [(+ x (* SHIP-R (Math/cos (+ angle da))))
              (+ y (* SHIP-R (Math/sin (+ angle da))))])
        [nx ny] (pt 0.0) [lx ly] (pt 2.6) [rx ry] (pt -2.6)]
    (rl/line! {:x1 (int nx)
               :y1 (int ny)
               :x2 (int lx)
               :y2 (int ly)
               :color rl/RAYWHITE})
    (rl/line! {:x1 (int lx)
               :y1 (int ly)
               :x2 (int rx)
               :y2 (int ry)
               :color rl/GRAY})
    (rl/line! {:x1 (int rx)
               :y1 (int ry)
               :x2 (int nx)
               :y2 (int ny)
               :color rl/RAYWHITE})))

(defn- draw-state
  [s]
  (rl/clear-background (rl/rgba 8 8 18 255))
  (doseq [a (:asteroids s)] (draw-asteroid a))
  (doseq [b (:bullets s)] (rl/circle! {:x (int (:x b))
                                       :y (int (:y b))
                                       :radius 2.0
                                       :color rl/GOLD}))
  (when (or (zero? (:invuln s)) (even? (quot (:invuln s) 5)))   ; blink while invulnerable
    (draw-ship (:ship s)))
  (rl/text! (str "SCORE " (:score s)) {:x 12
                                       :y 12
                                       :size 22
                                       :color rl/RAYWHITE})
  (rl/text! (str "LIVES " (:lives s)) {:x (- W 128)
                                       :y 12
                                       :size 22
                                       :color rl/RAYWHITE})
  (when (zero? (:score s))
    (rl/text! "LEFT/RIGHT rotate - UP thrust - SPACE fire" {:x 190
                                                            :y (- H 34)
                                                            :size 18
                                                            :color rl/GRAY}))
  (when (:over? s)
    (rl/text! "GAME OVER" {:x 262
                           :y 180
                           :size 50
                           :color rl/RED})
    (rl/text! "press ENTER to restart" {:x 280
                                        :y 250
                                        :size 20
                                        :color rl/RAYWHITE})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "asteroids"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0 s (initial-state)]
      (when (app/keep-running? deadline)
        (let [s' (step s)]
          (rl/begin-drawing)
          (draw-state s')
          (app/maybe-screenshot! frame 70)
          (rl/end-drawing)
          (recur (inc frame) s')))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
