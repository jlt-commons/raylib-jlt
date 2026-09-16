(ns net.b12n.raylib-jlt.point-cloud
  "raylib [models] example - a cloud of ~1500 points, each a tiny rlgl cube,
  colored by position and slowly rotating via the matrix stack. (rlgl has no
  RL_POINTS mode, so points are drawn as small cubes.) See
  docs/guide/rlgl-immediate-mode.md."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def n-points 1500)

(defn- make-points
  []
  (vec (repeatedly n-points
                   (fn []
                     (let [x (/ (rl/get-random-value -50 50) 10.0)
                           y (/ (rl/get-random-value -50 50) 10.0)
                           z (/ (rl/get-random-value -50 50) 10.0)
                           col (rl/rgba (int (+ 128 (* 25 x)))
                                        (int (+ 128 (* 25 y)))
                                        (int (+ 128 (* 25 z))) 255)]
                       [x y z col])))))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [models] example - point cloud"})
  (rl/set-target-fps 60)
  (let [points (make-points)
        deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/BLACK)
        (rl/with-camera-3d {:pos-x 0.0
                            :pos-y 0.0
                            :pos-z 12.0}
          (fn []
            (rl/rl-push-matrix)
            (rl/rl-rotatef (* frame 0.3) 0.0 1.0 0.0)
            (doseq [[x y z col] points]
              (rl/cube! {:pos [x y z]
                         :size 0.06
                         :color col}))
            (rl/rl-pop-matrix)))
        (rl/text! (str n-points " points, each a tiny rlgl cube")
                  {:x 10
                   :y 10
                   :size 20
                   :color rl/RAYWHITE})
        (rl/maybe-screenshot! frame 10)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
