(ns net.b12n.raylib-jlt.mouse-trail
  "raylib [shapes] example - mouse trail (`joltc -M:mouse-trail`).

  A fading trail of circles follows the cursor: each frame the newest mouse
  position is pushed onto a bounded history, and the whole history is drawn with
  fading alpha + shrinking radius (older = fainter and smaller)."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const N 60)

(defn -main
  [& _]
  (rl/window! {:title "raylib [shapes] example - mouse trail"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 trail []]
      (when (rl/keep-running? deadline)
        (let [trail (vec (take-last N (conj trail [(rl/get-mouse-x) (rl/get-mouse-y)])))
              n     (count trail)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (doseq [i (range n)]
            (let [[x y] (nth trail i)
                  t (/ (inc i) (double n))]            ; 0..1, newest = 1
              (rl/circle! {:x x
                           :y y
                           :radius (* 30.0 t)
                           :color (rl/rgba 230 41 55 (int (* 255 t)))})))
          (rl/text! "move the mouse for a fading trail" {:x 10
                                                         :y 10
                                                         :size 20
                                                         :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) trail)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
