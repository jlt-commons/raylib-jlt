(ns net.b12n.raylib-jlt.app
  "The headless smoke harness every example's main loop runs through.

  RAYLIB_APP_AUTO_QUIT_MS ends the loop on a timer and RAYLIB_APP_SHOT dumps one
  PNG, so a windowed example proves itself with nobody at the keyboard. This is
  a property of the example suite rather than of the raylib bindings, which is
  why it lives here and not in net.b12n.raylib."
  (:require
   [net.b12n.raylib.core :as core]
   [net.b12n.raylib.rlgl :as rlgl]))

;; --- smoke-test loop guards --------------------------------------------------
(defn auto-quit-deadline
  "RAYLIB_APP_AUTO_QUIT_MS=<n> ends the loop after n ms, so a window example is
  smoke-testable with no person at the keyboard. Returns an absolute ms deadline
  or nil."
  []
  (when-let [v (System/getenv "RAYLIB_APP_AUTO_QUIT_MS")]
    (try (let [ms (Integer/parseInt v)]
           (when (pos? ms) (+ (System/currentTimeMillis) ms)))
         (catch Exception _ nil))))

(defn keep-running?
  "True while the window is open and any RAYLIB_APP_AUTO_QUIT_MS deadline is unmet."
  [deadline]
  (and (not (core/window-should-close?))
       (or (nil? deadline) (< (System/currentTimeMillis) deadline))))

(def ^:private shot-path (System/getenv "RAYLIB_APP_SHOT"))

;; Which frame to dump. Each example picks a frame that shows it at its best, and
;; that is the right default. Overriding it is how you ask a different question:
;; capture the same example at two distant frames and compare, and an identical
;; pair means the example does not animate unattended. That matters before
;; recording a GIF, because a static example records as a technically valid
;; animation of one repeated image, which no frame-count check can tell from a
;; real one.
(def ^:private shot-at
  (when-let [v (System/getenv "RAYLIB_APP_SHOT_AT")]
    (try (Integer/parseInt v) (catch Exception _ nil))))

(defn maybe-screenshot!
  "RAYLIB_APP_SHOT=/path dumps one PNG on frame `at`, or on RAYLIB_APP_SHOT_AT
  when that is set. Headless visual proof a
  frame rendered. Flushes raylib's batched geometry first (DrawText etc. is
  deferred until EndDrawing, so a mid-frame TakeScreenshot would miss it). raylib
  writes the file's basename into the current working directory."
  [frame at]
  (when (and shot-path (= frame (or shot-at at)))
    (rlgl/flush-batch)
    (core/take-screenshot shot-path)
    (binding [*out* *err*] (println "[net.b12n.raylib-jlt] SHOT" shot-path))))
