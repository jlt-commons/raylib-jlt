(ns net.b12n.raylib-jlt.l-system
  "raylib [generative] example - an L-system fractal plant. A string is rewritten by
  production rules, then drawn with turtle graphics (F=forward, +/-=turn, []=branch);
  the plant reveals itself segment by segment, then regrows."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def rules {\X "F+[[X]-X]-F[-FX]+X"
            \F "FF"})
(def iterations 5)
(def angle-rad (* 25.0 (/ Math/PI 180.0)))
(def step-len 3.2)

(defn- expand
  [s]
  (apply str (map (fn [ch] (get rules ch (str ch))) s)))

(defn- lsystem-string
  []
  (nth (iterate expand "X") iterations))

(defn- build-segments
  [s]
  (let [a0 (- (/ Math/PI 2.0))]
    (loop [chars (seq s) x 400.0 y 445.0 a a0 stack [] segs []]
      (if (empty? chars)
        segs
        (let [ch (first chars) more (rest chars)]
          (cond
            (= ch \F) (let [nx (+ x (* step-len (Math/cos a)))
                            ny (+ y (* step-len (Math/sin a)))]
                        (recur more nx ny a stack (conj segs [x y nx ny])))
            (= ch \+) (recur more x y (+ a angle-rad) stack segs)
            (= ch \-) (recur more x y (- a angle-rad) stack segs)
            (= ch \[) (recur more x y a (conj stack [x y a]) segs)
            (= ch \]) (let [[px py pa] (peek stack)]
                        (recur more px py pa (pop stack) segs))
            :else (recur more x y a stack segs)))))))

(defn -main
  [& _]
  (rl/window! {:width 800
               :height 450
               :title "raylib [generative] example - L-system plant"})
  (rl/set-target-fps 60)
  (let [segs (build-segments (lsystem-string))
        total (count segs)
        deadline (app/auto-quit-deadline)]
    (loop [frame 0]
      (when (app/keep-running? deadline)
        ;; grow over ~33 frames, hold full, regrow every 360 frames
        (let [shown (min total (* (mod frame 360) 45))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (dotimes [i shown]
            (let [[x1 y1 x2 y2] (nth segs i)]
              (rl/line! {:x1 (int x1)
                         :y1 (int y1)
                         :x2 (int x2)
                         :y2 (int y2)
                         :color rl/LIME})))
          (rl/text! (str total " segments") {:x 8
                                             :y 6
                                             :size 18
                                             :color rl/GRAY})
          (app/maybe-screenshot! frame 45)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
