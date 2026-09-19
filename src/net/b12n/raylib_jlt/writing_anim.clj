(ns net.b12n.raylib-jlt.writing-anim
  "raylib [text] example - writing animation (`joltc -M:writing-anim`).

  A message types itself out one character at a time, pauses at the end, then
  restarts. A growing substring of the full text driven by the frame counter."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const msg "This message types itself out, one character at a time...")

(defn -main
  [& _]
  (rl/window! {:width W
               :height 450
               :title "raylib [text] example - writing animation"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        n (count msg)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [chars (mod (quot frame 3) (+ n 40))   ; +40-frame pause at the end
              shown (subs msg 0 (min n chars))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "(a self-typing message)" {:x 40
                                               :y 20
                                               :size 20
                                               :color rl/GRAY})
          (rl/text! shown {:x 40
                           :y 200
                           :size 24
                           :color rl/DARKBLUE})
          (app/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
