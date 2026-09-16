(ns net.b12n.raylib-jlt.pacman
  "Pac-Man (`jolt -M:pacman`).

  Arrows or WASD steer, ENTER restarts after GAME OVER.

  The ghosts keep the personalities the 1980 original gave them, which is where
  the game's character comes from. Blinky heads for Pac-Man's tile. Pinky aims
  four tiles AHEAD of him, to cut him off. Inky reflects Blinky's tile through a
  point two tiles ahead of Pac-Man, which is why he seems to change his mind.
  Clyde chases until he is within eight tiles, then breaks for his corner. They
  alternate scatter and chase on a timer, and turn blue while a power pellet is
  in effect.

  The whole world is one immutable map threaded through the loop; `step` reads
  input and returns the next state, and nothing under it draws. Movement is the
  part worth reading: a turn is BUFFERED on the keypress and applied at the next
  tile centre where it is legal, because deciding a direction anywhere but a
  centre is what lets an entity drift into a wall.

  Pac-Man himself is `rl/sector!` — a pie slice from the far side of his mouth
  round to the near side, so the mouth is the missing wedge. The rest is
  rectangles, circles and text."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

;; --- the maze ----------------------------------------------------------------
;; # wall, . dot, o power pellet, - ghost-house door, P pac-man start,
;; G ghost start. Row 10 has open ends: those are the side tunnels.

(def maze
  ["###################"
   "#........#........#"
   "#o##.###.#.###.##o#"
   "#.................#"
   "#.##.#.#####.#.##.#"
   "#....#...#...#....#"
   "####.###.#.###.####"
   "#....#.......#....#"
   "#.##.#.##-##.#.##.#"
   "#.##...#GGG#...##.#"
   ".....#.#GGG#.#....."
   "#.##...#####...##.#"
   "#.##.#...#...#.##.#"
   "#....#.#####.#....#"
   "####.#...#...#.####"
   "#o.......P.......o#"
   "#.###.#######.###.#"
   "#...#....#....#...#"
   "###.#.##.#.##.#.###"
   "#.................#"
   "###################"])

(def MW (count (first maze)))
(def MH (count maze))

;; --- screen geometry ---------------------------------------------------------
(def ^:const CELL 30)
(def ^:const OX 20)
(def ^:const OY 70)
(def W (+ (* MW CELL) (* 2 OX)))
(def H (+ (* MH CELL) OY 30))

(defn px [gx] (+ OX (* gx CELL)))
(defn py [gy] (+ OY (* gy CELL)))

;; --- palette -----------------------------------------------------------------
(def BACKGROUND (rl/rgba 6 6 14 255))
(def WALL (rl/rgba 33 33 222 255))
(def WALL-EDGE (rl/rgba 20 20 130 255))
(def PELLET (rl/rgba 255 224 40 255))
(def LABEL (rl/rgba 240 240 245 255))
(def DOOR (rl/rgba 255 184 174 255))
(def PUPIL (rl/rgba 20 20 60 255))
(def SCARED (rl/rgba 40 60 230 255))
(def OVER (rl/rgba 255 90 80 255))

;; --- reading the maze --------------------------------------------------------

(defn tile-at
  "The maze character at x,y. x wraps, which is the side tunnel; anything above
  or below the maze reads as wall."
  [x y]
  (if (or (< y 0) (>= y MH))
    \#
    (nth (nth maze y) (mod x MW))))

(defn wall?
  "Blocked for Pac-Man: walls and the ghost-house door."
  [x y]
  (let [c (tile-at x y)]
    (or (= \# c) (= \- c))))

(defn ghost-wall?
  "Blocked for a ghost: walls only, so a ghost can pass its own door."
  [x y]
  (= \# (tile-at x y)))

(defn find-tile
  [ch]
  (first (for [y (range MH)
               x (range MW)
               :when (= ch (tile-at x y))]
           [x y])))

(def pac-start (or (find-tile \P) [9 15]))
(def door (or (find-tile \-) [9 8]))
;; The tile just outside the door: where a ghost heads for on its way out.
(def door-exit [(first door) (dec (second door))])

(def house-tiles
  (vec (for [y (range MH)
             x (range MW)
             :when (= \G (tile-at x y))]
         [x y])))

;; The top row of the house, one slot per ghost.
(def house-slots
  (vec (filter #(= (second (first house-tiles)) (second %)) house-tiles)))

(defn initial-dots
  []
  (into #{} (for [y (range MH)
                  x (range MW)
                  :when (#{\. \o} (tile-at x y))]
              [x y])))

(defn centre-of [[x y]] [(+ 0.5 x) (+ 0.5 y)])

;; --- state -------------------------------------------------------------------

(def ghost-specs
  [["blinky" (rl/rgba 255 60 50 255) [(dec MW) 0] :door-exit 0.0]
   ["pinky" (rl/rgba 255 160 200 255) [0 0] 0 1.5]
   ["inky" (rl/rgba 90 220 240 255) [(dec MW) (dec MH)] 1 3.5]
   ["clyde" (rl/rgba 255 170 60 255) [0 (dec MH)] 2 5.5]])

(defn initial-ghosts
  []
  (mapv (fn [[nm color scatter slot delay]]
          (let [[sx sy] (centre-of (if (= :door-exit slot)
                                     door-exit
                                     (nth house-slots slot)))]
            {:name nm :color color :scatter scatter
             :x sx :y sy :dx 0 :dy -1
             :frightened 0.0 :home-timer delay}))
        ghost-specs))

(defn initial-pac
  []
  (let [[sx sy] (centre-of pac-start)]
    ;; dx/dy is the current heading, ndx/ndy the BUFFERED turn, fx/fy the last
    ;; non-zero heading — which is where the mouth points while stopped.
    {:x sx :y sy :dx -1 :dy 0 :ndx -1 :ndy 0 :fx -1 :fy 0 :mouth 0.0}))

(defn new-game
  "A fresh world. With `previous`, score and lives carry over and the level
  advances — the between-levels reset."
  ([] (new-game nil))
  ([previous]
   {:pac (initial-pac)
    :ghosts (initial-ghosts)
    :dots (initial-dots)
    :score (if previous (:score previous) 0)
    :lives (if previous (:lives previous) 3)
    :level (if previous (inc (:level previous)) 1)
    :mode-timer 7.0
    :chase? false
    :combo 0
    :message nil
    :message-timer 0.0
    :cleared? false
    :over? false
    :clock 0.0}))

;; --- movement ----------------------------------------------------------------

(defn tile-of [v] (long (Math/floor v)))

(defn step-entity
  "Advance an entity by speed*dt, deciding its direction only at tile centres.

  `entity` is {:x :y :dx :dy}; `decide` is (fn [tx ty dx dy] -> [dx dy]) called
  when the step reaches the centre of the current tile, and a blocked or zero
  choice stops there; `walls?` is the entity's own blocking predicate. Returns
  {:x :y :dx :dy}."
  [{:keys [x y dx dy]} {:keys [speed dt decide walls?]}]
  (let [dist (* speed dt)
        cx (+ (Math/floor x) 0.5)
        cy (+ (Math/floor y) 0.5)
        to-centre (+ (* dx (- cx x)) (* dy (- cy y)))]
    (if (and (>= to-centre -1.0e-9) (<= to-centre dist))
      (let [tx (tile-of cx)
            ty (tile-of cy)
            [ndx ndy] (decide tx ty dx dy)
            leftover (- dist to-centre)]
        (if (or (and (zero? ndx) (zero? ndy))
                (walls? (+ tx ndx) (+ ty ndy)))
          {:x cx :y cy :dx ndx :dy ndy}
          {:x (mod (+ cx (* ndx leftover)) MW)
           :y (+ cy (* ndy leftover))
           :dx ndx :dy ndy}))
      {:x (mod (+ x (* dx dist)) MW) :y (+ y (* dy dist)) :dx dx :dy dy})))

(def ^:const PAC-SPEED 5.6)

(defn move-pac
  [pac dt]
  (let [{:keys [ndx ndy]} pac
        decide (fn [tx ty dx dy]
                 (cond
                   ;; The buffered turn wins whenever it is possible here.
                   (and (or (not= ndx dx) (not= ndy dy))
                        (not (wall? (+ tx ndx) (+ ty ndy))))
                   [ndx ndy]
                   (not (wall? (+ tx dx) (+ ty dy))) [dx dy]
                   :else [0 0]))
        moved (step-entity pac {:speed PAC-SPEED :dt dt
                                :decide decide :walls? wall?})
        moving? (not (and (zero? (:dx moved)) (zero? (:dy moved))))]
    (merge pac moved
           {:fx (if moving? (:dx moved) (:fx pac))
            :fy (if moving? (:dy moved) (:fy pac))
            :mouth (+ (:mouth pac) (* dt (if moving? 9.0 0.0)))})))

(defn ghost-target
  "The classic personalities, in tile coordinates."
  [g pac ghosts]
  (let [{:keys [x y fx fy]} pac
        ptx (tile-of x)
        pty (tile-of y)]
    (case (:name g)
      "blinky" [ptx pty]
      "pinky" [(+ ptx (* 4 fx)) (+ pty (* 4 fy))]
      "inky" (let [b (first (filter #(= "blinky" (:name %)) ghosts))
                   ax (+ ptx (* 2 fx))
                   ay (+ pty (* 2 fy))]
               [(- (* 2 ax) (tile-of (:x b)))
                (- (* 2 ay) (tile-of (:y b)))])
      "clyde" (let [d (+ (Math/abs (- (:x g) x)) (Math/abs (- (:y g) y)))]
                (if (> d 8) [ptx pty] (:scatter g))))))

(defn ghost-choose-dir
  "At a tile centre, the direction that gets closest to the target without
  reversing — reversing is forbidden in the original too, which is what makes a
  ghost commit to a route. A frightened ghost picks at random instead."
  [[tx ty] [dx dy] {:keys [target frightened?]}]
  (let [opts (vec (for [[ndx ndy] [[0 -1] [-1 0] [0 1] [1 0]]
                        :when (and (not (and (= ndx (- dx)) (= ndy (- dy))))
                                   (not (ghost-wall? (+ tx ndx) (+ ty ndy))))]
                    [ndx ndy]))
        ;; A dead end leaves nothing but the reversal.
        opts (if (seq opts) opts [[(- dx) (- dy)]])]
    (if frightened?
      (rand-nth opts)
      (let [[gx gy] target]
        (apply min-key
               (fn [[ndx ndy]]
                 (let [ax (+ tx ndx)
                       ay (+ ty ndy)]
                   (+ (* (- ax gx) (- ax gx)) (* (- ay gy) (- ay gy)))))
               opts)))))

(def ^:const GHOST-SPEED 4.6)
(def ^:const GHOST-SPEED-SCARED 3.1)

(defn move-ghost
  [g {:keys [pac ghosts chase? dt]}]
  (if (pos? (:home-timer g))
    ;; Waiting inside the house until released.
    (update g :home-timer - dt)
    (let [g (update g :frightened #(max 0.0 (- % dt)))
          frightened? (pos? (:frightened g))
          target (if chase? (ghost-target g pac ghosts) (:scatter g))
          decide (fn [tx ty dx dy]
                   ;; Still inside the house: head for the door first.
                   (ghost-choose-dir [tx ty] [dx dy]
                                     {:target (if (= \G (tile-at tx ty))
                                                door-exit
                                                target)
                                      :frightened? frightened?}))]
      (merge g (step-entity g {:speed (if frightened?
                                        GHOST-SPEED-SCARED
                                        GHOST-SPEED)
                               :dt dt :decide decide :walls? ghost-wall?})))))

;; --- game rules --------------------------------------------------------------

(defn eat
  [s]
  (let [pac (:pac s)
        tx (tile-of (:x pac))
        ty (tile-of (:y pac))]
    (if-not (contains? (:dots s) [tx ty])
      s
      (let [pellet? (= \o (tile-at tx ty))]
        (cond-> (-> s
                    (update :dots disj [tx ty])
                    (update :score + (if pellet? 50 10)))
          ;; A pellet frightens every ghost and restarts the eat-combo, which is
          ;; what makes 200/400/800/1600 possible.
          pellet? (-> (assoc :combo 0)
                      (update :ghosts
                              (fn [gs] (mapv #(assoc % :frightened 7.0) gs)))))))))

(defn caught-by-ghost
  [s]
  (let [lives (dec (:lives s))]
    (if (pos? lives)
      (assoc s
             :lives lives
             :message "CAUGHT!" :message-timer 1.2
             :pac (initial-pac)
             :ghosts (initial-ghosts))
      (assoc s :lives 0 :over? true :message "GAME OVER"))))

(defn eat-ghost
  [s i g]
  (let [combo (inc (:combo s))
        [hx hy] (centre-of (nth house-slots 1))]
    (-> s
        ;; 200, 400, 800, 1600 within one pellet.
        (update :score + (* 200 (bit-shift-left 1 (dec combo))))
        (assoc :combo combo)
        (assoc-in [:ghosts i] (merge g {:x hx :y hy :dx 0 :dy -1
                                        :frightened 0.0 :home-timer 1.5})))))

(defn collide
  [s]
  (let [pac (:pac s)]
    (reduce (fn [s i]
              (let [g (get-in s [:ghosts i])
                    d (+ (Math/abs (- (:x g) (:x pac)))
                         (Math/abs (- (:y g) (:y pac))))]
                (cond
                  (> d 0.75) s
                  (pos? (:frightened g)) (eat-ghost s i g)
                  :else (caught-by-ghost s))))
            s
            (range (count (:ghosts s))))))

(def ^:const SCATTER-SECONDS 20.0)
(def ^:const CHASE-SECONDS 7.0)

(defn advance-mode
  "Scatter and chase alternate on a timer, as in the original."
  [s dt]
  (let [t (- (:mode-timer s) dt)]
    (if (pos? t)
      (assoc s :mode-timer t)
      (assoc s
             :chase? (not (:chase? s))
             :mode-timer (if (:chase? s) CHASE-SECONDS SCATTER-SECONDS)))))

(defn read-input
  "Buffer a steering direction; `step-entity` applies it at the next legal tile
  centre. ENTER restarts once the game is over."
  [s]
  (let [dir (cond
              (or (rl/key-down? rl/KEY-LEFT) (rl/key-down? rl/KEY-A)) [-1 0]
              (or (rl/key-down? rl/KEY-RIGHT) (rl/key-down? rl/KEY-D)) [1 0]
              (or (rl/key-down? rl/KEY-UP) (rl/key-down? rl/KEY-W)) [0 -1]
              (or (rl/key-down? rl/KEY-DOWN) (rl/key-down? rl/KEY-S)) [0 1])]
    (cond
      (and (:over? s) (rl/key-pressed? rl/KEY-ENTER)) (new-game)
      dir (update s :pac assoc :ndx (first dir) :ndy (second dir))
      :else s)))

(defn step
  "One frame: read input, advance the world, and answer the next state. dt is
  clamped so a stalled frame cannot step an entity through a wall."
  [s]
  (let [s (read-input s)
        dt (min 0.05 (rl/get-frame-time))
        s (update s :clock + dt)]
    (cond
      (:over? s) s
      ;; The LEVEL CLEARED message shows for its two seconds, then the reset.
      (and (:cleared? s) (zero? (:message-timer s))) (new-game s)
      :else
      (let [s (update s :message-timer #(max 0.0 (- % dt)))
            s (if (zero? (:message-timer s)) (assoc s :message nil) s)
            s (advance-mode s dt)
            s (update s :pac move-pac dt)
            s (eat s)
            s (update s :ghosts
                      (fn [gs]
                        (mapv #(move-ghost % {:pac (:pac s) :ghosts gs
                                              :chase? (:chase? s) :dt dt})
                              gs)))
            s (collide s)]
        (if (empty? (:dots s))
          (assoc s :message "LEVEL CLEARED" :message-timer 2.0 :cleared? true)
          s)))))

;; --- drawing -----------------------------------------------------------------

(defn draw-maze!
  [dots blink?]
  (dotimes [y MH]
    (dotimes [x MW]
      (let [c (tile-at x y)
            sx (px x)
            sy (py y)]
        (cond
          (= \# c)
          (do (rl/rect! :x sx :y sy :width CELL :height CELL :color WALL-EDGE)
              (rl/rect! :x (+ sx 3) :y (+ sy 3)
                        :width (- CELL 6) :height (- CELL 6) :color WALL))
          (= \- c)
          (rl/rect! :x sx :y (+ sy (quot CELL 2) -2)
                    :width CELL :height 4 :color DOOR)))))
  (doseq [[x y] dots]
    ;; Power pellets are bigger and blink; plain dots do neither.
    (let [pellet? (= \o (tile-at x y))]
      (when (or (not pellet?) blink?)
        (rl/circle! :x (+ (px x) (quot CELL 2))
                    :y (+ (py y) (quot CELL 2))
                    :radius (if pellet? 7.0 3.0)
                    :color PELLET)))))

(defn heading-deg
  "The heading (fx, fy) as a sector! angle: 0 points up and increases clockwise,
  so right is 90 and down is 180."
  [fx fy]
  (Math/toDegrees (Math/atan2 (double fx) (double (- fy)))))

(defn draw-pac!
  [{:keys [x y fx fy mouth]}]
  ;; The mouth is the wedge the sector does NOT cover: sweep from its far lip
  ;; clockwise all the way round to its near one. It opens and closes as :mouth
  ;; advances, which only happens while Pac-Man is moving.
  (let [open (Math/toDegrees (* 0.42 (+ 1.0 (Math/sin mouth))))
        from (+ (heading-deg fx fy) open)]
    (rl/sector! :cx (px x) :cy (py y)
                :radius (* 0.46 CELL)
                :start-deg from
                :end-deg (+ from (- 360.0 (* 2.0 open)))
                :segments 28
                :color PELLET)))

(defn draw-ghost!
  [g blink?]
  (let [cx (long (px (:x g)))
        cy (long (py (:y g)))
        r (long (* 0.44 CELL))
        color (cond
                (and (pos? (:frightened g)) blink?) LABEL
                (pos? (:frightened g)) SCARED
                :else (:color g))]
    ;; A dome over a body, with three feet along the bottom.
    (rl/circle! :x cx :y (- cy 2) :radius r :color color)
    (rl/rect! :x (- cx r) :y (- cy 2) :width (* 2 r) :height (+ r 2) :color color)
    (dotimes [i 3]
      (rl/circle! :x (+ (- cx r) (* i r) (quot r 2)) :y (+ cy r)
                  :radius (/ (double r) 2.6) :color color))
    ;; The eyes look along the direction of travel; a frightened ghost has none.
    (let [ex (long (* 4 (:dx g)))
          ey (long (* 4 (:dy g)))]
      (rl/circle! :x (- cx 5) :y (- cy 5) :radius 5.0 :color LABEL)
      (rl/circle! :x (+ cx 5) :y (- cy 5) :radius 5.0 :color LABEL)
      (when-not (pos? (:frightened g))
        (rl/circle! :x (+ (- cx 5) ex) :y (+ (- cy 5) ey) :radius 2.5 :color PUPIL)
        (rl/circle! :x (+ cx 5 ex) :y (+ (- cy 5) ey) :radius 2.5 :color PUPIL)))))

(defn draw-hud!
  [s]
  (rl/text! (str "SCORE " (:score s)) :x 20 :y 20 :size 28 :color LABEL)
  (rl/text! (str "LEVEL " (:level s)) :x (- W 340) :y 20 :size 28 :color LABEL)
  (dotimes [i (:lives s)]
    (rl/circle! :x (+ (- W 150) (* i 34)) :y 33 :radius 11.0 :color PELLET))
  (when-let [m (:message s)]
    ;; MeasureText is how a scalar API centres text: ask, then place.
    (let [size 46]
      (rl/text! m
                :x (quot (- W (rl/text-width m :size size)) 2)
                :y (- (quot H 2) 24)
                :size size
                :color (if (:over? s) OVER PELLET)))))

(defn draw-state!
  [s]
  (rl/clear-background BACKGROUND)
  (draw-maze! (:dots s) (< (mod (:clock s) 0.4) 0.25))
  (draw-pac! (:pac s))
  (doseq [g (:ghosts s)]
    (draw-ghost! g (< (mod (:clock s) 0.25) 0.12)))
  (draw-hud! s))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "pac-man")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        final (loop [frame 0
                     s (new-game)]
                (if-not (rl/keep-running? deadline)
                  s
                  (let [s' (step s)]
                    (rl/begin-drawing)
                    (draw-state! s')
                    (rl/maybe-screenshot! frame 60)
                    (rl/end-drawing)
                    (recur (inc frame) s'))))]
    (rl/close-window)
    (println "final score:" (:score final) "level:" (:level final))))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
