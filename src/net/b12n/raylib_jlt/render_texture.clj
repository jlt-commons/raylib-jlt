(ns net.b12n.raylib-jlt.render-texture
  "raylib [textures] example - render texture (`jolt -M:render-texture`).

  A bouncing-ball scene is drawn once into an off-screen framebuffer, then that
  framebuffer's texture is drawn back to the window four times at different
  scales, tints and rotogravure-ish offsets. Drawing the scene is paid for once no
  matter how many copies appear.

  raylib spells this BeginTextureMode / EndTextureMode over a RenderTexture2D
  struct; rl/with-render-texture is the same thing in scalar rlgl calls (see
  raylib.clj). One wrinkle carries over from OpenGL: a framebuffer texture is
  stored bottom-up, so it is drawn back with :v0 1.0 :v1 0.0 to flip it."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const RT-W 320)
(def ^:const RT-H 240)

(defn- draw-scene
  "The contents of the off-screen target. Coordinates are RT-W x RT-H, not screen
  size: with-render-texture reprojects to the target's own size."
  [t]
  (rl/clear-background (rl/rgba 20 24 34 255))
  (dotimes [i 6]
    (let [ph (* i 0.9)
          x (+ (/ RT-W 2.0) (* 110 (Math/sin (+ t ph))))
          y (+ (/ RT-H 2.0) (* 70 (Math/cos (* 1.4 (+ t ph)))))]
      (rl/circle! {:x (int x)
                   :y (int y)
                   :radius 18
                   :color (rl/rgba (mod (* i 60) 256) 200 (- 255 (mod (* i 60) 256)) 255)})))
  (rl/text! "off-screen" {:x 10
                          :y 10
                          :size 20
                          :color rl/RAYWHITE})
  (rl/rect-lines! {:x 0
                   :y 0
                   :width RT-W
                   :height RT-H
                   :color rl/GRAY}))

(def ^:private copies
  ;; [x y scale tint-label tint]
  [[40 90 1.0 "1:1"] [400 90 0.6 "60%"] [400 260 0.35 "35%"] [610 260 0.5 "50% tinted"]])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - render texture"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        rt (rl/render-texture RT-W RT-H)]
    (if-not rt
      (binding [*out* *err*]
        (println "render-texture: the driver reported an incomplete framebuffer"))
      (do
        (loop [frame 0]
          (when (rl/keep-running? deadline)
            (let [t (rl/get-time)]
              (rl/begin-drawing)
              ;; The scene is rendered once, before anything touches the window.
              (rl/with-render-texture rt (fn [] (draw-scene t)))
              (rl/clear-background rl/RAYWHITE)
              (rl/text! "one scene rendered once, drawn back four times"
                        {:x 40
                         :y 40
                         :size 20
                         :color rl/DARKGRAY})
              (doseq [[x y s label] copies]
                (let [w (int (* RT-W s))
                      h (int (* RT-H s))
                      tint (if (= label "50% tinted") (rl/rgba 255 180 180 255) rl/WHITE)]
                  ;; v0 1.0 -> v1 0.0 flips the bottom-up framebuffer texture.
                  (rl/texture! (:texture rt)
                               {:x x
                                :y y
                                :width w
                                :height h
                                :v0 1.0
                                :v1 0.0
                                :tint tint})
                  (rl/text! label {:x x
                                   :y (+ y h 4)
                                   :size 14
                                   :color rl/GRAY})))
              (rl/maybe-screenshot! frame 5)
              (rl/end-drawing))
            (recur (inc frame))))
        (rl/unload-render-texture! rt))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
