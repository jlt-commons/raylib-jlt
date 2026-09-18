(ns net.b12n.raylib-jlt.audio-stream-callback
  "raylib [audio] example - stream callback (`jolt -M:audio-stream-callback`).

  Port of raylib's examples/audio/audio_stream_callback.c. A 44.1kHz mono stream
  that raudio pulls from rather than one this code pushes to: UP and DOWN change
  the pitch, LEFT and RIGHT cycle sine, square, triangle and sawtooth. The
  samples raudio actually took are drawn underneath.

  The pull is the point, and it is the harder of the suite's two callbacks.
  `custom-logging` gives raylib a function pointer raylib calls on whichever
  thread called into it, which is this one. raudio runs its OWN audio thread and
  calls back from there, on a thread jolt never started, so the entry point needs
  jolt's `:collect-safe`, which reactivates the thread before any jolt code runs
  on it. Without that the process dies with a memory fault no handler can catch.

  Being on the audio thread also sets the budget: the callback owes raudio its
  samples before the device underruns, so the loop writes floats straight into
  raudio's buffer with `ffi/write` and allocates nothing per sample. The scope at
  the bottom is the same discipline. It is a plain native ring buffer the
  callback writes a second copy into, with its cursor kept in the last four bytes
  of the same block, so drawing what played costs the audio thread one more
  float store and no allocation at all.

  Contrast with `audio-raw-stream`, which pushes: it asks
  `IsAudioStreamProcessed` every frame and refills with `UpdateAudioStream` when
  raudio has drained a buffer. Same sound, opposite direction, and the push
  version never leaves the main thread."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SAMPLE-RATE 44100)
(def ^:const BUFFER-SIZE 4096)
(def ^:const MIN-FREQ 20)
(def ^:const MAX-FREQ 12500)
;; The scope holds a tenth of a second, which is enough to show several cycles
;; of anything down to a low hum. The cursor lives in the four bytes after it.
(def ^:const SCOPE-FRAMES 4410)
(def ^:const CURSOR-OFFSET (* 4 SCOPE-FRAMES))

(def ^:private wave-names ["sine" "square" "triangle" "sawtooth"])

(defn- wave
  "One sample of wave `kind` at phase `t`, which runs 0 to 1 over a cycle."
  [kind t]
  (case (int kind)
    0 (Math/sin (* 2.0 Math/PI t))
    1 (if (< t 0.5) 1.0 -1.0)
    2 (- 1.0 (* 4.0 (Math/abs (- t 0.5))))
    (- (* 2.0 t) 1.0)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [audio] example - stream callback"})
  (rl/set-target-fps 30)
  (rl/init-audio-device)
  (rl/set-audio-stream-buffer-size-default BUFFER-SIZE)
  (let [deadline (rl/auto-quit-deadline)
        stream (rl/load-audio-stream SAMPLE-RATE 32 1)
        scope (ffi/alloc (+ CURSOR-OFFSET 4))
        ;; Read by the callback, written by the main loop. A long and an int,
        ;; so a deref costs nothing the audio thread cannot afford.
        *freq (atom 440)
        *kind (atom 0)
        *phase (atom 0.0)
        fill! (fn [buf frames]
                (let [kind @*kind
                      step (/ (double @*freq) SAMPLE-RATE)]
                  (loop [i 0
                         t @*phase
                         cursor (ffi/read scope :int CURSOR-OFFSET)]
                    (if (< i frames)
                      (let [s (wave kind t)]
                        (ffi/write buf :float s (* 4 i))
                        (ffi/write scope :float s (* 4 cursor))
                        (recur (inc i)
                               (let [t' (+ t step)] (if (>= t' 1.0) (- t' 1.0) t'))
                               (let [c (inc cursor)] (if (>= c SCOPE-FRAMES) 0 c))))
                      (do (reset! *phase t)
                          (ffi/write scope :int cursor CURSOR-OFFSET)))))
                nil)
        entry (rl/on-audio-stream! stream fill!)]
    (ffi/write scope :int 0 CURSOR-OFFSET)
    (rl/play-audio-stream stream)
    (loop [frame 0]
      (when (rl/keep-running? deadline)
        (when (rl/key-down? rl/KEY-UP)
          (swap! *freq (fn [f] (min MAX-FREQ (+ f 10)))))
        (when (rl/key-down? rl/KEY-DOWN)
          (swap! *freq (fn [f] (max MIN-FREQ (- f 10)))))
        (when (rl/key-pressed? rl/KEY-RIGHT)
          (swap! *kind (fn [k] (mod (inc k) 4))))
        (when (rl/key-pressed? rl/KEY-LEFT)
          (swap! *kind (fn [k] (mod (dec k) 4))))
        (rl/begin-drawing)
        (rl/clear-background rl/RAYWHITE)
        ;; One scope column per pixel, read straight out of the block the audio
        ;; thread has been filling. The window shown is about four cycles wide
        ;; whatever the pitch, which is what keeps a 12kHz square from arriving
        ;; as a picket fence: at 440Hz that is 401 of the 4410 samples held, and
        ;; the rest of the ring is there for the low end.
        (let [cursor (ffi/read scope :int CURSOR-OFFSET)
              visible (-> (quot (* 4 SAMPLE-RATE) @*freq) (max 64) (min SCOPE-FRAMES))
              start (mod (- cursor visible) SCOPE-FRAMES)
              mid (/ H 2.0)
              amp (* 0.32 H)
              at (fn [col]
                   (let [idx (mod (+ start (quot (* col visible) W)) SCOPE-FRAMES)]
                     (- mid (* amp (ffi/read scope :float (* 4 idx))))))]
          (rl/line! {:x1 0
                     :y1 mid
                     :x2 W
                     :y2 mid
                     :color (rl/rgba 225 228 233 255)})
          (dotimes [col (dec W)]
            (rl/line! {:x1 col
                       :y1 (at col)
                       :x2 (inc col)
                       :y2 (at (inc col))
                       :color rl/MAROON})))
        (rl/text! (str "frequency: " @*freq " Hz") {:x (- W 260)
                                                    :y 10
                                                    :size 20
                                                    :color rl/RED})
        (rl/text! (str "wave type: " (nth wave-names @*kind)) {:x (- W 260)
                                                               :y 34
                                                               :size 20
                                                               :color rl/RED})
        (rl/text! "UP/DOWN change the frequency" {:x 10
                                                  :y 10
                                                  :size 20
                                                  :color rl/DARKGRAY})
        (rl/text! "LEFT/RIGHT change the wave" {:x 10
                                                :y 34
                                                :size 20
                                                :color rl/DARKGRAY})
        (rl/text! "raudio pulls these samples from its own thread"
                  {:x 10
                   :y (- H 20)
                   :size 10
                   :color rl/GRAY})
        (rl/maybe-screenshot! frame 20)
        (rl/end-drawing)
        (recur (inc frame))))
    ;; Order matters on the way out: raudio has to stop calling the pointer
    ;; before the pointer stops existing.
    (rl/clear-audio-stream-callback! stream)
    (rl/unload-audio-stream stream)
    (rl/free-callable! entry)
    (ffi/free scope))
  (rl/close-audio-device)
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
