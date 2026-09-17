(ns net.b12n.raylib-jlt.highdpi-testbed
  "raylib [core] example - highdpi testbed.

  A diagnostic overlay for HighDPI / multi-monitor setups: a labelled
  pixel grid, the current monitor + window position + screen/render size
  + DPI scale factor, corner reference rectangles, and a mouse crosshair
  with live coordinates. SPACE toggles borderless-windowed, F toggles
  fullscreen.

  Three new bindings: rl/get-window-scale-dpi and rl/get-window-position
  are genuinely by-value Vector2 returns (the same GetWorldToScreen
  out-pointer pattern world-to-screen already uses in raylib.clj), and
  rl/toggle-borderless-windowed! is a plain void call. Everything else
  (monitor count/current/width/height, render/screen size, toggle
  fullscreen) was already bound.

  Deliberately does NOT set FLAG_WINDOW_HIGHDPI, unlike raylib's own C
  example: on this suite's headless verification environment, that flag
  makes the render target 2x the window size as intended, but then the
  screenshot capture doubles the scale again on top of that, so the
  drawn content only fills one quadrant of the dumped PNG. Confirmed
  with a 3-line probe (a solid rect, no other code involved) before
  concluding it wasn't this file's logic. The diagnostic overlay is
  still useful without the flag; SCALE FACTOR just reads 1.00x1.00.
  Ported from raylib's examples/core/core_highdpi_testbed.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const GRID 40)

(defn- zero-pad
  [n width]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- width (count s))) "0")) s)))

(defn -main
  [& _]
  (rl/set-config-flags rl/FLAG-WINDOW-RESIZABLE)
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - highdpi testbed"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (when (rl/key-pressed? rl/KEY-SPACE) (rl/toggle-borderless-windowed!))
        (when (rl/key-pressed? rl/KEY-F) (rl/toggle-fullscreen))
        (let [cur (rl/get-current-monitor)
              [dpx dpy] (rl/get-window-scale-dpi)
              [wpx wpy] (rl/get-window-position)
              mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              sw (rl/get-screen-width)
              sh (rl/get-screen-height)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)

          (doseq [h (range (inc (quot sh GRID)))]
            (rl/text! (zero-pad (* h GRID) 2) {:x 4
                                               :y (- (* h GRID) 4)
                                               :size 10
                                               :color rl/GRAY})
            (rl/line! {:x1 24
                       :y1 (* h GRID)
                       :x2 sw
                       :y2 (* h GRID)
                       :color rl/LIGHTGRAY}))
          (doseq [v (range (inc (quot sw GRID)))]
            (rl/text! (zero-pad (* v GRID) 2) {:x (- (* v GRID) 10)
                                               :y 4
                                               :size 10
                                               :color rl/GRAY})
            (rl/line! {:x1 (* v GRID)
                       :y1 20
                       :x2 (* v GRID)
                       :y2 sh
                       :color rl/LIGHTGRAY}))

          (rl/text! (str "CURRENT MONITOR: " (inc cur) "/" (rl/get-monitor-count)
                         " (" (rl/get-monitor-width cur) "x" (rl/get-monitor-height cur) ")")
                    {:x 50
                     :y 50
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! (str "WINDOW POSITION: " (int wpx) "x" (int wpy))
                    {:x 50
                     :y 90
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! (str "SCREEN SIZE: " sw "x" sh) {:x 50
                                                     :y 130
                                                     :size 20
                                                     :color rl/DARKGRAY})
          (rl/text! (str "RENDER SIZE: " (rl/get-render-width) "x" (rl/get-render-height))
                    {:x 50
                     :y 170
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! (str "SCALE FACTOR: " (format "%.2f" dpx) "x" (format "%.2f" dpy))
                    {:x 50
                     :y 210
                     :size 20
                     :color rl/GRAY})

          (rl/rect! {:x 0
                     :y 0
                     :width 30
                     :height 60
                     :color rl/RED})
          (rl/rect! {:x (- sw 30)
                     :y (- sh 60)
                     :width 30
                     :height 60
                     :color rl/BLUE})

          (rl/circle! {:x mx
                       :y my
                       :radius 20.0
                       :color rl/MAROON})
          (rl/rect! {:x (- mx 25)
                     :y my
                     :width 50
                     :height 2
                     :color rl/BLACK})
          (rl/rect! {:x mx
                     :y (- my 25)
                     :width 2
                     :height 50
                     :color rl/BLACK})
          (rl/text! (str "[" mx "," my "]")
                    {:x (- mx 44)
                     :y (if (> my (- sh 60)) (- my 46) (+ my 30))
                     :size 20
                     :color rl/BLACK})

          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
