(ns net.b12n.raylib-jlt.format-text
  "raylib [text] example - formatted text (`joltc -M:format-text`).

  A zero-padded score and an MM:SS timer counting up, built with
  clojure.core/format and drawn each frame."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [text] example - formatted text"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [secs  (quot frame 60)
              score (* frame 7)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! (format "SCORE: %08d" score) {:x 60
                                                  :y 150
                                                  :size 40
                                                  :color rl/MAROON})
          (rl/text! (format "TIME: %02d:%02d" (quot secs 60) (mod secs 60))
                    {:x 60
                     :y 230
                     :size 40
                     :color rl/DARKBLUE})
          (app/maybe-screenshot! frame 90)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
