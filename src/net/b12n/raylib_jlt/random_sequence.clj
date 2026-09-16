(ns net.b12n.raylib-jlt.random-sequence
  "raylib [core] example - random sequence (`jolt -M:random-sequence`).

  Port of raylib's examples/core/core_random_sequence.c. A row of coloured bars
  whose heights are a shuffled sequence. SPACE reshuffles, UP and DOWN change how
  many bars there are.

  The example is about LoadRandomSequence, which hands back each value in a range
  exactly once in random order. That is a different thing from GetRandomValue,
  which is bound here and which samples with replacement: calling it n times gives
  duplicates and gaps, so the bars would not be a permutation and reshuffling
  would change the set rather than just the order. Clojure's shuffle is the same
  guarantee, so this uses it and leaves get-random-value to the examples that
  genuinely want independent draws.

  Reshuffling only reorders. Watch a colour move rather than change: the bars
  keep their heights and hues across a SPACE, which is the property the example
  is demonstrating."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MIN-BARS 4)
(def ^:const MAX-BARS 60)

(defn- make-bars
  "n bars, each with a distinct height rank and a hue derived from that rank, in
  shuffled order. Height comes from the rank rather than a fresh random draw, so
  every bar is a different height and the set is stable under reshuffling."
  [n]
  (let [max-h (* 0.75 H)]
    (shuffle
     (for [i (range n)]
       (let [t (/ (double (inc i)) n)]
         {:height (* max-h t)
          :color  (rl/rgba (int (* 255 t))
                           (int (* 255 (- 1.0 (Math/abs (- (* 2.0 t) 1.0)))))
                           (int (* 255 (- 1.0 t)))
                           255)})))))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [core] example - random sequence")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           n 20
           bars (make-bars 20)]
      (when (rl/keep-running? deadline)
        (let [up?   (rl/key-pressed? rl/KEY-UP)
              down? (rl/key-pressed? rl/KEY-DOWN)
              n'    (cond
                      (and up? (< n MAX-BARS))     (inc n)
                      (and down? (> n MIN-BARS))   (dec n)
                      :else                        n)
              bars  (cond
                      (not= n' n)                  (make-bars n')
                      (rl/key-pressed? rl/KEY-SPACE) (shuffle bars)
                      :else                        bars)
              size  (/ (double W) n')]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; Above the bars, where there is empty window. The two captions below
          ;; overlap the bars in the C too, so they are left where it puts them.
          (rl/text! (str n' " bars, each height used exactly once")
                    :x 10 :y 10 :size 20 :color rl/DARKGRAY)
          (dotimes [i (count bars)]
            (let [{:keys [height color]} (nth bars i)]
              (rl/rect! :x (int (* i size)) :y (int (- H height))
                        :width (int (- size 1)) :height (int height) :color color)))
          (rl/text! "Press SPACE to shuffle the current sequence"
                    :x 10 :y (- H 96) :size 20 :color rl/BLACK)
          (rl/text! "Press UP or DOWN to change the sequence length"
                    :x 10 :y (- H 66) :size 20 :color rl/BLACK)

          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) n' bars)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
