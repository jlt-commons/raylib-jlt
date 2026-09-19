(ns net.b12n.raylib-jlt.core
  "raylib [core] example - basic window (the default: `joltc -M:run`).

  Ported from raylib's examples/core/core_basic_window.c: an 800x450 window that
  clears to RAYWHITE and draws one line of text. The FFI bindings and the keyword-
  argument drawing API live in net.b12n.raylib-jlt.raylib. See README.md for the full example list
  and the Color-by-value explanation."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [core] example - basic window"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/text! "Congrats! You created your first window!"
                  {:x 190
                   :y 200
                   :size 20
                   :color rl/LIGHTGRAY})
        (app/maybe-screenshot! frame 10)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
