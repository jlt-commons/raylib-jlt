(ns net.b12n.raylib-jlt.window-should-close
  "raylib [core] example - window should close (`jolt -M:window-should-close`).

  Port of raylib's examples/core/core_window_should_close.c. Press ESC, or click
  the window's close button, and instead of the window vanishing you get a
  confirmation panel: Y closes, N goes back.

  The mechanism is SetExitKey. By default raylib closes on ESC and
  WindowShouldClose reports it, so an example has no chance to intervene. Passing
  KEY-NULL takes that binding away, leaving the close request as something the
  loop can see and answer. Both are bound here for the first time; every other
  example takes the default.

  This one owns its exit condition rather than deferring to rl/keep-running?, so
  the auto-quit deadline is checked alongside its own flag. Without that the
  headless smoke would never terminate."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - window should close"})
  ;; Take ESC away from raylib so the confirmation below can answer for it.
  (rl/set-exit-key rl/KEY-NULL)
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           asked? false
           exit? false]
      ;; Deliberately NOT rl/keep-running?, which folds window-should-close? into
      ;; its answer. Clicking the close button would end the loop before the
      ;; confirmation ever drew, which is the exact behaviour this example exists
      ;; to replace. Only the auto-quit deadline is honoured here.
      (when (and (not exit?)
                 (or (nil? deadline) (< (System/currentTimeMillis) deadline)))
        (let [asked? (or asked?
                         (rl/key-pressed? rl/KEY-ESCAPE)
                         (rl/window-should-close?))
              exit?  (and asked? (rl/key-pressed? rl/KEY-Y))
              asked? (if (and asked? (rl/key-pressed? rl/KEY-N)) false asked?)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (if asked?
            (do
              (rl/rect! {:x 0
                         :y 100
                         :width W
                         :height 200
                         :color rl/BLACK})
              (rl/text! "Are you sure you want to exit program? [Y/N]"
                        {:x 40
                         :y 180
                         :size 30
                         :color rl/WHITE}))
            (rl/text! "Try to close the window to get confirmation message!"
                      {:x 120
                       :y 200
                       :size 20
                       :color rl/LIGHTGRAY}))
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) asked? exit?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
