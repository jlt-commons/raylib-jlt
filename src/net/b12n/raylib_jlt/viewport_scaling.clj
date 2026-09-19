(ns net.b12n.raylib-jlt.viewport-scaling
  "raylib [core] example - viewport scaling.

  A game scene rendered at a fixed 'game resolution' into a render texture,
  scaled into a resizable window under six viewport policies (keep aspect /
  height / width, each with an integer pixel-snap variant). Two < > button
  pairs cycle the game resolution (64x64 / 256x240 / 320x180 / 4K) and the
  policy; a LIME circle tracks the mouse mapped into game space. Resize the
  window to see each policy react. No new bindings, and no atom-boxing
  workaround either: unlike the jank port, a Clojure loop/recur can carry
  the render-texture map directly as accumulator state, recreated (unload +
  render-texture) whenever the window or the chosen mode changes.
  Ported from raylib's examples/core/core_viewport_scaling.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:private RESOLUTIONS [[64 64] [256 240] [320 180] [3840 2160]])
(def ^:private TYPE-NAMES ["KEEP_ASPECT_INTEGER" "KEEP_HEIGHT_INTEGER" "KEEP_WIDTH_INTEGER"
                           "KEEP_ASPECT" "KEEP_HEIGHT" "KEEP_WIDTH"])

(defn- fmt2
  [v]
  (format "%.2f" (double v)))

(defn- keep-aspect-rects
  "KEEP_ASPECT[_INTEGER]: letterbox both axes; integer? snaps the scale
  ratio to a whole number (pixel-perfect)."
  [integer? sw sh gw gh]
  (let [rr (if integer?
             (double (min (quot sw gw) (quot sh gh)))
             (min (/ (double sw) gw) (/ (double sh) gh)))]
    {:sx 0.0
     :sy (double gh)
     :sw (double gw)
     :sh (- (double gh))
     :dx (double (int (* (- sw (* gw rr)) 0.5)))
     :dy (double (int (* (- sh (* gh rr)) 0.5)))
     :dw (double (int (* gw rr)))
     :dh (double (int (* gh rr)))}))

(defn- keep-height-rects
  "KEEP_HEIGHT[_INTEGER]: fill the window height, widen the game view.
  `_gw` is unused (matches the ported original) -- kept so every keep-*-rects
  fn shares one 4-arg (sw sh gw gh) shape for compute-rects's dispatch table."
  [sw sh _gw gh]
  (let [rr (/ (double sh) gh)
        srcw (double (int (/ (double sw) rr)))]
    {:sx 0.0
     :sy 0.0
     :sw srcw
     :sh (- (double gh))
     :dx (double (int (* (- sw (* srcw rr)) 0.5)))
     :dy (double (int (* (- sh (* gh rr)) 0.5)))
     :dw (double (int (* srcw rr)))
     :dh (double (int (* gh rr)))}))

(defn- keep-width-rects
  "KEEP_WIDTH[_INTEGER]: fill the window width, deepen the game view.
  `_gh` is unused (matches the ported original) -- kept so every keep-*-rects
  fn shares one 4-arg (sw sh gw gh) shape for compute-rects's dispatch table."
  [sw sh gw _gh]
  (let [rr (/ (double sw) gw)
        srch (double (int (/ (double sh) rr)))]
    {:sx 0.0
     :sy 0.0
     :sw (double gw)
     :sh (- srch)
     :dx (double (int (* (- sw (* gw rr)) 0.5)))
     :dy (double (int (* (- sh (* srch rr)) 0.5)))
     :dw (double (int (* gw rr)))
     :dh (double (int (* srch rr)))}))

(defn- compute-rects
  [vtype sw sh gw gh]
  (case vtype
    0 (keep-aspect-rects true sw sh gw gh)
    1 (keep-height-rects sw sh gw gh)
    2 (keep-width-rects sw sh gw gh)
    3 (keep-aspect-rects false sw sh gw gh)
    4 (keep-height-rects sw sh gw gh)
    (keep-width-rects sw sh gw gh)))

(defn- over-button?
  [mx my bx by]
  (and (<= bx mx (+ bx 10.0)) (<= by my (+ by 10.0))))

(defn -main
  [& _]
  (rl/set-config-flags rl/FLAG-WINDOW-RESIZABLE)
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - viewport scaling"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        r0 (compute-rects 0 (rl/get-screen-width) (rl/get-screen-height) 64 64)
        t0 (rl/render-texture (int (:sw r0)) (int (- (:sh r0))))]
    (loop [frame 0 ri 0 vtype 0 rects r0 target t0]
      (when (app/keep-running? deadline)
        (let [mx (double (rl/get-mouse-x))
              my (double (rl/get-mouse-y))
              resized? (rl/window-resized?)
              pressed? (rl/mouse-pressed? rl/MOUSE-LEFT)
              dec-res? (and pressed? (over-button? mx my 200.0 30.0))
              inc-res? (and pressed? (over-button? mx my 215.0 30.0))
              dec-type? (and pressed? (over-button? mx my 200.0 45.0))
              inc-type? (and pressed? (over-button? mx my 215.0 45.0))
              ri (cond dec-res? (mod (+ ri 3) 4)
                       inc-res? (mod (+ ri 1) 4)
                       :else ri)
              [gw gh] (nth RESOLUTIONS ri)
              vtype (cond dec-type? (mod (+ vtype 5) 6)
                          inc-type? (mod (+ vtype 1) 6)
                          :else vtype)
              dirty? (or resized? dec-res? inc-res? dec-type? inc-type?)
              rects (if dirty?
                      (compute-rects vtype (rl/get-screen-width) (rl/get-screen-height) gw gh)
                      rects)
              target (if dirty?
                       (do (rl/unload-render-texture! target)
                           (rl/render-texture (int (:sw rects)) (int (- (:sh rects)))))
                       target)
              ratio-x (/ (:sw rects) (:dw rects))
              tmx (* (- mx (:dx rects)) ratio-x)
              tmy (* (- my (:dy rects)) ratio-x)
              scale-x (/ (:dw rects) (:sw rects))
              scale-y (- (/ (:dh rects) (:sh rects)))]
          (rl/with-render-texture
            target
            (fn []
              (rl/clear-background rl/WHITE)
              (rl/circle! {:x (int tmx)
                           :y (int tmy)
                           :radius 20.0
                           :color rl/LIME})))
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          ;; :sh is always negative (see the three keep-*-rects fns) -- the
          ;; render-texture-is-bottom-up flip, same fixed v0/v1 every other
          ;; render-texture blit in this suite uses.
          (rl/texture! (:texture target)
                       {:x (:dx rects)
                        :y (:dy rects)
                        :width (:dw rects)
                        :height (:dh rects)
                        :v0 1.0
                        :v1 0.0
                        :tint rl/WHITE})
          (rl/rect! {:x 5
                     :y 5
                     :width 330
                     :height 105
                     :color (rl/rgba 200 200 200 178)})
          (rl/rect-lines! {:x 5
                           :y 5
                           :width 330
                           :height 105
                           :color rl/BLUE})
          (rl/text! (str "Window Resolution: " (rl/get-screen-width) " x " (rl/get-screen-height))
                    {:x 15
                     :y 15
                     :size 10
                     :color rl/BLACK})
          (rl/text! (str "Game Resolution: " gw " x " gh) {:x 15
                                                           :y 30
                                                           :size 10
                                                           :color rl/BLACK})
          (rl/text! (str "Type: " (nth TYPE-NAMES vtype)) {:x 15
                                                           :y 45
                                                           :size 10
                                                           :color rl/BLACK})
          (if (or (< scale-x 0.001) (< scale-y 0.001))
            (rl/text! "Scale ratio: INVALID" {:x 15
                                              :y 60
                                              :size 10
                                              :color rl/BLACK})
            (rl/text! (str "Scale ratio: " (fmt2 scale-x) " x " (fmt2 scale-y))
                      {:x 15
                       :y 60
                       :size 10
                       :color rl/BLACK}))
          (rl/text! (str "Source size: " (fmt2 (:sw rects)) " x " (fmt2 (- (:sh rects))))
                    {:x 15
                     :y 75
                     :size 10
                     :color rl/BLACK})
          (rl/text! (str "Destination size: " (fmt2 (:dw rects)) " x " (fmt2 (:dh rects)))
                    {:x 15
                     :y 90
                     :size 10
                     :color rl/BLACK})
          (rl/rect! {:x 200
                     :y 30
                     :width 10
                     :height 10
                     :color rl/SKYBLUE})
          (rl/rect! {:x 215
                     :y 30
                     :width 10
                     :height 10
                     :color rl/SKYBLUE})
          (rl/rect! {:x 200
                     :y 45
                     :width 10
                     :height 10
                     :color rl/SKYBLUE})
          (rl/rect! {:x 215
                     :y 45
                     :width 10
                     :height 10
                     :color rl/SKYBLUE})
          (rl/text! "<" {:x 203
                         :y 31
                         :size 10
                         :color rl/BLACK})
          (rl/text! ">" {:x 218
                         :y 31
                         :size 10
                         :color rl/BLACK})
          (rl/text! "<" {:x 203
                         :y 46
                         :size 10
                         :color rl/BLACK})
          (rl/text! ">" {:x 218
                         :y 46
                         :size 10
                         :color rl/BLACK})
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) ri vtype rects target)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
