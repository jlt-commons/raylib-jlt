(ns net.b12n.raylib-jlt.camera-3d-first-person
  "raylib [core] example - first-person 3D camera (`joltc -M:camera-3d-first-person`).

  Walk a yard of random columns in first person: WASD moves, the mouse looks. Same
  3D path as camera-3d (Camera3D by value + rlgl cube!). The look direction is
  built from yaw/pitch and handed to the camera as its target; mouse-look uses
  GetMouseX/GetMouseY deltas over a free cursor, so turning is bounded by the
  window edges (a proper lock would need GetMouseDelta, which returns a by-value
  Vector2)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const EYE-Y 2.0)
(def ^:const SPEED 0.25)
(def ^:const SENS 0.004)
(def ^:const N-COLUMNS 40)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [core] example - first person")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        columns  (vec (repeatedly N-COLUMNS
                                  (fn []
                                    {:x (double (rl/get-random-value -20 20))
                                     :z (double (rl/get-random-value -20 20))
                                     :h (double (rl/get-random-value 2 12))
                                     :color (rl/rgba (rl/get-random-value 60 255)
                                                     (rl/get-random-value 60 255)
                                                     (rl/get-random-value 60 255) 255)})))]
    (loop [frame 0 px 0.0 pz 0.0 yaw 0.0 pitch 0.0 last-mx nil last-my nil]
      (when (rl/keep-running? deadline)
        (let [mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              yaw   (if last-mx (+ yaw (* SENS (- mx last-mx))) yaw)
              pitch (if last-my (-> (- pitch (* SENS (- my last-my))) (max -1.4) (min 1.4)) pitch)
              fwx (Math/cos yaw) fwz (Math/sin yaw)          ; horizontal forward
              rgx (- fwz)        rgz fwx                     ; right = cross(forward, up)
              dx (+ (if (rl/key-down? rl/KEY-W) fwx 0.0) (if (rl/key-down? rl/KEY-S) (- fwx) 0.0)
                    (if (rl/key-down? rl/KEY-D) rgx 0.0) (if (rl/key-down? rl/KEY-A) (- rgx) 0.0))
              dz (+ (if (rl/key-down? rl/KEY-W) fwz 0.0) (if (rl/key-down? rl/KEY-S) (- fwz) 0.0)
                    (if (rl/key-down? rl/KEY-D) rgz 0.0) (if (rl/key-down? rl/KEY-A) (- rgz) 0.0))
              px (+ px (* SPEED dx))
              pz (+ pz (* SPEED dz))
              cp (Math/cos pitch)
              tx (+ px (* cp fwx))                           ; look target = eye + forward
              ty (+ EYE-Y (Math/sin pitch))
              tz (+ pz (* cp fwz))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 140 190 230 255))    ; sky
          (rl/with-camera-3d {:pos-x px
                              :pos-y EYE-Y
                              :pos-z pz
                              :target-x tx
                              :target-y ty
                              :target-z tz
                              :fovy 60.0
                              :projection 0}
            (fn []
              (rl/draw-grid 40 1.0)
              (doseq [c columns]
                (rl/cube! :pos [(:x c) (/ (:h c) 2.0) (:z c)]
                          :size [2.0 (:h c) 2.0] :color (:color c)))))
          (rl/text! "WASD move - mouse look" :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (rl/fps! :x 10 :y (- H 30))
          (rl/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) px pz yaw pitch mx my)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
