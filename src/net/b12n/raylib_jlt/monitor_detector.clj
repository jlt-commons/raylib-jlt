(ns net.b12n.raylib-jlt.monitor-detector
  "raylib [core] example - monitor detector (`jolt -M:monitor-detector`).

  Every display attached to the machine, with its resolution and refresh rate,
  and which one the window is currently on. Move the window to another screen and
  the highlight follows it.

  All four queries are scalar, so they bind directly. GetMonitorPosition is the
  one that does not: it returns a Vector2 by value, which is why the layout below
  is a list rather than a to-scale map of where the displays sit."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - monitor detector"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (let [n (rl/get-monitor-count)
              current (rl/get-current-monitor)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! (str n (if (= 1 n) " monitor detected" " monitors detected"))
                    {:x 40
                     :y 34
                     :size 24
                     :color rl/DARKGRAY})
          (dotimes [i n]
            (let [y (+ 90 (* i 90))
                  here? (= i current)
                  mw (rl/get-monitor-width i)
                  mh (rl/get-monitor-height i)]
              (rl/rect! {:x 40
                         :y y
                         :width (- W 80)
                         :height 74
                         :color (if here? (rl/rgba 0 121 241 40) (rl/rgba 0 0 0 12))})
              (rl/rect-lines! {:x 40
                               :y y
                               :width (- W 80)
                               :height 74
                               :color (if here? rl/BLUE rl/LIGHTGRAY)})
              (rl/text! (str i ": " (rl/get-monitor-name i))
                        {:x 56
                         :y (+ y 12)
                         :size 20
                         :color (if here? rl/DARKBLUE rl/DARKGRAY)})
              (rl/text! (str mw " x " mh " at " (rl/get-monitor-refresh-rate i) " Hz")
                        {:x 56
                         :y (+ y 40)
                         :size 16
                         :color rl/GRAY})
              (when here?
                (rl/text! "window is here" {:x (- W 220)
                                            :y (+ y 40)
                                            :size 16
                                            :color rl/BLUE}))))
          (rl/text! "drag the window to another display to move the highlight"
                    {:x 40
                     :y (- H 34)
                     :size 14
                     :color rl/GRAY})
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing))
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
