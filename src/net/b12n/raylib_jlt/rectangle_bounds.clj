(ns net.b12n.raylib-jlt.rectangle-bounds
  "raylib [text] example - rectangle bounds.

  Text laid out inside a container rectangle you can resize by dragging the
  little box at its bottom-right corner. SPACE toggles word-wrap (on = break
  between words, off = break mid-word at the edge); text that spills past
  the box height is clipped one line at a time rather than via a scissor
  rect, the same check raylib's own DrawTextBoxed uses. ESC quits.

  No new FFI: word/char wrap only needs MeasureText's int width (already
  bound as text-width), so there's no MeasureTextEx / per-glyph Font
  indexing to route around here. 'Faded' border/handle colors while
  hovering or resizing are a hand-picked lighter maroon rather than a
  Fade call, since alpha blending onto a fixed RAYWHITE background is one
  fixed color either way.
  Ported from raylib's examples/text/text_rectangle_bounds.c."
  (:require
   [clojure.string :as str]
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const CX 25.0)
(def ^:const CY 25.0)
(def ^:const MIN-W 60.0)
(def ^:const MIN-H 60.0)
(def ^:const MAX-W (- W 50.0))
(def ^:const MAX-H (- H 160.0))
(def ^:const FONT-SIZE 20)
(def ^:const LINE-H (int (+ FONT-SIZE (/ FONT-SIZE 2))))
(def ^:const MAROON-FADED (rl/rgba 223 160 169 255))

(def ^:const TEXT
  (str "Text cannot escape  this container  ...word wrap also works when "
       "active so here's a long text for testing.\n\n"
       "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do "
       "eiusmod tempor incididunt ut labore et dolore magna aliqua. Nec "
       "ullamcorper sit amet risus nullam eget felis eget."))

(defn- measure
  [s]
  (rl/text-width s {:size FONT-SIZE}))

(defn- wrap-chars
  "Character-wrap of `line` to `width` (used when word-wrap is off, and by
  wrap-words to break a single word wider than the container)."
  [line width]
  (let [n (count line)]
    (if (zero? n)
      [""]
      (loop [i 0 cur "" acc []]
        (if (< i n)
          (let [c (subs line i (inc i))
                trial (str cur c)]
            (if (> (measure trial) width)
              (recur (inc i) (if (= c " ") "" c) (conj acc cur))
              (recur (inc i) trial acc)))
          (if (= cur "") acc (conj acc cur)))))))

(defn- wrap-words
  "Greedy word-wrap of `line` to `width`; a word wider than `width` falls
  back to a mid-word character break, so text cannot escape the container
  horizontally either way."
  [line width]
  (if (= line "")
    [""]
    (let [words (str/split line #" ")
          nw (count words)]
      (loop [wi 0 cur "" acc []]
        (if (< wi nw)
          (let [w (nth words wi)
                trial (if (= cur "") w (str cur " " w))]
            (if (> (measure trial) width)
              (if (= cur "")
                (let [pieces (wrap-chars w width)]
                  (recur (inc wi) (peek pieces) (into acc (pop pieces))))
                (recur wi "" (conj acc cur)))
              (recur (inc wi) trial acc)))
          (if (= cur "")
            (if (= acc []) [""] acc)
            (conj acc cur)))))))

(defn- build-display
  "Word/char-wrap the whole text to `width`, flattened to display lines."
  [wrap? width]
  (let [lines (str/split-lines TEXT)]
    (loop [li 0 acc []]
      (if (< li (count lines))
        (recur (inc li)
               (into acc (if wrap?
                           (wrap-words (nth lines li) width)
                           (wrap-chars (nth lines li) width))))
        acc))))

(defn- point-in-rect?
  [mx my rx ry rw rh]
  (and (<= rx mx (+ rx rw)) (<= ry my (+ ry rh))))

(defn- clampf
  [v lo hi]
  (cond (< v lo) lo (> v hi) hi :else v))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [text] example - rectangle bounds"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 cw MAX-W ch 200.0 resizing? false wrap? true last-mx 0.0 last-my 0.0]
      (when (rl/keep-running? deadline)
        (let [mx (double (rl/get-mouse-x))
              my (double (rl/get-mouse-y))
              rx (- (+ CX cw) 17.0)
              ry (- (+ CY ch) 17.0)
              hover? (point-in-rect? mx my CX CY cw ch)
              over-resizer? (point-in-rect? mx my rx ry 14.0 14.0)
              wrap? (if (rl/key-pressed? rl/KEY-SPACE) (not wrap?) wrap?)
              dw (if resizing? (- mx last-mx) 0.0)
              dh (if resizing? (- my last-my) 0.0)
              cw (clampf (+ cw dw) MIN-W MAX-W)
              ch (clampf (+ ch dh) MIN-H MAX-H)
              resizing? (cond
                          (and resizing? (rl/mouse-released? rl/MOUSE-LEFT)) false
                          resizing? true
                          (and (rl/mouse-down? rl/MOUSE-LEFT) over-resizer?) true
                          :else false)
              faded? (or hover? resizing?)
              col (if faded? MAROON-FADED rl/MAROON)
              display (build-display wrap? (- cw 8.0))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect-lines! {:x (int CX)
                           :y (int CY)
                           :width (int cw)
                           :height (int ch)
                           :color col})
          (loop [i 0]
            (when (and (< i (count display)) (<= (+ 24.0 (* i LINE-H)) ch))
              (rl/text! (nth display i)
                        {:x (int (+ CX 4.0))
                         :y (int (+ CY 4.0 (* i LINE-H)))
                         :size FONT-SIZE
                         :color rl/GRAY})
              (recur (inc i))))
          (rl/rect! {:x (int rx)
                     :y (int ry)
                     :width 14
                     :height 14
                     :color col})
          (rl/rect! {:x 0
                     :y (- H 54)
                     :width W
                     :height 54
                     :color rl/GRAY})
          (rl/text! "Word Wrap: " {:x 313
                                   :y (- H 115)
                                   :size 20
                                   :color rl/BLACK})
          (rl/text! (if wrap? "ON" "OFF")
                    {:x 447
                     :y (- H 115)
                     :size 20
                     :color (if wrap? rl/RED rl/BLACK)})
          (rl/text! "Press [SPACE] to toggle word wrap" {:x 218
                                                         :y (- H 86)
                                                         :size 20
                                                         :color rl/GRAY})
          (rl/text! "Click, hold & drag the corner box to resize the container"
                    {:x 130
                     :y (- H 38)
                     :size 20
                     :color rl/RAYWHITE})
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) cw ch resizing? wrap? mx my)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
