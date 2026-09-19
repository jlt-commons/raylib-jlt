(ns net.b12n.raylib-jlt.camera-2d-split-screen
  "raylib [core] example - 2D camera split screen.

  Two players roam one shared grid world, each rendered through its own
  Camera2D into its own half-screen render texture, then the two textures are
  drawn side by side. W/S/A/D moves player one (red), the arrow keys move
  player two (blue). No new FFI: composes with-camera-2d, rl/render-texture
  and rl/texture! exactly as camera2d.clj and render_texture.clj already do.
  Ported from raylib's examples/core/core_2d_camera_split_screen.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 440)
(def ^:const HALF-W 400)
(def ^:const PSIZE 40)
(def ^:const COLS 20)
(def ^:const ROWS 11)

(defn- draw-scene
  "The shared world both half-screens render: a labelled grid plus both
  players. Coordinates are world space; each half's own Camera2D decides
  what part of it lands on screen."
  [p1x p1y p2x p2y]
  (dotimes [i (inc COLS)]
    (rl/line! {:x1 (* PSIZE i)
               :y1 0
               :x2 (* PSIZE i)
               :y2 H
               :color rl/LIGHTGRAY}))
  (dotimes [j (inc ROWS)]
    (rl/line! {:x1 0
               :y1 (* PSIZE j)
               :x2 W
               :y2 (* PSIZE j)
               :color rl/LIGHTGRAY}))
  (dotimes [i COLS]
    (dotimes [j ROWS]
      (rl/text! (str "[" i "," j "]")
                {:x (+ 10 (* PSIZE i))
                 :y (+ 15 (* PSIZE j))
                 :size 10
                 :color rl/LIGHTGRAY})))
  (rl/rect! {:x (int p1x)
             :y (int p1y)
             :width PSIZE
             :height PSIZE
             :color rl/RED})
  (rl/rect! {:x (int p2x)
             :y (int p2y)
             :width PSIZE
             :height PSIZE
             :color rl/BLUE}))

(defn- draw-half!
  [rt target-x target-y p1x p1y p2x p2y banner-text banner-color]
  (rl/with-render-texture rt
    (fn []
      (rl/clear-background rl/RAYWHITE)
      (rl/with-camera-2d {:offset-x 200.0
                          :offset-y 200.0
                          :target-x target-x
                          :target-y target-y}
        (fn [] (draw-scene p1x p1y p2x p2y)))
      ;; RAYWHITE at 60% alpha (Fade(RAYWHITE, 0.6)) behind the banner text.
      (rl/rect! {:x 0
                 :y 0
                 :width HALF-W
                 :height 30
                 :color (rl/rgba 245 245 245 153)})
      (rl/text! banner-text {:x 10
                             :y 10
                             :size 10
                             :color banner-color}))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - 2d camera split screen"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        rt1       (rl/render-texture HALF-W H)
        rt2       (rl/render-texture HALF-W H)]
    (if-not (and rt1 rt2)
      (binding [*out* *err*]
        (println "camera-2d-split-screen: the driver reported an incomplete framebuffer"))
      (do
        (loop [frame 0 p1x 200.0 p1y 200.0 p2x 250.0 p2y 200.0]
          (when (app/keep-running? deadline)
            (let [p1y (cond (rl/key-down? rl/KEY-S) (+ p1y 3.0)
                            (rl/key-down? rl/KEY-W) (- p1y 3.0)
                            :else p1y)
                  p1x (cond (rl/key-down? rl/KEY-D) (+ p1x 3.0)
                            (rl/key-down? rl/KEY-A) (- p1x 3.0)
                            :else p1x)
                  p2y (cond (rl/key-down? rl/KEY-UP) (- p2y 3.0)
                            (rl/key-down? rl/KEY-DOWN) (+ p2y 3.0)
                            :else p2y)
                  p2x (cond (rl/key-down? rl/KEY-RIGHT) (+ p2x 3.0)
                            (rl/key-down? rl/KEY-LEFT) (- p2x 3.0)
                            :else p2x)]
              (draw-half! rt1 p1x p1y p1x p1y p2x p2y "PLAYER1: W/S/A/D to move" rl/MAROON)
              (draw-half! rt2 p2x p2y p1x p1y p2x p2y "PLAYER2: UP/DOWN/LEFT/RIGHT to move" rl/DARKBLUE)
              (rl/begin-drawing)
              (rl/clear-background rl/BLACK)
              (rl/texture! (:texture rt1)
                           {:x 0
                            :y 0
                            :width HALF-W
                            :height H
                            :v0 1.0
                            :v1 0.0
                            :tint rl/WHITE})
              (rl/texture! (:texture rt2)
                           {:x HALF-W
                            :y 0
                            :width HALF-W
                            :height H
                            :v0 1.0
                            :v1 0.0
                            :tint rl/WHITE})
              (rl/rect! {:x (- HALF-W 2)
                         :y 0
                         :width 4
                         :height H
                         :color rl/LIGHTGRAY})
              (app/maybe-screenshot! frame 30)
              (rl/end-drawing)
              (recur (inc frame) p1x p1y p2x p2y))))
        (rl/unload-render-texture! rt1)
        (rl/unload-render-texture! rt2))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
