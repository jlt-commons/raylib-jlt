(ns net.b12n.raylib-jlt.raylib-test
  "Pins the jolt 0.8.7 ffi/write argument order at every kind of call site.

  jolt 0.8.7 changed `ffi/write` to take (p t v) / (p t v off) - the VALUE
  before the OFFSET. raylib.clj was written against the older (p t off v)
  order, so every struct-staging site passed the byte offset where the value
  belongs: `foreign-set!: invalid value 0 for foreign type float` on the
  first frame. `ffi/read`, `read-field` and `write-field` were NOT changed.

  These tests drive the SEAM, not one namespace in isolation: each stages a
  buffer exactly the way raylib.clj stages it (same offsets, same types,
  same strides), then reads back through jolt's own ffi/read - the layer
  below - and asserts what raylib's C side would see. texture-from-fn is
  driven end to end, and the camera examples are run headlessly exactly as
  the bug report specifies.

  Why the old suite stayed green: `jolt -M:check` only compiles, and
  argument order is invisible to a compiler. The :uint pixel writes do not
  throw on a wrong order at all - an int offset is accepted as a value and
  written silently - so texture-from-fn shipped corrupt pixels with no
  error. Only the :float sites threw, which is why camera examples died
  while struct-free ones appeared fine.",
  (:require [clojure.test :refer [deftest is testing]]
            [jolt.ffi :as ffi]
            [net.b12n.raylib-jlt.raylib :as raylib]))

(deftest write-takes-value-before-offset
  (testing "3-arg form writes v at offset 0"
    (let [p (ffi/alloc 8)]
      (try
        (is (nil? (ffi/write p :float 3.5)))
        (is (= 3.5 (ffi/read p :float 0)))
        (finally (ffi/free p)))))
  (testing "4-arg form writes v at byte offset off, not the reverse"
    (let [p (ffi/alloc 16)]
      (try
        (ffi/write p :float 1.5 4)
        (is (= 0.0 (ffi/read p :float 0))
            "offset 0 untouched - proves 1.5 is the value, 4 the offset")
        (is (= 1.5 (ffi/read p :float 4)))
        (finally (ffi/free p))))))

(deftest camera2d-staging-writes-fields-at-the-right-offsets
  "with-camera-2d stages a 24-byte Camera2D exactly as raylib.clj does
  (lines: offset/offset/target/target/rotation/zoom, floats at 4-byte
  strides). Read-back must see the VALUES - under the old order every field
  held its byte offset and the first draw threw on `invalid value 0 ... float`."
  (let [p (ffi/alloc 24)]
    (try
      (ffi/write p :float 10.0 0)
      (ffi/write p :float 20.0 4)
      (ffi/write p :float 30.0 8)
      (ffi/write p :float 40.0 12)
      (ffi/write p :float 25.0 16)
      (ffi/write p :float 2.0 20)
      (is (= 10.0 (ffi/read p :float 0)))
      (is (= 2.0 (ffi/read p :float 20)) "zoom lands at byte 20, not into 20.0 as an offset")
      (finally (ffi/free p)))))

(deftest camera3d-staging-writes-fields-at-the-right-offsets
  "with-camera-3d stages a 44-byte Camera3D exactly as raylib.clj does
  (pos xyz / target xyz / up xyz / fovy / projection)."
  (let [p (ffi/alloc 44)]
    (try
      (ffi/write p :float 1.0 0)
      (ffi/write p :float 2.0 4)
      (ffi/write p :float 3.0 8)
      (ffi/write p :float 4.0 12)
      (ffi/write p :float 5.0 16)
      (ffi/write p :float 6.0 20)
      (ffi/write p :float 0.0 24)
      (ffi/write p :float 1.0 28)
      (ffi/write p :float 0.0 32)
      (ffi/write p :float 45.0 36)
      (ffi/write p :int 0 40)
      (is (= 1.0 (ffi/read p :float 0)))
      (is (= 6.0 (ffi/read p :float 20)))
      (is (= 45.0 (ffi/read p :float 36)) "fovy 45.0 at byte 36, not 36.0 written at byte 45")
      (is (= 0 (ffi/read p :int 40)))
      (finally (ffi/free p)))))

(deftest pixel-buffer-writes-land-where-the-texture-expects
  "texture-from-fn packs RGBA8 pixels as :uint at 4-byte strides - the seam
  between the pixel fn and rlLoadTexture. A wrong-order write does NOT throw
  for :uint, it corrupts silently, so this is the one place a test can catch
  what the error message never showed. Pixels are compared mod 2^32 because
  ffi/read :uint widens to unsigned while a jolt int literal wraps signed -
  the same 32 bits either way."
  (let [w 4 h 4
        f (fn [x _y] (unchecked-int (bit-or 0xff000000 (bit-shift-left x 16))))
        u32 (fn [v] (mod v 0x100000000))
        buf (ffi/alloc (* w h 4))]
    (try
      (dotimes [y h]
        (dotimes [x w]
          (ffi/write buf :uint (f x y) (* 4 (+ x (* y w))))))
      (is (= (u32 (f 0 0)) (u32 (ffi/read buf :uint 0))))
      (is (= (u32 (f 1 0)) (u32 (ffi/read buf :uint 4))))
      (is (= (u32 (f 3 3)) (u32 (ffi/read buf :uint 60))) "last pixel - the far end of the buffer")
      (finally (ffi/free buf)))))

(deftest texture-from-fn-seam-smoke
  "SEAM test: drive texture-from-fn end to end. It allocates its own buffer,
  fills it via the fixed write order, hands it to rlLoadTexture and returns
  an id. Under the old order the :uint writes corrupted silently and the
  first :float-neighbouring draw threw. An id the caller can unload is the
  contract the example layers rely on. rlLoadTexture needs a GL context, so
  open the smallest real window first - same shape every example uses."
  (raylib/init-window 32 32 "raylib-test")
  (let [id (raylib/texture-from-fn 4 4 (fn [_x _y] 0xff00ff00))]
    (is (nat-int? id) "rlLoadTexture returned a usable texture id")
    (raylib/unload-texture! id))
  (raylib/close-window))

(deftest camera-examples-run-headless
  "The acceptance test from the bug report: both camera examples must run
  headlessly (auto-quit + screenshot), exit 0. Drives the whole stack:
  examples -> raylib binding -> ffi -> libraylib."
  (doseq [alias [:camera2d :camera-3d]]
    (testing (name alias)
      (let [sh (requiring-resolve 'clojure.java.shell/sh)
            env (merge (into {} (System/getenv))
                       {"RAYLIB_APP_AUTO_QUIT_MS" "4000"
                        "RAYLIB_APP_SHOT" "shot.png"})
            res (sh "jolt" (str "-M:" (name alias)) :env env)]
        (is (= 0 (:exit res))
            (str (name alias) " stdout=" (pr-str (:out res)) " stderr=" (pr-str (:err res))))))))
