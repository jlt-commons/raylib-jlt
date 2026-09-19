(ns net.b12n.raylib-jlt.camera-2d-platformer
  "raylib [core] example - 2d camera platformer (`jolt -M:camera-2d-platformer`).

  Port of raylib's examples/core/core_2d_camera_platformer.c. Arrows walk, SPACE
  jumps, C cycles five ways a camera can follow a player and R puts everything
  back.

  The five modes are the example. Watching the same jump under each one is the
  fastest way to feel why a platformer's camera is a design decision rather than
  a formula:

  - centre: target the player exactly, so the world lurches with every hop.
  - clamped: the same, but never past the map edges, so the level's border stays
    put instead of revealing emptiness.
  - smooth: chase the player at a speed proportional to the distance, so the
    camera lags and catches up.
  - even-out: follow horizontally at once but ease vertically only after landing,
    which stops a jump from moving the view at all.
  - bounds push: hold still until the player reaches a margin, then shove.

  Two upstream helpers are not bound, and both fall out of arithmetic already
  here. GetWorldToScreen2D is the camera transform, used by the clamped mode to
  ask where a map corner has landed on screen. Vector2Length and friends are
  ordinary maths.

  Collision is deliberately the C's: a one-way platform test that only catches a
  player falling THROUGH the top edge this frame. Walking into a platform's side
  does nothing, which is what makes the level feel like a platformer rather than
  a maze. See camera-2d for the simpler follow, and camera-2d-mouse-zoom for
  zoom pinned to the cursor."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const GRAVITY 400.0)
(def ^:const JUMP-SPEED 350.0)
(def ^:const WALK-SPEED 200.0)

;; [x y w h blocking? [r g b]]
(def ^:private env-items
  [[0.0    0.0   1000.0 400.0 false [200 200 200]]
   [0.0  400.0   1000.0 200.0 true  [130 130 130]]
   [300.0 200.0   400.0  10.0 true  [130 130 130]]
   [250.0 300.0   100.0  10.0 true  [130 130 130]]
   [650.0 300.0   100.0  10.0 true  [130 130 130]]])

(def ^:private modes
  [[:centre     "follow player centre"]
   [:clamped    "follow centre, clamped to the map edges"]
   [:smooth     "follow centre, smoothed"]
   [:even-out   "follow horizontally, ease vertically after landing"]
   [:push       "player pushes the camera at the screen edge"]])

(defn- world->screen
  "GetWorldToScreen2D, for an unrotated camera."
  [wx wy {:keys [offset-x offset-y target-x target-y zoom]}]
  [(+ (* (- wx target-x) zoom) offset-x)
   (+ (* (- wy target-y) zoom) offset-y)])

(defn- update-player
  "The C's physics exactly, including the one-way platform rule: a blocking item
  stops the player only when the fall would cross its top edge this frame."
  [{:keys [x y speed]
    :as p} dt]
  (let [x (cond-> x
            (rl/key-down? rl/KEY-LEFT)  (- (* WALK-SPEED dt))
            (rl/key-down? rl/KEY-RIGHT) (+ (* WALK-SPEED dt)))
        [speed jumped?] (if (and (rl/key-down? rl/KEY-SPACE) (:can-jump? p))
                          [(- JUMP-SPEED) true]
                          [speed false])
        hit (some (fn [[ex ey ew _ blocking? _]]
                    (when (and blocking?
                               (<= ex x) (>= (+ ex ew) x)
                               (>= ey y) (<= ey (+ y (* speed dt))))
                      ey))
                  env-items)]
    (if (and hit (not jumped?))
      (assoc p :x x :y hit :speed 0.0 :can-jump? true)
      (assoc p :x x
             :y (+ y (* speed dt))
             :speed (+ speed (* GRAVITY dt))
             :can-jump? false))))

(defn- update-camera
  [mode cam {:keys [x y]} dt]
  (let [half-w (/ (double W) 2.0)
        half-h (/ (double H) 2.0)
        cam    (assoc cam :offset-x half-w :offset-y half-h)]
    (case mode
      :centre (assoc cam :target-x x :target-y y)

      :clamped
      (let [c (assoc cam :target-x x :target-y y)
            xs (mapcat (fn [[ex ey ew eh _ _]] [[ex ey] [(+ ex ew) (+ ey eh)]]) env-items)
            min-x (apply min (map first xs))  max-x (apply max (map first xs))
            min-y (apply min (map second xs)) max-y (apply max (map second xs))
            [sx-max sy-max] (world->screen max-x max-y c)
            [sx-min sy-min] (world->screen min-x min-y c)]
        (cond-> c
          (< sx-max W) (assoc :offset-x (- W (- sx-max half-w)))
          (< sy-max H) (assoc :offset-y (- H (- sy-max half-h)))
          (> sx-min 0) (assoc :offset-x (- half-w sx-min))
          (> sy-min 0) (assoc :offset-y (- half-h sy-min))))

      :smooth
      ;; Move a fraction of the remaining distance per second, with a floor so
      ;; the last few pixels do not crawl.
      (let [dx (- x (:target-x cam))
            dy (- y (:target-y cam))
            len (Math/sqrt (+ (* dx dx) (* dy dy)))]
        (if (> len 10.0)
          (let [speed (max 30.0 (* 0.8 len))
                k (/ (* speed dt) len)]
            (-> cam (update :target-x + (* dx k)) (update :target-y + (* dy k))))
          cam))

      :even-out
      ;; Horizontal is immediate; vertical only closes once the player is on
      ;; something, so a jump does not move the view.
      (let [cam (assoc cam :target-x x)
            dy  (- y (:target-y cam))]
        (if (> (Math/abs dy) 1.0)
          (update cam :target-y + (* dy (min 1.0 (* 4.0 dt))))
          (assoc cam :target-y y)))

      :push
      ;; A dead zone: the camera only moves once the player leaves the middle.
      (let [bx 0.2 by 0.2
            [sx sy] (world->screen x y cam)
            left (* W bx) right (* W (- 1.0 bx))
            top (* H by) bottom (* H (- 1.0 by))]
        (cond-> cam
          (< sx left)   (update :target-x + (/ (- sx left) (:zoom cam)))
          (> sx right)  (update :target-x + (/ (- sx right) (:zoom cam)))
          (< sy top)    (update :target-y + (/ (- sy top) (:zoom cam)))
          (> sy bottom) (update :target-y + (/ (- sy bottom) (:zoom cam)))))

      cam)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - 2d camera platformer"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        start    {:x 400.0
                  :y 280.0
                  :speed 0.0
                  :can-jump? false}
        start-cam {:offset-x (/ (double W) 2.0)
                   :offset-y (/ (double H) 2.0)
                   :target-x 400.0
                   :target-y 280.0
                   :zoom 1.0}]
    (loop [frame 0
           player start
           cam start-cam
           mode-idx 0]
      (when (app/keep-running? deadline)
        (let [dt       (rl/get-frame-time)
              reset?   (rl/key-pressed? rl/KEY-R)
              mode-idx (if (rl/key-pressed? rl/KEY-C) (mod (inc mode-idx) (count modes)) mode-idx)
              player   (if reset? start (update-player player dt))
              cam      (if reset? start-cam (update-camera (first (nth modes mode-idx)) cam player dt))]
          (rl/begin-drawing)
          (rl/clear-background rl/LIGHTGRAY)
          (rl/with-camera-2d cam
            (fn []
              (doseq [[ex ey ew eh _ [r g b]] env-items]
                (rl/rect! {:x ex
                           :y ey
                           :width ew
                           :height eh
                           :color (rl/rgba r g b 255)}))
              ;; The player is a 40x40 square standing ON its position, which is
              ;; why the collision test compares against the item's top edge.
              (rl/rect! {:x (- (:x player) 20)
                         :y (- (:y player) 40)
                         :width 40
                         :height 40
                         :color rl/RED})))
          (rl/text! "[ARROWS] walk  [SPACE] jump  [C] camera mode  [R] reset"
                    {:x 20
                     :y 20
                     :size 20
                     :color rl/BLACK})
          (rl/text! (str "mode " (inc mode-idx) "/" (count modes) ": "
                         (second (nth modes mode-idx)))
                    {:x 20
                     :y 46
                     :size 20
                     :color rl/DARKGRAY})
          (app/maybe-screenshot! frame 90)
          (rl/end-drawing)
          (recur (inc frame) player cam mode-idx)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
