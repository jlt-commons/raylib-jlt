(ns net.b12n.raylib-jlt.ball-physics
  "raylib [shapes] example - 2D balls under gravity, bouncing off the window
  edges with restitution. SPACE respawns a fresh set. Pure scalar math + circle!."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def width 800)
(def height 450)
(def gravity 0.4)
(def restitution 0.8)
(def palette [rl/RED rl/ORANGE rl/GOLD rl/GREEN rl/SKYBLUE rl/VIOLET])

(defn- spawn-balls
  []
  (vec (for [i (range 6)]
         {:x (rl/get-random-value 100 700)
          :y (rl/get-random-value 50 200)
          :vx (/ (rl/get-random-value -40 40) 10.0)
          :vy 0.0
          :r (rl/get-random-value 15 35)
          :color (nth palette i)})))

(defn- step
  [{:keys [x y vx vy r]
    :as b}]
  (let [vy (+ vy gravity)
        [nx vx] (cond (< (- (+ x vx) r) 0)      [r (* (- vx) restitution)]
                      (> (+ x vx r) width)      [(- width r) (* (- vx) restitution)]
                      :else [(+ x vx) vx])
        [ny vy] (if (> (+ y vy r) height)
                  [(- height r) (* (- vy) restitution)]
                  [(+ y vy) vy])]
    (assoc b :x nx :y ny :vx vx :vy vy)))

(defn -main
  [& _]
  (rl/window! {:width width
               :height height
               :title "raylib [shapes] example - ball physics"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           balls (spawn-balls)]
      (when (rl/keep-running? deadline)
        (let [balls (if (rl/key-pressed? rl/KEY-SPACE) (spawn-balls) (mapv step balls))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [{:keys [x y r color]} balls]
            (rl/circle! {:x (int x)
                         :y (int y)
                         :radius r
                         :color color}))
          (rl/text! "Balls under gravity - SPACE respawns"
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) balls)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
