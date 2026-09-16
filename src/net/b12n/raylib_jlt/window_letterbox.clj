(ns net.b12n.raylib-jlt.window-letterbox
  "raylib [core] example - window letterbox (`jolt -M:window-letterbox`).

  The picture is drawn at a fixed 480x360 whatever size the window is. Press R to
  make the window resizable, then drag a corner: the picture scales to the largest
  multiple that fits, centred, with black bars on whichever axis has room left
  over. The readout along the bottom reports the window size, the scale and the
  bar size.

  This is what a render texture is for outside of effects: it decouples the
  resolution you design against from the one the viewer happens to have. The mouse
  is mapped back through the same transform, so the crosshair lands on the right
  virtual pixel at any window size.

  The window starts fixed and R turns resizability on at runtime rather than
  SetConfigFlags turning it on before InitWindow. Same end state, but a window
  that is resizable from creation reports a frame the demo recorder cannot keep
  up with, and every capture of this example failed on a mid-recording size
  change."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const VW 480)               ; the virtual resolution everything is drawn at
(def ^:const VH 360)

(defn- fit
  "Largest integer-ish scale that fits VW x VH inside w x h, plus the centring
  offset. Returns [scale off-x off-y]."
  [w h]
  (let [s (min (/ (double w) VW) (/ (double h) VH))]
    [s (/ (- w (* VW s)) 2.0) (/ (- h (* VH s)) 2.0)]))

(defn- draw-virtual
  "The 480x360 picture: a checker border, a title, and a crosshair at the mouse."
  [t mx my]
  (rl/clear-background (rl/rgba 24 28 38 255))
  (dotimes [i 12]
    (let [c (if (even? i) (rl/rgba 0 121 241 255) (rl/rgba 102 191 255 255))]
      (rl/rect! {:x (* i 40)
                 :y 0
                 :width 40
                 :height 12
                 :color c})
      (rl/rect! {:x (* i 40)
                 :y (- VH 12)
                 :width 40
                 :height 12
                 :color c})))
  (rl/circle! {:x (int (+ (/ VW 2.0) (* 90 (Math/sin t))))
               :y (int (/ VH 2.0))
               :radius 26
               :color rl/GOLD})
  (rl/text! (str VW "x" VH " virtual resolution") {:x 20
                                                   :y 40
                                                   :size 18
                                                   :color rl/RAYWHITE})
  (rl/text! "resize the window - this never changes size"
            {:x 20
             :y 68
             :size 13
             :color rl/LIGHTGRAY})
  ;; Only draw the crosshair when the pointer is actually over the picture.
  (when (and (<= 0 mx VW) (<= 0 my VH))
    (rl/line! {:x1 (- mx 10)
               :y1 my
               :x2 (+ mx 10)
               :y2 my
               :color rl/RED})
    (rl/line! {:x1 mx
               :y1 (- my 10)
               :x2 mx
               :y2 (+ my 10)
               :color rl/RED})))

(defn -main
  [& _]
  ;; SetConfigFlags only has an effect before InitWindow.
  ;; The window is the suite's usual 16:9 and the virtual screen is 4:3, so the
  ;; bars are there from the first frame rather than only after someone resizes.
  (rl/window! {:width 800
               :height 450
               :title "raylib [core] example - window letterbox"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        rt (rl/render-texture VW VH)]
    (if-not rt
      (binding [*out* *err*]
        (println "window-letterbox: the driver reported an incomplete framebuffer"))
      (do
        (loop [frame 0]
          (when (rl/keep-running? deadline)
            ;; Resizability is switched on at runtime rather than through
            ;; SetConfigFlags before InitWindow. Same end state, but a window that
            ;; is resizable from creation reports a frame the demo recorder cannot
            ;; keep up with, and every capture of this example failed on a
            ;; mid-recording size change.
            (when (rl/key-pressed? rl/KEY-R)
              (if (rl/window-state? rl/FLAG-WINDOW-RESIZABLE)
                (rl/clear-window-state rl/FLAG-WINDOW-RESIZABLE)
                (rl/set-window-state rl/FLAG-WINDOW-RESIZABLE)))
            (let [w (rl/get-screen-width)
                  h (rl/get-screen-height)
                  [s ox oy] (fit w h)
                  ;; The inverse of the letterbox transform, so the pointer lands
                  ;; on the virtual pixel that is under it on screen.
                  mx (int (/ (- (rl/get-mouse-x) ox) s))
                  my (int (/ (- (rl/get-mouse-y) oy) s))]
              (rl/begin-drawing)
              (rl/with-render-texture rt (fn [] (draw-virtual (rl/get-time) mx my)))
              (rl/clear-background rl/BLACK)
              (rl/texture! (:texture rt)
                           {:x (int ox)
                            :y (int oy)
                            :width (int (* VW s))
                            :height (int (* VH s))
                            :v0 1.0
                            :v1 0.0})
              ;; The readout is about the window, not the picture, so it sits on
              ;; the window's own edge - backed by a strip because with one axis
              ;; letterboxed the other has no bar to write into.
              (rl/rect! {:x 0
                         :y (- h 24)
                         :width w
                         :height 24
                         :color (rl/rgba 0 0 0 190)})
              (rl/text! (str "window " w "x" h "   scale " (format "%.2f" s)
                             "   bars " (int ox) "x" (int oy))
                        {:x 10
                         :y (- h 19)
                         :size 14
                         :color rl/LIGHTGRAY})
              (rl/text! (if (rl/window-state? rl/FLAG-WINDOW-RESIZABLE)
                          "resizable - drag a corner"
                          "R makes the window resizable")
                        {:x (- w 250)
                         :y (- h 19)
                         :size 14
                         :color (if (rl/window-state? rl/FLAG-WINDOW-RESIZABLE) rl/GREEN rl/GRAY)})
              (rl/maybe-screenshot! frame 5)
              (rl/end-drawing))
            (recur (inc frame))))
        (rl/unload-render-texture! rt))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
