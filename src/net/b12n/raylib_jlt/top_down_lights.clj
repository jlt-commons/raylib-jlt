(ns net.b12n.raylib-jlt.top-down-lights
  "raylib [shapes] example - top down lights (`jolt -M:top-down-lights`).

  Port of raylib's examples/shapes/shapes_top_down_lights.c, contributed there by
  Jeffery Myers. Drag light #1 with the left mouse button, right-click to drop
  another one (16 in total), F1 shows the shadow volumes light #1 is casting.

  The trick is that nothing here draws light. Every light renders a full-screen
  mask whose ALPHA channel is the only part that matters, all the masks are
  merged into one, and that one is drawn over the scene as black. Alpha 0 reads
  as lit because the black is fully transparent there; alpha 1 reads as dark.

  Two custom blend equations do the merging, and they are why this example needs
  `rl/set-blend-factors`. GL_MIN keeps the smaller of source and destination, so
  a light's gradient punches its transparent centre into a mask cleared to opaque
  white, and the master mask ends up with the brightest of the lights that reach
  each pixel. GL_MAX keeps the larger, so a shadow quad drawn at alpha 1 cuts
  itself straight back out of the disc. Note GL_MIN and GL_MAX ignore the
  src/dst factors entirely, which is why the same SRC_ALPHA pair is passed to
  both: only the equation is doing work.

  Three deviations from the C. The shadow quads are two rlgl triangles wound
  front-facing rather than a DrawTriangleFan, whose Vector2 array is by value.
  A light standing inside a box still redraws its mask, so it goes properly
  dark; the C returns early there and leaves the previous frame's mask up. And
  light #1 walks a slow figure-eight until the first drag, because no synthetic
  input actuates a raylib window (see the note in scripts/demo_manifest.edn), so
  an example with no motion of its own records as a single frame. Touch the
  light once and it stays wherever you leave it."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MAX-LIGHTS 16)
(def ^:const N-BOXES 20)
(def ^:const TILE 64)

(defn- front-facing?
  "Whether a b c are wound the way raylib treats as front-facing, so the default
  backface culling keeps the triangle. Same cross-product test as rlgl-triangle;
  negative is front because the y axis points down."
  [[ax ay] [bx by] [cx cy]]
  (neg? (- (* (- bx ax) (- cy ay))
           (* (- by ay) (- cx ax)))))

(defn- quad!
  "A convex quad as two rlgl triangles, reversed first if its corners came out
  back-facing. The immediate-mode stand-in for DrawTriangleFan over 4 points."
  [corners color]
  (let [[a b c d] (if (front-facing? (nth corners 0) (nth corners 1) (nth corners 2))
                    corners
                    (vec (reverse corners)))]
    (rl/rl-begin rl/RL-TRIANGLES)
    (rl/rl-color! color)
    (doseq [[x y] [a b c a c d]]
      (rl/rl-vertex-2f x y))
    (rl/rl-end)))

(defn- shadow-quad
  "The shadow volume one edge casts: the edge itself, plus both endpoints pushed
  directly away from the light by twice its radius so the volume always outruns
  the lit disc."
  [[lx ly] radius [sx sy] [ex ey]]
  (let [ext (* 2.0 radius)
        away (fn [px py]
               (let [dx (- px lx)
                     dy (- py ly)
                     len (Math/sqrt (+ (* dx dx) (* dy dy)))
                     len (if (zero? len) 1.0 len)]
                 [(+ px (* ext (/ dx len)))
                  (+ py (* ext (/ dy len)))]))]
    [[sx sy] [ex ey] (away ex ey) (away sx sy)]))

(defn- box-shadows
  "Every shadow volume one box casts for a light: one per edge the light is on
  the outside of, plus the box's own footprint."
  [[lx ly :as pos] radius {:keys [x y w h]}]
  (let [tl [x y]
        tr [(+ x w) y]
        br [(+ x w) (+ y h)]
        bl [x (+ y h)]
        edges (cond-> []
                (> ly y)       (conj [tl tr])
                (< lx (+ x w)) (conj [tr br])
                (< ly (+ y h)) (conj [br bl])
                (> lx x)       (conj [bl tl]))]
    (conj (mapv (fn [[sp ep]] (shadow-quad pos radius sp ep)) edges)
          [tl bl br tr])))

(defn- overlaps?
  "CheckCollisionRecs between a box and the light's bounding square."
  [{:keys [x y w h]} [lx ly] radius]
  (and (< (- lx radius) (+ x w)) (> (+ lx radius) x)
       (< (- ly radius) (+ y h)) (> (+ ly radius) y)))

(defn- inside?
  "CheckCollisionPointRec: the light is standing in the box, so nothing escapes."
  [{:keys [x y w h]} [lx ly]]
  (and (>= lx x) (<= lx (+ x w)) (>= ly y) (<= ly (+ y h))))

(defn- draw-light-mask!
  "Render one light's alpha mask: the lit disc punched out with GL_MIN, then the
  shadows put back with GL_MAX. Both passes need the factors set BEFORE the mode
  is entered, since rlgl only re-reads them when the blend mode changes."
  [{:keys [mask pos radius valid? shadows]}]
  (rl/with-render-texture mask
    (fn []
      (rl/clear-background rl/WHITE)
      (rl/set-blend-factors rl/GL-SRC-ALPHA rl/GL-SRC-ALPHA rl/GL-MIN)
      (rl/begin-blend-mode rl/BLEND-CUSTOM)
      (when valid?
        (rl/circle-gradient! {:x (first pos)
                              :y (second pos)
                              :radius radius
                              :inner (rl/rgba 255 255 255 0)
                              :outer rl/WHITE}))
      (rl/end-blend-mode)
      (rl/set-blend-factors rl/GL-SRC-ALPHA rl/GL-SRC-ALPHA rl/GL-MAX)
      (rl/begin-blend-mode rl/BLEND-CUSTOM)
      (doseq [q shadows]
        (quad! q rl/WHITE))
      (rl/end-blend-mode))))

(defn- refresh-light
  "Recompute a light's shadow volumes against the world and redraw its mask.
  Returns the light with :dirty? cleared."
  [light boxes]
  (let [{:keys [pos radius]} light
        blocked? (some (fn [b] (inside? b pos)) boxes)
        shadows (if blocked?
                  []
                  (into [] (comp (filter (fn [b] (overlaps? b pos radius)))
                                 (mapcat (fn [b] (box-shadows pos radius b))))
                        boxes))
        light (assoc light :dirty? false :valid? (not blocked?) :shadows shadows)]
    (draw-light-mask! light)
    light))

(defn- merge-masks!
  "Build the master mask: black everywhere, with each light's alpha merged in by
  GL_MIN so the brightest light wins every pixel it touches."
  [master lights]
  (rl/with-render-texture master
    (fn []
      (rl/clear-background rl/BLACK)
      (rl/set-blend-factors rl/GL-SRC-ALPHA rl/GL-SRC-ALPHA rl/GL-MIN)
      (rl/begin-blend-mode rl/BLEND-CUSTOM)
      (doseq [{:keys [mask]} lights]
        (rl/texture! (:texture mask) {:x 0
                                      :y 0
                                      :width W
                                      :height H
                                      :v0 1.0
                                      :v1 0.0}))
      (rl/end-blend-mode))))

(defn- make-light
  "A light at x,y, with its own full-screen mask. nil if the driver refuses the
  framebuffer, which the caller drops rather than drawing with."
  [x y radius]
  (when-let [mask (rl/render-texture W H)]
    {:pos [(double x) (double y)]
     :radius (double radius)
     :mask mask
     :dirty? true
     :valid? false
     :shadows []}))

(defn- idle-path
  "Where light #1 sits on frame `n` while nobody has touched it: a Lissajous
  loop wide enough to sweep its shadows across most of the boxes."
  [n]
  (let [t (* n 0.012)]
    [(+ (/ W 2.0) (* 0.34 W (Math/sin t)))
     (+ (/ H 2.0) (* 0.30 H (Math/sin (* 2.0 t))))]))

(defn- setup-boxes
  "The world. Two boxes are placed by hand so the opening frame always has
  something casting, the rest are scattered. The C's other three fixed boxes sit
  outside an 800x450 window, so they are dropped rather than carried over."
  []
  (into [{:x 150.0
          :y 80.0
          :w 40.0
          :h 40.0}
         {:x 500.0
          :y 350.0
          :w 40.0
          :h 40.0}]
        (repeatedly (- N-BOXES 2)
                    (fn []
                      {:x (double (rl/get-random-value 0 W))
                       :y (double (rl/get-random-value 0 H))
                       :w (double (rl/get-random-value 10 100))
                       :h (double (rl/get-random-value 10 100))}))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - top down lights"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        boxes (setup-boxes)
        ;; GenImageChecked in the C. texture-from-fn leaves the wrap mode at
        ;; REPEAT, so one 64x64 tile covers the window by texcoords alone.
        ground (rl/texture-from-fn TILE TILE
                                   (fn [x y]
                                     (if (= (< x (/ TILE 2)) (< y (/ TILE 2)))
                                       rl/DARKBROWN
                                       rl/DARKGRAY)))
        master (rl/render-texture W H)]
    (if-not master
      (binding [*out* *err*]
        (println "top-down-lights: the driver reported an incomplete framebuffer"))
      (let [remaining
            (loop [frame 0
                   lights (vec (keep identity [(make-light 600 400 300)]))
                   show-lines? false
                   steered? false]
              (if-not (app/keep-running? deadline)
                lights
                (let [mx (rl/get-mouse-x)
                      my (rl/get-mouse-y)
                      dragging? (and (seq lights) (rl/mouse-down? rl/MOUSE-LEFT))
                      steered? (or steered? dragging?)
                      lights (cond
                               dragging?
                               (update lights 0 assoc :pos [(double mx) (double my)] :dirty? true)

                               (and (not steered?) (seq lights))
                               (update lights 0 assoc :pos (idle-path frame) :dirty? true)

                               :else lights)
                      lights (if (and (rl/mouse-pressed? rl/MOUSE-RIGHT)
                                      (< (count lights) MAX-LIGHTS))
                               (if-let [l (make-light mx my 200)]
                                 (conj lights l)
                                 lights)
                               lights)
                      show-lines? (if (rl/key-pressed? rl/KEY-F1)
                                    (not show-lines?)
                                    show-lines?)
                      any-dirty? (boolean (some :dirty? lights))
                      lights (mapv (fn [l] (if (:dirty? l) (refresh-light l boxes) l)) lights)
                      lead (first lights)]
                  ;; Masks are rendered before BeginDrawing, the way the C does:
                  ;; a framebuffer switch mid-frame would flush the window batch.
                  (when any-dirty?
                    (merge-masks! master lights))
                  (rl/begin-drawing)
                  (rl/clear-background rl/BLACK)
                  (rl/texture! ground {:x 0
                                       :y 0
                                       :width W
                                       :height H
                                       :u1 (/ (double W) TILE)
                                       :v1 (/ (double H) TILE)})
                  (rl/texture! (:texture master) {:x 0
                                                  :y 0
                                                  :width W
                                                  :height H
                                                  :v0 1.0
                                                  :v1 0.0
                                                  :tint (if show-lines?
                                                          (rl/rgba 255 255 255 191)
                                                          rl/WHITE)})
                  (doseq [[i {:keys [pos]}] (map-indexed vector lights)]
                    (rl/circle! {:x (first pos)
                                 :y (second pos)
                                 :radius 10
                                 :color (if (zero? i) rl/YELLOW rl/WHITE)}))
                  (when (and show-lines? lead)
                    (doseq [q (:shadows lead)]
                      (quad! q rl/DARKPURPLE))
                    (doseq [{:keys [x y w h]
                             :as b} boxes]
                      (when (overlaps? b (:pos lead) (:radius lead))
                        (rl/rect! {:x x
                                   :y y
                                   :width w
                                   :height h
                                   :color rl/PURPLE}))
                      (rl/rect-lines! {:x x
                                       :y y
                                       :width w
                                       :height h
                                       :color rl/DARKBLUE})))
                  (rl/text! (if show-lines?
                              "(F1) hide shadow volumes"
                              "(F1) show shadow volumes")
                            {:x 10
                             :y 50
                             :size 10
                             :color rl/GREEN})
                  (rl/text! "drag to move light #1" {:x 10
                                                     :y 10
                                                     :size 10
                                                     :color rl/DARKGREEN})
                  (rl/text! "right click to add a new light" {:x 10
                                                              :y 30
                                                              :size 10
                                                              :color rl/DARKGREEN})
                  (rl/fps! {:x (- W 80)
                            :y 10})
                  (app/maybe-screenshot! frame 20)
                  (rl/end-drawing)
                  (recur (inc frame) lights show-lines? steered?))))]
        (doseq [{:keys [mask]} remaining]
          (rl/unload-render-texture! mask))
        (rl/unload-render-texture! master)
        (rl/unload-texture! ground))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
