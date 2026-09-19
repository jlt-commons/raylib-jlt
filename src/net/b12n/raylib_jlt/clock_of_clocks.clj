(ns net.b12n.raylib-jlt.clock-of-clocks
  "raylib [shapes] example - clock of clocks (`jolt -M:clock-of-clocks`).

  Port of raylib's examples/shapes/shapes_clock_of_clocks.c. Six digits, each a
  4x6 grid of little clock faces, whose 48 hands swing into place to spell the
  time. Every digit is a lookup of 24 hand-angle pairs, and a second's tick lerps
  from wherever the hands are to wherever the next digit wants them.

  raylib draws each hand with DrawRectanglePro, a rotated rectangle. Nothing here
  binds that, because it takes a Rectangle and a Vector2 by value, both of which
  the pointer trick does not cover. A hand is a thick line from the centre
  outward, so line-ex! draws the same thing from the same angle.

  Angles are raylib's screen convention: 0 points right, and positive turns
  clockwise because y grows downward. So TL, the top-left corner of a digit
  outline, is {0, 90}: one hand right, one hand down, meeting its neighbours.

  See analog-clock for the same idea reduced to one large face."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const FACE 24.0)        ; diameter of one little clock
(def ^:const GAP 8.0)          ; between faces
(def ^:const SECTION 16.0)     ; between digits
(def ^:const MOVE-SECS 0.5)    ; how long the hands take to swing

;; The six hand pairs a cell can hold. Each is [hand-1-deg hand-2-deg].
(def ^:private TL [0.0 90.0])    ; corner opening right and down
(def ^:private TR [90.0 180.0])  ; down and left
(def ^:private BR [180.0 270.0]) ; left and up
(def ^:private BL [0.0 270.0])   ; right and up
(def ^:private HH [0.0 180.0])   ; horizontal bar
(def ^:private VV [90.0 270.0])  ; vertical bar
(def ^:private ZZ [135.0 135.0]) ; blank: both hands together, pointing nowhere

;; 10 digits, each 24 cells read left-to-right then top-to-bottom, 4 wide by 6
;; tall. Transcribed from the C table.
(def ^:private digit-angles
  [[TL HH HH TR, VV TL TR VV, VV VV VV VV, VV VV VV VV, VV BL BR VV, BL HH HH BR]  ; 0
   [TL HH TR ZZ, BL TR VV ZZ, ZZ VV VV ZZ, ZZ VV VV ZZ, TL BR BL TR, BL HH HH BR]  ; 1
   [TL HH HH TR, BL HH TR VV, TL HH BR VV, VV TL HH BR, VV BL HH TR, BL HH HH BR]  ; 2
   [TL HH HH TR, BL HH TR VV, TL HH BR VV, BL HH TR VV, TL HH BR VV, BL HH HH BR]  ; 3
   [TL TR TL TR, VV VV VV VV, VV BL BR VV, BL HH TR VV, ZZ ZZ VV VV, ZZ ZZ BL BR]  ; 4
   [TL HH HH TR, VV TL HH BR, VV BL HH TR, BL HH TR VV, TL HH BR VV, BL HH HH BR]  ; 5
   [TL HH HH TR, VV TL HH BR, VV BL HH TR, VV TL TR VV, VV BL BR VV, BL HH HH BR]  ; 6
   [TL HH HH TR, BL HH TR VV, ZZ ZZ VV VV, ZZ ZZ VV VV, ZZ ZZ VV VV, ZZ ZZ BL BR]  ; 7
   [TL HH HH TR, VV TL TR VV, VV BL BR VV, VV TL TR VV, VV BL BR VV, BL HH HH BR]  ; 8
   [TL HH HH TR, VV TL TR VV, VV BL BR VV, BL HH TR VV, TL HH BR VV, BL HH HH BR]]) ; 9

(defn- lerp [a b t] (+ a (* (- b a) t)))

(defn- shortest-from
  "Sets the source angle so the hand sweeps forward into the target rather than
  winding backwards. raylib does this by subtracting a full turn from any source
  that leads its destination."
  [src dst]
  (if (> src dst) (- src 360.0) src))

(defn- digits-now
  "The six digits of the current time, most significant first."
  [hour-mode]
  (let [[h m s] (rl/local-time)
        h (mod h hour-mode)]
    (mapv (fn [^long n] n)
          [(quot h 10) (mod h 10) (quot m 10) (mod m 10) (quot s 10) (mod s 10)])))

(defn- target-angles
  "Every cell's destination pair for a set of digits. A leading zero in 12-hour
  mode blanks its digit, which is what the C does with the ZZ pair."
  [digits hour-mode]
  (vec (for [[idx d] (map-indexed vector digits)]
         (if (and (zero? idx) (= hour-mode 12) (zero? d))
           (vec (repeat 24 ZZ))
           (nth digit-angles d)))))

(defn- hand!
  [cx cy deg len color]
  (let [t (Math/toRadians deg)]
    (rl/line-ex! {:x1 cx
                  :y1 cy
                  :x2 (+ cx (* len (Math/cos t)))
                  :y2 (+ cy (* len (Math/sin t)))
                  :thick 4
                  :color color})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shapes] example - clock of clocks"})
  (rl/set-target-fps 60)
  (let [deadline    (app/auto-quit-deadline)
        bg          (rl/rgba 8 12 28 255)
        hands-color (rl/rgba 255 249 196 255)
        bezel       (rl/rgba 80 80 80 255)
        step        (+ FACE GAP)]
    (loop [frame     0
           prev-sec  -1
           hour-mode 24
           current   (vec (repeat 6 (vec (repeat 24 ZZ))))
           src       (vec (repeat 6 (vec (repeat 24 ZZ))))
           dst       (vec (repeat 6 (vec (repeat 24 ZZ))))
           timer     0.0]
      (when (app/keep-running? deadline)
        (let [hour-mode (if (rl/key-pressed? rl/KEY-SPACE)
                          (if (= hour-mode 24) 12 24)
                          hour-mode)
              [_ _ sec] (rl/local-time)
              ticked?   (not= sec prev-sec)
              digits    (digits-now hour-mode)
              new-dst   (if ticked? (target-angles digits hour-mode) dst)
              new-src   (if ticked?
                          (vec (map (fn [cells targets]
                                      (vec (map (fn [[a b] [ta tb]]
                                                  [(shortest-from a ta) (shortest-from b tb)])
                                                cells targets)))
                                    current new-dst))
                          src)
              timer     (if ticked? 0.0 (+ timer (rl/get-frame-time)))
              t         (min 1.0 (/ timer MOVE-SECS))
              current   (vec (map (fn [ss dd]
                                    (vec (map (fn [[sa sb] [da db]]
                                                [(lerp sa da t) (lerp sb db t)])
                                              ss dd)))
                                  new-src new-dst))]
          (rl/begin-drawing)
          (rl/clear-background bg)
          (rl/text! (str hour-mode "-h mode, SPACE to change")
                    {:x 10
                     :y 30
                     :size 20
                     :color rl/RAYWHITE})
          ;; x-offset walks left to right, gaining a colon and sectionSpacing
          ;; after each odd digit, so hh:mm:ss groups apart. Carried through the
          ;; loop rather than computed per digit, because the colon makes the
          ;; stride uneven.
          (loop [digit 0 x-offset 4.0]
            (when (< digit 6)
              (dotimes [row 6]
                (dotimes [col 4]
                  (let [cx (+ x-offset (* col step) (* FACE 0.5))
                        cy (+ 100.0 (* row step) (* FACE 0.5))
                        [a b] (get-in current [digit (+ (* row 4) col)])]
                    (rl/ring! {:cx cx
                               :cy cy
                               :inner (- (* FACE 0.5) 2.0)
                               :outer (* FACE 0.5)
                               :start-deg 0
                               :end-deg 360
                               :segments 24
                               :color bezel})
                    (hand! cx cy a (+ (* FACE 0.5) 2.0) hands-color)
                    (hand! cx cy b (* FACE 0.5) hands-color))))
              (let [x (+ x-offset (* step 4))]
                (if (odd? digit)
                  (do (rl/ring! {:cx (+ x 4.0)
                                 :cy 160.0
                                 :inner 6.0
                                 :outer 8.0
                                 :start-deg 0
                                 :end-deg 360
                                 :segments 24
                                 :color hands-color})
                      (rl/ring! {:cx (+ x 4.0)
                                 :cy 225.0
                                 :inner 6.0
                                 :outer 8.0
                                 :start-deg 0
                                 :end-deg 360
                                 :segments 24
                                 :color hands-color})
                      (recur (inc digit) (+ x SECTION)))
                  (recur (inc digit) x)))))
          (app/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame) sec hour-mode current new-src new-dst timer)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
