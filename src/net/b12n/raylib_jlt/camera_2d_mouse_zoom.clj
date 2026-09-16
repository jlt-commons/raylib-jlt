(ns net.b12n.raylib-jlt.camera-2d-mouse-zoom
  "raylib [core] example - 2d camera mouse zoom (`jolt -M:camera-2d-mouse-zoom`).

  Port of raylib's examples/core/core_2d_camera_mouse_zoom.c. Drag with the left
  button to pan. Key 1 zooms on the mouse wheel, key 2 zooms by dragging with the
  right button.

  Zooming toward the cursor is the trick worth reading. Scaling the camera alone
  zooms toward its target, which is wherever the camera happens to be looking, so
  the point under the cursor slides away. Pinning it takes three steps in order:
  find the world point under the cursor at the current zoom, move the camera's
  offset to the cursor's screen position, then set its target to that world
  point. After that the same world point sits under the same pixel at any zoom.

  Zoom is stepped in log space, `exp(log(zoom) + 0.2*wheel)`, so each notch is a
  constant ratio. Adding to zoom directly makes a notch feel enormous when zoomed
  out and negligible when zoomed in.

  Neither GetScreenToWorld2D nor GetMouseDelta is bound. Both take or return a
  Vector2 by value, which the pointer trick does not cover. Both are a couple of
  lines of arithmetic here: the inverse camera transform, and the difference
  against the previous frame's cursor position."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MIN-ZOOM 0.125)
(def ^:const MAX-ZOOM 64.0)

(defn- screen->world
  "GetScreenToWorld2D, for a camera with no rotation."
  [sx sy {:keys [offset-x offset-y target-x target-y zoom]}]
  [(+ (/ (- sx offset-x) zoom) target-x)
   (+ (/ (- sy offset-y) zoom) target-y)])

(defn- clamp [v lo hi] (max lo (min hi v)))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [core] example - 2d camera mouse zoom")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           cam {:offset-x 0.0
                :offset-y 0.0
                :target-x 0.0
                :target-y 0.0
                :zoom 1.0}
           mode :wheel
           prev-mx 0.0
           prev-my 0.0]
      (when (rl/keep-running? deadline)
        (let [mx    (double (rl/get-mouse-x))
              my    (double (rl/get-mouse-y))
              dx    (- mx prev-mx)          ; GetMouseDelta, by hand
              dy    (- my prev-my)
              mode  (cond (rl/key-pressed? rl/KEY-ONE) :wheel
                          (rl/key-pressed? rl/KEY-TWO) :drag
                          :else mode)
              zoom  (:zoom cam)
              ;; Left-drag pans. The delta is in screen pixels, so dividing by
              ;; zoom keeps the world moving with the cursor at any scale.
              cam   (if (rl/mouse-down? rl/MOUSE-LEFT)
                      (-> cam
                          (update :target-x - (/ dx zoom))
                          (update :target-y - (/ dy zoom)))
                      cam)
              wheel (rl/get-mouse-wheel)
              ;; Both modes pin the cursor first, then change zoom.
              pin   (fn [c]
                      (let [[wx wy] (screen->world mx my c)]
                        (assoc c :offset-x mx :offset-y my :target-x wx :target-y wy)))
              cam   (cond
                      (and (= mode :wheel) (not (zero? wheel)))
                      (let [c (pin cam)]
                        (assoc c :zoom (clamp (Math/exp (+ (Math/log (:zoom c)) (* 0.2 wheel)))
                                              MIN-ZOOM MAX-ZOOM)))

                      (and (= mode :drag) (rl/mouse-pressed? rl/MOUSE-RIGHT))
                      (pin cam)

                      (and (= mode :drag) (rl/mouse-down? rl/MOUSE-RIGHT))
                      (assoc cam :zoom (clamp (Math/exp (+ (Math/log (:zoom cam)) (* 0.005 dx)))
                                              MIN-ZOOM MAX-ZOOM))

                      :else cam)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-2d cam
            (fn []
              ;; A world-space grid, so panning and zooming are visible.
              (dotimes [gy 21]
                (dotimes [gx 21]
                  (let [x (* (- gx 10) 50) y (* (- gy 10) 50)]
                    (rl/rect-lines! :x x :y y :width 50 :height 50
                                    :color (rl/rgba 220 220 220 255)))))
              (rl/rect! :x -20 :y -20 :width 40 :height 40 :color rl/RED)
              (rl/circle! :x 200 :y 100 :radius 30 :color rl/BLUE)
              (rl/rect! :x -300 :y 150 :width 120 :height 60 :color rl/LIME)
              (rl/text! "world origin" :x 30 :y 30 :size 20 :color rl/DARKGRAY)))
          ;; Crosshair at the cursor, in screen space, so the pinning is obvious.
          (rl/line! :x1 (- mx 12) :y1 my :x2 (+ mx 12) :y2 my :color rl/DARKGRAY)
          (rl/line! :x1 mx :y1 (- my 12) :x2 mx :y2 (+ my 12) :color rl/DARKGRAY)
          ;; Along the bottom, not the top. The camera starts with offset and
          ;; target at zero, which puts world (0,0) in the top-left corner, so
          ;; anything drawn there collides with the world content underneath it.
          (rl/text! "[1] wheel zoom  [2] right-drag zoom  -  left-drag to pan"
                    :x 10 :y (- H 56) :size 20 :color rl/DARKGRAY)
          (rl/text! (str "mode " (name mode) "   zoom " (format "%.3f" (:zoom cam)))
                    :x 10 :y (- H 30) :size 20 :color rl/MAROON)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) cam mode mx my)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
