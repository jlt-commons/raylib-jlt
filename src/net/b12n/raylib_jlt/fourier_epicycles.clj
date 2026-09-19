(ns net.b12n.raylib-jlt.fourier-epicycles
  "raylib [generative] example - a chain of rotating circles (a Fourier series for a
  square wave) whose tip traces the wave. Classic 'drawing with epicycles'."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def cx 200.0)
(def cy 225.0)
(def n-terms 8)
(def scale 55.0)

(defn- epicycles
  [theta]
  (loop [k 1 x cx y cy centers [[cx cy]] radii []]
    (if (> k (dec (* 2 n-terms)))
      {:centers centers
       :radii radii}
      (let [radius (* scale (/ 4.0 Math/PI) (/ 1.0 k))
            nx (+ x (* radius (Math/cos (* k theta))))
            ny (+ y (* radius (Math/sin (* k theta))))]
        (recur (+ k 2) nx ny (conj centers [nx ny]) (conj radii radius))))))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [generative] example - fourier epicycles"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           theta 0.0
           path []]
      (when (app/keep-running? deadline)
        (let [{:keys [centers radii]} (epicycles theta)
              [tx ty] (last centers)
              path (vec (take 400 (cons ty path)))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (dotimes [i (count radii)]
            (let [[ox oy] (nth centers i)
                  [nx ny] (nth centers (inc i))]
              (rl/circle-lines! {:x (int ox)
                                 :y (int oy)
                                 :radius (nth radii i)
                                 :color (rl/rgba 70 70 80 255)})
              (rl/line! {:x1 (int ox)
                         :y1 (int oy)
                         :x2 (int nx)
                         :y2 (int ny)
                         :color rl/GRAY})))
          (rl/line! {:x1 (int tx)
                     :y1 (int ty)
                     :x2 400
                     :y2 (int ty)
                     :color (rl/rgba 90 90 90 255)})
          (let [pts (map-indexed (fn [i y] [(+ 400.0 (* i 1.0)) y]) path)]
            (doseq [[[x1 y1] [x2 y2]] (partition 2 1 pts)]
              (rl/line! {:x1 (int x1)
                         :y1 (int y1)
                         :x2 (int x2)
                         :y2 (int y2)
                         :color rl/GOLD})))
          (app/maybe-screenshot! frame 80)
          (rl/end-drawing)
          (recur (inc frame) (+ theta 0.05) path)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
