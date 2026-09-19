(ns net.b12n.raylib-jlt.strings-management
  "raylib [text] example - strings management (`jolt -M:strings-management`).

  Port of raylib's examples/text/text_strings_management.c. A sentence is one
  bouncing text particle. Drag it with the left button and let go to throw it,
  right-click to cut it in half, hold SHIFT and right-click to shatter it into
  single characters, middle-click to shake everything, and hold CTRL while
  dragging one particle over another to glue the two into a longer one. With
  exactly one particle left, typing a character splits the sentence on it. 1 to
  6 reset the sentence through a different case transform each.

  Zero new FFI, and only rl/MOUSE-MIDDLE is new as a constant. That is the
  point of this one: the C is a tour of raylib's own string helpers, TextCopy,
  TextSubtext, TextSplit, TextLength, TextFormat and the six TextTo* case
  conversions, which exist because C has no string library. Clojure does, so
  every one of them is subs, count, str and clojure.string here, and the only
  raylib call left in the text path is MeasureText, which has to be raylib's
  because only raylib knows how wide its font draws.

  One deviation. The C tracks the grabbed particle by pointer, so a slice can
  leave that pointer dangling into a shifted array. Particles here are a vector
  and the grab is an index into it, so anything that rebuilds the vector drops
  the grab rather than carrying a stale index forward."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [clojure.string :as str]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const FONT-SIZE 30)
(def ^:const PADDING 5.0)
(def ^:const BORDER 5.0)
(def ^:const FRICTION 0.99)
(def ^:const ELASTICITY 0.9)
(def ^:const MAX-PARTICLES 100)

(def ^:private sentence "raylib => fun videogames programming!")
(def ^:private snake-sentence "raylib_fun_videogames_programming")
(def ^:private pascal-sentence "RaylibFunVideogamesProgramming")

(defn- words
  "The underscore-separated words of a snake_case string."
  [s]
  (remove str/blank? (str/split s #"_")))

(defn- capitalize-first
  [s]
  (str (str/upper-case (subs s 0 1)) (subs s 1)))

(defn- to-pascal
  "TextToPascal: raylib_fun_x -> RaylibFunX."
  [s]
  (apply str (map capitalize-first (words s))))

(defn- to-camel
  "TextToCamel: raylib_fun_x -> raylibFunX."
  [s]
  (let [[head & tail] (words s)]
    (apply str head (map capitalize-first tail))))

(defn- to-snake
  "TextToSnake: RaylibFunX -> raylib_fun_x. Every capital starts a new word,
  which is the rule the C applies one character at a time. The leading
  underscore only appears when the input itself started with a capital, so it
  is stripped rather than dropped blind."
  [s]
  (let [marked (->> s
                    (mapcat (fn [c] (if (Character/isUpperCase c) [\_ c] [c])))
                    (apply str)
                    str/lower-case)]
    (if (str/starts-with? marked "_") (subs marked 1) marked)))

(defn- random-color
  []
  (rl/rgba (rl/get-random-value 0 255)
           (rl/get-random-value 0 255)
           (rl/get-random-value 0 255)
           255))

(defn- particle
  "One text particle, sized to whatever MeasureText says its text draws as."
  [text x y color]
  (let [w (+ (rl/text-width text {:size FONT-SIZE}) (* 2.0 PADDING))
        h (+ FONT-SIZE (* 2.0 PADDING))]
    {:text text
     :x (double x)
     :y (double y)
     :px (double x)
     :py (double y)
     :w (double w)
     :h (double h)
     :vx (double (rl/get-random-value -200 200))
     :vy (double (rl/get-random-value -200 200))
     :color color
     :grabbed? false}))

(defn- first-particle
  [text]
  [(particle text (/ W 2.0) (/ H 2.0) rl/RAYWHITE)])

(defn- inside?
  [{:keys [x y w h]} mx my]
  (and (<= x mx (+ x w)) (<= y my (+ y h))))

(defn- overlaps?
  [a b]
  (and (< (:x a) (+ (:x b) (:w b))) (> (+ (:x a) (:w a)) (:x b))
       (< (:y a) (+ (:y b) (:h b))) (> (+ (:y a) (:h a)) (:y b))))

(defn- topmost-at
  "The index of the last particle under the cursor, last because that is the one
  drawn on top."
  [particles mx my]
  (->> (map-indexed vector particles)
       (filter (fn [[_ p]] (inside? p mx my)))
       last
       first))

(defn- step
  "One frame of free flight: move, bounce off the four walls losing a tenth of
  the speed each time, then shed a hundredth to friction."
  [{:keys [x y w h vx vy]
    :as p} dt]
  (let [x (+ x (* vx dt))
        y (+ y (* vy dt))
        [x vx] (cond
                 (>= (+ x w) W) [(- W w) (* -1.0 vx ELASTICITY)]
                 (<= x 0)       [0.0 (* -1.0 vx ELASTICITY)]
                 :else          [x vx])
        [y vy] (cond
                 (>= (+ y h) H) [(- H h) (* -1.0 vy ELASTICITY)]
                 (<= y 0)       [0.0 (* -1.0 vy ELASTICITY)]
                 :else          [y vy])]
    (assoc p :x x :y y :vx (* vx FRICTION) :vy (* vy FRICTION))))

(defn- drag
  "Pin a grabbed particle to the cursor, and read its velocity off how far it
  moved this frame so releasing it throws it."
  [{:keys [px py]
    :as p} mx my ox oy dt]
  (let [x (- mx ox)
        y (- my oy)]
    (assoc p
           :x x
           :y y
           :px x
           :py y
           :vx (if (pos? dt) (/ (- x px) dt) 0.0)
           :vy (if (pos? dt) (/ (- y py) dt) 0.0))))

(defn- slice
  "Cut a particle into chunks of `n` characters, each landing at the spot along
  the original box its characters occupied."
  [{:keys [text x y w]} n]
  (let [len (count text)]
    (for [i (range 0 len n)]
      (particle (subs text i (min len (+ i n)))
                (+ x (* i (/ w len)))
                y
                (random-color)))))

(defn- slice-on-char
  "Split on a typed character: one particle per run between occurrences, plus
  one carrying each occurrence itself. nil when the character is not in there."
  [{:keys [text x y w]
    :as p} ch]
  (let [hits (count (filter (fn [c] (= c ch)) text))
        tokens (remove str/blank? (map (fn [run] (apply str run))
                                       (remove (fn [run] (= (first run) ch))
                                               (partition-by (fn [c] (= c ch)) text))))]
    (when (and (pos? hits) (seq tokens))
      (concat (repeat hits (particle (str ch) (:x p) y (random-color)))
              (map-indexed (fn [i token]
                             (particle token
                                       (+ x (* i (/ w (max 1 (count tokens)))))
                                       y
                                       (random-color)))
                           tokens)))))

(defn- without
  "The vector minus the particle at `i`."
  [particles i]
  (into (subvec particles 0 i) (subvec particles (inc i))))

(defn- replace-with
  "Swap the particle at `i` for the pieces it broke into, unless that would
  overrun the particle budget or there was nothing to break."
  [particles i pieces]
  (if (and (seq pieces)
           (> (count pieces) 1)
           (< (+ (count particles) (count pieces)) MAX-PARTICLES))
    (into (without particles i) pieces)
    particles))

(defn- glue
  "Merge the grabbed particle into the one it is resting on, keeping the hold on
  the result. Returns [particles grabbed-index]."
  [particles i]
  (let [held (nth particles i)
        target (first (keep-indexed (fn [j p]
                                      (when (and (not= j i) (overlaps? held p)) j))
                                    particles))]
    (if (and target (< (count particles) MAX-PARTICLES))
      (let [merged (assoc (particle (str (:text held) (:text (nth particles target)))
                                    (:x held) (:y held) rl/RAYWHITE)
                          :grabbed? true)
            rest-of (keep-indexed (fn [j p] (when-not (#{i target} j) p)) particles)
            kept (vec rest-of)]
        [(conj kept merged) (count kept)])
      [particles i])))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [text] example - strings management"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        resets {rl/KEY-ONE sentence
                rl/KEY-TWO (str/upper-case sentence)
                rl/KEY-THREE (str/lower-case sentence)
                rl/KEY-FOUR (to-pascal snake-sentence)
                rl/KEY-FIVE (to-snake pascal-sentence)
                rl/KEY-SIX (to-camel snake-sentence)}]
    (loop [frame 0
           particles (first-particle sentence)
           grabbed nil
           ox 0.0
           oy 0.0]
      (when (app/keep-running? deadline)
        (let [dt (rl/get-frame-time)
              mx (rl/get-mouse-x)
              my (rl/get-mouse-y)
              ;; grab
              hit (when (rl/mouse-pressed? rl/MOUSE-LEFT) (topmost-at particles mx my))
              [particles grabbed ox oy]
              (if hit
                [(assoc-in particles [hit :grabbed?] true)
                 hit
                 (- mx (:x (nth particles hit)))
                 (- my (:y (nth particles hit)))]
                [particles grabbed ox oy])
              ;; release
              [particles grabbed] (if (and grabbed (rl/mouse-released? rl/MOUSE-LEFT))
                                    [(assoc-in particles [grabbed :grabbed?] false) nil]
                                    [particles grabbed])
              ;; slice, or shatter with SHIFT held
              cut (when (rl/mouse-pressed? rl/MOUSE-RIGHT) (topmost-at particles mx my))
              [particles grabbed]
              (if cut
                (let [p (nth particles cut)
                      n (if (rl/key-down? rl/KEY-LEFT-SHIFT) 1 (max 1 (quot (count (:text p)) 2)))
                      next-particles (replace-with particles cut (slice p n))]
                  [next-particles (when (identical? next-particles particles) grabbed)])
                [particles grabbed])
              ;; shake everything that is not being held
              particles (if (rl/mouse-pressed? rl/MOUSE-MIDDLE)
                          (mapv (fn [p]
                                  (if (:grabbed? p)
                                    p
                                    (assoc p
                                           :vx (double (rl/get-random-value -2000 2000))
                                           :vy (double (rl/get-random-value -2000 2000)))))
                                particles)
                          particles)
              ;; 1 to 6 put the sentence back through a different case transform
              reset-key (first (filter rl/key-pressed? (keys resets)))
              [particles grabbed] (if reset-key
                                    [(first-particle (get resets reset-key)) nil]
                                    [particles grabbed])
              ;; a typed character splits the last particle standing on it
              typed (rl/get-char-pressed)
              particles (if (and (= 1 (count particles)) (pos? typed))
                          (or (some->> (slice-on-char (first particles) (char typed))
                                       vec
                                       (replace-with particles 0))
                              particles)
                          particles)
              ;; physics, then the glue check for whatever is being dragged
              particles (mapv (fn [p] (if (:grabbed? p) p (step p dt))) particles)
              particles (if grabbed
                          (update particles grabbed drag mx my ox oy dt)
                          particles)
              [particles grabbed] (if (and grabbed (rl/key-down? rl/KEY-LEFT-CONTROL))
                                    (glue particles grabbed)
                                    [particles grabbed])]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [{:keys [text x y w h color]} particles]
            (rl/rect! {:x (- x BORDER)
                       :y (- y BORDER)
                       :width (+ w (* 2 BORDER))
                       :height (+ h (* 2 BORDER))
                       :color rl/BLACK})
            (rl/rect! {:x x
                       :y y
                       :width w
                       :height h
                       :color color})
            (rl/text! text {:x (+ x PADDING)
                            :y (+ y PADDING)
                            :size FONT-SIZE
                            :color rl/BLACK}))
          (doseq [[i line] (map-indexed vector
                                        ["drag a particle with the left button, release to throw it"
                                         "right-click one to cut it in half"
                                         "SHIFT and right-click to shatter it into characters"
                                         "CTRL while dragging glues two particles together"
                                         "middle-click to shake, 1 to 6 to reset the sentence"
                                         "with one particle left, type a character to split on it"])]
            (rl/text! line {:x 10
                            :y (+ 10 (* 20 i))
                            :size 10
                            :color rl/DARKGRAY}))
          (rl/text! (str "TEXT PARTICLE COUNT: " (count particles))
                    {:x 10
                     :y (- H 30)
                     :size 20
                     :color rl/BLACK})
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) particles grabbed ox oy)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
