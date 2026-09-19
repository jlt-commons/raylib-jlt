(ns net.b12n.raylib-jlt.digital-clock
  "raylib [shapes] example - digital clock. A seven-segment HH:MM:SS display: each
  digit's lit segments come from a lookup table, drawn as filled rects (unlit segments
  dimly visible, like a real display). Time from rl/local-time; the colons blink each
  second. Port of shapes_digital_clock (digital half)."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

;; segments: a=top g=middle d=bottom  f=top-left b=top-right  e=bot-left c=bot-right
(def ^:private segs
  {0 #{:a :b :c :d :e :f}
   1 #{:b :c}
   2 #{:a :b :g :e :d}
   3 #{:a :b :g :c :d}
   4 #{:f :g :b :c}
   5 #{:a :f :g :c :d}
   6 #{:a :f :g :e :c :d}
   7 #{:a :b :c}
   8 #{:a :b :c :d :e :f :g}
   9 #{:a :b :c :d :f :g}})

(defn- draw-digit
  [d x y w hh t on off]
  (let [lit (get segs d)
        seg (fn [k rx ry rw rh]
              (rl/rect! {:x (int rx)
                         :y (int ry)
                         :width (int rw)
                         :height (int rh)
                         :color (if (contains? lit k) on off)}))]
    (seg :a x y w t)
    (seg :g x (+ y hh) w t)
    (seg :d x (+ y (* 2 hh)) w t)
    (seg :f x y t hh)
    (seg :b (+ x (- w t)) y t hh)
    (seg :e x (+ y hh) t hh)
    (seg :c (+ x (- w t)) (+ y hh) t hh)))

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - digital clock"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        on  (rl/rgba 80 230 120 255)
        off (rl/rgba 28 44 34 255)
        w 54 hh 46 t 12 y 110
        ;; six digit x's (with colon gaps after HH and MM) + two colon x's
        xs [150 216 318 384 486 552]
        colon-xs [288 456]]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        (let [[h m s] (rl/local-time)
              digits [(quot h 10) (mod h 10) (quot m 10) (mod m 10) (quot s 10) (mod s 10)]]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 16 20 18 255))
          (dotimes [i 6]
            (draw-digit (nth digits i) (nth xs i) y w hh t on off))
          (let [cc (if (even? s) on off)]   ; colons blink: bright on even seconds, dim on odd
            (doseq [cx colon-xs]
              (rl/rect! {:x cx
                         :y (+ y 30)
                         :width t
                         :height t
                         :color cc})
              (rl/rect! {:x cx
                         :y (+ y (* 2 hh) -18)
                         :width t
                         :height t
                         :color cc})))
          (rl/text! "seven-segment (libc local time)" {:x 10
                                                       :y 10
                                                       :size 20
                                                       :color (rl/rgba 120 180 140 255)})
          (app/maybe-screenshot! frame 12)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
