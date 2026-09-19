(ns net.b12n.raylib-jlt.random-values
  "raylib [core] example - random values (`joltc -M:random-values`).

  A new random value (0-99) every two seconds via GetRandomValue, with a small
  history of recent rolls."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)

(defn -main
  [& _]
  (rl/window! {:width W
               :height 450
               :title "raylib [core] example - random values"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0 value (rl/get-random-value 0 99) history []]
      (when (app/keep-running? deadline)
        (let [roll?   (zero? (mod frame 120))            ; every 2s at 60fps
              value   (if roll? (rl/get-random-value 0 99) value)
              history (if roll? (vec (take-last 8 (conj history value))) history)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "a new random value every 2 seconds" {:x 40
                                                          :y 40
                                                          :size 20
                                                          :color rl/DARKGRAY})
          (rl/text! (str value) {:x 350
                                 :y 150
                                 :size 90
                                 :color rl/MAROON})
          (rl/text! (str "recent: " (apply str (interpose " " history)))
                    {:x 40
                     :y 380
                     :size 20
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) value history)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
