(ns net.b12n.raylib.ffi-write-test
  "The one property every binding in this project silently depends on:
  `jolt.ffi/write` puts the VALUE where the OFFSET says.

  jolt 0.8.0 (jolt-lang/jolt#802) swapped `ffi/write`'s last two arguments,
  from `(write p type offset value)` to `(write p type value offset)`. Both
  are plain integers, so a call written for one order does not fail on the
  other. It writes to the wrong address and reports nothing.

  Every 4-argument `ffi/write` in this project is written in the post-#802
  order. Until this file existed nothing here executed one during CI: the gate
  compiles the examples and never runs them, so the whole suite passed against
  a jolt whose `ffi/write` took its arguments the other way round. A gate that
  cannot go red is worse than no gate, because it reads as coverage.

  The test itself requires only `jolt.ffi`, never the bindings, so it opens no
  window and needs no display. It is not native-free, though: jolt loads this
  project's `:jolt/native` entries when it resolves the project, before any
  alias runs, so the system libraylib must be present for the suite to
  start at all. That is a property of the project, not of these assertions."
  (:require [clojure.test :refer [deftest is testing]]
            [jolt.ffi :as ffi]))

(deftest write-puts-the-value-at-the-offset
  (testing "integers land at the offset, not the other way round"
    (let [p (ffi/alloc 32)]
      (ffi/write p :int 111 0)
      (ffi/write p :int 222 4)
      (ffi/write p :int 333 8)
      (is (= 111 (ffi/read p :int 0)))
      (is (= 222 (ffi/read p :int 4)))
      (is (= 333 (ffi/read p :int 8)))
      (ffi/free p)))

  (testing "a value that is also a plausible offset is still a value"
    ;; The exact ambiguity #802 introduced: 4 and 8 read as either argument.
    ;; A wrongly-resolved order writes 4 at offset 8 and would satisfy a laxer
    ;; assertion, so both slots are checked. Fresh buffer, because asserting
    ;; on a slot this test did not write is only meaningful when it is zeroed.
    (let [p (ffi/alloc 32)]
      (ffi/write p :int 4 8)
      (is (= 4 (ffi/read p :int 8)))
      (is (not= 8 (ffi/read p :int 4)))
      (ffi/free p))))

(deftest struct-packing-round-trips
  ;; Mirrors the shape the bindings actually pack: consecutive fields at fixed
  ;; byte offsets, the layout `with-camera-2d` and friends build by hand. Under
  ;; a flipped argument order every one of these writes lands somewhere else.
  (testing "six floats at four-byte offsets, as a camera struct is packed"
    (let [p (ffi/alloc 24)
          fields [[0 1.5] [4 -2.5] [8 100.0] [12 -200.0] [16 0.25] [20 3.75]]]
      (doseq [[off v] fields]
        (ffi/write p :float (double v) off))
      (doseq [[off v] fields]
        (is (== v (ffi/read p :float off))
            (str "float at offset " off)))
      (ffi/free p)))

  (testing "a mixed-type struct, floats then a trailing int"
    (let [p (ffi/alloc 44)]
      (ffi/write p :float (double 10.0) 36)
      (ffi/write p :int (int 3) 40)
      (is (== 10.0 (ffi/read p :float 36)))
      (is (= 3 (ffi/read p :int 40)))
      (ffi/free p)))

  (testing "bytes indexed by position, as pixel buffers are filled"
    (let [n 8
          p (ffi/alloc n)]
      (dotimes [i n]
        (ffi/write p :uint8 (* 3 i) i))
      (dotimes [i n]
        (is (= (* 3 i) (ffi/read p :uint8 i))
            (str "byte at index " i)))
      (ffi/free p))))
