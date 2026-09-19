(ns net.b12n.raylib-jlt.raw-data
  "raylib [textures] example - raw data (`jolt -M:raw-data`).

  Port of raylib's examples/textures/textures_raw_data.c. The C does two
  things: it loads fudesumi.raw, a headerless dump of RGBA bytes, and it
  MemAllocs a Color array, fills it with a checkerboard by hand and hands
  that to LoadTextureFromImage. Only the second half survives here, because
  no files ship in this suite, but the second half was always the
  interesting one. Both are the same idea anyway: a texture is a flat block
  of bytes, and nothing says those bytes have to come from a decoder.

  Both panels go up through rl/texture-from-fn, which allocates a w*h*4
  staging buffer, walks it writing one packed RGBA8 word per pixel, and
  frees it once rlLoadTexture has copied it to the GPU. Left is upstream's
  checkerboard. Right computes all four channels separately from x and y, so
  the colour at a pixel is arithmetic and nothing else, and it is re-uploaded
  every frame with rl/update-texture-from-fn! to make that visible.

  Layout is row-major with four bytes to a pixel, so pixel (x,y) begins at
  byte 4*(x + y*w). Get that stride wrong and you get a diagonal smear, which
  is the classic tell."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const PANEL 256)
(def ^:const XA 60)
(def ^:const XB 470)
(def ^:const Y0 120)

;; upstream's checks are 32 pixels across
(def ^:const CHECK 32)

;; the animated panel is computed at this size and drawn scaled up, so the
;; per-frame cost stays at 128*128 packed writes rather than 256*256
(def ^:const LIVE 128)

(defn- checker
  "Upstream's checkerboard, as a pure function of the pixel coordinate."
  [x y]
  (if (even? (+ (quot x CHECK) (quot y CHECK)))
    rl/ORANGE
    rl/GOLD))

(defn- channels
  "Each channel its own function of position and time. Nothing here samples an
  image, so whatever appears on screen was computed one pixel at a time."
  [t x y]
  (let [fx (/ (double x) LIVE)
        fy (/ (double y) LIVE)
        r (int (* 255 (Math/abs (Math/sin (+ (* fx 6.0) t)))))
        g (int (* 255 (Math/abs (Math/sin (+ (* fy 6.0) (* t 0.7))))))
        b (int (* 255 (Math/abs (Math/cos (+ (* (+ fx fy) 4.0) (* t 1.3))))))]
    (rl/rgba r g b 255)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - raw data"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        checked (rl/texture-from-fn PANEL PANEL checker)
        live (rl/texture-from-fn LIVE LIVE (fn [x y] (channels 0.0 x y)))]
    (loop [frame 0]
      (if-not (app/keep-running? deadline)
        (do (rl/unload-texture! checked)
            (rl/unload-texture! live))
        (let [t (* frame 0.03)]
          (rl/update-texture-from-fn! live LIVE LIVE (fn [x y] (channels t x y)))
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "raylib [textures] example - raw data"
                    {:x 40
                     :y 20
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! "textures built from a byte buffer this program filled in itself"
                    {:x 40
                     :y 48
                     :size 13
                     :color rl/GRAY})

          (rl/texture! checked {:x XA
                                :y Y0
                                :width PANEL
                                :height PANEL})
          (rl/rect-lines! {:x XA
                           :y Y0
                           :width PANEL
                           :height PANEL
                           :color rl/DARKGRAY})
          (rl/text! "checkerboard, uploaded once"
                    {:x XA
                     :y (- Y0 22)
                     :size 13
                     :color rl/DARKGRAY})

          (rl/texture! live {:x XB
                             :y Y0
                             :width PANEL
                             :height PANEL})
          (rl/rect-lines! {:x XB
                           :y Y0
                           :width PANEL
                           :height PANEL
                           :color rl/DARKGRAY})
          (rl/text! "RGB computed per pixel, re-uploaded every frame"
                    {:x XB
                     :y (- Y0 22)
                     :size 13
                     :color rl/DARKGRAY})

          (rl/text! "RGBA8, row-major: pixel (x,y) starts at byte 4*(x + y*w)"
                    {:x 40
                     :y (- H 40)
                     :size 13
                     :color rl/GRAY})
          ;; GetFPS is not bound, and a caption is a poor reason to bind it.
          ;; The frame time says the same thing and is already here
          (let [ft (rl/get-frame-time)]
            (rl/text! (str "fps " (if (pos? ft) (int (/ 1.0 ft)) 0))
                      {:x (- W 95)
                       :y (- H 40)
                       :size 13
                       :color rl/GRAY}))
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame)))))))
