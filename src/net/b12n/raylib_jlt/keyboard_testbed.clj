(ns net.b12n.raylib-jlt.keyboard-testbed
  "raylib [core] example - keyboard testbed.

  An on-screen ENG-US keyboard: every key you hold lights up MAROON, the key
  under the mouse gets a red overlay, and the last GetKeyPressed/
  GetCharPressed codes are shown top-right. `rl/set-exit-key rl/KEY-NULL`
  disables the ESC shortcut (ESC is a testbed key here) -- close the window
  to quit. No new bindings: point-in-rect is plain arithmetic instead of
  CheckCollisionPointRec, and Fade(RED, 0.2) is just rl/rgba with a reduced
  alpha byte. Ported from raylib's examples/core/core_keyboard_testbed.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SPACING 4)
(def ^:const OFF-X 26.0)

;; Characters 32..126 in code order, for the char-pressed readout.
(def ^:private ASCII " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~")

(defn- k [w label code] {:w w
                         :label label
                         :code code})

(def ^:private ROW-01
  [(k 45 "ESC" rl/KEY-ESCAPE) (k 45 "F1" rl/KEY-F1) (k 45 "F2" rl/KEY-F2)
   (k 45 "F3" rl/KEY-F3) (k 45 "F4" rl/KEY-F4) (k 45 "F5" rl/KEY-F5)
   (k 45 "F6" rl/KEY-F6) (k 45 "F7" rl/KEY-F7) (k 45 "F8" rl/KEY-F8)
   (k 45 "F9" rl/KEY-F9) (k 45 "F10" rl/KEY-F10) (k 45 "F11" rl/KEY-F11)
   (k 45 "F12" rl/KEY-F12) (k 62 "PRINTSCR" rl/KEY-PRINT-SCREEN)
   (k 45 "PAUSE" rl/KEY-PAUSE)])

(def ^:private ROW-02
  [(k 25 "`" rl/KEY-GRAVE) (k 45 "1" rl/KEY-ONE) (k 45 "2" rl/KEY-TWO)
   (k 45 "3" rl/KEY-THREE) (k 45 "4" rl/KEY-FOUR) (k 45 "5" rl/KEY-FIVE)
   (k 45 "6" rl/KEY-SIX) (k 45 "7" rl/KEY-SEVEN) (k 45 "8" rl/KEY-EIGHT)
   (k 45 "9" rl/KEY-NINE) (k 45 "0" rl/KEY-ZERO) (k 45 "-" rl/KEY-MINUS)
   (k 45 "=" rl/KEY-EQUAL) (k 82 "BACK" rl/KEY-BACKSPACE) (k 45 "DEL" rl/KEY-DELETE)])

(def ^:private ROW-03
  [(k 50 "TAB" rl/KEY-TAB) (k 45 "Q" rl/KEY-Q) (k 45 "W" rl/KEY-W)
   (k 45 "E" rl/KEY-E) (k 45 "R" rl/KEY-R) (k 45 "T" rl/KEY-T)
   (k 45 "Y" rl/KEY-Y) (k 45 "U" rl/KEY-U) (k 45 "I" rl/KEY-I)
   (k 45 "O" rl/KEY-O) (k 45 "P" rl/KEY-P) (k 45 "[" rl/KEY-LEFT-BRACKET)
   (k 45 "]" rl/KEY-RIGHT-BRACKET) (k 57 "\\" rl/KEY-BACKSLASH) (k 45 "INS" rl/KEY-INSERT)])

(def ^:private ROW-04
  [(k 68 "CAPS" rl/KEY-CAPS-LOCK) (k 45 "A" rl/KEY-A) (k 45 "S" rl/KEY-S)
   (k 45 "D" rl/KEY-D) (k 45 "F" rl/KEY-F) (k 45 "G" rl/KEY-G)
   (k 45 "H" rl/KEY-H) (k 45 "J" rl/KEY-J) (k 45 "K" rl/KEY-K)
   (k 45 "L" rl/KEY-L) (k 45 ";" rl/KEY-SEMICOLON) (k 45 "'" rl/KEY-APOSTROPHE)
   (k 88 "ENTER" rl/KEY-ENTER) (k 45 "PGUP" rl/KEY-PAGE-UP)])

(def ^:private ROW-05
  [(k 80 "LSHIFT" rl/KEY-LEFT-SHIFT) (k 45 "Z" rl/KEY-Z) (k 45 "X" rl/KEY-X)
   (k 45 "C" rl/KEY-C) (k 45 "V" rl/KEY-V) (k 45 "B" rl/KEY-B)
   (k 45 "N" rl/KEY-N) (k 45 "M" rl/KEY-M) (k 45 "," rl/KEY-COMMA)
   (k 45 "." rl/KEY-PERIOD) (k 45 "/" rl/KEY-SLASH) (k 76 "RSHIFT" rl/KEY-RIGHT-SHIFT)
   (k 45 "UP" rl/KEY-UP) (k 45 "PGDOWN" rl/KEY-PAGE-DOWN)])

(def ^:private ROW-06
  [(k 80 "LCTRL" rl/KEY-LEFT-CONTROL) (k 45 "WIN" rl/KEY-LEFT-SUPER)
   (k 45 "LALT" rl/KEY-LEFT-ALT) (k 208 "SPACE" rl/KEY-SPACE)
   (k 45 "ALTGR" rl/KEY-RIGHT-ALT) (k 60 "RCTRL" rl/KEY-RIGHT-CONTROL)
   (k 45 "LEFT" rl/KEY-LEFT) (k 45 "DOWN" rl/KEY-DOWN) (k 45 "RIGHT" rl/KEY-RIGHT)])

;; Row y positions, matching the original C layout: 80, then +34/+38-ish steps.
(def ^:private ROWS
  [{:y 80.0
    :h 30.0
    :row ROW-01}
   {:y 114.0
    :h 38.0
    :row ROW-02}
   {:y 156.0
    :h 38.0
    :row ROW-03}
   {:y 198.0
    :h 38.0
    :row ROW-04}
   {:y 240.0
    :h 38.0
    :row ROW-05}
   {:y 282.0
    :h 38.0
    :row ROW-06}])

(defn- point-in-rect?
  [px py x y w h]
  (and (<= x px (+ x w)) (<= y py (+ y h))))

(defn- draw-key!
  [{:keys [x y w h code label]} mx my]
  (rl/rect-lines! {:x (int x)
                   :y (int y)
                   :width (int w)
                   :height (int h)
                   :color rl/LIGHTGRAY})
  (let [held? (rl/key-down? code)]
    (rl/rect-lines! {:x (int x)
                     :y (int y)
                     :width (int w)
                     :height (int h)
                     :color (if held? rl/MAROON rl/DARKGRAY)})
    (rl/text! label {:x (int (+ x 4))
                     :y (int (+ y 4))
                     :size 10
                     :color (if held? rl/MAROON rl/DARKGRAY)}))
  (when (point-in-rect? mx my x y w h)
    (rl/rect! {:x (int x)
               :y (int y)
               :width (int w)
               :height (int h)
               :color (rl/rgba 230 41 55 51)})
    (rl/rect-lines! {:x (int x)
                     :y (int y)
                     :width (int w)
                     :height (int h)
                     :color rl/RED})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - keyboard testbed"})
  (rl/set-exit-key rl/KEY-NULL)
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 last-key 0 last-ch 0]
      (when (rl/keep-running? deadline)
        (let [kp (rl/get-key-pressed)
              ch (rl/get-char-pressed)
              last-key (if (pos? kp) kp last-key)
              last-ch (if (pos? ch) ch last-ch)
              mx (rl/get-mouse-x)
              my (rl/get-mouse-y)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "KEYBOARD LAYOUT: ENG-US" {:x 26
                                               :y 38
                                               :size 20
                                               :color rl/LIGHTGRAY})
          (rl/text! (str "key: " last-key "  char: "
                         (if (<= 32 last-ch 125)
                           (subs ASCII (- last-ch 32) (- last-ch 31))
                           "")
                         " (" last-ch ")")
                    {:x 560
                     :y 38
                     :size 20
                     :color rl/GRAY})
          (doseq [{:keys [y h row]} ROWS]
            (loop [i 0 x OFF-X]
              (when (< i (count row))
                (let [km (nth row i)]
                  (draw-key! (assoc km :x x :y y :h h) mx my)
                  (recur (inc i) (+ x (:w km) SPACING))))))
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) last-key last-ch)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
