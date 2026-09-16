(ns net.b12n.raylib-jlt.boids
  "raylib [generative] example - Reynolds boids. ~70 agents flock via separation,
  alignment, and cohesion; each drawn as a dot with a heading line along its velocity.
  Pure vector math."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def width 800)
(def height 450)
(def n 70)
(def radius 40.0)
(def max-speed 3.0)

(defn- spawn
  []
  (vec (repeatedly n (fn []
                       {:x (double (rl/get-random-value 0 width))
                        :y (double (rl/get-random-value 0 height))
                        :vx (- (/ (rl/get-random-value 0 200) 100.0) 1.0)
                        :vy (- (/ (rl/get-random-value 0 200) 100.0) 1.0)}))))

(defn- limit
  [vx vy m]
  (let [s (Math/sqrt (+ (* vx vx) (* vy vy)))]
    (if (> s m) [(* (/ vx s) m) (* (/ vy s) m)] [vx vy])))

(defn- near-boids
  [b boids]
  (filter (fn [o]
            (let [dx (- (:x o) (:x b)) dy (- (:y o) (:y b))]
              (< (+ (* dx dx) (* dy dy)) (* radius radius))))
          boids))

(defn- step-boid
  [b boids]
  (let [near (near-boids b boids)
        k (count near)
        ax (/ (reduce + (map :x near)) k)
        ay (/ (reduce + (map :y near)) k)
        avx (/ (reduce + (map :vx near)) k)
        avy (/ (reduce + (map :vy near)) k)
        sx (reduce + (map (fn [o] (- (:x b) (:x o))) near))
        sy (reduce + (map (fn [o] (- (:y b) (:y o))) near))
        vx (+ (:vx b) (* 0.0008 (- ax (:x b))) (* 0.05 (- avx (:vx b))) (* 0.0010 sx))
        vy (+ (:vy b) (* 0.0008 (- ay (:y b))) (* 0.05 (- avy (:vy b))) (* 0.0010 sy))
        [vx vy] (limit vx vy max-speed)]
    {:x (mod (+ (:x b) vx) width)
     :y (mod (+ (:y b) vy) height)
     :vx vx
     :vy vy}))

(defn- draw-boid
  [{:keys [x y vx vy]}]
  (let [s (Math/sqrt (+ (* vx vx) (* vy vy)))
        s (if (< s 0.001) 1.0 s)
        ux (/ vx s) uy (/ vy s)]
    (rl/line! {:x1 (int x)
               :y1 (int y)
               :x2 (int (+ x (* ux 11.0)))
               :y2 (int (+ y (* uy 11.0)))
               :color (rl/rgba 120 200 255 255)})
    (rl/circle! {:x (int x)
                 :y (int y)
                 :radius 3
                 :color rl/SKYBLUE})))

(defn -main
  [& _]
  (rl/window! {:width width
               :height height
               :title "raylib [generative] example - boids"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           boids (spawn)]
      (when (rl/keep-running? deadline)
        (let [boids (mapv (fn [b] (step-boid b boids)) boids)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 20 20 30 255))
          (doseq [b boids] (draw-boid b))
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) boids)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
