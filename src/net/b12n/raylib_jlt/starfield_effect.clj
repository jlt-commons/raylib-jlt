(ns net.b12n.raylib-jlt.starfield-effect
  "raylib [shapes] example - starfield effect.

  Stars fly toward the camera under a simple perspective projection: MOUSE
  WHEEL changes speed, SPACE toggles streak-lines vs circles. Distinct from
  stars.clj (a static per-star twinkle, not a raylib example port). Ported
  from raylib's examples/shapes/shapes_starfield_effect.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const STAR-COUNT 350)

(defn- clamp [v lo hi] (max lo (min hi v)))
(defn- lerp [t a b] (+ a (* t (- b a))))

(defn- proj-x [x z] (+ (* W 0.5) (/ x z)))
(defn- proj-y [y z] (+ (* H 0.5) (/ y z)))

(defn- new-star
  []
  {:x (rl/get-random-value (int (/ W -2)) (int (/ W 2)))
   :y (rl/get-random-value (int (/ H -2)) (int (/ H 2)))
   :z 1.0})

(defn- offscreen?
  [px py]
  (or (< px 0.0) (< py 0.0) (> px W) (> py H)))

(defn- update-star
  [s dt speed]
  (let [z' (- (:z s) (* dt speed))
        px (proj-x (:x s) z')
        py (proj-y (:y s) z')]
    (if (or (< z' 0.0) (offscreen? px py))
      (new-star)
      (assoc s :z z'))))

(defn- draw-star!
  [s lines?]
  (let [z  (:z s)
        px (int (proj-x (:x s) z))
        py (int (proj-y (:y s) z))]
    (if lines?
      ;; Streak from a slightly-deeper projection to the current position.
      (let [t  (clamp (+ z (/ 1.0 32.0)) 0.01 1.0)
            sx (int (proj-x (:x s) t))
            sy (int (proj-y (:y s) t))]
        (rl/line! {:x1 sx
                   :y1 sy
                   :x2 px
                   :y2 py
                   :color rl/RAYWHITE}))
      (rl/circle! {:x px
                   :y py
                   :radius (lerp z 1.0 5.0)
                   :color rl/RAYWHITE}))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - starfield effect"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           stars (mapv (fn [_] (new-star)) (range STAR-COUNT))
           speed (/ 10.0 9.0)
           lines? true]
      (when (rl/keep-running? deadline)
        (let [wheel  (rl/get-mouse-wheel)
              speed  (clamp (+ speed (* 2.0 (/ wheel 9.0))) 0.1 2.0)
              lines? (if (rl/key-pressed? rl/KEY-SPACE) (not lines?) lines?)
              dt     (rl/get-frame-time)
              stars  (mapv (fn [s] (update-star s dt speed)) stars)]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [s stars] (draw-star! s lines?))
          (rl/text! (str "[MOUSE WHEEL] Speed: " (int (* 9.0 (/ speed 2.0))))
                    {:x 10
                     :y 40
                     :size 20
                     :color rl/RAYWHITE})
          (rl/text! (str "[SPACE] Mode: " (if lines? "Lines" "Circles"))
                    {:x 10
                     :y 70
                     :size 20
                     :color rl/RAYWHITE})
          (rl/fps! {:x 10
                    :y 10})
          (rl/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame) stars speed lines?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
