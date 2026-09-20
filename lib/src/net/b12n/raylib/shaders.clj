(ns net.b12n.raylib.shaders
  "Shader compile/link and the uniform setter family: LoadShaderFromMemory (by
  value, the same [:by-value [:struct ...]] convention this library's other
  by-value structs use), with-shader for the BeginShaderMode/EndShaderMode
  pair, and SetShaderValue* staged through net.b12n.raylib.native/staged the
  same way every other variadic raylib call in this library does.

  set-uniform-texture! lives here rather than in textures or images because it
  is a shader uniform setter first: SetShaderValueTexture needs
  net.b12n.raylib.native/texture2d-layout to stage the by-value Texture2D it
  takes, and requiring textures for that one setter would invert a dependency
  this library does not otherwise have."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.native :as native]))

;; raylib's Shader is {unsigned int id; int *locs;} - 16 bytes, passed and
;; returned BY VALUE. jolt 0.7.23's [:by-value [:struct ...]] expresses that
;; directly, so this calls raylib's real shader API rather than reaching under it
;; to rlgl the way net.b12n.raylib.textures has to. In particular
;; LoadShaderFromMemory fills the locations array itself; nothing here builds one.
;;
;; The struct descriptor is spelled out in every signature on purpose: it is a
;; compile-time literal and a def'd alias is rejected with
;;   jolt.ffi return type must be a keyword or [:by-value [:struct ...]], got V2
(ffi/defcfn ^:private load-shader-from-memory "LoadShaderFromMemory" [:pointer :string]
  [:by-value [:struct [[:id :uint] [:locs :pointer]]]])
(ffi/defcfn ^:private begin-shader-mode "BeginShaderMode"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]]] :void)
(ffi/defcfn end-shader-mode "EndShaderMode" [] :void)
(ffi/defcfn ^:private get-shader-location "GetShaderLocation"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]] :string] :int)
(ffi/defcfn ^:private set-shader-value-raw "SetShaderValue"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]] :int :pointer :int] :void)
(ffi/defcfn ^:private set-shader-value-v-raw "SetShaderValueV"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]] :int :pointer :int :int] :void)
(ffi/defcfn ^:private unload-shader-raw "UnloadShader"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]]] :void)

;; Two by-value structs in one signature, and they take different ABI paths on
;; arm64: Shader is 16 bytes and rides in general-purpose registers, Texture2D is
;; 20 and so is passed INDIRECTLY, by a pointer the caller supplies. Both
;; measured with clang, not assumed.
(ffi/defcfn ^:private set-shader-value-texture-raw "SetShaderValueTexture"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]]
   :int
   [:by-value [:struct [[:id :uint] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]]] :void)

(def shader-layout (ffi/layout [:struct [[:id :uint] [:locs :pointer]]]))

;; ShaderUniformDataType, raylib 6.0. The UINT variants at 8-11 are new in 6.0
;; and pushed SAMPLER2D from 8 to 12 - a silent break for anything carrying the
;; 5.5 value, since a wrong type tag binds the wrong slot without erroring.
(def ^:const UNIFORM-FLOAT 0)  (def ^:const UNIFORM-VEC2 1)
(def ^:const UNIFORM-VEC3 2)   (def ^:const UNIFORM-VEC4 3)
(def ^:const UNIFORM-INT 4)    (def ^:const UNIFORM-IVEC2 5)
(def ^:const UNIFORM-IVEC3 6)  (def ^:const UNIFORM-IVEC4 7)
(def ^:const UNIFORM-SAMPLER2D 12)

(defn shader
  "Compile `fs-source` as a fragment shader against raylib's default vertex
  shader. Returns a pointer to the Shader struct, or nil if the program did not
  link (raylib prints the compiler log to stderr). Pair with `unload-shader!`.

  GLSL is a string here rather than a file, because LoadShaderFromMemory takes
  source: nothing is read from disk, so each example stays self-contained and the
  demo recorder never has a working-directory question. The source must open with
  `#version 330` - the desktop backend is GL 3.3 core.

  ffi/null rather than nil for the vertex stage, meaning \"use raylib's default\".
  jolt carries nil across a :string as NULL only since jolt#708, which is merged
  but not in a release, so the :pointer spelling keeps this working on a stock
  0.7.23."
  [fs-source]
  (let [p (ffi/alloc (ffi/layout-size shader-layout))]
    (load-shader-from-memory p ffi/null fs-source)
    (if (pos? (ffi/read-field p shader-layout :id))
      p
      (do (ffi/free p) nil))))

(defn unload-shader!
  "UnloadShader, then release the struct this side."
  [sh]
  (unload-shader-raw sh)
  (ffi/free sh))

(defn uniform-loc
  "The location of a named uniform, or -1 if the shader does not declare it (or
  the compiler optimised it away). Look these up once, outside the frame loop -
  each call is a GL query."
  [sh name]
  (get-shader-location sh name))

(defn with-shader
  "Run (f) with `sh` active. BeginShaderMode / EndShaderMode, so raylib does the
  batch flush on both edges."
  [sh f]
  (begin-shader-mode sh)
  (try
    (f)
    (finally (end-shader-mode))))

;; SetShaderValue takes a POINTER to the value, so each setter stages its floats
;; or ints in native memory for the length of the call. An undeclared uniform
;; gives -1, which the nat-int? guards skip: an example whose shader drops an
;; unused uniform keeps working rather than erroring.
(defn set-uniform-float!
  [sh loc v]
  (when (nat-int? loc)
    (native/staged :float [v] (fn [p] (set-shader-value-raw sh loc p UNIFORM-FLOAT)))))

(defn set-uniform-vec2!
  [sh loc x y]
  (when (nat-int? loc)
    (native/staged :float [x y] (fn [p] (set-shader-value-raw sh loc p UNIFORM-VEC2)))))

(defn set-uniform-vec3!
  [sh loc x y z]
  (when (nat-int? loc)
    (native/staged :float [x y z] (fn [p] (set-shader-value-raw sh loc p UNIFORM-VEC3)))))

(defn set-uniform-vec4!
  [sh loc x y z w]
  (when (nat-int? loc)
    (native/staged :float [x y z w] (fn [p] (set-shader-value-raw sh loc p UNIFORM-VEC4)))))

(defn set-uniform-int!
  [sh loc v]
  (when (nat-int? loc)
    (native/staged :int [v] (fn [p] (set-shader-value-raw sh loc p UNIFORM-INT)))))

(defn set-uniform-ivec3-array!
  "An array of `n` ivec3s from a flat sequence of 3n ints - how a palette reaches
  a shader as `uniform ivec3 palette[8]`."
  [sh loc ints n]
  (when (nat-int? loc)
    (native/staged :int ints (fn [p] (set-shader-value-v-raw sh loc p UNIFORM-IVEC3 n)))))

(defn set-uniform-texture!
  "Bind a texture id to a `sampler2D` uniform - the second and later samplers,
  since raylib binds the drawn texture to slot 0 itself.

  This suite carries textures as bare rlgl ids, so the Texture2D raylib wants is
  staged here from the id plus its dimensions. mipmaps 1 and format RGBA8 match
  what `texture-from-fn` uploads; raylib only reads `id` for this call, but the
  rest is filled in truthfully rather than left as whatever the allocation held."
  [sh loc tex-id w h]
  (when (nat-int? loc)
    (ffi/with-layout [t native/texture2d-layout]
      (ffi/write-field t native/texture2d-layout :id tex-id)
      (ffi/write-field t native/texture2d-layout :width (int w))
      (ffi/write-field t native/texture2d-layout :height (int h))
      (ffi/write-field t native/texture2d-layout :mipmaps 1)
      (ffi/write-field t native/texture2d-layout :format native/PIXELFORMAT-R8G8B8A8)
      (set-shader-value-texture-raw sh loc t))))

;; --- custom vertex shaders ---------------------------------------------------
;; `shader` above passes NULL for the vertex stage, which makes raylib supply its
;; own. A lighting shader cannot do that: it needs the world-space position and
;; the transformed normal passed through from the vertex stage, and raylib's
;; default vertex shader emits neither. Hence a second entry point that takes
;; both sources. The C function is the same one; only the first argument differs,
;; so it needs its own binding with :string rather than :pointer there.
(ffi/defcfn ^:private load-shader-vf-raw "LoadShaderFromMemory" [:string :string]
  [:by-value [:struct [[:id :uint] [:locs :pointer]]]])

(defn shader-vf
  "Compile `vs-source` and `fs-source` together. Returns a pointer to the Shader
  struct, or nil if the program did not link (raylib prints the log to stderr).
  Pair with `unload-shader!`, exactly like `shader`."
  [vs-source fs-source]
  (let [p (ffi/alloc (ffi/layout-size shader-layout))]
    (load-shader-vf-raw p vs-source fs-source)
    (if (pos? (ffi/read-field p shader-layout :id))
      p
      (do (ffi/free p) nil))))

;; --- binding a sampler to a texture slot by hand -----------------------------
;; set-uniform-texture! above goes through SetShaderValueTexture, which raylib
;; applies through its own render batch. That covers the 2D calls and the
;; immediate-mode helpers, and it does NOT cover DrawMesh, which draws outside
;; that batch. A shader whose VERTEX stage samples a texture therefore reads
;; zeroes, which is a quiet failure: the sample returns 0, so a displacement
;; comes out flat and a colour comes out at whichever end of its ramp 0 maps to.
;;
;; The fix is what raylib's own example does, and it is a one-off GL state
;; change rather than a per-frame uniform: select a slot, bind the texture to
;; it, and tell the sampler which slot to read. Slot 0 belongs to raylib for the
;; material's diffuse map, so anything else starts at 1.
(ffi/defcfn rl-enable-shader "rlEnableShader" [:uint] :void)
(ffi/defcfn rl-active-texture-slot "rlActiveTextureSlot" [:int] :void)
(ffi/defcfn rl-enable-texture "rlEnableTexture" [:uint] :void)
(ffi/defcfn rl-set-uniform-sampler "rlSetUniformSampler" [:int :uint] :void)

(defn bind-sampler!
  "Bind `tex-id` to texture slot `slot` and point the sampler at `loc` to it.
  Call once after the shader links, not per frame: this changes GL state rather
  than queueing a uniform, and it survives until something else rebinds the
  slot.

  Use this rather than set-uniform-texture! whenever the sampler is read by the
  VERTEX stage, or whenever the draw goes through DrawMesh, since neither goes
  through the batch SetShaderValueTexture feeds."
  [sh loc tex-id slot]
  (when (nat-int? loc)
    (rl-enable-shader (ffi/read-field sh shader-layout :id))
    (rl-active-texture-slot (int slot))
    (rl-enable-texture tex-id)
    (rl-set-uniform-sampler (int loc) (int slot)))
  sh)
