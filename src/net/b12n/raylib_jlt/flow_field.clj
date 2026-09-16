(ns net.b12n.raylib-jlt.flow-field
  "raylib [generative] example - particles steered by a smooth sine-layered field,
  each leaving a short trail. The field angle is a pure function of position + time;
  trails are per-particle position history (double-buffer-safe), redrawn each frame."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def width 800)
(def height 450)
(def n 500)
(def speed 1.7)
(def trail-len 16)

(defn- field-angle
  [x y t]
  (* 2.0 Math/PI (* 0.5 (+ (Math/sin (+ (* x 0.008) t)) (Math/cos (- (* y 0.008) t))))))

(defn- spawn
  []
  (vec (repeatedly n (fn []
                       (let [x (double (rl/get-random-value 0 width))
                             y (double (rl/get-random-value 0 height))]
                         {:x x
                          :y y
                          :trail [[x y]]})))))

(defn- step-part
  [{:keys [x y trail]} t]
  (let [a (field-angle x y t)
        nx (+ x (* speed (Math/cos a)))
        ny (+ y (* speed (Math/sin a)))
        [nx w1] (cond (< nx 0) [(+ nx width) true] (>= nx width) [(- nx width) true] :else [nx false])
        [ny w2] (cond (< ny 0) [(+ ny height) true] (>= ny height) [(- ny height) true] :else [ny false])
        trail (if (or w1 w2) [[nx ny]] (vec (take trail-len (cons [nx ny] trail))))]
    {:x nx
     :y ny
     :trail trail}))

(defn- trail-color
  [a]
  (rl/rgba (int (+ 128 (* 100 (Math/cos a)))) 120
           (int (+ 160 (* 90 (Math/sin a)))) 200))

(defn -main
  [& _]
  (rl/window! :width width :height height :title "raylib [generative] example - flow field")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           t 0.0
           parts (spawn)]
      (when (rl/keep-running? deadline)
        (let [parts (mapv (fn [p] (step-part p t)) parts)]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [{:keys [x y trail]} parts]
            (let [col (trail-color (field-angle x y t))]
              (doseq [[[x1 y1] [x2 y2]] (partition 2 1 trail)]
                (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color col))))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) (+ t 0.005) parts)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
