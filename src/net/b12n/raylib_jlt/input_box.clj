(ns net.b12n.raylib-jlt.input-box
  "raylib [text] example - a text input box. Type printable characters
  (GetCharPressed), backspace deletes, a blinking cursor blinks, ENTER clears.
  Uses the get-char-pressed / get-key-pressed binds. See README.md."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def max-len 20)

(defn- drain-chars
  [s]
  (loop [s s]
    (let [c (rl/get-char-pressed)]
      (if (pos? c)
        (recur (if (and (< (count s) max-len) (>= c 32) (<= c 125))
                 (str s (char c))
                 s))
        s))))

(defn -main
  [& _]
  (rl/window! :width 800 :height 450 :title "raylib [text] example - input box")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           text ""]
      (when (rl/keep-running? deadline)
        (let [text (drain-chars text)
              text (if (rl/key-pressed? rl/KEY-BACKSPACE)
                     (subs text 0 (max 0 (dec (count text))))
                     text)
              text (if (rl/key-pressed? rl/KEY-ENTER) "" text)
              cursor? (even? (quot frame 30))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "Type something:" :x 200 :y 140 :size 20 :color rl/DARKGRAY)
          (rl/rect-lines! :x 200 :y 180 :width 400 :height 50 :color rl/DARKGRAY)
          (rl/text! (str text (if cursor? "_" "")) :x 210 :y 192 :size 30 :color rl/MAROON)
          (rl/text! (str (count text) "/" max-len) :x 200 :y 245 :size 20 :color rl/GRAY)
          (rl/text! "BACKSPACE deletes · ENTER clears" :x 200 :y 275 :size 16 :color rl/GRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) text)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
