(ns net.b12n.raylib.util
  "Two unrelated things live here: libc `time`/`localtime`, and raylib's
  hashing plus Base64 helpers. Both are \"neither drawing nor windowing\",
  which is a weak bond, but they are kept in one module rather than split
  into two ~30-line files. That is a deliberate choice, not an accident; if
  this module grows past about 150 lines in a later arc, split it then."
  (:require
   [jolt.ffi :as ffi]))

;; --- libc time (the one NON-raylib FFI) --------------------------------------
;; time()/localtime() live in libc (always loaded); jolt.ffi resolves them exactly
;; like raylib's symbols. localtime returns a pointer to a struct tm whose first
;; three ints are tm_sec, tm_min, tm_hour (offsets 0/4/8 on Darwin and glibc). This
;; is this library's only non-raylib FFI call, proof jolt binds any C ABI symbol.
(ffi/defcfn ^:private c-time      "time"      [:pointer] :long)
(ffi/defcfn ^:private c-localtime "localtime" [:pointer] :pointer)

(defn local-time
  "Current wall-clock local time as [hour minute second] via libc time()/localtime()."
  []
  (let [buf (ffi/alloc 8)]
    (try
      (c-time buf)
      (let [tm (c-localtime buf)]
        [(ffi/read tm :int 8) (ffi/read tm :int 4) (ffi/read tm :int 0)])
      (finally (ffi/free buf)))))

;; --- hashing + base64 (compute-hash) --------------------------------------
(ffi/defcfn compute-crc32 "ComputeCRC32" [:string :int] :uint)
(ffi/defcfn ^:private compute-md5-raw "ComputeMD5" [:string :int] :pointer)
(ffi/defcfn ^:private compute-sha1-raw "ComputeSHA1" [:string :int] :pointer)
(ffi/defcfn ^:private compute-sha256-raw "ComputeSHA256" [:string :int] :pointer)
(ffi/defcfn ^:private encode-base64-raw "EncodeDataBase64" [:string :int :pointer] :string)

(defn- read-words
  "n consecutive :uint (4-byte) words at ptr, as a vector. ComputeMD5/SHA1/
  SHA256 return a pointer into raylib's own static buffer (per raylib.h's
  own comment), so there's nothing to free here."
  [ptr n]
  (mapv (fn [i] (bit-and (ffi/read ptr :int (* i 4)) 0xffffffff)) (range n)))

(defn compute-md5
  "ComputeMD5. 4 u32 words."
  [s]
  (read-words (compute-md5-raw s (count s)) 4))

(defn compute-sha1
  "ComputeSHA1. 5 u32 words."
  [s]
  (read-words (compute-sha1-raw s (count s)) 5))

(defn compute-sha256
  "ComputeSHA256. 8 u32 words."
  [s]
  (read-words (compute-sha256-raw s (count s)) 8))

(defn base64-encode
  "EncodeDataBase64. The C's own comment admits every recompute leaks the
  malloc'd result (\"memory must be MemFree()\", never called in the
  upstream example either); a demo box's worth of Base64 text per ENTER
  press is not worth chasing across this FFI boundary."
  [s]
  (let [out-size (ffi/alloc 4)]
    (try (or (encode-base64-raw s (count s) out-size) "")
         (finally (ffi/free out-size)))))
