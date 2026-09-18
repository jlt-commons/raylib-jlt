(ns net.b12n.raylib-jlt.highdpi-demo
  "raylib [core] example - highdpi demo (`jolt -M:highdpi-demo`).

  Port of raylib's examples/core/core_highdpi_demo.c. Two rulers over the same
  window: the orange one counts the logical points raylib draws in, the blue one
  counts the physical pixels the display actually has. On a Retina panel the blue
  ruler runs twice as far in the same space, which is the whole lesson. N moves
  the window to the next monitor, and the window is resizable so you can watch
  both rulers re-measure.

  Two new bindings, both scalar: `SetWindowMinSize` and `SetWindowMonitor`.

  This is the example that needs `FLAG_WINDOW_HIGHDPI`, and it is the only one in
  the suite that sets it. Measured here rather than assumed: without the flag
  `GetScreenWidth` and `GetRenderWidth` both answer 800 and the DPI scale is 1.0,
  so the two rulers would be identical and there would be nothing to show. With
  it the render target is 1600 wide against 800 logical points, scale 2.0.

  The cost is that its headless screenshot is not trustworthy. `highdpi-testbed`
  documents why: the capture doubles the scale again on top of the flag's own
  doubling, so the drawn content fills one quadrant of the dumped PNG. That is a
  property of the capture path, not of this example, and the window itself is
  correct. `highdpi-testbed` is the one to run when you want the same numbers in
  a shot that behaves.

  The C centres its labels with `MeasureTextEx` and `DrawTextEx` at a spacing of
  3. `MeasureText` and `DrawText` are already bound and answer the same question
  for the default font, so the labels are centred with those."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CELL 50)
(def ^:const LOGICAL-DESC-Y 120)
(def ^:const LOGICAL-LABEL-Y 150)
(def ^:const LOGICAL-TOP 180)
(def ^:const LOGICAL-BOTTOM 260)
(def ^:const PIXEL-TOP 240)
(def ^:const PIXEL-BOTTOM 320)
(def ^:const PIXEL-LABEL-Y 350)
(def ^:const PIXEL-DESC-Y 380)
(def ^:const MIN-TEXT-SPACE 30)

(defn- text-center!
  "DrawText with the string centred on x rather than starting at it."
  [s x y size color]
  (rl/text! s {:x (- x (/ (rl/text-width s {:size size}) 2))
               :y (- y (/ size 2))
               :size size
               :color color}))

(defn -main
  [& _]
  (rl/set-config-flags (bit-or rl/FLAG-WINDOW-HIGHDPI rl/FLAG-WINDOW-RESIZABLE))
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - highdpi demo"})
  (rl/set-window-min-size 450 450)
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [monitors (rl/get-monitor-count)
              current (rl/get-current-monitor)]
          (when (and (> monitors 1) (rl/key-pressed? rl/KEY-N))
            (rl/set-window-monitor (mod (inc current) monitors)))
          (let [[dpi-x _] (rl/get-window-scale-dpi)
                screen-w (rl/get-screen-width)
                render-w (rl/get-render-width)
                centre (/ screen-w 2)
                cell-px (/ (double CELL) dpi-x)]
            (rl/begin-drawing)
            (rl/clear-background rl/RAYWHITE)
            (text-center! (format "DPI scale: %.2f" dpi-x) centre 30 40 rl/DARKGRAY)
            (text-center! (str "monitor: " (inc current) "/" monitors "  (N for the next one)")
                          centre 70 20 rl/LIGHTGRAY)
            (text-center! (str "window is " screen-w " logical points wide")
                          centre LOGICAL-DESC-Y 20 rl/ORANGE)
            ;; The logical ruler: one band per CELL points, which is what every
            ;; other example in this suite is drawing in.
            (loop [i CELL odd? true]
              (when (< i screen-w)
                (when odd?
                  (rl/rect! {:x i
                             :y LOGICAL-TOP
                             :width CELL
                             :height (- LOGICAL-BOTTOM LOGICAL-TOP)
                             :color rl/ORANGE}))
                (text-center! (str i) i LOGICAL-LABEL-Y 10 rl/LIGHTGRAY)
                (rl/line! {:x1 i
                           :y1 (+ LOGICAL-LABEL-Y 10)
                           :x2 i
                           :y2 LOGICAL-BOTTOM
                           :color rl/GRAY})
                (recur (+ i CELL) (not odd?))))
            ;; The pixel ruler: one band per CELL PIXELS, drawn at the logical
            ;; position those pixels land on, so it runs denser by the DPI scale.
            (loop [i CELL odd? true last-label (- MIN-TEXT-SPACE)]
              (when (< i render-w)
                (let [x (int (/ (double i) dpi-x))]
                  (when odd?
                    (rl/rect! {:x x
                               :y PIXEL-TOP
                               :width (int cell-px)
                               :height (- PIXEL-BOTTOM PIXEL-TOP)
                               :color (rl/rgba 0 121 241 100)}))
                  (rl/line! {:x1 x
                             :y1 PIXEL-TOP
                             :x2 x
                             :y2 (- PIXEL-LABEL-Y 10)
                             :color rl/GRAY})
                  (if (>= (- x last-label) MIN-TEXT-SPACE)
                    (do (text-center! (str i) x PIXEL-LABEL-Y 10 rl/LIGHTGRAY)
                        (recur (+ i CELL) (not odd?) x))
                    (recur (+ i CELL) (not odd?) last-label)))))
            (text-center! (str "window is " render-w " physical pixels wide")
                          centre PIXEL-DESC-Y 20 rl/BLUE)
            (rl/text! "can you see this?" {:x (- screen-w (rl/text-width "can you see this?" {:size 20}) 5)
                                           :y (- (rl/get-screen-height) 25)
                                           :size 20
                                           :color rl/LIGHTGRAY})
            (rl/maybe-screenshot! frame 20)
            (rl/end-drawing)
            (recur (inc frame))))))
    (rl/close-window)))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
