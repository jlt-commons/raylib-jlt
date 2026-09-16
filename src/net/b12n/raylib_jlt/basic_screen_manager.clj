(ns net.b12n.raylib-jlt.basic-screen-manager
  "raylib [core] example - screen manager (`joltc -M:basic-screen-manager`).

  A minimal screen/state machine: LOGO → TITLE → GAMEPLAY → ENDING. It advances on
  ENTER, and also auto-advances on a timer so the headless smoke test flows through
  every screen. A keyword state drives the background + label."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const KEY-ENTER 257)

(def ^:private screens [:logo :title :gameplay :ending])
(def ^:private bg    {:logo rl/RAYWHITE
                      :title rl/DARKBLUE
                      :gameplay rl/DARKGREEN
                      :ending rl/MAROON})
(def ^:private label {:logo "LOGO"
                      :title "TITLE SCREEN"
                      :gameplay "GAMEPLAY"
                      :ending "ENDING"})

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [core] example - screen manager")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 idx 0]
      (when (rl/keep-running? deadline)
        (let [advance? (or (rl/key-pressed? KEY-ENTER)
                           (zero? (mod (inc frame) 90)))   ; auto-advance every ~1.5s
              idx (if advance? (mod (inc idx) (count screens)) idx)
              scr (nth screens idx)
              tc  (if (= scr :logo) rl/DARKGRAY rl/RAYWHITE)]
          (rl/begin-drawing)
          (rl/clear-background (bg scr))
          (rl/text! (label scr) :x 60 :y 200 :size 40 :color tc)
          (rl/text! "press ENTER to advance" :x 60 :y 260 :size 20 :color tc)
          (rl/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) idx)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
