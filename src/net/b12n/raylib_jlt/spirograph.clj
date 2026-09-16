(ns net.b12n.raylib-jlt.spirograph
  "raylib [generative] example - an animated hypotrochoid (spirograph). A pen offset d
  on a wheel of radius r rolling inside a ring of radius R traces roulette curves;
  resets with new random r/d after a fixed number of points."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def cx 400.0)
(def cy 225.0)
(def big-r 170.0)

(defn- new-params
  []
  {:r (double (rl/get-random-value 30 95))
   :d (double (rl/get-random-value 40 130))
   :t 0.0
   :points []})

(defn- pt
  [r d t]
  (let [k (/ (- big-r r) r)]
    [(+ cx (* (- big-r r) (Math/cos t)) (* d (Math/cos (* k t))))
     (+ cy (* (- big-r r) (Math/sin t)) (- (* d (Math/sin (* k t)))))]))

(defn- rainbow
  [i]
  (let [h (mod (* i 3) 360)]
    (cond (< h 60)  (rl/rgba 255 (int (* 255 (/ h 60.0))) 0 255)
          (< h 120) (rl/rgba (int (* 255 (/ (- 120 h) 60.0))) 255 0 255)
          (< h 180) (rl/rgba 0 255 (int (* 255 (/ (- h 120) 60.0))) 255)
          (< h 240) (rl/rgba 0 (int (* 255 (/ (- 240 h) 60.0))) 255 255)
          (< h 300) (rl/rgba (int (* 255 (/ (- h 240) 60.0))) 0 255 255)
          :else     (rl/rgba 255 0 (int (* 255 (/ (- 360 h) 60.0))) 255))))

(defn -main
  [& _]
  (rl/window! :width 800 :height 450 :title "raylib [generative] example - spirograph")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           st (new-params)]
      (when (rl/keep-running? deadline)
        (let [{:keys [r d t points]} st
              new-pts (mapv (fn [i] (pt r d (+ t (* i 0.04)))) (range 8))
              points (into points new-pts)
              t (+ t 0.32)
              st (if (> (count points) 1600) (new-params) (assoc st :t t :points points))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [[i [[x1 y1] [x2 y2]]] (map-indexed vector (partition 2 1 (:points st)))]
            (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color (rainbow i)))
          (rl/maybe-screenshot! frame 70)
          (rl/end-drawing)
          (recur (inc frame) st)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
