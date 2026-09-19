(ns net.b12n.raylib.textures
  "GPU-side textures and off-screen render targets, reached through rlgl's
  scalar layer rather than raylib's own by-value Texture2D API: upload,
  filter/wrap state, drawing a texture id as a quad, and framebuffers
  (BeginTextureMode's rlLoadFramebuffer/rlFramebufferAttach/rlEnableFramebuffer
  plumbing, plus the viewport/projection bookkeeping with-render-texture
  replicates)."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.color :as color]
   [net.b12n.raylib.core :as core]
   [net.b12n.raylib.native :as native]
   [net.b12n.raylib.rlgl :as rlgl]))

;; --- rlgl textures -----------------------------------------------------------
;; raylib's own texture API is unreachable from jolt: LoadTexture returns a
;; 20-byte Texture2D BY VALUE, which the AArch64 ABI hands back through the x8
;; indirect-result register, and Chez's foreign-procedure cannot express that.
;; rlgl's layer underneath it is entirely scalar, though, rlLoadTexture takes a
;; raw pixel pointer and returns the GL texture id as an unsigned int, and
;; rlSetTexture/rlTexCoord2f draw with it in immediate mode. So a texture here is
;; just that id: an int, no struct anywhere. What is lost is raylib's file
;; loaders (LoadTexture/LoadImage decode PNGs into an Image struct); textures in
;; this suite are therefore built pixel by pixel in native memory instead.
(ffi/defcfn rl-load-texture       "rlLoadTexture"       [:pointer :int :int :int :int] :uint)
(ffi/defcfn rl-unload-texture     "rlUnloadTexture"     [:uint] :void)
(ffi/defcfn rl-update-texture     "rlUpdateTexture"     [:uint :int :int :int :int :int :pointer] :void)
(ffi/defcfn rl-texture-parameters "rlTextureParameters" [:uint :int :int] :void)
(ffi/defcfn rl-set-texture        "rlSetTexture"        [:uint] :void)
(ffi/defcfn ^:private rl-tex-coord-2f-raw       "rlTexCoord2f"        [:float :float] :void)

(defn rl-tex-coord-2f
  "Texture coordinate for the next vertex.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1]
  (rl-tex-coord-2f-raw (double a0) (double a1)))

(ffi/defcfn ^:private rl-normal-3f-raw          "rlNormal3f"          [:float :float :float] :void)

(defn rl-normal-3f
  "Normal for the next vertex.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-normal-3f-raw (double a0) (double a1) (double a2)))

;; PIXELFORMAT-R8G8B8A8 and PIXELFORMAT-R8G8B8 live in net.b12n.raylib.native:
;; images.clj's LoadImageFromTexture bridge needs the first, net.b12n.raylib.shaders
;; needs it too, and images must not require textures, so native is where
;; both this module and images reach them from.
(def ^:const RL-QUADS 7)
(def ^:const RL-TEXTURE-WRAP-S 0x2802)        (def ^:const RL-TEXTURE-WRAP-T 0x2803)
(def ^:const RL-TEXTURE-WRAP-REPEAT 0x2901)   (def ^:const RL-TEXTURE-WRAP-CLAMP 0x812F)
(def ^:const RL-TEXTURE-MAG-FILTER 0x2800)    (def ^:const RL-TEXTURE-MIN-FILTER 0x2801)
(def ^:const RL-TEXTURE-FILTER-NEAREST 0x2600)
(def ^:const RL-TEXTURE-FILTER-LINEAR 0x2601)

(defn texture-filter!
  "Set both min and mag filters on a texture id (RL-TEXTURE-FILTER-NEAREST for
  crisp pixel art, RL-TEXTURE-FILTER-LINEAR for smooth scaling)."
  [id filter]
  (rl-texture-parameters id RL-TEXTURE-MIN-FILTER filter)
  (rl-texture-parameters id RL-TEXTURE-MAG-FILTER filter))

(defn texture-wrap!
  "Set both S and T wrap modes on a texture id (REPEAT lets texcoords past 1.0
  tile the image, CLAMP stretches the edge pixel)."
  [id wrap]
  (rl-texture-parameters id RL-TEXTURE-WRAP-S wrap)
  (rl-texture-parameters id RL-TEXTURE-WRAP-T wrap))

(defn texture-from-fn
  "Build a `w` x `h` RGBA8 texture on the GPU from (f x y) -> packed Color, and
  return its rlgl texture id. Frees the staging buffer once rlLoadTexture has
  copied it to the GPU. Pair with `unload-texture!` when done.

  A packed Color is already r | g<<8 | b<<16 | a<<24, which is byte-for-byte what
  RGBA8 wants on a little-endian machine, so each pixel is one :uint write."
  [w h f]
  (let [buf (ffi/alloc (* w h 4))]
    (try
      (dotimes [y h]
        (dotimes [x w]
          (ffi/write buf :uint (f x y) (* 4 (+ x (* y w))))))
      (let [id (rl-load-texture buf w h native/PIXELFORMAT-R8G8B8A8 1)]
        (texture-filter! id RL-TEXTURE-FILTER-NEAREST)
        (texture-wrap! id RL-TEXTURE-WRAP-REPEAT)
        id)
      (finally (ffi/free buf)))))

(defn update-texture-from-fn!
  "rlUpdateTexture - re-upload the whole `w` x `h` RGBA8 surface behind an
  existing texture id from (f x y) -> packed Color. Cheaper than unloading and
  reloading, and every quad already drawing that id picks the new texels up with
  no change of its own."
  [id w h f]
  (let [buf (ffi/alloc (* w h 4))]
    (try
      (dotimes [y h]
        (dotimes [x w]
          (ffi/write buf :uint (f x y) (* 4 (+ x (* y w))))))
      (rl-update-texture id 0 0 w h native/PIXELFORMAT-R8G8B8A8 buf)
      (finally (ffi/free buf)))))

(defn unload-texture!
  "rlUnloadTexture, release a texture id created by texture-from-fn."
  [id]
  (rl-unload-texture id))

(defn texture!
  "Draw a texture id as a quad, the immediate-mode stand-in for DrawTexturePro
  (whose Rectangle/Vector2 args are by value). Emits the same topLeft ->
  bottomLeft -> bottomRight -> topRight winding raylib's own DrawTexturePro
  uses, so it batches identically.
    :x :y                  where the origin lands in screen space
    :width :height         destination rectangle size
    :u0 :v0 :u1 :v1        source texcoords (default the whole texture; values
                           past 1.0 tile when the wrap mode is REPEAT, and
                           v0 > v1 flips vertically, which is what a framebuffer
                           texture needs)
    :rotation              degrees, clockwise, about the origin (default 0)
    :origin-x :origin-y    the pivot, as an offset into the destination
                           rectangle (default 0 0, meaning its top-left
                           corner). Pass half the width and height to spin
                           about the centre.
    :tint                  packed Color multiplied into the texels (default WHITE)

  At the default origin with no rotation the four corners come out at exactly
  (x, y) through (x + width, y + height), so every axis-aligned call predating
  the rotation support is unaffected."
  [id & {:keys [x y width height u0 v0 u1 v1 tint rotation origin-x origin-y]
         :or {x 0
              y 0
              width 100
              height 100
              u0 0.0
              v0 0.0
              u1 1.0
              v1 1.0
              rotation 0.0
              origin-x 0.0
              origin-y 0.0
              tint color/WHITE}}]
  (let [px (double x) py (double y)
        ox (double origin-x) oy (double origin-y)
        ;; corner offsets from the pivot, before any rotation
        lx (- ox) rx (- (double width) ox)
        ty (- oy) by (- (double height) oy)
        rad (Math/toRadians (double rotation))
        c (Math/cos rad) s (Math/sin rad)
        ;; y grows downward here, so [c -s; s c] turns clockwise on screen,
        ;; which is the direction DrawTexturePro's positive rotation goes
        vx (fn [dx dy] (+ px (- (* dx c) (* dy s))))
        vy (fn [dx dy] (+ py (* dx s) (* dy c)))]
    (rl-set-texture id)
    (rlgl/rl-begin RL-QUADS)
    (rlgl/rl-color! tint)
    (rl-normal-3f 0.0 0.0 1.0)
    (rl-tex-coord-2f (double u0) (double v0)) (rlgl/rl-vertex-2f (vx lx ty) (vy lx ty))
    (rl-tex-coord-2f (double u0) (double v1)) (rlgl/rl-vertex-2f (vx lx by) (vy lx by))
    (rl-tex-coord-2f (double u1) (double v1)) (rlgl/rl-vertex-2f (vx rx by) (vy rx by))
    (rl-tex-coord-2f (double u1) (double v0)) (rlgl/rl-vertex-2f (vx rx ty) (vy rx ty))
    (rlgl/rl-end)
    (rl-set-texture 0)))

;; --- rlgl framebuffers (render textures) -------------------------------------
;; raylib's LoadRenderTexture returns a RenderTexture2D by value and so is out of
;; reach for the same reason LoadTexture is, but rlgl's framebuffer calls are all
;; scalar: rlLoadFramebuffer returns the FBO id, rlFramebufferAttach wires a color
;; texture and a depth renderbuffer to it, and rlEnableFramebuffer binds it. What
;; BeginTextureMode adds on top is viewport and projection bookkeeping, which
;; with-render-texture replicates below.
(ffi/defcfn rl-load-framebuffer      "rlLoadFramebuffer"      [] :uint)
(ffi/defcfn rl-framebuffer-attach    "rlFramebufferAttach"    [:uint :uint :int :int :int] :void)
(ffi/defcfn rl-enable-framebuffer    "rlEnableFramebuffer"    [:uint] :void)
(ffi/defcfn rl-disable-framebuffer   "rlDisableFramebuffer"   [] :void)
(ffi/defcfn rl-unload-framebuffer    "rlUnloadFramebuffer"    [:uint] :void)
(ffi/defcfn rl-load-texture-depth    "rlLoadTextureDepth"     [:int :int :int] :uint)
(ffi/defcfn rl-viewport              "rlViewport"             [:int :int :int :int] :void)
(ffi/defcfn rl-matrix-mode           "rlMatrixMode"           [:int] :void)
(ffi/defcfn rl-load-identity         "rlLoadIdentity"         [] :void)
(ffi/defcfn rl-ortho                 "rlOrtho"                [:double :double :double :double :double :double] :void)
(ffi/defcfn rl-set-framebuffer-width  "rlSetFramebufferWidth"  [:int] :void)
(ffi/defcfn rl-set-framebuffer-height "rlSetFramebufferHeight" [:int] :void)
(ffi/defcfn rl-get-framebuffer-width  "rlGetFramebufferWidth"  [] :int)
(ffi/defcfn rl-get-framebuffer-height "rlGetFramebufferHeight" [] :int)
(ffi/defcfn rl-mult-matrix-f         "rlMultMatrixf"          [:pointer] :void)
(ffi/defcfn get-render-width         "GetRenderWidth"         [] :int)
(ffi/defcfn get-render-height        "GetRenderHeight"        [] :int)
(ffi/defcfn ^:private framebuffer-complete-raw "rlFramebufferComplete" [:uint] :int)

(def ^:const RL-PROJECTION 0x1701)
(def ^:const RL-MODELVIEW  0x1700)
(def ^:const RL-ATTACHMENT-COLOR-CHANNEL0 0)
(def ^:const RL-ATTACHMENT-DEPTH 100)
(def ^:const RL-ATTACHMENT-TEXTURE2D 100)
(def ^:const RL-ATTACHMENT-RENDERBUFFER 200)

(defn render-texture
  "Create an off-screen render target: an FBO with a `w` x `h` RGBA8 color
  texture and a depth renderbuffer. Returns {:fbo :texture :width :height}, or
  nil if the driver reports the framebuffer incomplete. Pair with
  `unload-render-texture!`.

  The color texture starts as an uninitialised buffer of the right size, rgba
  black is written so a target that is drawn before it is first rendered into
  reads as transparent rather than as whatever was in that allocation."
  [w h]
  (let [fbo (rl-load-framebuffer)
        tex (texture-from-fn w h (fn [_ _] (color/rgba 0 0 0 0)))
        depth (rl-load-texture-depth w h 1)]     ; useRenderBuffer = true
    (texture-filter! tex RL-TEXTURE-FILTER-LINEAR)
    (texture-wrap! tex RL-TEXTURE-WRAP-CLAMP)
    (rl-framebuffer-attach fbo tex RL-ATTACHMENT-COLOR-CHANNEL0 RL-ATTACHMENT-TEXTURE2D 0)
    (rl-framebuffer-attach fbo depth RL-ATTACHMENT-DEPTH RL-ATTACHMENT-RENDERBUFFER 0)
    (when-not (zero? (bit-and (framebuffer-complete-raw fbo) 0xff))
      {:fbo fbo
       :texture tex
       :width w
       :height h})))

(defn unload-render-texture!
  "Release the FBO and its color texture. The depth renderbuffer goes with the
  FBO, so it needs no separate call."
  [{:keys [fbo texture]}]
  (rl-unload-texture texture)
  (rl-unload-framebuffer fbo))

(defn- restore-screen-projection!
  "Put the viewport and both matrices back the way raylib leaves them for window
  drawing. This is EndTextureMode's SetupViewport call plus the screen-scale
  matrix BeginDrawing multiplies in, reproduced from the two scalar getters that
  expose what CORE holds privately.

  The scale matters and is easy to miss. On a HiDPI display raylib keeps the
  window at its logical size (GetScreenWidth) while rendering at the physical one
  (GetRenderWidth), projects in physical pixels, and bridges the two with a
  modelview scale of render/screen. Restoring only the viewport and the
  projection leaves that scale at identity, and every subsequent frame draws at
  half size in the lower-left corner. rlGetFramebufferWidth is NOT that number:
  it reports the logical size, so it cannot stand in for GetRenderWidth here."
  []
  (let [rw (get-render-width)
        rh (get-render-height)
        sx (/ (double rw) (max 1 (core/get-screen-width)))
        sy (/ (double rh) (max 1 (core/get-screen-height)))
        m (ffi/alloc 64)]                       ; 16 floats, column-major
    (try
      (rl-viewport 0 0 rw rh)
      (rl-set-framebuffer-width rw)
      (rl-set-framebuffer-height rh)
      (rl-matrix-mode RL-PROJECTION)
      (rl-load-identity)
      (rl-ortho 0.0 (double rw) (double rh) 0.0 0.0 1.0)
      (rl-matrix-mode RL-MODELVIEW)
      (rl-load-identity)
      (dotimes [i 16] (ffi/write m :float 0.0 (* 4 i)))
      (ffi/write m :float sx 0)
      (ffi/write m :float sy 20)
      (ffi/write m :float 1.0 40)
      (ffi/write m :float 1.0 60)
      (rl-mult-matrix-f m)
      (finally (ffi/free m)))))

(defn with-render-texture
  "Run (f) with drawing redirected into `rt`, then restore the screen - the
  BeginTextureMode/EndTextureMode pair, spelled out in scalar rlgl calls.

  Both halves flush the batch first: rlgl defers geometry until a draw call is
  forced, so without the flush the shapes queued before the switch would be
  rendered into whichever target happens to be bound afterwards.

  Note the resulting texture is bottom-up in GL's convention: draw it back with
  :v0 1.0 :v1 0.0 (as `texture!`'s docstring notes) or the image appears
  upside down."
  [{:keys [fbo width height]} f]
  (rlgl/flush-batch)
  (rl-enable-framebuffer fbo)
  (rl-viewport 0 0 width height)
  (rl-set-framebuffer-width width)
  (rl-set-framebuffer-height height)
  (rl-matrix-mode RL-PROJECTION)
  (rl-load-identity)
  (rl-ortho 0.0 (double width) (double height) 0.0 0.0 1.0)
  (rl-matrix-mode RL-MODELVIEW)
  (rl-load-identity)
  (try
    (f)
    (finally
      (rlgl/flush-batch)
      (rl-disable-framebuffer)
      (restore-screen-projection!))))
