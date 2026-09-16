(ns net.b12n.raylib-jlt.fireworks
  "raylib [generative] example - fireworks. Rockets rise and explode into particles
  that fall under gravity and fade out via the alpha channel."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def width 800)
(def height 450)
(def gravity 0.07)

(defn- palette
  []
  (nth [[255 80 80] [80 180 255] [255 220 80] [180 120 255] [120 255 160]]
       (rl/get-random-value 0 4)))

(defn- new-rocket
  []
  {:x (double (rl/get-random-value 100 700))
   :y (double height)
   :vy (- (/ (rl/get-random-value 60 85) 10.0))
   :color (palette)})

(defn- explode
  [{:keys [x y color]}]
  (vec (repeatedly 40
                   (fn []
                     (let [a (/ (rl/get-random-value 0 628) 100.0)
                           sp (/ (rl/get-random-value 5 32) 10.0)]
                       {:x x
                        :y y
                        :vx (* sp (Math/cos a))
                        :vy (* sp (Math/sin a))
                        :life 1.0
                        :color color})))))

(defn -main
  [& _]
  (rl/window! :width width :height height :title "raylib [generative] example - fireworks")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           rockets []
           parts (explode {:x 400.0
                           :y 170.0
                           :color [255 220 80]})]
      (when (rl/keep-running? deadline)
        (let [rockets (if (zero? (mod frame 35)) (conj rockets (new-rocket)) rockets)
              rockets (mapv (fn [r] (-> r (update :y + (:vy r)) (update :vy + gravity))) rockets)
              exploded (filter (fn [r] (>= (:vy r) 0)) rockets)
              rockets (filterv (fn [r] (< (:vy r) 0)) rockets)
              parts (into (mapv (fn [p]
                                  (-> p (update :x + (:vx p)) (update :y + (:vy p))
                                      (update :vy + gravity) (update :life - 0.012)))
                                parts)
                          (mapcat explode exploded))
              parts (filterv (fn [p] (> (:life p) 0)) parts)]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (doseq [r rockets]
            (let [[cr cg cb] (:color r)]
              (rl/circle! :x (int (:x r)) :y (int (:y r)) :radius 3 :color (rl/rgba cr cg cb 255))))
          (doseq [p parts]
            (let [[cr cg cb] (:color p)]
              (rl/circle! :x (int (:x p)) :y (int (:y p)) :radius 2
                          :color (rl/rgba cr cg cb (int (* 255 (:life p)))))))
          (rl/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame) rockets parts)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
