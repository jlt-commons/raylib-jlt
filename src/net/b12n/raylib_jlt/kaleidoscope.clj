(ns net.b12n.raylib-jlt.kaleidoscope
  "A kaleidoscope (`joltc -M:kaleidoscope`).

  A moving stroke replicated with 6-fold rotational symmetry (plus a mirror) around
  the centre. A bounded trail of stroke points is redrawn each frame so the pattern
  reads as a symmetric whole without needing a render texture."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const FOLDS 6)
(def ^:const TAU 6.283185307179586)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "kaleidoscope")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        cx (/ W 2.0) cy (/ H 2.0)]
    (loop [frame 0 trail []]
      (when (rl/keep-running? deadline)
        (let [t     (* 0.08 frame)
              px    (* 160 (Math/cos t))
              py    (* 160 (Math/sin (* 1.7 t)))
              trail (vec (take-last 150 (conj trail [px py])))
              n     (count trail)]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 12 12 20 255))
          (doseq [i (range 1 n)]
            (let [[ax ay] (nth trail (dec i))
                  [bx by] (nth trail i)
                  age (/ (double i) n)
                  hue (rl/rgba (int (* 255 age)) 120 (int (* 255 (- 1.0 age))) 255)]
              (doseq [k (range FOLDS)]
                (let [ang (* TAU (/ (double k) FOLDS))
                      ca (Math/cos ang) sa (Math/sin ang)
                      x1 (+ cx (- (* ax ca) (* ay sa))) y1 (+ cy (+ (* ax sa) (* ay ca)))
                      x2 (+ cx (- (* bx ca) (* by sa))) y2 (+ cy (+ (* bx sa) (* by ca)))
                      mx1 (+ cx (- (* (- ax) ca) (* ay sa))) my1 (+ cy (+ (* (- ax) sa) (* ay ca)))
                      mx2 (+ cx (- (* (- bx) ca) (* by sa))) my2 (+ cy (+ (* (- bx) sa) (* by ca)))]
                  (rl/line! :x1 (int x1) :y1 (int y1) :x2 (int x2) :y2 (int y2) :color hue)
                  (rl/line! :x1 (int mx1) :y1 (int my1) :x2 (int mx2) :y2 (int my2) :color hue)))))
          (rl/maybe-screenshot! frame 120)
          (rl/end-drawing)
          (recur (inc frame) trail)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
