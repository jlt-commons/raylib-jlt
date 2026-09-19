(ns net.b12n.raylib.native
  "The FFI plumbing more than one module needs: struct layouts for raylib's two
  vector types and its Texture2D descriptor, the rlgl/raylib pixel-format
  constants that get staged into that descriptor, and the two ways a Clojure
  value is handed to C.

  These are public because a consumer binding a raylib function this library
  does not cover needs exactly them. `staged` is the pattern every variadic
  raylib call here uses: copy a Clojure sequence into a scratch native buffer,
  call C with the pointer, free it. `vec2->ptr!` is the manual version for a
  single Vector2, and the caller frees."
  (:require
   [jolt.ffi :as ffi]))

(def vector2-layout
  "The `ffi/layout` value for raylib's Vector2: {float x, y}."
  (ffi/layout [:struct [[:x :float] [:y :float]]]))

(def vector3-layout
  "The `ffi/layout` value for raylib's Vector3: {float x, y, z}."
  (ffi/layout [:struct [[:x :float] [:y :float] [:z :float]]]))

(def rectangle-layout
  "The `ffi/layout` value for raylib's Rectangle: {float x, y, width, height}.
  Sixteen bytes, so on arm64 it travels in registers rather than indirectly."
  (ffi/layout [:struct [[:x :float] [:y :float]
                        [:width :float] [:height :float]]]))

(def texture2d-layout
  "The `ffi/layout` value for raylib's Texture2D: {uint id; int width, height,
  mipmaps, format;}, 20 bytes. Shared by net.b12n.raylib.textures (upload),
  net.b12n.raylib.images (the LoadTextureFromImage/LoadImageFromTexture
  bridge) and net.b12n.raylib.shaders (SetShaderValueTexture) -- three
  consumers, none of which may depend on either of the other two, so it
  lives on this dependency-free leaf instead."
  (ffi/layout [:struct [[:id :uint] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]))

(assert (= 20 (ffi/layout-size texture2d-layout)) "Texture2D is five 4-byte fields")

;; rlPixelFormat/PixelFormat enum values, not a struct layout, but the same
;; multi-consumer shape as texture2d-layout above (textures, images, and
;; net.b12n.raylib.shaders all read PIXELFORMAT-R8G8B8A8; nothing else in
;; this file reads PIXELFORMAT-R8G8B8, but the two are a sibling pair from
;; the same raylib enum and splitting them across two modules would be
;; worse than a home neither strictly needs). ^:const to
;; match every other raylib/rlgl enum constant in this library (RL-QUADS,
;; the KEY-*/MOUSE-* families, FLAG-*), a deliberate choice rather than an
;; accident of moving through an alias -- this file's existing vector2-layout/
;; vector3-layout are plain `def` because they're structured data, not a
;; single scalar constant.
(def ^:const PIXELFORMAT-R8G8B8A8 7)          ; rlPixelFormat, 32bpp RGBA
(def ^:const PIXELFORMAT-R8G8B8 4)            ; 24bpp, no alpha channel at all

(defn vec2->ptr!
  "Allocate a vector2-layout buffer and write [x y] into it. Caller frees."
  [[x y]]
  (let [p (ffi/alloc (ffi/layout-size vector2-layout))]
    (ffi/write-field p vector2-layout :x (double x))
    (ffi/write-field p vector2-layout :y (double y))
    p))

(defn rect->ptr!
  "Allocate a rectangle-layout buffer and write [x y width height] into it.
  Caller frees. Sibling of vec2->ptr!, for the several raylib calls that take a
  Rectangle by value."
  [[x y width height]]
  (let [p (ffi/alloc (ffi/layout-size rectangle-layout))]
    (ffi/write-field p rectangle-layout :x (double x))
    (ffi/write-field p rectangle-layout :y (double y))
    (ffi/write-field p rectangle-layout :width (double width))
    (ffi/write-field p rectangle-layout :height (double height))
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
