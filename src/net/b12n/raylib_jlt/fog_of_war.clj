(ns net.b12n.raylib-jlt.fog-of-war
  "raylib [textures] example - fog of war (`jolt -M:fog-of-war`).

  Port of raylib's examples/textures/textures_fog_of_war.c. A 25x15 tile map
  under a fog that lifts where the player has been: arrow keys walk, tiles
  within two of the player go clear, and tiles walked through earlier stay at a
  dimmer remembered fog rather than going black again.

  Zero new FFI, and the trick is the same one the C uses. The fog is drawn into
  a render texture that is exactly ONE PIXEL PER TILE, 25x15, then stretched over
  the whole map on the way out. Bilinear filtering does the rest: the hard edges
  between a lit tile and a dark one come back as a smooth gradient for free,
  which is far cheaper than drawing a soft-edged sprite per tile. `render-texture`
  already sets the LINEAR filter and CLAMP wrap that needs.

  The deviation is idle motion: the player walks a patrol loop until an arrow key
  is pressed, because no synthetic input actuates a raylib window (see the note
  in scripts/demo_manifest.edn) and a fog that never lifts is a single frame."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const TILES-X 25)
(def ^:const TILES-Y 15)
(def ^:const TILE 32)
(def ^:const PLAYER 16)
(def ^:const VISIBILITY 2)
(def ^:const SPEED 5)

(defn- patrol
  "Where the player stands on frame `n` while nobody is steering: a slow loop
  around the map that reaches all four quadrants, so the fog actually lifts."
  [n]
  (let [t (* n 0.008)
        max-x (- (* TILES-X TILE) PLAYER)
        max-y (- (* TILES-Y TILE) PLAYER)]
    [(* max-x (+ 0.5 (* 0.45 (Math/sin t))))
     (* max-y (+ 0.5 (* 0.45 (Math/sin (* 1.7 t)))))]))

(defn- tile-of
  [x y]
  [(int (/ (+ x (/ TILE 2.0)) TILE))
   (int (/ (+ y (/ TILE 2.0)) TILE))])

(defn- age-fog
  "Everything currently lit drops to remembered, ready for this frame's
  visibility pass to light what the player can actually see now."
  [fog]
  (mapv (fn [v] (if (= v 1) 2 v)) fog))

(defn- light-around
  "Set the tiles within VISIBILITY of [tx ty] to fully lit."
  [fog tx ty]
  (reduce (fn [f [x y]]
            (if (and (>= x 0) (< x TILES-X) (>= y 0) (< y TILES-Y))
              (assoc f (+ x (* y TILES-X)) 1)
              f))
          fog
          (for [y (range (- ty VISIBILITY) (+ ty VISIBILITY))
                x (range (- tx VISIBILITY) (+ tx VISIBILITY))]
            [x y])))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [textures] example - fog of war"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        ;; Two random tile shades, so the map under the fog is not flat.
        tile-ids (vec (repeatedly (* TILES-X TILES-Y) (fn [] (rl/get-random-value 0 1))))
        ;; One pixel per tile. Stretching THIS is what softens the fog edges.
        fog-rt (rl/render-texture TILES-X TILES-Y)]
    (if-not fog-rt
      (binding [*out* *err*]
        (println "fog-of-war: the driver reported an incomplete framebuffer"))
      (do
        (loop [frame 0
               px 180.0
               py 130.0
               fog (vec (repeat (* TILES-X TILES-Y) 0))
               steered? false]
          (when (app/keep-running? deadline)
            (let [dx (+ (if (rl/key-down? rl/KEY-RIGHT) SPEED 0) (if (rl/key-down? rl/KEY-LEFT) (- SPEED) 0))
                  dy (+ (if (rl/key-down? rl/KEY-DOWN) SPEED 0) (if (rl/key-down? rl/KEY-UP) (- SPEED) 0))
                  steered? (or steered? (not (zero? dx)) (not (zero? dy)))
                  [px py] (if steered?
                            [(-> (+ px dx) (max 0.0) (min (double (- (* TILES-X TILE) PLAYER))))
                             (-> (+ py dy) (max 0.0) (min (double (- (* TILES-Y TILE) PLAYER))))]
                            (patrol frame))
                  [tx ty] (tile-of px py)
                  fog (-> fog age-fog (light-around tx ty))]
              ;; The fog target is filled before BeginDrawing, the way the C
              ;; does: a framebuffer switch mid-frame flushes the window batch.
              (rl/with-render-texture fog-rt
                (fn []
                  (rl/clear-background (rl/rgba 0 0 0 0))
                  (dotimes [y TILES-Y]
                    (dotimes [x TILES-X]
                      (let [v (nth fog (+ x (* y TILES-X)))]
                        (when-not (= v 1)
                          (rl/rect! {:x x
                                     :y y
                                     :width 1
                                     :height 1
                                     :color (if (zero? v)
                                              rl/BLACK
                                              (rl/rgba 0 0 0 204))})))))))
              (rl/begin-drawing)
              (rl/clear-background rl/RAYWHITE)
              (dotimes [y TILES-Y]
                (dotimes [x TILES-X]
                  (rl/rect! {:x (* x TILE)
                             :y (* y TILE)
                             :width TILE
                             :height TILE
                             :color (if (zero? (nth tile-ids (+ x (* y TILES-X))))
                                      rl/BLUE
                                      (rl/rgba 0 121 241 230))})
                  (rl/rect-lines! {:x (* x TILE)
                                   :y (* y TILE)
                                   :width TILE
                                   :height TILE
                                   :color (rl/rgba 0 82 172 128)})))
              (rl/rect! {:x px
                         :y py
                         :width PLAYER
                         :height PLAYER
                         :color rl/RED})
              ;; v0 1.0 -> v1 0.0 flips the bottom-up framebuffer texture.
              (rl/texture! (:texture fog-rt) {:x 0
                                              :y 0
                                              :width (* TILES-X TILE)
                                              :height (* TILES-Y TILE)
                                              :v0 1.0
                                              :v1 0.0})
              (rl/text! (str "current tile: [" tx "," ty "]") {:x 10
                                                               :y 10
                                                               :size 20
                                                               :color rl/RAYWHITE})
              (rl/text! (if steered? "ARROW KEYS to move" "patrolling until you press an arrow key")
                        {:x 10
                         :y (- H 25)
                         :size 20
                         :color rl/RAYWHITE})
              (app/maybe-screenshot! frame 120)
              (rl/end-drawing)
              (recur (inc frame) px py fog steered?))))
        (rl/unload-render-texture! fog-rt))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
