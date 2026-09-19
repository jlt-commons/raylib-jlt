(ns net.b12n.raylib.images
  "raylib's CPU-side Image: the procedural generators (GenImageColor through
  GenImageText), the processing family (colour, flip, blur, format, the
  GPU round trip through LoadImageFromTexture/LoadTextureFromImage), and
  geometry/convolution (crop, resize, kernel convolution). Every generator
  hands back an rlgl texture id rather than the Image, so the result draws
  through the same net.b12n.raylib.textures surface a texture-from-fn
  texture does."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.native :as native]))

;; --- Image: raylib's CPU-side pixel buffer, by value ---------------------
;; Image is {void *data; int width, height, mipmaps, format;}, 24 bytes, returned
;; by value from every generator and taken by value by everything that consumes
;; one. The generators are the reason to bind it at all: they are raylib's own
;; procedural textures, checkerboards through Perlin and cellular noise, and this
;; suite ships no image files, so generating is the only way it ever had.
;;
;; What comes back to the caller is an rlgl texture id, not the Image and not the
;; Texture2D. That keeps the whole existing drawing surface usable unchanged:
;; texture!, texture-filter!, texture-wrap! and unload-texture! all speak ids
;; already, so an image generated here draws through the same path a
;; texture-from-fn one does. The Image itself is freed inside each call, since
;; its pixels have been copied to the GPU by then.
(def ^:private image-layout
  (ffi/layout [:struct [[:data :pointer] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]))

;; texture2d-layout lives in net.b12n.raylib.native, not here: this module's
;; image->texture-id!/image->texture bridge needs it, so does textures.clj, and
;; so does net.b12n.raylib.shaders (SetShaderValueTexture) -- three consumers,
;; and native is the dependency-free leaf all three can reach without
;; duplicating it.
;;
;; The five fields ARE written out again in every signature below, and that is
;; forced rather than sloppy: a struct descriptor is a compile-time literal, so
;; a def'd alias is rejected with "return type must be a keyword or [:by-value
;; [:struct ...]]". Same constraint the shader section documents, same shape of
;; repetition, and native/texture2d-layout still earns its keep for reading
;; fields back.
(assert (= 24 (ffi/layout-size image-layout)) "Image is a pointer and four ints")

(ffi/defcfn ^:private gen-image-color-raw "GenImageColor" [:int :int :uint]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-checked-raw "GenImageChecked" [:int :int :int :int :uint :uint]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-gradient-linear-raw "GenImageGradientLinear" [:int :int :int :uint :uint]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-gradient-radial-raw "GenImageGradientRadial" [:int :int :float :uint :uint]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-gradient-square-raw "GenImageGradientSquare" [:int :int :float :uint :uint]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-white-noise-raw "GenImageWhiteNoise" [:int :int :float]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-perlin-noise-raw "GenImagePerlinNoise" [:int :int :int :int :float]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-cellular-raw "GenImageCellular" [:int :int :int]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private gen-image-text-raw "GenImageText" [:int :int :string]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private unload-image-raw "UnloadImage"
  [[:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]]] :void)
(ffi/defcfn ^:private load-texture-from-image-raw "LoadTextureFromImage"
  [[:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]]]
  [:by-value [:struct [[:id :uint] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])

(defn- image->texture-id!
  "Upload a filled Image buffer to the GPU, free the Image, and answer the rlgl
  texture id. 0 means the upload failed, which raylib has already logged."
  [img]
  (let [tex (ffi/alloc (ffi/layout-size native/texture2d-layout))]
    (try
      (load-texture-from-image-raw tex img)
      (unload-image-raw img)
      (ffi/read-field tex native/texture2d-layout :id)
      (finally (ffi/free tex)))))

(defn- with-image
  "Run `f` against a freshly allocated Image buffer, then hand the result on.
  `f` fills the buffer by calling one of the generators with it as the return
  slot, which is jolt's convention for an aggregate return."
  [f]
  (let [img (ffi/alloc (ffi/layout-size image-layout))]
    (try
      (f img)
      (image->texture-id! img)
      (finally (ffi/free img)))))

(defn image-color
  "GenImageColor as a texture id: a plain `w` x `h` field of one colour."
  [w h color]
  (with-image (fn [img] (gen-image-color-raw img (int w) (int h) color))))

(defn image-checked
  "GenImageChecked as a texture id: `checks-x` by `checks-y` squares alternating
  between two colours."
  [w h checks-x checks-y c1 c2]
  (with-image (fn [img] (gen-image-checked-raw img (int w) (int h)
                                               (int checks-x) (int checks-y) c1 c2))))

(defn image-gradient-linear
  "GenImageGradientLinear as a texture id. `direction` is in degrees, 0 vertical."
  [w h direction start end]
  (with-image (fn [img] (gen-image-gradient-linear-raw img (int w) (int h)
                                                       (int direction) start end))))

(defn image-gradient-radial
  "GenImageGradientRadial as a texture id, `density` shaping the falloff."
  [w h density inner outer]
  (with-image (fn [img] (gen-image-gradient-radial-raw img (int w) (int h)
                                                       (double density) inner outer))))

(defn image-gradient-square
  "GenImageGradientSquare as a texture id, `density` shaping the falloff."
  [w h density inner outer]
  (with-image (fn [img] (gen-image-gradient-square-raw img (int w) (int h)
                                                       (double density) inner outer))))

(defn image-white-noise
  "GenImageWhiteNoise as a texture id. `factor` is the fraction of white pixels."
  [w h factor]
  (with-image (fn [img] (gen-image-white-noise-raw img (int w) (int h) (double factor)))))

(defn image-perlin-noise
  "GenImagePerlinNoise as a texture id. The offsets slide the sample window, so
  animating one of them scrolls the field rather than regenerating it."
  [w h offset-x offset-y scale]
  (with-image (fn [img] (gen-image-perlin-noise-raw img (int w) (int h)
                                                    (int offset-x) (int offset-y) (double scale)))))

(defn image-cellular
  "GenImageCellular as a texture id. A bigger `tile-size` means bigger cells."
  [w h tile-size]
  (with-image (fn [img] (gen-image-cellular-raw img (int w) (int h) (int tile-size)))))

(defn image-text
  "GenImageText as a texture id: `text` rasterised with raylib's default font
  into a `w` x `h` greyscale field."
  [w h text]
  (with-image (fn [img] (gen-image-text-raw img (int w) (int h) text))))

;; --- Image processing: raylib's own pixel operations ---------------------
;; The generators above make an Image; these change one. Note the asymmetry in
;; raylib's own API, which is why these bind so differently: every processor
;; takes `Image *` and works IN PLACE, so it is a plain :pointer argument and the
;; 24-byte by-value dance does not arise. Only ImageCopy and LoadImageFromTexture
;; move whole Images across the boundary.
;;
;; LoadImageFromTexture is what lets this suite process a picture at all. It
;; reads a GPU texture back to CPU memory, so an image authored pixel by pixel
;; with texture-from-fn can be handed to raylib's blur, its channel flips and its
;; colour operations. Without it there is no source image here, since no example
;; ships one on disk.
(ffi/defcfn ^:private load-image-from-texture-raw "LoadImageFromTexture"
  [[:by-value [:struct [[:id :uint] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]]]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private image-copy-raw "ImageCopy"
  [[:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]]]
  [:by-value [:struct [[:data :pointer] [:width :int] [:height :int]
                       [:mipmaps :int] [:format :int]]]])
(ffi/defcfn ^:private image-format-raw         "ImageFormat"          [:pointer :int] :void)
(ffi/defcfn ^:private image-color-invert-raw   "ImageColorInvert"     [:pointer] :void)
(ffi/defcfn ^:private image-color-grayscale-raw "ImageColorGrayscale" [:pointer] :void)
(ffi/defcfn ^:private image-color-tint-raw     "ImageColorTint"       [:pointer :uint] :void)
(ffi/defcfn ^:private image-color-contrast-raw "ImageColorContrast"   [:pointer :int] :void)
(ffi/defcfn ^:private image-color-brightness-raw "ImageColorBrightness" [:pointer :int] :void)
(ffi/defcfn ^:private image-flip-horizontal-raw "ImageFlipHorizontal" [:pointer] :void)
(ffi/defcfn ^:private image-flip-vertical-raw  "ImageFlipVertical"    [:pointer] :void)
(ffi/defcfn ^:private image-blur-gaussian-raw  "ImageBlurGaussian"    [:pointer :int] :void)

(defn image-from-texture!
  "LoadImageFromTexture: read an rlgl texture back off the GPU into a fresh
  Image buffer, which the caller owns and must pass to unload-image!. The
  texture is described truthfully from `w` and `h`; raylib reads the id, the
  size and the format to work out how many bytes to pull back."
  [tex-id w h]
  (let [img (ffi/alloc (ffi/layout-size image-layout))]
    (ffi/with-layout [t native/texture2d-layout]
      (ffi/write-field t native/texture2d-layout :id tex-id)
      (ffi/write-field t native/texture2d-layout :width (int w))
      (ffi/write-field t native/texture2d-layout :height (int h))
      (ffi/write-field t native/texture2d-layout :mipmaps 1)
      (ffi/write-field t native/texture2d-layout :format native/PIXELFORMAT-R8G8B8A8)
      (load-image-from-texture-raw img t))
    img))

(defn image-copy!
  "ImageCopy: a duplicate the caller owns, so an original can be kept while a
  processor chews through the copy."
  [img]
  (let [out (ffi/alloc (ffi/layout-size image-layout))]
    (image-copy-raw out img)
    out))

(defn unload-image!
  "UnloadImage, then release the 24-byte struct this side."
  [img]
  (unload-image-raw img)
  (ffi/free img))

(defn image->texture
  "Upload an Image the caller still owns to the GPU and answer its rlgl texture
  id. Unlike the generators, this does NOT consume the Image."
  [img]
  (let [tex (ffi/alloc (ffi/layout-size native/texture2d-layout))]
    (try
      (load-texture-from-image-raw tex img)
      (ffi/read-field tex native/texture2d-layout :id)
      (finally (ffi/free tex)))))

(defn image-format!
  "ImageFormat, in place. RGBA8 is PIXELFORMAT-R8G8B8A8; a processor that ran
  on a narrower format needs putting back before it is uploaded."
  [img format]
  (image-format-raw img (int format)))

(defn image-color-invert! [img] (image-color-invert-raw img))
(defn image-color-grayscale! [img] (image-color-grayscale-raw img))
(defn image-color-tint! [img color] (image-color-tint-raw img color))
(defn image-color-contrast! [img contrast] (image-color-contrast-raw img (int contrast)))
(defn image-color-brightness! [img brightness] (image-color-brightness-raw img (int brightness)))
(defn image-flip-horizontal! [img] (image-flip-horizontal-raw img))
(defn image-flip-vertical! [img] (image-flip-vertical-raw img))
(defn image-blur-gaussian! [img size] (image-blur-gaussian-raw img (int size)))

;; --- Image geometry and convolution --------------------------------------
;; Both in place on an Image*, like the colour operations above, except that
;; ImageCrop's Rectangle is by value: four floats, 16 bytes, which on arm64 fits
;; in registers rather than going indirect. ImageKernelConvolution takes a flat
;; float array and its LENGTH, not its side, so a 3x3 kernel is nine floats and
;; the argument is 9.
(def ^:private rectangle-layout
  (ffi/layout [:struct [[:x :float] [:y :float] [:width :float] [:height :float]]]))

(ffi/defcfn ^:private image-crop-raw "ImageCrop"
  [:pointer [:by-value [:struct [[:x :float] [:y :float]
                                 [:width :float] [:height :float]]]]] :void)
(ffi/defcfn ^:private image-kernel-convolution-raw "ImageKernelConvolution"
  [:pointer :pointer :int] :void)
(ffi/defcfn ^:private image-resize-raw "ImageResize" [:pointer :int :int] :void)

(defn image-crop!
  "ImageCrop, in place. :x :y :width :height in pixels."
  [img & {:keys [x y width height]
          :or {x 0
               y 0
               width 1
               height 1}}]
  (let [r (ffi/alloc (ffi/layout-size rectangle-layout))]
    (try
      (ffi/write-field r rectangle-layout :x (double x))
      (ffi/write-field r rectangle-layout :y (double y))
      (ffi/write-field r rectangle-layout :width (double width))
      (ffi/write-field r rectangle-layout :height (double height))
      (image-crop-raw img r)
      (finally (ffi/free r)))))

(defn image-resize!
  "ImageResize, in place, bicubic."
  [img w h]
  (image-resize-raw img (int w) (int h)))

(defn image-convolve!
  "ImageKernelConvolution, in place. `kernel` is a flat sequence of floats whose
  count is a perfect square, so a 3x3 is nine of them. raylib takes the COUNT
  rather than the side length."
  [img kernel]
  (native/staged :float kernel (fn [p] (image-kernel-convolution-raw img p (count kernel)))))
