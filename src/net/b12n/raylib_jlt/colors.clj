(ns net.b12n.raylib-jlt.colors
  "raylib named-colors showcase (`joltc -M:colors`).

  Not a 1:1 port of one raylib example, a 5x5 grid that draws every named color
  from net.b12n.raylib-jlt.raylib as a labelled swatch, exercising the `rgba` Color packing across
  the whole palette."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:private palette
  [["LIGHTGRAY" rl/LIGHTGRAY] ["GRAY" rl/GRAY]     ["DARKGRAY" rl/DARKGRAY]
   ["YELLOW" rl/YELLOW]       ["GOLD" rl/GOLD]     ["ORANGE" rl/ORANGE]
   ["PINK" rl/PINK]           ["RED" rl/RED]       ["MAROON" rl/MAROON]
   ["GREEN" rl/GREEN]         ["LIME" rl/LIME]     ["DARKGREEN" rl/DARKGREEN]
   ["SKYBLUE" rl/SKYBLUE]     ["BLUE" rl/BLUE]     ["DARKBLUE" rl/DARKBLUE]
   ["PURPLE" rl/PURPLE]       ["VIOLET" rl/VIOLET] ["DARKPURPLE" rl/DARKPURPLE]
   ["BEIGE" rl/BEIGE]         ["BROWN" rl/BROWN]   ["DARKBROWN" rl/DARKBROWN]
   ["WHITE" rl/WHITE]         ["BLACK" rl/BLACK]   ["MAGENTA" rl/MAGENTA]
   ["RAYWHITE" rl/RAYWHITE]])

(def ^:private cols 5)
(def ^:private pad 12)
(def ^:private cw 145)   ; swatch width
(def ^:private sh 55)    ; swatch height
(def ^:private ch 71)    ; cell height (swatch + label)
(def ^:private grid-top 50)

(defn- draw-swatch
  [i [label color]]
  (let [col (mod i cols)
        row (quot i cols)
        x   (+ pad (* col (+ cw pad)))
        y   (+ grid-top (* row ch))]
    (rl/rect! {:x x
               :y y
               :width cw
               :height sh
               :color color})
    ;; a light border so the near-white swatches (WHITE, RAYWHITE) stay visible
    (rl/rect-lines! {:x x
                     :y y
                     :width cw
                     :height sh
                     :color rl/LIGHTGRAY})
    (rl/text! label {:x (+ x 4)
                     :y (+ y sh 2)
                     :size 10
                     :color rl/DARKGRAY})))

(defn -main
  [& _]
  (rl/window! {:title "raylib named colors"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        (rl/text! "raylib named colors" {:x 12
                                         :y 15
                                         :size 22
                                         :color rl/DARKGRAY})
        (doseq [i (range (count palette))]
          (draw-swatch i (nth palette i)))
        (rl/maybe-screenshot! frame 5)
        (rl/end-drawing)
        (recur (inc frame)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
