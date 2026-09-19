(ns net.b12n.raylib-jlt.clipboard-text
  "raylib [core] example - clipboard text (`jolt -M:clipboard-text`).

  Type into the box, press C to copy it to the system clipboard and V to paste
  whatever is there back in. The panel underneath shows what raylib last read out
  of the clipboard, so a copy made in another application shows up here too.

  Both calls take or return a plain `const char *`, which is the one string shape
  that crosses this FFI boundary without ceremony. GetClipboardText can hand back
  NULL when the clipboard holds something that is not text, so the read is
  guarded rather than trusted."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MAX-LEN 40)

(defn- clipboard
  "GetClipboardText, defensively: NULL for a non-text clipboard would otherwise
  surface as a nil where a string is expected."
  []
  (or (try (rl/get-clipboard-text) (catch Exception _ nil)) ""))

(defn- type-into
  "Drain raylib's character queue into `s`, and handle backspace."
  [s]
  (let [s (loop [s s]
            (let [c (rl/get-char-pressed)]
              (cond
                (zero? c) s
                (>= (count s) MAX-LEN) (recur s)
                (<= 32 c 126) (recur (str s (char c)))
                :else (recur s))))]
    (if (and (rl/key-pressed? rl/KEY-BACKSPACE) (pos? (count s)))
      (subs s 0 (dec (count s)))
      s)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - clipboard text"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           typed "jolt talks to the system clipboard"
           pasted ""
           note ""]
      (when (app/keep-running? deadline)
        (let [typed (type-into typed)
              copy? (rl/key-pressed? rl/KEY-C)
              paste? (rl/key-pressed? rl/KEY-V)
              _ (when copy? (rl/set-clipboard-text typed))
              pasted (if paste? (clipboard) pasted)
              note (cond copy? "copied to the clipboard"
                         paste? "pasted from the clipboard"
                         :else note)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "clipboard text" {:x 40
                                      :y 34
                                      :size 24
                                      :color rl/DARKGRAY})
          (rl/text! "type here, then C to copy, V to paste"
                    {:x 40
                     :y 68
                     :size 16
                     :color rl/GRAY})
          (rl/rect! {:x 40
                     :y 100
                     :width (- W 80)
                     :height 54
                     :color (rl/rgba 0 0 0 12)})
          (rl/rect-lines! {:x 40
                           :y 100
                           :width (- W 80)
                           :height 54
                           :color rl/BLUE})
          (rl/text! typed {:x 54
                           :y 116
                           :size 20
                           :color rl/DARKBLUE})
          ;; A caret that blinks twice a second, so an empty box still reads as
          ;; a text field waiting for input.
          (when (< (mod (rl/get-time) 1.0) 0.5)
            (rl/text! "_" {:x (+ 54 (rl/text-width typed :size 20))
                           :y 116
                           :size 20
                           :color rl/DARKBLUE}))
          (rl/text! "last read from the clipboard:" {:x 40
                                                     :y 190
                                                     :size 16
                                                     :color rl/GRAY})
          (rl/rect! {:x 40
                     :y 216
                     :width (- W 80)
                     :height 54
                     :color (rl/rgba 0 0 0 12)})
          (rl/text! (if (seq pasted) pasted "(nothing pasted yet)")
                    {:x 54
                     :y 232
                     :size 20
                     :color (if (seq pasted) rl/DARKGRAY rl/LIGHTGRAY)})
          (rl/text! note {:x 40
                          :y 300
                          :size 16
                          :color rl/GREEN})
          (app/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) typed pasted note)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
