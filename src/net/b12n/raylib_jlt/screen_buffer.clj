(ns net.b12n.raylib-jlt.screen-buffer
  "raylib [textures] example - screen buffer.

  The classic DOS fire effect as a software screen buffer: a low-res
  palette-indexed grid is simulated every frame (embers grow at the
  bottom row, rise with random drift and decay) and blitted through a
  256-color flame palette into a texture drawn scaled up.

  No new FFI: the ember grid is a plain Clojure vector rather than a
  native buffer (doom.clj's own zbuffer takes the same approach), and
  the blit reuses rl/update-texture-from-fn! exactly as every other
  procedural-texture example does. Runs the simulation at 200x112
  rather than the C's 400x225: a persistent-vector rewrite of ~11000
  live cells a frame is about the budget that keeps this near 60fps in
  jolt, and the DOS original was chunky-pixel by nature anyway.
  Ported from raylib's examples/textures/textures_screen_buffer.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const IMG-W 200)
(def ^:const IMG-H 112)
(def ^:const SCALE 4)
(def ^:const MAX-COLORS 256)

(defn- hsv->color
  "Full HSV -> packed Color (s and v both vary here, unlike the s=v=1
  hue wheels elsewhere in this suite)."
  [h s v]
  (let [h' (/ (mod h 360.0) 60.0)
        i (int (Math/floor h'))
        f (- h' i)
        p (* v (- 1.0 s))
        q (* v (- 1.0 (* s f)))
        t (* v (- 1.0 (* s (- 1.0 f))))
        [r g b] (case (mod i 6)
                  0 [v t p]
                  1 [q v p]
                  2 [p v t]
                  3 [p q v]
                  4 [t p v]
                  5 [v p q])]
    (rl/rgba (int (* 255 r)) (int (* 255 g)) (int (* 255 b)) 255)))

(def ^:private PALETTE
  "The flame ramp: hue eased by t*t so the low indices stay dark."
  (mapv (fn [i]
          (let [t (/ i (double (dec MAX-COLORS)))
                hue (* t t)]
            (hsv->color (+ 250.0 (* 150.0 hue)) t t)))
        (range MAX-COLORS)))

(defn- grow-roots
  "Embers at the bottom brighten over time, capped at 255. Columns 0
  and 1 are left untouched, matching the C's loop starting at x=2."
  [roots]
  (mapv (fn [x v] (if (>= x 2) (min 255 (+ v (rl/get-random-value 0 2))) v))
        (range IMG-W) roots))

(defn- seed-and-clear
  "Copy the ember roots into the bottom row, and blank the top row so
  nothing can rise past it."
  [buf roots]
  (let [base (* (dec IMG-H) IMG-W)]
    (as-> buf b
      (reduce (fn [b x] (assoc b (+ x base) (nth roots x))) b (range IMG-W))
      (reduce (fn [b x] (assoc b x 0)) b (range IMG-W)))))

(defn- rise
  "Every lit cell moves up one row with a random sideways drift and a
  random decay, clearing the cell it came from. Ascending row order is
  load-bearing: a newly written cell in an already-visited row waits
  for next frame instead of rising twice in one pass."
  [buf]
  (loop [y 1 b buf]
    (if (< y IMG-H)
      (recur (inc y)
             (loop [x 0 b b]
               (if (>= x IMG-W)
                 b
                 (let [i (+ x (* y IMG-W))
                       ci (nth b i)]
                   (if (zero? ci)
                     (recur (inc x) b)
                     (let [b (assoc b i 0)
                           mv (dec (rl/get-random-value 0 2))
                           nx (+ x mv)]
                       (if (and (> nx 0) (< nx IMG-W))
                         (let [ia (+ (- i IMG-W) mv)
                               d (rl/get-random-value 0 3)
                               nc (- ci (min d ci))]
                           (recur (inc x) (assoc b ia nc)))
                         (recur (inc x) b))))))))
      b)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - screen buffer"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        tex (rl/texture-from-fn IMG-W IMG-H (fn [_ _] rl/BLACK))]
    (try
      (loop [frame 0 buf (vec (repeat (* IMG-W IMG-H) 0)) roots (vec (repeat IMG-W 0))]
        (when (rl/keep-running? deadline)
          (let [roots (grow-roots roots)
                buf (rise (seed-and-clear buf roots))]
            (rl/update-texture-from-fn!
             tex IMG-W IMG-H
             (fn [x y] (nth PALETTE (nth buf (+ x (* y IMG-W))))))
            (rl/begin-drawing)
            (rl/clear-background rl/RAYWHITE)
            (rl/texture! tex {:x 0
                              :y 0
                              :width (* IMG-W SCALE)
                              :height (* IMG-H SCALE)})
            (rl/maybe-screenshot! frame 200)
            (rl/end-drawing)
            (recur (inc frame) buf roots))))
      (finally (rl/unload-texture! tex))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
