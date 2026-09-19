(ns net.b12n.raylib-jlt.npatch-drawing
  "raylib [textures] example - npatch drawing (`jolt -M:npatch-drawing`).

  Port of raylib's examples/textures/textures_npatch_drawing.c. Three panels
  stretched by the mouse: a nine-patch that grows in both axes, and two
  three-patches that grow in one. Move the pointer and watch the corners stay
  exactly the size they were drawn at while the edges and the middle take up the
  slack.

  Zero new FFI, and hand-rolling it is the point. The C calls
  `DrawTextureNPatch` with an `NPatchInfo` struct; `npatch!` here is that call
  written out, nine `texture!` quads whose source rectangles carve the image into
  a 3x3 and whose destination rectangles put the corners back at their original
  size. Once it is spelled out, the two three-patch modes stop being separate
  modes at all: a horizontal one is the same routine with no top or bottom
  border, a vertical one with no left or right.

  The C loads `resources/ninepatch_button.png`. This suite ships no image files,
  so the source is drawn with `texture-from-fn`, with a deliberately busy border
  and a plain middle, which is what makes a stretched patch legible: if the
  corners smeared you would see it immediately."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SRC 64)
(def ^:const BORDER 16)

(defn- patch-pixel
  "A panel with a bright bevelled border, corner studs and a quiet middle. The
  studs are what give a smeared corner away."
  [x y]
  (let [edge (min x y (- SRC 1 x) (- SRC 1 y))
        corner? (and (< x BORDER) (< y BORDER))
        corner2? (and (>= x (- SRC BORDER)) (< y BORDER))
        corner3? (and (< x BORDER) (>= y (- SRC BORDER)))
        corner4? (and (>= x (- SRC BORDER)) (>= y (- SRC BORDER)))
        stud? (and (or corner? corner2? corner3? corner4?)
                   (< 4 edge 9))]
    (cond
      stud? (rl/rgba 255 220 90 255)
      (< edge 2) (rl/rgba 20 28 48 255)
      (< edge 6) (rl/rgba 90 150 230 255)
      (< edge 10) (rl/rgba 45 80 150 255)
      :else (rl/rgba 235 240 250 255))))

(defn- npatch!
  "Draw `tex` stretched into a destination rectangle with fixed-size borders.
  `left`/`right`/`top`/`bottom` are source pixels that must not scale; a zero on
  both of one axis collapses that axis to a single stretched band, which is what
  a three-patch is."
  [tex & {:keys [x y width height left right top bottom]
          :or {x 0
               y 0
               width 64
               height 64
               left BORDER
               right BORDER
               top BORDER
               bottom BORDER}}]
  (let [;; destination spans, with the middle taking whatever is left over
        xs [x (+ x left) (+ x (- width right))]
        ws [left (max 0 (- width left right)) right]
        ys [y (+ y top) (+ y (- height bottom))]
        hs [top (max 0 (- height top bottom)) bottom]
        ;; the same three-way split in source texture coordinates
        us [0.0 (/ (double left) SRC) (/ (double (- SRC right)) SRC) 1.0]
        vs [0.0 (/ (double top) SRC) (/ (double (- SRC bottom)) SRC) 1.0]]
    (dotimes [row 3]
      (dotimes [col 3]
        (let [w (nth ws col)
              h (nth hs row)]
          ;; A zero-width or zero-height cell is a border this patch does not
          ;; have, so it is skipped rather than drawn empty.
          (when (and (pos? w) (pos? h))
            (rl/texture! tex {:x (nth xs col)
                              :y (nth ys row)
                              :width w
                              :height h
                              :u0 (nth us col)
                              :u1 (nth us (inc col))
                              :v0 (nth vs row)
                              :v1 (nth vs (inc row))})))))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - npatch drawing"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        tex (rl/texture-from-fn SRC SRC patch-pixel)]
    ;; CLAMP, not the REPEAT texture-from-fn leaves behind: a stretched edge
    ;; cell samples right up to its border and REPEAT would wrap the far side
    ;; of the image into it.
    (rl/texture-wrap! tex rl/RL-TEXTURE-WRAP-CLAMP)
    (rl/texture-filter! tex rl/RL-TEXTURE-FILTER-NEAREST)
    (loop [frame 0]
      (if-not (app/keep-running? deadline)
        (rl/unload-texture! tex)
        (let [;; Until the pointer moves the panels breathe on their own.
              mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              idle? (and (< mx 2) (< my 2))
              t (* frame 0.02)
              px (if idle? (+ 420 (* 180 (Math/sin t))) mx)
              py (if idle? (+ 260 (* 120 (Math/sin (* 1.3 t)))) my)
              nine-w (-> (- px 380) (max 40.0) (min 360.0))
              nine-h (-> (- py 150) (max 40.0) (min 250.0))
              horiz-w (-> (- px 60) (max 40.0) (min 280.0))
              vert-h (-> (- py 150) (max 40.0) (min 250.0))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 245 246 250 255))
          (npatch! tex {:x 380
                        :y 150
                        :width nine-w
                        :height nine-h})
          (npatch! tex {:x 60
                        :y 100
                        :width horiz-w
                        :height 48
                        :top 0
                        :bottom 0})
          (npatch! tex {:x 60
                        :y 150
                        :width 48
                        :height vert-h
                        :left 0
                        :right 0})
          (rl/text! "nine-patch: corners fixed, edges and middle stretch"
                    {:x 20
                     :y 20
                     :size 20
                     :color rl/DARKGRAY})
          (rl/text! (if idle?
                      "breathing on its own - move the mouse to size them"
                      "move the mouse to size them")
                    {:x 20
                     :y 46
                     :size 10
                     :color rl/GRAY})
          (rl/text! "3-patch H" {:x 60
                                 :y 82
                                 :size 10
                                 :color rl/GRAY})
          (rl/text! "3-patch V" {:x 118
                                 :y 156
                                 :size 10
                                 :color rl/GRAY})
          (rl/text! "9-patch" {:x 380
                               :y 132
                               :size 10
                               :color rl/GRAY})
          (rl/texture! tex {:x (- W SRC 20)
                            :y (- H SRC 20)
                            :width SRC
                            :height SRC})
          (rl/text! "source" {:x (- W SRC 20)
                              :y (- H 18)
                              :size 10
                              :color rl/GRAY})
          (app/maybe-screenshot! frame 60)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
