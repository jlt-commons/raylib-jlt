(ns net.b12n.raylib-jlt.amp-envelope
  "raylib [audio] example - amp envelope.

  An ADSR amplitude envelope on a 440Hz tone: HOLD SPACE to play, the gain
  rises through Attack, falls through Decay, holds at Sustain and fades
  through Release when you let go. Q/A, W/S, E/D, R/F adjust the four
  parameters, with the envelope shape drawn and a live gain dot. ESC quits.

  No new FFI: reuses audio-raw-stream.clj's exact refill pattern
  (rl/update-audio-stream takes a plain Clojure seq of floats), just with
  the sine's amplitude shaped by an ADSR envelope stepped once per sample
  and threaded through the buffer-fill loop.
  Ported from raylib's examples/audio/audio_amp_envelope.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const BUFFER-SIZE 4096)
(def ^:const SAMPLE-RATE 44100.0)
(def ^:const SAMPLE-TIME (/ 1.0 SAMPLE-RATE))
(def ^:const TAU (* 2.0 Math/PI))
(def ^:const TONE-HZ 440.0)
(def ^:const LIGHTGRAY-50 (rl/rgba 223 223 223 255))
(def ^:const LIGHTGRAY-30 (rl/rgba 231 231 231 255))

(defn- step-envelope
  "One sample of ADSR. Pure: takes and returns [value state]."
  [value state {:keys [attack decay sustain release]}]
  (case state
    :attack
    (let [v (+ value (* (/ 1.0 attack) SAMPLE-TIME))]
      (if (>= v 1.0) [1.0 :decay] [v :attack]))

    :decay
    (let [v (- value (* (/ (- 1.0 sustain) decay) SAMPLE-TIME))]
      (if (<= v sustain) [sustain :sustain] [v :decay]))

    :sustain [sustain :sustain]

    :release
    (let [v (- value (* (/ sustain release) SAMPLE-TIME))]
      (if (<= v 0.001) [0.0 :idle] [v :release]))

    [value state]))

(defn- fill-buffer
  "One refill's worth of envelope-shaped sine samples, plus the envelope
  state after them."
  [{:keys [value state time]} params]
  (loop [i 0 v value s state tm time samples (transient [])]
    (if (>= i BUFFER-SIZE)
      [(persistent! samples) {:value v
                              :state s
                              :time tm}]
      (let [[v' s'] (step-envelope v s params)
            sample (* v' (Math/sin (* TAU TONE-HZ tm)))]
        (recur (inc i) v' s' (+ tm SAMPLE-TIME) (conj! samples sample))))))

(defn- clampf
  [v lo hi]
  (cond (< v lo) lo (> v hi) hi :else v))

(defn- param-bar!
  [{:keys [y label keys- value lo hi]}]
  (rl/text! label {:x 20
                   :y (+ y 10)
                   :size 10
                   :color rl/DARKGRAY})
  (rl/rect! {:x 100
             :y y
             :width 400
             :height 30
             :color LIGHTGRAY-50})
  (rl/rect! {:x 100
             :y y
             :width (int (* (/ (- value lo) (- hi lo)) 400))
             :height 30
             :color rl/SKYBLUE})
  (rl/rect-lines! {:x 100
                   :y y
                   :width 400
                   :height 30
                   :color rl/GRAY})
  (rl/text! (str (format "%.2f" value) "  (" keys- ")") {:x 510
                                                         :y (+ y 10)
                                                         :size 10
                                                         :color rl/DARKGRAY}))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [audio] example - amp envelope"})
  (rl/set-target-fps 60)
  (rl/init-audio-device)
  (rl/set-audio-stream-buffer-size-default BUFFER-SIZE)
  (let [stream (rl/load-audio-stream (int SAMPLE-RATE) 32 1)
        deadline (app/auto-quit-deadline)]
    (rl/play-audio-stream stream)
    (loop [frame 0 attack 1.0 decay 1.0 sustain 0.5 release 1.0
           env {:value 0.0
                :state :idle
                :time 0.0}]
      (when (app/keep-running? deadline)
        (let [step 0.02
              attack (clampf (cond (rl/key-down? rl/KEY-Q) (+ attack step)
                                   (rl/key-down? rl/KEY-A) (- attack step)
                                   :else attack) 0.1 3.0)
              decay (clampf (cond (rl/key-down? rl/KEY-W) (+ decay step)
                                  (rl/key-down? rl/KEY-S) (- decay step)
                                  :else decay) 0.1 3.0)
              sustain (clampf (cond (rl/key-down? rl/KEY-E) (+ sustain 0.01)
                                    (rl/key-down? rl/KEY-D) (- sustain 0.01)
                                    :else sustain) 0.0 1.0)
              release (clampf (cond (rl/key-down? rl/KEY-R) (+ release step)
                                    (rl/key-down? rl/KEY-F) (- release step)
                                    :else release) 0.1 3.0)
              params {:attack attack
                      :decay decay
                      :sustain sustain
                      :release release}
              env (cond
                    (rl/key-pressed? rl/KEY-SPACE) (assoc env :state :attack)
                    (and (rl/key-released? rl/KEY-SPACE) (not= (:state env) :idle))
                    (assoc env :state :release)
                    :else env)
              env (if (rl/audio-stream-processed? stream)
                    (let [[samples env'] (fill-buffer env params)]
                      (rl/update-audio-stream stream samples)
                      env')
                    env)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)

          (param-bar! {:y 60
                       :label "Attack (s)"
                       :keys- "Q/A"
                       :value attack
                       :lo 0.1
                       :hi 3.0})
          (param-bar! {:y 100
                       :label "Decay (s)"
                       :keys- "W/S"
                       :value decay
                       :lo 0.1
                       :hi 3.0})
          (param-bar! {:y 140
                       :label "Sustain"
                       :keys- "E/D"
                       :value sustain
                       :lo 0.0
                       :hi 1.0})
          (param-bar! {:y 180
                       :label "Release (s)"
                       :keys- "R/F"
                       :value release
                       :lo 0.1
                       :hi 3.0})

          (let [total (+ attack decay 1.0 release)
                scale-x (/ 400.0 total)
                peak-x (+ 100.0 (* attack scale-x))
                sus-x (+ peak-x (* decay scale-x))
                sus-y (+ 250.0 (* (- 1.0 sustain) 100.0))
                rel-x (+ sus-x scale-x)
                end-x (+ rel-x (* release scale-x))]
            (rl/rect! {:x 100
                       :y 250
                       :width 400
                       :height 100
                       :color LIGHTGRAY-30})
            (rl/rect-lines! {:x 100
                             :y 250
                             :width 400
                             :height 100
                             :color rl/GRAY})
            (rl/line! {:x1 100
                       :y1 350
                       :x2 (int peak-x)
                       :y2 250
                       :color rl/SKYBLUE})
            (rl/line! {:x1 (int peak-x)
                       :y1 250
                       :x2 (int sus-x)
                       :y2 (int sus-y)
                       :color rl/BLUE})
            (rl/line! {:x1 (int sus-x)
                       :y1 (int sus-y)
                       :x2 (int rel-x)
                       :y2 (int sus-y)
                       :color rl/DARKBLUE})
            (rl/line! {:x1 (int rel-x)
                       :y1 (int sus-y)
                       :x2 (int end-x)
                       :y2 350
                       :color rl/ORANGE})
            (rl/text! "ADSR Visualizer" {:x 100
                                         :y 230
                                         :size 10
                                         :color rl/DARKGRAY}))

          (let [gain (:value env)]
            (rl/circle! {:x 520
                         :y (int (- 350.0 (* gain 100.0)))
                         :radius 5.0
                         :color rl/MAROON})
            (rl/text! (str "Current Gain: " (format "%.2f" gain))
                      {:x 535
                       :y (int (- 345.0 (* gain 100.0)))
                       :size 10
                       :color rl/MAROON}))

          (rl/text! "HOLD SPACE to PLAY the sound!" {:x 200
                                                     :y 400
                                                     :size 20
                                                     :color rl/LIGHTGRAY})

          (app/maybe-screenshot! frame 30)
          (rl/end-drawing)
          (recur (inc frame) attack decay sustain release env))))
    (rl/unload-audio-stream stream))
  (rl/close-audio-device)
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
