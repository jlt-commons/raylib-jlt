(ns net.b12n.raylib-jlt.rectangle-scaling
  "raylib [shapes] example - rectangle scaling. Drag the bottom-right corner handle to
  resize a rectangle (clamped to a minimum). The live W x H is shown. Port of
  shapes_rectangle_scaling (mouse-driven; headless it shows the initial size)."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:private handle 18)

(defn- handle-tri!
  [hx hy color]
  (rl/rl-begin rl/RL-TRIANGLES)
  (rl/rl-color! color)
  (rl/rl-vertex-2f (double (- hx handle)) (double hy))
  (rl/rl-vertex-2f (double hx) (double hy))
  (rl/rl-vertex-2f (double hx) (double (- hy handle)))
  (rl/rl-end))

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - rectangle scaling"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        rx 300 ry 165]
    (loop [frame 0 rw 220 rh 150]
      (when (app/keep-running? deadline)
        (let [mx (rl/get-mouse-x) my (rl/get-mouse-y)
              hx (+ rx rw) hy (+ ry rh)
              over? (and (>= mx (- hx handle)) (<= mx (+ hx 6))
                         (>= my (- hy handle)) (<= my (+ hy 6)))
              drag? (and over? (rl/mouse-down? rl/MOUSE-LEFT))
              rw (if drag? (max 70 (- mx rx)) rw)
              rh (if drag? (max 50 (- my ry)) rh)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect! {:x rx
                     :y ry
                     :width rw
                     :height rh
                     :color (rl/rgba 70 130 200 90)})
          (rl/rect-lines! {:x rx
                           :y ry
                           :width rw
                           :height rh
                           :color rl/BLUE})
          (handle-tri! (+ rx rw) (+ ry rh) (if over? rl/RED rl/DARKBLUE))
          (rl/text! (format "W x H: %d x %d  (drag the corner handle)" rw rh)
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame) rw rh)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
