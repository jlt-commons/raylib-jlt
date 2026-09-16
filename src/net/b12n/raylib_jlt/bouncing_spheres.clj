(ns net.b12n.raylib-jlt.bouncing-spheres
  "raylib [models] example - spheres bouncing inside a 3D box under gravity, with
  wall restitution. Uses the rlgl-tessellated rl/sphere! helper (DrawSphere takes
  Vector3 by value, so it's built from rlgl triangles). SPACE respawns. See
  docs/guide/rlgl-immediate-mode.md."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def bound 4.0)
(def gravity 0.01)
(def restitution 0.9)
(def palette [rl/RED rl/ORANGE rl/GREEN rl/SKYBLUE rl/VIOLET rl/GOLD])

(defn- spawn
  []
  (vec (for [i (range 6)]
         {:x (/ (rl/get-random-value -30 30) 10.0)
          :y (/ (rl/get-random-value 10 40) 10.0)
          :z (/ (rl/get-random-value -30 30) 10.0)
          :vx (/ (rl/get-random-value -10 10) 100.0)
          :vy 0.0
          :vz (/ (rl/get-random-value -10 10) 100.0)
          :r (/ (rl/get-random-value 3 6) 10.0)
          :color (nth palette i)})))

(defn- reflect
  [p v r]
  (cond (< (- p r) (- bound)) [(+ (- bound) r) (* (- v) restitution)]
        (> (+ p r) bound)     [(- bound r) (* (- v) restitution)]
        :else [p v]))

(defn- step
  [{:keys [x y z vx vy vz r]
    :as b}]
  (let [vy (- vy gravity)
        [nx vx] (reflect (+ x vx) vx r)
        [ny vy] (reflect (+ y vy) vy r)
        [nz vz] (reflect (+ z vz) vz r)]
    (assoc b :x nx :y ny :z nz :vx vx :vy vy :vz vz)))

(defn -main
  [& _]
  (rl/window! :width 800 :height 450 :title "raylib [models] example - bouncing spheres")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           spheres (spawn)]
      (when (rl/keep-running? deadline)
        (let [spheres (if (rl/key-pressed? rl/KEY-SPACE) (spawn) (mapv step spheres))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x 10.0
                              :pos-y 8.0
                              :pos-z 10.0}
            (fn []
              (rl/draw-grid 10 1.0)
              (doseq [{:keys [x y z r color]} spheres]
                (rl/sphere! :pos [x y z] :radius r :rings 10 :slices 14 :color color))))
          (rl/text! "Spheres bouncing in a 3D box - SPACE respawns"
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) spheres)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
