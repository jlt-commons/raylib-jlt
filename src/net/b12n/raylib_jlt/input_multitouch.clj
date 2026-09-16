(ns net.b12n.raylib-jlt.input-multitouch
  "raylib [core] example - input multitouch (`jolt -M:input-multitouch`).

  Every active touch point, drawn as a labelled circle at its position. On a
  touchscreen that is one circle per finger; on a desktop raylib synthesises point
  0 from the mouse, so holding the left button drags a single point around and the
  count reads 1.

  Worth knowing about the desktop case: raylib updates touch position 0 from the
  cursor-position callback, so GetTouchX / GetTouchY follow the pointer whether or
  not a button is down, while GetTouchPointCount counts only actual presses. The
  hollow ring below tracks the pointer for that reason and the filled circle
  appears only when the count is non-zero.

  GetTouchPointCount and GetTouchPointId are scalar, but raylib's per-index
  GetTouchPosition returns a Vector2 by value and has no binding here, so this
  reads point 0 through the scalar GetTouchX / GetTouchY pair. That is the honest
  limit: the ids of every point are visible, the coordinates of only the first."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [core] example - input multitouch")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           trail []]
      (when (rl/keep-running? deadline)
        (let [n (rl/get-touch-point-count)
              tx (rl/get-touch-x)
              ty (rl/get-touch-y)
              down? (rl/mouse-down? rl/MOUSE-LEFT)
              trail (if down?
                      (vec (take-last 90 (conj trail [tx ty])))
                      trail)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          ;; A fading breadcrumb trail, so a still screenshot still shows motion.
          (dotimes [i (count trail)]
            (let [[x y] (nth trail i)
                  a (int (* 160 (/ (double (inc i)) (count trail))))]
              (rl/circle! :x x :y y :radius 6 :color (rl/rgba 0 121 241 a))))
          ;; The ring is where touch position 0 is, pressed or not.
          (rl/circle-lines! :x tx :y ty :radius 26 :color rl/LIGHTGRAY)
          (rl/circle! :x tx :y ty :radius 3 :color rl/GRAY)
          (when (pos? n)
            (rl/circle! :x tx :y ty :radius 40 :color (rl/rgba 0 121 241 90))
            (rl/circle-lines! :x tx :y ty :radius 40 :color rl/BLUE)
            (rl/text! (str "id " (rl/get-touch-point-id 0))
                      :x (- tx 22) :y (- ty 8) :size 16 :color rl/DARKBLUE))
          (rl/text! (str n " touch " (if (= 1 n) "point" "points"))
                    :x 40 :y 30 :size 24 :color rl/DARKGRAY)
          (rl/text! "on a desktop raylib makes the mouse touch point 0 - hold and drag"
                    :x 40 :y 62 :size 14 :color rl/GRAY)
          (rl/text! (str "touch position 0: " tx ", " ty
                         "   (tracks the pointer even at 0 points)")
                    :x 40 :y 86 :size 14 :color rl/LIGHTGRAY)
          (when (> n 1)
            (rl/text! (str "other point ids: "
                           (apply str (interpose ", " (map (fn [i] (rl/get-touch-point-id i))
                                                           (range 1 n)))))
                      :x 40 :y 86 :size 14 :color rl/GRAY))
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) trail)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
