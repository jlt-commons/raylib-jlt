(ns net.b12n.raylib-jlt.inline-styling
  "raylib [text] example - inline styling (`jolt -M:inline-styling`).

  Port of raylib's examples/text/text_inline_styling.c. A tiny markup inside the
  string itself sets colours mid-line: `[cRRGGBBAA]` changes the foreground,
  `[bRRGGBBAA]` paints a background behind the run, and `[r]` puts both back. The
  alphas in the markup are multiplied by the base colour's alpha, so one fade
  value still controls the whole line while each run keeps its own styling.

  Zero new FFI. The C draws through `DrawTextEx` with an explicit Font and
  spacing; `MeasureText` and `DrawText` are already bound here and answer the
  same questions for the default font, so the parser is the whole example and it
  is ordinary Clojure. The parse is deliberately positional rather than a regex
  sweep: a tag is either `[r]` or a letter plus exactly eight hex digits in
  brackets, and anything that does not fit that shape is text, including a
  stray `[`."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const HEX-LEN 8)
(def ^:const COLOR-TAG-LEN 11)      ; [ + c|b + 8 hex + ]
(def ^:const RESET-TAG-LEN 3)       ; [r]

(defn- hex->color
  "RRGGBBAA to a packed Color, with the tag's alpha scaled by the base alpha so
  fading the base fades everything the markup set."
  [hex base-alpha]
  (let [v (Long/parseLong hex 16)]
    (rl/rgba (bit-and (bit-shift-right v 24) 0xff)
             (bit-and (bit-shift-right v 16) 0xff)
             (bit-and (bit-shift-right v 8) 0xff)
             (quot (* (bit-and v 0xff) base-alpha) 255))))

(defn- tag-at
  "The style tag starting at `i`, as [length kind hex], or nil when the bracket
  there is just a bracket."
  [s i]
  (let [n (count s)]
    (when (= \[ (nth s i))
      (cond
        (and (<= (+ i RESET-TAG-LEN) n)
             (= "[r]" (subs s i (+ i RESET-TAG-LEN))))
        [RESET-TAG-LEN :reset nil]

        (and (<= (+ i COLOR-TAG-LEN) n)
             (contains? #{\c \b} (nth s (inc i)))
             (= \] (nth s (+ i COLOR-TAG-LEN -1)))
             (re-matches #"[0-9a-fA-F]{8}" (subs s (+ i 2) (+ i 2 HEX-LEN))))
        [COLOR-TAG-LEN
         (if (= \c (nth s (inc i))) :fg :bg)
         (subs s (+ i 2) (+ i 2 HEX-LEN))]))))

(defn- parse-styled
  "Split marked-up text into runs of {:text :fg :bg}, where nil means the base
  colour for :fg and nothing painted for :bg."
  [s base]
  (let [alpha (bit-and (bit-shift-right base 24) 0xff)
        n (count s)]
    (loop [i 0
           start 0
           fg nil
           bg nil
           runs []]
      (if (>= i n)
        (cond-> runs
          (> n start) (conj {:text (subs s start)
                             :fg fg
                             :bg bg}))
        (if-let [[len kind hex] (tag-at s i)]
          (let [runs (cond-> runs
                       (> i start) (conj {:text (subs s start i)
                                          :fg fg
                                          :bg bg}))]
            (recur (+ i len)
                   (+ i len)
                   ;; A tag touches one slot and leaves the other alone, so
                   ;; each default is its OWN previous value. Defaulting the
                   ;; foreground to `bg` here is the mistake that makes
                   ;; [cGREEN][bRED] draw black on red instead of green on red,
                   ;; and it only shows when both tags are in play.
                   (case kind
                     :reset nil
                     :fg (hex->color hex alpha)
                     fg)
                   (case kind
                     :reset nil
                     :bg (hex->color hex alpha)
                     bg)
                   runs))
          (recur (inc i) start fg bg runs))))))

(defn- styled-width
  [runs size]
  (reduce + 0 (map (fn [r] (rl/text-width (:text r) {:size size})) runs)))

(defn- draw-styled!
  "Draw the runs left to right from x, painting each run's background first."
  [runs x y size base]
  (reduce (fn [dx {:keys [text fg bg]}]
            (let [w (rl/text-width text {:size size})]
              (when bg
                (rl/rect! {:x dx
                           :y y
                           :width w
                           :height size
                           :color bg}))
              (rl/text! text {:x dx
                              :y y
                              :size size
                              :color (or fg base)})
              (+ dx w)))
          x
          runs))

(def ^:private lines
  [[80 20 "This changes the [cFF0000FF]foreground color[r] of provided text!!!"]
   [120 20 "This changes the [bFF00FFFF]background color[r] of provided text!!!"]
   [160 20 "This changes the [c00ff00ff][bff0000ff]foreground and background colors[r]!!!"]])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [text] example - inline styling"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        faded (rl/rgba 0 0 0 100)]
    (loop [frame 0
           tint rl/RED]
      (when (app/keep-running? deadline)
        (let [tint (if (zero? (mod frame 20))
                     (rl/rgba (rl/get-random-value 0 255)
                              (rl/get-random-value 0 255)
                              (rl/get-random-value 0 255)
                              255)
                     tint)
              ;; The same markup the lines above use, built at run time so the
              ;; colour changes every twenty frames.
              creative (format "Let's be [c%02x%02x%02xFF]CREATIVE[r] !!!"
                               (bit-and tint 0xff)
                               (bit-and (bit-shift-right tint 8) 0xff)
                               (bit-and (bit-shift-right tint 16) 0xff))
              creative-runs (parse-styled creative rl/BLACK)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [[y size text] lines]
            (draw-styled! (parse-styled text rl/BLACK) 100 y size rl/BLACK))
          ;; The fourth line's base colour is only 100/255 opaque, so every tag
          ;; on it comes out proportionally lighter than the same tag above.
          (draw-styled! (parse-styled
                         "This changes the [c00ff00ff]alpha[r] relative [cffffffff][b000000ff]from source[r] [cff000088]color[r]!!!"
                         faded)
                        100 200 20 faded)
          (draw-styled! creative-runs 100 240 40 rl/BLACK)
          (rl/rect-lines! {:x 100
                           :y 240
                           :width (styled-width creative-runs 40)
                           :height 40
                           :color rl/GREEN})
          (rl/text! "markup: [cRRGGBBAA] foreground - [bRRGGBBAA] background - [r] reset"
                    {:x 100
                     :y 320
                     :size 10
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) tint)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
