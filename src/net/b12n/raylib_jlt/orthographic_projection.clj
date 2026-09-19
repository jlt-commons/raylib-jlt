(ns net.b12n.raylib-jlt.orthographic-projection
  "raylib [core] example - the same 3D scene under perspective vs orthographic
  projection. Press SPACE to toggle with-camera-3d's :projection (0/1). For
  orthographic, fovy is the view height (raylib convention). See
  docs/guide/struct-by-value-pointer-trick.md."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [core] example - orthographic projection"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           ortho? false]
      (when (app/keep-running? deadline)
        (let [ortho? (if (rl/key-pressed? rl/KEY-SPACE) (not ortho?) ortho?)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x 5.0
                              :pos-y 5.0
                              :pos-z 5.0
                              :fovy (if ortho? 12.0 45.0)
                              :projection (if ortho? 1 0)}
            (fn []
              (rl/draw-grid 10 1.0)
              (rl/cube! {:pos [-1.5 0.5 0.0]
                         :size 1.0
                         :color rl/RED})
              (rl/cube! {:pos [0.0 0.5 0.0]
                         :size 1.0
                         :color rl/GREEN})
              (rl/cube! {:pos [1.5 0.5 0.0]
                         :size 1.0
                         :color rl/BLUE})))
          (rl/text! (if ortho? "ORTHOGRAPHIC  (SPACE to toggle)" "PERSPECTIVE  (SPACE to toggle)")
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) ortho?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
