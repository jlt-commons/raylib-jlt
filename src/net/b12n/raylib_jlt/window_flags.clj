(ns net.b12n.raylib-jlt.window-flags
  "raylib [core] example - window flags (`jolt -M:window-flags`).

  The window's configuration bits, toggled live. Number keys 1-4 flip vsync,
  resizable, undecorated and always-on-top; the list shows which are on, read back
  from raylib with IsWindowState rather than from a local copy, so it stays honest
  if the window manager refuses one.

  A bouncing box gives the frame rate something to say: with vsync off the FPS
  counter climbs to whatever the machine can manage, and the box moves by delta
  time so its speed does not change with it."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private toggles
  [["1" "FLAG_VSYNC_HINT" rl/FLAG-VSYNC-HINT]
   ["2" "FLAG_WINDOW_RESIZABLE" rl/FLAG-WINDOW-RESIZABLE]
   ["3" "FLAG_WINDOW_UNDECORATED" rl/FLAG-WINDOW-UNDECORATED]
   ["4" "FLAG_WINDOW_TOPMOST" rl/FLAG-WINDOW-TOPMOST]])

(def ^:private keys-for-toggles [rl/KEY-ONE rl/KEY-TWO rl/KEY-THREE rl/KEY-FOUR])

(defn -main
  [& _]
  (rl/set-config-flags rl/FLAG-VSYNC-HINT)
  (rl/window! :width W :height H :title "raylib [core] example - window flags")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           x 60.0
           vx 220.0]
      (when (rl/keep-running? deadline)
        (dotimes [i 4]
          (when (rl/key-pressed? (nth keys-for-toggles i))
            (let [flag (nth (nth toggles i) 2)]
              ;; Ask raylib what is set rather than tracking it here: the window
              ;; manager gets the final say and can decline a flag.
              (if (rl/window-state? flag)
                (rl/clear-window-state flag)
                (rl/set-window-state flag)))))
        (let [dt (rl/get-frame-time)
              sw (rl/get-screen-width)
              nx (+ x (* vx dt))
              vx (if (or (< nx 0) (> (+ nx 60) sw)) (- vx) vx)
              nx (max 0.0 (min (double (- sw 60)) nx))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 24 28 38 255))
          (rl/rect! :x (int nx) :y (- (rl/get-screen-height) 90)
                    :width 60 :height 60 :color rl/GOLD)
          (rl/text! "window flags" :x 30 :y 26 :size 24 :color rl/RAYWHITE)
          (dotimes [i 4]
            (let [[k label flag] (nth toggles i)
                  on? (rl/window-state? flag)]
              (rl/text! (str "[" k "] " label)
                        :x 30 :y (+ 76 (* i 30)) :size 18
                        :color (if on? rl/GREEN rl/GRAY))
              (rl/text! (if on? "on" "off")
                        :x 400 :y (+ 76 (* i 30)) :size 18
                        :color (if on? rl/GREEN rl/DARKGRAY))))
          (rl/text! (str "screen " (rl/get-screen-width) "x" (rl/get-screen-height)
                         "   render " (rl/get-render-width) "x" (rl/get-render-height))
                    :x 30 :y 220 :size 16 :color rl/LIGHTGRAY)
          (rl/text! "turn vsync off and watch the FPS counter climb"
                    :x 30 :y 250 :size 14 :color rl/GRAY)
          (rl/fps! :x 30 :y 280)
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) nx vx)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
