(ns net.b12n.raylib-jlt.pong
  "Pong (`joltc -M:pong`).

  The classic, ported to jolt: your paddle is on the left (W / S); the right paddle
  is a CPU that tracks the ball. The ball speeds up nothing fancy. It just takes
  english off where it hits a paddle. First to 7 wins; ENTER restarts.

  One immutable state map threaded through the loop; `step` reads input and returns
  the next state."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PW 12)                  ; paddle width
(def ^:const PH 80)                  ; paddle height
(def ^:const BS 12)                  ; ball size
(def ^:const PSPEED 6.0)
(def ^:const AISPEED 4.5)
(def ^:const LEFT-X 30)
(def ^:const RIGHT-X (- W 30 PW))
(def ^:const WIN 7)
(def ^:const KEY-ENTER 257)

(defn- clamp
  [v lo hi]
  (max lo (min hi v)))

(defn- fabs
  [x]
  (if (neg? x) (- x) x))

(defn- reset-ball
  [dir]
  {:bx (/ W 2.0)
   :by (/ H 2.0)
   :bvx (* dir (+ 4.0 (/ (rl/get-random-value 0 20) 10.0)))          ; 4.0 .. 6.0
   :bvy (* 3.0 (- (/ (rl/get-random-value 0 200) 100.0) 1.0))})      ; -3.0 .. 3.0

(defn- initial-state
  []
  (merge {:ly (/ (- H PH) 2.0)
          :ry (/ (- H PH) 2.0)
          :ls 0
          :rs 0
          :over? false
          :winner nil}
         (reset-ball 1)))

(defn- step
  [s]
  (if (:over? s)
    (if (rl/key-pressed? KEY-ENTER) (initial-state) s)
    (let [ly    (clamp (+ (:ly s) (cond (rl/key-down? rl/KEY-W) (- PSPEED)
                                        (rl/key-down? rl/KEY-S) PSPEED
                                        :else 0.0))
                       0.0 (- H PH))
          ;; CPU paddle chases the ball, capped speed
          target (- (:by s) (/ PH 2.0))
          ry    (clamp (+ (:ry s) (clamp (- target (:ry s)) (- AISPEED) AISPEED)) 0.0 (- H PH))
          bx0   (+ (:bx s) (:bvx s))
          by0   (+ (:by s) (:bvy s))
          [by bvy0] (cond (<= by0 0)          [0.0 (- (:bvy s))]
                          (>= (+ by0 BS) H)   [(double (- H BS)) (- (:bvy s))]
                          :else               [by0 (:bvy s)])
          bcy   (+ by (/ BS 2.0))
          hitL? (and (< (:bvx s) 0) (<= bx0 (+ LEFT-X PW)) (>= (+ bx0 BS) LEFT-X)
                     (>= bcy ly) (<= bcy (+ ly PH)))
          hitR? (and (> (:bvx s) 0) (>= (+ bx0 BS) RIGHT-X) (<= bx0 (+ RIGHT-X PW))
                     (>= bcy ry) (<= bcy (+ ry PH)))
          bx    (cond hitL? (double (+ LEFT-X PW)) hitR? (double (- RIGHT-X BS)) :else bx0)
          bvx   (cond hitL? (fabs (:bvx s)) hitR? (- (fabs (:bvx s))) :else (:bvx s))
          bvy   (cond hitL? (+ bvy0 (* 0.08 (- bcy (+ ly (/ PH 2.0)))))
                      hitR? (+ bvy0 (* 0.08 (- bcy (+ ry (/ PH 2.0)))))
                      :else bvy0)
          scored (cond (< bx 0) :right (> bx W) :left :else nil)
          ls    (+ (:ls s) (if (= scored :left) 1 0))
          rs    (+ (:rs s) (if (= scored :right) 1 0))
          ball  (if scored (reset-ball (if (= scored :left) 1 -1))
                    {:bx bx
                     :by by
                     :bvx bvx
                     :bvy bvy})]
      (merge {:ly ly
              :ry ry
              :ls ls
              :rs rs
              :over?  (or (>= ls WIN) (>= rs WIN))
              :winner (cond (>= ls WIN) "PLAYER 1" (>= rs WIN) "CPU" :else nil)}
             ball))))

(defn- draw-state
  [s]
  (rl/clear-background (rl/rgba 10 10 18 255))
  (doseq [y (range 0 H 30)]                                   ; dashed centre line
    (rl/rect! {:x (- (quot W 2) 2)
               :y y
               :width 4
               :height 16
               :color rl/GRAY}))
  (rl/rect! {:x LEFT-X
             :y (int (:ly s))
             :width PW
             :height PH
             :color rl/RAYWHITE})
  (rl/rect! {:x RIGHT-X
             :y (int (:ry s))
             :width PW
             :height PH
             :color rl/RAYWHITE})
  (rl/rect! {:x (int (:bx s))
             :y (int (:by s))
             :width BS
             :height BS
             :color rl/GOLD})
  (rl/text! (str (:ls s)) {:x (- (quot W 2) 80)
                           :y 30
                           :size 50
                           :color rl/RAYWHITE})
  (rl/text! (str (:rs s)) {:x (+ (quot W 2) 50)
                           :y 30
                           :size 50
                           :color rl/RAYWHITE})
  (rl/text! "W / S" {:x 20
                     :y (- H 30)
                     :size 18
                     :color rl/GRAY})
  (rl/text! "CPU"   {:x (- W 70)
                     :y (- H 30)
                     :size 18
                     :color rl/GRAY})
  (when (:over? s)
    (rl/text! (str (:winner s) " WINS!") {:x 250
                                          :y 180
                                          :size 40
                                          :color rl/GOLD})
    (rl/text! "ENTER to restart" {:x 295
                                  :y 240
                                  :size 20
                                  :color rl/RAYWHITE})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "pong"})
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
