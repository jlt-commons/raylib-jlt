(ns net.b12n.raylib-jlt.reasings
  "raylib's easing functions, the Clojure counterpart of examples/shapes/reasings.h.

  Shared rather than copied per example, because the upstream examples share
  reasings.h the same way. This namespace has no `-main`, which is how
  bb check:registration tells the binding layer and its helpers apart from
  examples: a runnable source with no registry row is a gap, a helper is not.

  Every function keeps raylib's four-argument shape:

      (ease t b c d)  ->  b + c * f(t/d)

  t is elapsed time, b the value at the start, c the TOTAL CHANGE rather than the
  end value, and d the duration. Normalising to [0,1] would read more naturally
  in Clojure and is deliberately not done, because passing a negative c is how
  the upstream examples shrink rather than grow, and keeping the signature lets a
  ported call site sit beside the C line it came from.

  t is clamped to d, so a counter that overruns its duration holds at the end
  value instead of continuing past it. The C relies on its callers to stop
  incrementing; clamping here makes each function total.

  Named for the header rather than for Clojure, so the correspondence is obvious.
  See easings for the curve family drawn side by side.")

(defn- norm
  "t/d clamped to [0,1]."
  ^double [t d]
  (let [d (double d)]
    (if (zero? d) 1.0 (min 1.0 (max 0.0 (/ (double t) d))))))

(defn linear [t b c d] (+ b (* c (norm t d))))

(defn cubic-in   [t b c d] (let [x (norm t d)] (+ b (* c x x x))))
(defn cubic-out  [t b c d] (let [x (- (norm t d) 1.0)] (+ b (* c (inc (* x x x))))))
(defn cubic-in-out
  [t b c d]
  (let [x (* 2.0 (norm t d))]
    (if (< x 1.0)
      (+ b (* (/ c 2.0) x x x))
      (let [x (- x 2.0)] (+ b (* (/ c 2.0) (+ (* x x x) 2.0)))))))

(defn quad-in  [t b c d] (let [x (norm t d)] (+ b (* c x x))))
(defn quad-out [t b c d] (let [x (norm t d)] (- b (* c x (- x 2.0)))))

(defn circ-in  [t b c d] (let [x (norm t d)] (+ b (* c (- 1.0 (Math/sqrt (- 1.0 (* x x))))))))
(defn circ-out [t b c d] (let [x (- (norm t d) 1.0)] (+ b (* c (Math/sqrt (- 1.0 (* x x)))))))

(defn elastic-in
  [t b c d]
  (let [x (norm t d)]
    (cond
      (zero? x) b
      (>= x 1.0) (+ b c)
      :else (let [p (* 0.3 1.0)
                  s (/ p 4.0)
                  x (- x 1.0)]
              (- b (* c (Math/pow 2.0 (* 10.0 x))
                      (Math/sin (/ (* (- x s) 2.0 Math/PI) p))))))))

(defn elastic-out
  [t b c d]
  (let [x (norm t d)]
    (cond
      (zero? x) b
      (>= x 1.0) (+ b c)
      :else (let [p (* 0.3 1.0)
                  s (/ p 4.0)]
              (+ b c (* c (Math/pow 2.0 (* -10.0 x))
                        (Math/sin (/ (* (- x s) 2.0 Math/PI) p))))))))

(defn bounce-out
  [t b c d]
  (let [x (norm t d)]
    (+ b (* c (cond
                (< x (/ 1.0 2.75))  (* 7.5625 x x)
                (< x (/ 2.0 2.75))  (let [x (- x (/ 1.5 2.75))] (+ (* 7.5625 x x) 0.75))
                (< x (/ 2.5 2.75))  (let [x (- x (/ 2.25 2.75))] (+ (* 7.5625 x x) 0.9375))
                :else               (let [x (- x (/ 2.625 2.75))] (+ (* 7.5625 x x) 0.984375)))))))

(defn bounce-in [t b c d] (+ b c (- (bounce-out (- d t) 0 c d))))

(defn sine-in    [t b c d] (+ b (* c (- 1.0 (Math/cos (* (norm t d) (/ Math/PI 2.0)))))))
(defn sine-out   [t b c d] (+ b (* c (Math/sin (* (norm t d) (/ Math/PI 2.0))))))

(defn back-out
  [t b c d]
  (let [s 1.70158
        x (- (norm t d) 1.0)]
    (+ b (* c (inc (+ (* x x (+ (* (inc s) x) s))))))))

(def by-name
  "Every curve, for an example that wants to show them side by side."
  {"linear" linear
   "quad-in" quad-in
   "quad-out" quad-out
   "sine-in" sine-in
   "sine-out" sine-out
   "cubic-in" cubic-in
   "cubic-out" cubic-out
   "cubic-in-out" cubic-in-out
   "circ-in" circ-in
   "circ-out" circ-out
   "back-out" back-out
   "elastic-in" elastic-in
   "elastic-out" elastic-out
   "bounce-in" bounce-in
   "bounce-out" bounce-out})
