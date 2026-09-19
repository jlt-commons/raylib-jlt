(ns net.b12n.raylib-jlt.mouse-painting
  "raylib [textures] example - mouse painting.

  A paint program on a render texture: LEFT paints with the selected color,
  RIGHT erases (temporarily switching to the background color), the mouse
  wheel sizes the brush, the top strip picks one of 23 colors, C clears, S
  or the SAVE button takes a whole-window screenshot. No new bindings.
  Ported from raylib's examples/textures/textures_mouse_painting.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const COLOR-COUNT 23)

;; The original example's palette, as r/g/b triples (raylib's named colors).
(def ^:private PALETTE
  [[245 245 245] [253 249 0] [255 203 0] [255 161 0] [255 109 194]
   [230 41 55] [190 33 55] [0 228 48] [0 158 47] [0 117 44]
   [102 191 255] [0 121 241] [0 82 172] [200 122 255] [135 60 190]
   [112 31 126] [211 176 131] [127 106 79] [76 63 47]
   [200 200 200] [130 130 130] [80 80 80] [0 0 0]])

(defn- palette-color
  [i]
  (let [[r g b] (nth PALETTE i)]
    (rl/rgba r g b 255)))

(defn- hovered-color
  "Palette swatch i sits at x = 10 + 32*i, y 10..40, 30x30. -1 for none."
  [mx my]
  (if (<= 10 my 40)
    (loop [i 0]
      (if (>= i COLOR-COUNT)
        -1
        (let [x (+ 10 (* 32 i))]
          (if (<= x mx (+ x 30)) i (recur (inc i))))))
    -1))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - mouse painting"})
  (rl/set-target-fps 120)
  (let [deadline (app/auto-quit-deadline)
        canvas (rl/render-texture W H)]
    (if-not canvas
      (binding [*out* *err*]
        (println "mouse-painting: the driver reported an incomplete framebuffer"))
      (do
        (rl/with-render-texture canvas (fn [] (rl/clear-background (palette-color 0))))
        (loop [frame 0 sel 0 prev 0 brush 20.0 rb-was? false save-show? false save-cnt 0]
          (when (app/keep-running? deadline)
            (let [mx (rl/get-mouse-x)
                  my (rl/get-mouse-y)
                  hover (hovered-color mx my)
                  left-clicked? (and (>= hover 0) (rl/mouse-pressed? rl/MOUSE-LEFT))
                  sel (if left-clicked? hover sel)
                  prev (if left-clicked? hover prev)
                  brush (max 2.0 (min 50.0 (+ brush (* 5.0 (rl/get-mouse-wheel)))))
                  rb-down? (rl/mouse-down? rl/MOUSE-RIGHT)
                  ;; RIGHT down: remember the color and erase with the background.
                  prev (if (and rb-down? (not rb-was?)) sel prev)
                  sel (if (and rb-down? (not rb-was?)) 0 sel)
                  sel (if (and (not rb-down?) rb-was?) prev sel)
                  btn-hover? (and (<= 750 mx 790) (<= 10 my 40))
                  save? (or (and btn-hover? (rl/mouse-released? rl/MOUSE-LEFT))
                            (rl/key-pressed? rl/KEY-S))
                  save-show? (or save? save-show?)
                  save-cnt (if save? 0 save-cnt)
                  save-cnt (if save-show? (inc save-cnt) save-cnt)
                  save-show? (if (> save-cnt 240) false save-show?)
                  save-cnt (if (> save-cnt 240) 0 save-cnt)]
              (when (rl/key-pressed? rl/KEY-C)
                (rl/with-render-texture canvas (fn [] (rl/clear-background (palette-color 0)))))
              ;; Paint (LEFT) or erase (RIGHT) into the canvas below the panel.
              (when (and (or (rl/mouse-down? rl/MOUSE-LEFT) rb-down?) (> my 50))
                (rl/with-render-texture canvas
                  (fn [] (rl/circle! {:x mx
                                      :y my
                                      :radius brush
                                      :color (palette-color sel)}))))
              (when save?
                (rl/take-screenshot "my_painting.png"))
              (rl/begin-drawing)
              (rl/clear-background rl/RAYWHITE)
              ;; The canvas, y-flipped (render textures are bottom-up in OpenGL).
              (rl/texture! (:texture canvas)
                           {:x 0
                            :y 0
                            :width W
                            :height H
                            :v0 1.0
                            :v1 0.0
                            :tint rl/WHITE})
              ;; Brush preview.
              (when (> my 50)
                (if rb-down?
                  (rl/circle-lines! {:x mx
                                     :y my
                                     :radius brush
                                     :color rl/GRAY})
                  (rl/circle! {:x mx
                               :y my
                               :radius brush
                               :color (palette-color sel)})))
              ;; Top panel with the palette.
              (rl/rect! {:x 0
                         :y 0
                         :width W
                         :height 50
                         :color rl/RAYWHITE})
              (rl/line! {:x1 0
                         :y1 50
                         :x2 W
                         :y2 50
                         :color rl/LIGHTGRAY})
              (dotimes [i COLOR-COUNT]
                (rl/rect! {:x (+ 10 (* 32 i))
                           :y 10
                           :width 30
                           :height 30
                           :color (palette-color i)}))
              (rl/rect-lines! {:x 10
                               :y 10
                               :width 30
                               :height 30
                               :color rl/LIGHTGRAY})
              (when (>= hover 0)
                (rl/rect! {:x (+ 10 (* 32 hover))
                           :y 10
                           :width 30
                           :height 30
                           :color (rl/rgba 255 255 255 153)}))
              (rl/rect-lines! {:x (- (+ 10 (* 32 sel)) 2)
                               :y 8
                               :width 34
                               :height 34
                               :color rl/BLACK})
              ;; Save button.
              (rl/rect-lines! {:x 750
                               :y 10
                               :width 40
                               :height 30
                               :color (if btn-hover? rl/RED rl/BLACK)})
              (rl/text! "SAVE!" {:x 755
                                 :y 20
                                 :size 10
                                 :color (if btn-hover? rl/RED rl/BLACK)})
              (when save-show?
                (rl/rect! {:x 0
                           :y 0
                           :width W
                           :height H
                           :color (rl/rgba 245 245 245 204)})
                (rl/rect! {:x 0
                           :y 150
                           :width W
                           :height 80
                           :color rl/BLACK})
                (rl/text! "IMAGE SAVED! (whole-window screenshot)"
                          {:x 150
                           :y 180
                           :size 20
                           :color rl/RAYWHITE}))
              (app/maybe-screenshot! frame 30)
              (rl/end-drawing)
              (recur (inc frame) sel prev brush rb-down? save-show? save-cnt))))
        (rl/unload-render-texture! canvas))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
