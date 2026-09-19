(ns net.b12n.raylib-jlt.smooth-pixelperfect
  "raylib [core] example - smooth pixelperfect.

  Three rectangles spin inside a tiny 160x90 pixel-art world rendered to a
  render texture and upscaled 5x to the window. The camera sways on a
  sine/cosine path; the world camera gets the integer part of its position
  and the screen camera gets the sub-pixel remainder, which smooths the
  upscaled motion. S toggles the smoothing, O toggles overscan. No new
  bindings: rl/rect-pro! already draws a rotated rectangle by origin, the
  same DrawRectanglePro stand-in easings-rectangles.clj already uses.
  Ported from raylib's examples/core/core_smooth_pixelperfect.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const VW 160)
(def ^:const VH 90)
(def ^:const RATIO 5.0)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - smooth pixelperfect"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        target (rl/render-texture VW VH)]
    (if-not target
      (binding [*out* *err*]
        (println "smooth-pixelperfect: the driver reported an incomplete framebuffer"))
      (do
        (loop [frame 0 rot 0.0 smooth? true overscan? false]
          (when (app/keep-running? deadline)
            (let [rot (+ rot (* 60.0 (rl/get-frame-time)))
                  t (rl/get-time)
                  cx (- (* (Math/sin t) 50.0) 10.0)
                  cy (* (Math/cos t) 30.0)
                  wx (double (long cx))
                  wy (double (long cy))
                  sx (* (- cx wx) RATIO)
                  sy (* (- cy wy) RATIO)
                  smooth? (if (rl/key-pressed? rl/KEY-S) (not smooth?) smooth?)
                  overscan? (if (rl/key-pressed? rl/KEY-O) (not overscan?) overscan?)
                  dx (if overscan? (- RATIO) 80.0)
                  dy (if overscan? (- RATIO) 45.0)
                  dw (if overscan? (+ W (* RATIO 2.0)) 640.0)
                  dh (if overscan? (+ H (* RATIO 2.0)) 360.0)]
              ;; The world, at virtual resolution.
              (rl/with-render-texture
                target
                (fn []
                  (rl/clear-background rl/RAYWHITE)
                  (rl/with-camera-2d {:offset-x 0.0
                                      :offset-y 0.0
                                      :target-x wx
                                      :target-y wy}
                    (fn []
                      (rl/rect-pro! {:x 70.0
                                     :y 35.0
                                     :width 20.0
                                     :height 20.0
                                     :rotation rot
                                     :color rl/BLACK})
                      (rl/rect-pro! {:x 90.0
                                     :y 55.0
                                     :width 30.0
                                     :height 10.0
                                     :rotation (- rot)
                                     :color rl/RED})
                      (rl/rect-pro! {:x 80.0
                                     :y 65.0
                                     :width 15.0
                                     :height 25.0
                                     :rotation (+ rot 45.0)
                                     :color rl/BLUE})))))
              ;; Blit upscaled, optionally through the sub-pixel screen camera.
              (rl/begin-drawing)
              (rl/clear-background rl/LIGHTGRAY)
              (let [blit! (fn []
                            (rl/texture! (:texture target)
                                         {:x dx
                                          :y dy
                                          :width dw
                                          :height dh
                                          :v0 1.0
                                          :v1 0.0
                                          :tint rl/WHITE}))]
                (if smooth?
                  (rl/with-camera-2d {:offset-x 0.0
                                      :offset-y 0.0
                                      :target-x sx
                                      :target-y sy}
                    blit!)
                  (blit!)))
              (rl/text! (str "Screen resolution: " W "x" H) {:x 10
                                                             :y 10
                                                             :size 20
                                                             :color rl/DARKBLUE})
              (rl/text! (str "World resolution: " VW "x" VH) {:x 10
                                                              :y 40
                                                              :size 20
                                                              :color rl/DARKGREEN})
              (rl/text! (str "Smooth: " (if smooth? "ON" "OFF") "  Overscan: " (if overscan? "ON" "OFF")
                             "  (S / O toggle)")
                        {:x 10
                         :y (- H 30)
                         :size 20
                         :color rl/RED})
              (rl/fps! {:x (- W 95)
                        :y 10})
              (app/maybe-screenshot! frame 30)
              (rl/end-drawing)
              (recur (inc frame) rot smooth? overscan?))))
        (rl/unload-render-texture! target))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
