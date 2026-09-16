(ns net.b12n.raylib-jlt.particles
  "raylib [shapes] example - simple particles.

  Move the mouse to emit particles. LEFT/RIGHT cycle the type (water falls,
  smoke rises and fades, fire shrinks yellow -> red). No new bindings:
  Fade(color, a) and ColorLerp are both just packed-int arithmetic on the
  already-bound rl/rgba, since a Color here is a plain :uint.
  Ported from raylib's examples/shapes/shapes_simple_particles.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MAX-PARTICLES 2000)
(def ^:const DEG2RAD (/ Math/PI 180.0))

(defn- lerp-byte
  [a b t]
  (int (+ a (* t (- b a)))))

;; rl/rgba's own :uint packing, but starting from an already-packed color --
;; unpack the channels, scale/lerp them, repack. Fade multiplies alpha only;
;; color-lerp blends every channel toward `to`.
(defn- unpack
  [c]
  [(bit-and c 0xff) (bit-and (bit-shift-right c 8) 0xff)
   (bit-and (bit-shift-right c 16) 0xff) (bit-and (bit-shift-right c 24) 0xff)])

(defn- fade
  [c alpha]
  (let [[r g b a] (unpack c)]
    (rl/rgba r g b (int (* a (max 0.0 (min 1.0 alpha)))))))

(defn- color-lerp
  [from to t]
  (let [[r0 g0 b0 a0] (unpack from)
        [r1 g1 b1 a1] (unpack to)
        t (max 0.0 (min 1.0 t))]
    (rl/rgba (lerp-byte r0 r1 t) (lerp-byte g0 g1 t) (lerp-byte b0 b1 t) (lerp-byte a0 a1 t))))

(defn- in-bounds?
  [x y r]
  (and (> x (- r)) (< x (+ W r)) (> y (- r)) (< y (+ H r))))

(defn- alive?
  [{:keys [type x y radius age]}]
  (and (in-bounds? x y radius)
       (case type
         :fire (> radius 0.5)
         :smoke (< age 1.8)
         true)))

(defn- type-name
  [t]
  (case t :water "WATER" :smoke "SMOKE" "FIRE"))

(defn- next-type
  [t]
  (case t :water :smoke :smoke :fire :water))

(defn- prev-type
  [t]
  (case t :water :fire :smoke :water :smoke))

(defn- emit-particle
  [ex ey type]
  (let [base (/ (rl/get-random-value 0 9) 5.0)
        speed (if (= type :fire) (/ base 10.0) base)
        rad (* (rl/get-random-value 0 359) DEG2RAD)
        radius (case type :water 5.0 :smoke 7.0 10.0)]
    {:type type
     :x ex
     :y ey
     :vx (* speed (Math/cos rad))
     :vy (* speed (Math/sin rad))
     :radius radius
     :age 0.0}))

(defn- update-particle
  [{:keys [type x y vx vy radius age]
    :as p}]
  (let [age (+ age 0.0166667)]
    (case type
      :water (let [vy' (+ vy 0.2)]
               (assoc p :x (+ x vx) :y (+ y vy') :vy vy' :age age))
      :smoke (let [vy' (- vy 0.05)]
               (assoc p :x (+ x vx) :y (+ y vy') :vy vy' :radius (+ radius 0.5) :age age))
      :fire (let [vy' (- vy 0.05)]
              (assoc p :x (+ x vx (Math/cos (* age 215.0))) :y (+ y vy')
                     :vy vy' :radius (- radius 0.15) :age age))
      p)))

(defn- draw-particle!
  [{:keys [type x y radius age]}]
  (case type
    :water (rl/circle! {:x (int x)
                        :y (int y)
                        :radius radius
                        :color rl/BLUE})
    :smoke (rl/circle! {:x (int x)
                        :y (int y)
                        :radius radius
                        :color (fade rl/GRAY (- 1.0 (/ age 1.8)))})
    :fire (rl/circle! {:x (int x)
                       :y (int y)
                       :radius radius
                       :color (color-lerp rl/YELLOW rl/RED (/ (- 10.0 radius) 10.0))})
    nil))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - simple particles"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 particles [] current-type :water]
      (when (rl/keep-running? deadline)
        (let [ex (rl/get-mouse-x)
              ey (rl/get-mouse-y)
              emitted (if (< (count particles) MAX-PARTICLES)
                        [(emit-particle ex ey current-type)
                         (emit-particle ex ey current-type)
                         (emit-particle ex ey current-type)]
                        [])
              particles (into (filterv alive? (mapv update-particle particles)) emitted)
              current-type (cond
                             (rl/key-pressed? rl/KEY-RIGHT) (next-type current-type)
                             (rl/key-pressed? rl/KEY-LEFT) (prev-type current-type)
                             :else current-type)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [p particles] (draw-particle! p))
          (rl/rect! {:x 5
                     :y 5
                     :width 360
                     :height 60
                     :color (fade rl/SKYBLUE 0.5)})
          (rl/rect-lines! {:x 5
                           :y 5
                           :width 360
                           :height 60
                           :color rl/BLUE})
          (rl/text! "Move mouse to emit - LEFT/RIGHT change type"
                    {:x 15
                     :y 15
                     :size 10
                     :color rl/BLACK})
          (rl/text! (str "Type: " (type-name current-type) "   Particles: " (count particles))
                    {:x 15
                     :y 40
                     :size 10
                     :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) particles current-type)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
