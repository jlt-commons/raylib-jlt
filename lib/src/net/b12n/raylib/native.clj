(ns net.b12n.raylib.native
  "The FFI plumbing more than one module needs: struct layouts for raylib's two
  vector types, and the two ways a Clojure value is handed to C.

  These are public because a consumer binding a raylib function this library
  does not cover needs exactly them. `staged` is the pattern every variadic
  raylib call here uses: copy a Clojure sequence into a scratch native buffer,
  call C with the pointer, free it. `vec2->ptr!` is the manual version for a
  single Vector2, and the caller frees."
  (:require
   [jolt.ffi :as ffi]))

(def vector2-layout (ffi/layout [:struct [[:x :float] [:y :float]]]))

(def vector3-layout
  (ffi/layout [:struct [[:x :float] [:y :float] [:z :float]]]))

(defn vec2->ptr!
  "Allocate a vector2-layout buffer and write [x y] into it. Caller frees."
  [[x y]]
  (let [p (ffi/alloc (ffi/layout-size vector2-layout))]
    (ffi/write-field p vector2-layout :x (double x))
    (ffi/write-field p vector2-layout :y (double y))
    p))

(defn staged
  "Copy `values` into a scratch native buffer of (* 4 (count values)) bytes,
  writing each one as a double when `write-type` is :float and as an int
  otherwise. Calls `f` with the pointer, then frees the buffer itself in a
  `finally` - `f` never frees it."
  [write-type values f]
  (let [p (ffi/alloc (* 4 (count values)))]
    (try
      (dotimes [i (count values)]
        (ffi/write p write-type
                   (if (= write-type :float)
                     (double (nth values i))
                     (int (nth values i)))
                   (* 4 i)))
      (f p)
      (finally (ffi/free p)))))
