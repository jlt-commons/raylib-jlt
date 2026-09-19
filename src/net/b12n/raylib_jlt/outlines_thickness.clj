(ns net.b12n.raylib-jlt.outlines-thickness
  "raylib [shapes] example - outlines thickness (`jolt -M:outlines-thickness`).

  Port of raylib's examples/shapes/shapes_outlines_thickness.c. Three outline
  calls that take a thickness, side by side, with one number driving all of
  them so the shapes can be compared at the same setting.

  The number goes negative, which is the part worth watching, though not for
  the reason the range suggests. raylib guards both rectangle calls with
  `if (thick > 0)`, so a negative thickness draws nothing at all for the plain
  rectangle and collapses the rounded one to a hairline. There is no outward
  band. ring! is built from two radii rather than a thickness, so it is the
  only one of the three that can grow outward, and it does.

  Upstream drives the value with a raygui slider. raygui is a separate library
  and this suite does not bind it, so the value sweeps on its own here and UP
  and DOWN take over once either is pressed.

  Two of these needed new bindings. DrawRectangleLinesEx,
  DrawRectangleRounded and DrawRectangleRoundedLinesEx take their Rectangle by
  value, which jolt could not do before 0.7.23. The circle is ring!, because
  DrawCircleLinesEx landed after the 6.0 tag and the released library this
  suite links does not export it at all. net.b12n.raylib-jlt.rounded-rectangle hand-rolls its corners out of
  sectors for exactly that reason and is left alone: the rlgl paths in this
  suite are not being migrated opportunistically."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MIN-THICK -30.0)
(def ^:const MAX-THICK 30.0)
(def ^:const STEP 1.0)

(def ^:const ROUNDNESS 0.2)
(def ^:const SEGMENTS 9)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - outlines thickness"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           thick 5.0
           manual? false]
      (if-not (app/keep-running? deadline)
        nil
        (let [up? (rl/key-down? rl/KEY-UP)
              down? (rl/key-down? rl/KEY-DOWN)
              manual?' (or manual? up? down?)
              ;; until a key is touched, sweep the whole range so both signs
              ;; of the thickness get shown without anyone driving it
              thick' (cond
                       up? (min MAX-THICK (+ thick STEP))
                       down? (max MIN-THICK (- thick STEP))
                       manual?' thick
                       :else (* MAX-THICK (Math/sin (* frame 0.02))))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)

          (rl/rect! {:x 35
                     :y 180
                     :width 220
                     :height 220
                     :color rl/LIGHTGRAY})
          (rl/rect-lines-ex! {:x 35
                              :y 180
                              :width 220
                              :height 220
                              :thick thick'
                              :color rl/BLUE})
          (rl/text! "rect-lines-ex!" {:x 35
                                      :y 140
                                      :size 13
                                      :color rl/BLACK})

          (rl/rect-rounded! {:x 290
                             :y 180
                             :width 220
                             :height 220
                             :roundness ROUNDNESS
                             :segments SEGMENTS
                             :color rl/LIGHTGRAY})
          (rl/rect-rounded-lines-ex! {:x 290
                                      :y 180
                                      :width 220
                                      :height 220
                                      :roundness ROUNDNESS
                                      :segments SEGMENTS
                                      :thick thick'
                                      :color rl/BLUE})
          (rl/text! "rect-rounded-lines-ex!" {:x 290
                                              :y 140
                                              :size 13
                                              :color rl/BLACK})

          (rl/circle! {:x 655
                       :y 290
                       :radius 110
                       :color rl/LIGHTGRAY})
          ;; ring! rather than a DrawCircleLinesEx binding: that call landed
          ;; after the 6.0 tag and the released library this links does not
          ;; export it. Two radii from a signed thickness give the same
          ;; inward/outward behaviour as the rectangles beside it.
          (let [inner (min 110.0 (- 110.0 thick'))
                outer (max 110.0 (- 110.0 thick'))]
            (when (> outer inner)
              (rl/ring! {:cx 655
                         :cy 290
                         :inner inner
                         :outer outer
                         :start-deg 0
                         :end-deg 360
                         :segments 64
                         :color rl/BLUE})))
          (rl/text! "ring!" {:x 560
                             :y 140
                             :size 13
                             :color rl/BLACK})

          (rl/text! "raylib [shapes] example - outlines thickness"
                    {:x 20
                     :y 20
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! (str "thickness " (format "%.2f" thick')
                         (if (neg? thick') "  (band grows outward)" "  (band grows inward)"))
                    {:x 20
                     :y 50
                     :size 18
                     :color rl/MAROON})
          (rl/text! "negative: raylib draws no rectangle outline; only ring! grows outward"
                    {:x 20
                     :y (- H 20)
                     :size 15
                     :color rl/GRAY})
          (rl/text! (if manual?'
                      "UP / DOWN change the thickness"
                      "sweeping: press UP or DOWN to take over")
                    {:x 20
                     :y 82
                     :size 16
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) thick' manual?'))))))
