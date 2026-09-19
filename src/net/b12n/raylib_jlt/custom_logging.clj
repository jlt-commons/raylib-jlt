(ns net.b12n.raylib-jlt.custom-logging
  "raylib [core] example - custom logging (`jolt -M:custom-logging`).

  Port of raylib's examples/core/core_custom_logging.c. raylib's own log goes to
  a jolt function instead of the console, and the captured lines are stamped,
  coloured by level and drawn in the window. SPACE compiles a deliberately
  broken shader, which is the cheapest way to make raylib produce warnings and
  errors rather than the wall of INFO it emits at startup. BACKSPACE clears.

  This is the suite's first callback INTO jolt. Every other binding calls out of
  jolt into C; `SetTraceLogCallback` hands raylib a function pointer and raylib
  calls it, which `ffi/foreign-callable` builds out of an ordinary jolt fn.

  The awkward part is the third parameter. raylib's callback signature ends in a
  `va_list`, which no FFI type describes, so `rl/on-trace-log!` takes it as an
  opaque pointer and hands it to libc's `vsnprintf` with the format string. That
  is the step that turns \"Target time per frame: %02.03f milliseconds\" into the
  line with the number in it, and skipping it would print raylib's format strings
  literally. libc is already the suite's one non-raylib FFI (see `local-time`),
  so nothing new is loaded to do it.

  Two deviations. The C tells you to check the console; drawing the log in the
  window is the whole point of having captured it. And the captured lines are
  revealed a few frames apart rather than all at once, so the example has motion
  of its own: no synthetic input actuates a raylib window (see the note in
  scripts/demo_manifest.edn), and a wall of text that never changes records as a
  single frame. The text is raylib's real log either way."
  (:require
   [clojure.string :as str]
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const ROW-H 15)
(def ^:const VISIBLE 26)
(def ^:const FRAMES-PER-REVEAL 4)

(def ^:private broken-shader
  "Deliberately invalid GLSL. raylib compiles it, fails, and logs the driver's
  own error text at WARNING and ERROR, which is what gives the colour key
  something to show."
  "#version 330\nout vec4 finalColor;\nvoid main() { finalColor = nope(1.0); }")

(defn- level-color
  [level]
  (condp = level
    rl/LOG-WARNING rl/ORANGE
    rl/LOG-ERROR rl/RED
    rl/LOG-FATAL rl/MAROON
    rl/LOG-DEBUG rl/VIOLET
    rl/LOG-TRACE rl/GRAY
    rl/DARKGRAY))

(defn- level-name
  [level]
  (condp = level
    rl/LOG-TRACE "TRACE"
    rl/LOG-DEBUG "DEBUG"
    rl/LOG-INFO "INFO "
    rl/LOG-WARNING "WARN "
    rl/LOG-ERROR "ERROR"
    rl/LOG-FATAL "FATAL"
    "?????"))

(defn -main
  [& _]
  (let [captured (atom [])
        ;; Installed BEFORE init-window, so the startup messages are the first
        ;; thing the log holds, exactly as in the C.
        entry (rl/on-trace-log!
               (fn [level text]
                 (let [[h m s] (rl/local-time)]
                   (swap! captured conj
                          {:level level
                           ;; A driver's compile log arrives with its own
                           ;; newlines in it, and DrawText honours those, so a
                           ;; single message would spill over the rows below it.
                           :text (str/join " " (str/split-lines text))
                           :at (format "%02d:%02d:%02d" h m s)}))))]
    (rl/window! {:width W
                 :height H
                 :title "raylib [core] example - custom logging"})
    (rl/set-target-fps 60)
    (let [deadline (app/auto-quit-deadline)]
      (loop [frame 0
             base 0]
        (when (app/keep-running? deadline)
          (when (rl/key-pressed? rl/KEY-SPACE)
            ;; Fails to link, so `shader` answers nil every time and there is
            ;; nothing to unload; the log is the whole output.
            (when-let [sh (rl/shader broken-shader)]
              (rl/unload-shader! sh)))
          (let [cleared? (rl/key-pressed? rl/KEY-BACKSPACE)
                _ (when cleared? (reset! captured []))
                ;; The reveal counts from the last clear, so emptying the log
                ;; replays the fill rather than snapping everything back at once.
                base (if cleared? frame base)
                rows @captured
                revealed (min (count rows)
                              (inc (quot (- frame base) FRAMES-PER-REVEAL)))
                shown (vec (take-last VISIBLE (subvec rows 0 revealed)))]
            (rl/begin-drawing)
            (rl/clear-background (rl/rgba 24 26 30 255))
            (doseq [[i {:keys [level text at]}] (map-indexed vector shown)]
              (let [y (+ 30 (* ROW-H i))]
                (rl/text! at {:x 8
                              :y y
                              :size 10
                              :color (rl/rgba 90 96 110 255)})
                (rl/text! (level-name level) {:x 70
                                              :y y
                                              :size 10
                                              :color (level-color level)})
                (rl/text! text {:x 120
                                :y y
                                :size 10
                                :color (rl/rgba 200 205 215 255)})))
            (rl/text! (str "raylib's log, captured by a jolt callback - "
                           revealed " of " (count rows) " lines")
                      {:x 8
                       :y 8
                       :size 10
                       :color rl/RAYWHITE})
            (rl/text! "SPACE compiles a broken shader to make it warn - BACKSPACE clears"
                      {:x 8
                       :y (- H 16)
                       :size 10
                       :color (rl/rgba 120 128 145 255)})
            (app/maybe-screenshot! frame 120)
            (rl/end-drawing)
            (recur (inc frame) base)))))
    (rl/close-window)
    ;; raylib is done calling it, so the entry point can go. Freeing it while the
    ;; window is still up would leave raylib holding a dead pointer.
    (rl/free-callable! entry)))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
