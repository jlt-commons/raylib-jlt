(ns net.b12n.raylib.log
  "raylib's trace log: the LOG-* level constants, and on-trace-log! which
  installs a jolt fn as raylib's SetTraceLogCallback so its own messages can
  be read instead of only printed."
  (:require
   [jolt.ffi :as ffi]))

;; --- the trace log, and the suite's first callback INTO jolt -------------
;; Every other binding in this library calls out of jolt into C.
;; SetTraceLogCallback goes the other way: raylib is handed a function pointer
;; and calls it for every message it would otherwise print. ffi/foreign-callable
;; builds that pointer out of a jolt fn, and the pointer stays live until
;; free-callable, which is why on-trace-log! hands it back rather than dropping
;; it on the floor.
;;
;; The third parameter is the awkward one. raylib's callback signature ends in a
;; va_list, which no FFI type describes, so it is taken as an opaque :pointer and
;; handed straight to libc's vsnprintf along with the format string. That is what
;; turns "Target time per frame: %02.03f milliseconds" into the line with the
;; number in it. A va_list can be walked once, which is fine here because
;; replacing the callback means raylib's own logger is no longer reading it.
;; Verified against a real window: 43 messages captured through InitWindow, with
;; every %i, %s and %f expanded.
(ffi/defcfn set-trace-log-callback "SetTraceLogCallback" [:pointer] :void)
(ffi/defcfn ^:private vsnprintf-raw "vsnprintf" [:pointer :uptr :pointer :pointer] :int)

(def ^:const LOG-TRACE 1)   (def ^:const LOG-DEBUG 2)
(def ^:const LOG-INFO 3)    (def ^:const LOG-WARNING 4)
(def ^:const LOG-ERROR 5)   (def ^:const LOG-FATAL 6)

(def ^:const TRACE-LOG-BUFFER 1024)

(defn on-trace-log!
  "SetTraceLogCallback with a jolt fn. `f` is called as (f level text) for every
  message raylib logs, `level` one of the LOG-* constants and `text` the format
  string already expanded by vsnprintf. Anything longer than TRACE-LOG-BUFFER is
  truncated, which vsnprintf does for us rather than overrunning.

  Returns the callable pointer. raylib keeps calling it until another callback
  replaces it, so the pointer has to outlive the window; free it with
  ffi/free-callable once the window is closed, not before. Install this BEFORE
  init-window if the startup messages are wanted."
  [f]
  (let [entry (ffi/foreign-callable
               (fn [level text-ptr va]
                 (let [buf (ffi/alloc TRACE-LOG-BUFFER)]
                   (try
                     (vsnprintf-raw buf TRACE-LOG-BUFFER text-ptr va)
                     (f level (ffi/ptr->string buf))
                     (finally (ffi/free buf))))
                 nil)
               [:int :pointer :pointer] :void)]
    (set-trace-log-callback entry)
    entry))

(defn free-callable!
  "Release a callable entry point built by on-trace-log!. C can call the pointer
  until this runs and not one instruction longer, so unregister it with the C
  library first: for the trace log that means closing the window, since raylib
  logs while it shuts down."
  [entry]
  (ffi/free-callable entry))
