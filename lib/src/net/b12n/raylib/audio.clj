(ns net.b12n.raylib.audio
  "raudio: InitAudioDevice/CloseAudioDevice, the AudioStream family (LoadAudioStream
  returns its 32-byte struct BY VALUE, the same [:by-value [:struct ...]]
  convention net.b12n.raylib.shaders' Shader uses) for play/update/pan, and
  SetAudioStreamCallback -- the one entry point in this library raudio invokes
  from a thread jolt never started, so the one that needs jolt's :collect-safe
  reactivation rather than its ordinary calling convention."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.native :as native]))

;; AudioStream is {rAudioBuffer* buffer; rAudioProcessor* processor; uint
;; sampleRate; uint sampleSize; uint channels} -- two pointers and three u32s,
;; passed BY VALUE everywhere raudio touches it. That is the same [:by-value
;; [:struct ...]] mechanism circle-gradient! already uses for its Vector2
;; centre; LoadAudioStream also RETURNS one by value, so its binding takes a
;; caller-allocated destination pointer FIRST (jolt's calling convention for
;; an aggregate return) and hands that same pointer back.
(def ^:private audio-stream-layout
  (ffi/layout [:struct [[:buffer :pointer]
                        [:processor :pointer]
                        [:sample-rate :uint32]
                        [:sample-size :uint32]
                        [:channels :uint32]]]))

(ffi/defcfn init-audio-device  "InitAudioDevice"  [] :void)
(ffi/defcfn close-audio-device "CloseAudioDevice" [] :void)
(ffi/defcfn set-audio-stream-buffer-size-default
  "SetAudioStreamBufferSizeDefault" [:int] :void)

(ffi/defcfn ^:private load-audio-stream-raw "LoadAudioStream"
  [:uint32 :uint32 :uint32]
  [:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                       [:sample-rate :uint32] [:sample-size :uint32]
                       [:channels :uint32]]]])
(ffi/defcfn ^:private unload-audio-stream-raw "UnloadAudioStream"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]]
  :void)
(ffi/defcfn ^:private play-audio-stream-raw "PlayAudioStream"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]]
  :void)
(ffi/defcfn ^:private is-audio-stream-processed-raw "IsAudioStreamProcessed"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]]
  :int)
(ffi/defcfn ^:private update-audio-stream-raw "UpdateAudioStream"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]
   :pointer :int]
  :void)
(ffi/defcfn ^:private set-audio-stream-pan-raw "SetAudioStreamPan"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]
   :float]
  :void)

(defn load-audio-stream
  "LoadAudioStream. Returns an opaque native pointer to the by-value AudioStream
  -- pass it to every other audio-stream fn below and release it with
  unload-audio-stream."
  [sample-rate sample-size channels]
  (let [stream (ffi/alloc (ffi/layout-size audio-stream-layout))]
    (load-audio-stream-raw stream sample-rate sample-size channels)))

(defn unload-audio-stream
  [stream]
  (unload-audio-stream-raw stream)
  (ffi/free stream))

(defn play-audio-stream
  [stream]
  (play-audio-stream-raw stream))

(defn audio-stream-processed?
  [stream]
  (not (zero? (bit-and (is-audio-stream-processed-raw stream) 0xff))))

(defn update-audio-stream
  "UpdateAudioStream. `samples` is a seq of floats for one refill; its count
  must match the stream's own frame-count-per-channel (mono here). Stages a
  scratch native buffer via `native/staged` the same way the shader uniform
  setters do -- a few refills a second is not a hot path."
  [stream samples]
  (native/staged :float samples (fn [p] (update-audio-stream-raw stream p (count samples)))))

(defn set-audio-stream-pan
  [stream pan]
  (set-audio-stream-pan-raw stream (double pan)))

;; --- the audio stream callback, on a thread jolt never started ----------
;; on-trace-log! (net.b12n.raylib.log) is a callback raylib invokes on
;; whichever thread called into it, which is this one. SetAudioStreamCallback
;; is the harder case: raudio runs its own audio thread and calls back from
;; there, so the entry point needs jolt's :collect-safe, which reactivates the
;; thread before any jolt code runs on it. Without it the process dies with a
;; memory fault no handler can catch.
;;
;; What happens inside is the caller's problem and a real-time one: the callback
;; owes raudio `frames` samples before the device underruns. Write them straight
;; into `buffer` with ffi/write and keep allocation out of the loop.
(ffi/defcfn ^:private set-audio-stream-callback-raw "SetAudioStreamCallback"
  [[:by-value [:struct [[:buffer :pointer] [:processor :pointer]
                        [:sample-rate :uint32] [:sample-size :uint32]
                        [:channels :uint32]]]]
   :pointer]
  :void)

(defn on-audio-stream!
  "SetAudioStreamCallback with a jolt fn. `f` is called as (f buffer frames) on
  raudio's audio thread and must fill `buffer` with `frames` samples, written as
  :float at 4-byte strides for a 32-bit mono stream.

  Returns the callable pointer. Clear the callback with
  clear-audio-stream-callback! BEFORE freeing that pointer, or raudio is left
  calling a dead address from another thread."
  [stream f]
  (let [entry (ffi/foreign-callable f [:pointer :uint32] :void :collect-safe)]
    (set-audio-stream-callback-raw stream entry)
    entry))

(defn clear-audio-stream-callback!
  "Hand raudio a NULL callback, so it goes back to waiting for
  update-audio-stream refills and stops calling into jolt."
  [stream]
  (set-audio-stream-callback-raw stream ffi/null))
