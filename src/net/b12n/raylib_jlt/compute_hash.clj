(ns net.b12n.raylib-jlt.compute-hash
  "raylib [core] example - compute hash.

  Type into the input box (BACKSPACE deletes) and press ENTER to compute
  the CRC32 / MD5 / SHA1 / SHA256 hashes of the text, plus its Base64
  encoding.

  New FFI, all scalar or raw-pointer, no structs: rl/compute-crc32 is a
  plain uint return; rl/compute-md5/sha1/sha256 read N consecutive u32
  words out of raylib's own static result buffer (ffi/read, the same
  primitive local-time already uses for struct tm); rl/base64-encode
  passes a scratch int* out-param it never reads back. Verified against
  the canonical CRC32/SHA1/SHA256 test vectors for this exact input
  string -- MD5 does NOT match the usual byte order, which is raylib's
  own documented behavior (its %08X-per-word display is the native
  little-endian word order, not the byte-swapped form most MD5 tools
  print).
  Ported from raylib's examples/core/core_compute_hash.c."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MAX-CHARS 95)
(def ^:const DEFAULT-TEXT "The quick brown fox jumps over the lazy dog.")
(def ^:const INPUT-BG (rl/rgba 224 224 224 255))
(def ^:const BUTTON-BG (rl/rgba 178 216 230 255))
(def ^:const ROW-BG (rl/rgba 226 226 226 255))

(defn- hex8
  "8-digit uppercase hex of a 32-bit word (the C's %08X)."
  [v]
  (let [v (bit-and v 0xffffffff)]
    (apply str (for [shift [28 24 20 16 12 8 4 0]]
                 (nth "0123456789ABCDEF" (bit-and (bit-shift-right v shift) 0xf))))))

(defn- words->hex
  [words]
  (apply str (map hex8 words)))

(defn- compute-hashes
  [s]
  {:crc (hex8 (rl/compute-crc32 s (count s)))
   :md5 (words->hex (rl/compute-md5 s))
   :sha1 (words->hex (rl/compute-sha1 s))
   :sha256 (words->hex (rl/compute-sha256 s))
   :b64 (rl/base64-encode s)})

(defn- drain-chars
  [s]
  (loop [s s]
    (let [c (rl/get-char-pressed)]
      (if (pos? c)
        (recur (if (and (< (count s) MAX-CHARS) (>= c 32) (<= c 125))
                 (str s (char c))
                 s))
        s))))

(defn- row!
  [y label value]
  (rl/text! label {:x 40
                   :y (+ y 11)
                   :size 10
                   :color rl/DARKGRAY})
  (rl/rect! {:x 160
             :y y
             :width 600
             :height 32
             :color ROW-BG})
  (rl/rect-lines! {:x 160
                   :y y
                   :width 600
                   :height 32
                   :color rl/GRAY})
  (rl/text! value {:x 168
                   :y (+ y 11)
                   :size 10
                   :color rl/DARKGREEN}))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - compute hash"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0 text DEFAULT-TEXT hashes nil]
      (when (app/keep-running? deadline)
        (let [text (drain-chars text)
              text (if (and (rl/key-pressed? rl/KEY-BACKSPACE) (pos? (count text)))
                     (subs text 0 (dec (count text)))
                     text)
              hashes (if (rl/key-pressed? rl/KEY-ENTER) (compute-hashes text) hashes)
              h (or hashes {:crc "00000000"
                            :md5 "00000000000000000000000000000000"
                            :sha1 "0000000000000000000000000000000000000000"
                            :sha256 "0000000000000000000000000000000000000000000000000000000000000000"
                            :b64 ""})]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)

          (rl/text! "INPUT DATA (TEXT):" {:x 40
                                          :y 32
                                          :size 20
                                          :color rl/DARKGRAY})
          (rl/rect! {:x 40
                     :y 64
                     :width 720
                     :height 32
                     :color INPUT-BG})
          (rl/rect-lines! {:x 40
                           :y 64
                           :width 720
                           :height 32
                           :color rl/DARKGRAY})
          (rl/text! text {:x 48
                          :y 71
                          :size 20
                          :color rl/MAROON})
          (when (and (< (count text) MAX-CHARS) (even? (quot frame 20)))
            (rl/text! "_" {:x (+ 50 (rl/text-width text {:size 20}))
                           :y 71
                           :size 20
                           :color rl/MAROON}))

          (rl/rect! {:x 40
                     :y 104
                     :width 720
                     :height 32
                     :color BUTTON-BG})
          (rl/rect-lines! {:x 40
                           :y 104
                           :width 720
                           :height 32
                           :color rl/DARKBLUE})
          (rl/text! "PRESS [ENTER] TO COMPUTE INPUT DATA HASHES"
                    {:x 158
                     :y 110
                     :size 20
                     :color rl/DARKBLUE})

          (rl/text! "INPUT DATA HASH VALUES:" {:x 40
                                               :y 166
                                               :size 20
                                               :color rl/DARKGRAY})
          (row! 200 "CRC32 [32 bit]:" (:crc h))
          (row! 236 "MD5 [128 bit]:" (:md5 h))
          (row! 272 "SHA1 [160 bit]:" (:sha1 h))
          (row! 308 "SHA256 [256 bit]:" (:sha256 h))

          (rl/text! "BONUS - BASE64 ENCODED STRING:" {:x 40
                                                      :y 356
                                                      :size 10
                                                      :color rl/DARKBLUE})
          (row! 380 "BASE64 ENCODING:" (:b64 h))

          (app/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) text hashes)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
