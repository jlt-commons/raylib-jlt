(ns net.b12n.raylib-jlt.dna-helix
  "raylib [models] example - DNA helix (`jolt -M:dna-helix`).

  A double helix: two phosphate backbones a half turn apart, with base pairs
  runged between them and coloured by base. The whole thing turns under an
  orbiting camera. UP/DOWN change how tightly it coils, SPACE stops the rotation.

  Backbone spheres and base pairs are rl/sphere! and rl/cube! from the library,
  which are rlgl immediate-mode geometry rather than raylib's DrawSphere
  and DrawCube: those take a Vector3 centre by value, which does not cross this
  FFI boundary (see net.b12n.raylib.models)."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const RUNGS 40)
(def ^:const RADIUS 2.2)
(def ^:const RISE 0.34)            ; vertical distance between successive rungs

;; The four bases, in the pairs they actually form.
(def ^:private base-pairs
  [[:A :T] [:T :A] [:G :C] [:C :G]])

(def ^:private base-color
  {:A (rl/rgba 230 41 55 255)
   :T (rl/rgba 0 228 48 255)
   :G (rl/rgba 0 121 241 255)
   :C (rl/rgba 255 203 0 255)})

(defn- strand-point
  [i turns phase]
  (let [a (+ phase (* i turns))
        y (- (* i RISE) (* RUNGS RISE 0.5))]
    [(* RADIUS (Math/cos a)) y (* RADIUS (Math/sin a))]))

(defn- draw-helix!
  [turns spin bases]
  (dotimes [i RUNGS]
    (let [p1 (strand-point i turns spin)
          ;; The second backbone is half a turn round, which is what makes the
          ;; major and minor grooves show up rather than a symmetric ladder.
          p2 (strand-point i turns (+ spin Math/PI))
          [a b] (nth bases i)]
      (rl/sphere! {:pos p1
                   :radius 0.34
                   :rings 8
                   :slices 12
                   :color (rl/rgba 200 200 210 255)})
      (rl/sphere! {:pos p2
                   :radius 0.34
                   :rings 8
                   :slices 12
                   :color (rl/rgba 200 200 210 255)})
      ;; Each rung is two half-length bars meeting in the middle, so the pair
      ;; reads as two bases rather than one bar.
      (dotimes [k 8]
        (let [t0 (/ (double k) 16.0)
              t1 (/ (double (+ k 8)) 16.0)]
          (rl/cube! {:pos (mapv (fn [c1 c2] (+ c1 (* t0 (- c2 c1)))) p1 p2)
                     :size 0.2
                     :color (base-color a)})
          (rl/cube! {:pos (mapv (fn [c1 c2] (+ c1 (* t1 (- c2 c1)))) p1 p2)
                     :size 0.2
                     :color (base-color b)}))))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - DNA helix"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        bases (mapv (fn [_] (nth base-pairs (rl/get-random-value 0 3))) (range RUNGS))]
    (loop [frame 0
           turns 0.55
           spin 0.0
           spinning? true]
      (when (app/keep-running? deadline)
        (let [spinning? (if (rl/key-pressed? rl/KEY-SPACE) (not spinning?) spinning?)
              turns (cond
                      (rl/key-down? rl/KEY-UP) (min 1.4 (+ turns 0.004))
                      (rl/key-down? rl/KEY-DOWN) (max 0.15 (- turns 0.004))
                      :else turns)
              spin (if spinning? (+ spin 0.012) spin)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 14 16 26 255))
          (rl/with-camera-3d
            ;; RUNGS * RISE is how tall the helix is; the camera sits far
            ;; enough back that both ends stay inside the frustum.
            {:pos-x 0.0
             :pos-y 0.0
             :pos-z 20.0
             :target-x 0.0
             :target-y 0.0
             :target-z 0.0
             :fovy 45}
            (fn [] (draw-helix! turns spin bases)))
          (rl/text! "DNA double helix" {:x 20
                                        :y 18
                                        :size 22
                                        :color rl/RAYWHITE})
          (rl/text! (format "%.2f rad between rungs   %d base pairs" turns RUNGS)
                    {:x 20
                     :y 46
                     :size 15
                     :color rl/SKYBLUE})
          (dotimes [i 4]
            (let [base (nth [:A :T :G :C] i)]
              (rl/rect! {:x (+ 20 (* i 56))
                         :y 76
                         :width 14
                         :height 14
                         :color (base-color base)})
              (rl/text! (name base) {:x (+ 40 (* i 56))
                                     :y 76
                                     :size 15
                                     :color rl/LIGHTGRAY})))
          (rl/text! (str "UP/DOWN coil   SPACE " (if spinning? "stop" "spin"))
                    {:x 20
                     :y (- H 30)
                     :size 14
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) turns spin spinning?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
