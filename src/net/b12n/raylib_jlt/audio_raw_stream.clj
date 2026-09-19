(ns net.b12n.raylib-jlt.audio-raw-stream
  "raylib [audio] example - raw stream.

  A sine wave generated sample-by-sample into a raw 44.1kHz mono float audio
  stream: UP/DOWN change the frequency (switched at the next zero crossing so
  it never clicks), LEFT/RIGHT pan. The same wave is plotted on screen. No
  bundled audio file -- like every other example in this suite, the signal is
  generated, not loaded. Ported from raylib's examples/audio/audio_raw_stream.c.

  AudioStream is the first raudio binding in this suite: two pointers and
  three u32s passed BY VALUE everywhere raudio touches it (see
  net.b12n.raylib.audio). `rl/update-audio-stream` stages the refill through
  the same `staged` helper the shader uniform setters use; at ~11
  refills/second this is nowhere near a hot path."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def W 800)
(def H 450)
(def ^:const SAMPLE-RATE 44100)
(def ^:const BUFFER-SIZE 4096)
(def ^:const TAU (* 2.0 Math/PI))
(def ^:const MIN-FREQ 20)
(def ^:const MAX-FREQ 12500)

(defn- fill-buffer
  "One refill's worth of sine samples, plus the oscillator state after them.
  A frequency change is held until the next zero crossing (idx wraps past the
  current wavelength), so switching mid-buffer never clicks."
  [{:keys [idx freq start]} new-freq now]
  (loop [i 0 idx idx freq freq start start samples (transient [])]
    (if (>= i BUFFER-SIZE)
      [(persistent! samples) {:idx idx
                              :freq freq
                              :start start}]
      (let [wavelength (quot SAMPLE-RATE freq)
            sample     (Math/sin (/ (* TAU idx) wavelength))
            idx'       (inc idx)
            samples'   (conj! samples sample)]
        (if (>= idx' wavelength)
          (recur (inc i) 0 new-freq now samples')
          (recur (inc i) idx' freq start samples'))))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [audio] example - raw stream"})
  (rl/set-target-fps 60)
  (rl/init-audio-device)
  (rl/set-audio-stream-buffer-size-default BUFFER-SIZE)
  (let [stream   (rl/load-audio-stream SAMPLE-RATE 32 1)
        deadline (app/auto-quit-deadline)]
    (rl/play-audio-stream stream)
    (loop [frame 0 new-freq 440 pan 0.0 osc {:idx 0
                                             :freq 440
                                             :start 0.0}]
      (when (app/keep-running? deadline)
        (let [new-freq (cond (rl/key-down? rl/KEY-UP) (min MAX-FREQ (+ new-freq 10))
                             (rl/key-down? rl/KEY-DOWN) (max MIN-FREQ (- new-freq 10))
                             :else new-freq)
              pan       (cond (rl/key-down? rl/KEY-LEFT) (max -1.0 (- pan 0.01))
                              (rl/key-down? rl/KEY-RIGHT) (min 1.0 (+ pan 0.01))
                              :else pan)
              _         (rl/set-audio-stream-pan stream pan)
              osc       (if (rl/audio-stream-processed? stream)
                          (let [[samples osc'] (fill-buffer osc new-freq (rl/get-time))]
                            (rl/update-audio-stream stream samples)
                            osc')
                          osc)
              freq      (:freq osc)
              wavelength (quot SAMPLE-RATE freq)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! (str "sine frequency: " freq)
                    {:x (- W 220)
                     :y 10
                     :size 20
                     :color rl/RED})
          (rl/text! (str "pan: " (format "%.2f" pan))
                    {:x (- W 220)
                     :y 30
                     :size 20
                     :color rl/RED})
          (rl/text! "Up/down to change frequency" {:x 10
                                                   :y 10
                                                   :size 20
                                                   :color rl/DARKGRAY})
          (rl/text! "Left/right to pan" {:x 10
                                         :y 30
                                         :size 20
                                         :color rl/DARKGRAY})
          ;; Plot the same sine as the one streaming to the speakers, phase-locked
          ;; to the last zero crossing so the plot doesn't jump on a freq change.
          (let [window-start (* (- (rl/get-time) (:start osc)) SAMPLE-RATE)
                window-size  (* 0.1 SAMPLE-RATE)]
            (dotimes [i W]
              (let [t0 (+ window-start (/ (* i window-size) W))
                    t1 (+ window-start (/ (* (inc i) window-size) W))]
                (rl/line! {:x1 i
                           :y1 (+ 250.0 (* 50.0 (Math/sin (/ (* TAU t0) wavelength))))
                           :x2 (inc i)
                           :y2 (+ 250.0 (* 50.0 (Math/sin (/ (* TAU t1) wavelength))))
                           :color rl/RED}))))
          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) new-freq pan osc))))
    (rl/unload-audio-stream stream))
  (rl/close-audio-device)
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
