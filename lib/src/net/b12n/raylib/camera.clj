(ns net.b12n.raylib.camera
  "Camera2D and Camera3D: the scoped with-camera-2d / with-camera-3d forms,
  the persistent native Camera3D UpdateCamera writes into directly
  (camera3d-alloc / camera3d-set-target! / camera3d-free!, paired with
  update-camera!), and world-to-screen (GetWorldToScreen).

  with-camera-2d and with-camera-3d use the pointer trick that predates
  jolt's [:by-value [:struct ...]]: they hand C a pointer and rely on
  AArch64 passing anything over 16 bytes indirectly, which is correct there
  and wrong on the x86-64 SysV ABI (see
  docs/guide/struct-by-value-pointer-trick.md). world-to-screen is not a
  migration of that trick; it is newer code and uses jolt's real by-value
  passing for both the Vector3 and the Camera3D, correct on either ABI.

  camera3d-layout stays private: net.b12n.raylib.rays, the one other reader
  of the Camera3D shape in this library, spells the struct out inline in its
  own signatures rather than sharing this layout.

  The 3D geometry a with-camera-3d block draws -- cube!, sphere!, draw-grid,
  the rlgl matrix stack -- is not here; it stays behind in raylib.clj for the
  net.b12n.raylib.models extraction."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.native :as native]))

;; #region camera2d-by-value
;; --- Camera2D: a struct passed BY VALUE (the pointer trick that predates by-value structs) ---
;; raylib's BeginMode2D(Camera2D) takes {Vector2 offset; Vector2 target; float
;; rotation; float zoom}, 24 bytes, passed by value. On the AArch64 (Apple) ABI a
;; composite larger than 16 bytes is passed INDIRECTLY: the caller allocates a
;; copy and passes a POINTER to it, so the binding is [:pointer] and we build the
;; struct (six little-endian floats) in native memory. NOTE: this is AArch64-
;; specific: on the x86-64 SysV ABI the 24 bytes are passed on the stack, which
;; a [:pointer] binding does NOT do (see README). For a portable alternative,
;; apply the same transform with the scalar rlgl matrix ops instead.
(ffi/defcfn ^:private begin-mode-2d-ptr "BeginMode2D" [:pointer] :void)
;; #endregion
(ffi/defcfn end-mode-2d "EndMode2D" [] :void)

(defn with-camera-2d
  "Run (f) with a Camera2D active. Allocates the 24-byte struct, writes the six
  floats (offset.x, offset.y, target.x, target.y, rotation, zoom), passes a
  pointer to BeginMode2D, runs f, then EndMode2D and frees. See the ABI note above."
  [{:keys [offset-x offset-y target-x target-y rotation zoom]
    :or {offset-x 0
         offset-y 0
         target-x 0
         target-y 0
         rotation 0
         zoom 1.0}} f]
  (let [p (ffi/alloc 24)]
    (try
      (ffi/write p :float (double offset-x) 0)
      (ffi/write p :float (double offset-y) 4)
      (ffi/write p :float (double target-x) 8)
      (ffi/write p :float (double target-y) 12)
      (ffi/write p :float (double rotation) 16)
      (ffi/write p :float (double zoom) 20)
      (begin-mode-2d-ptr p)
      (f)
      (end-mode-2d)
      (finally (ffi/free p)))))

(ffi/defcfn begin-mode-3d-ptr "BeginMode3D" [:pointer] :void)
(ffi/defcfn end-mode-3d "EndMode3D" [] :void)

(defn with-camera-3d
  "Run (f) with a Camera3D active (BeginMode3D → f → EndMode3D). Builds the
  44-byte struct in native memory (nine floats + fovy + projection int) and passes
  a pointer. Keys: :pos-x/y/z :target-x/y/z :up-x/y/z :fovy :projection (0 =
  perspective). See the ABI note above."
  [{:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z fovy projection]
    :or {pos-x 0
         pos-y 0
         pos-z 0
         target-x 0
         target-y 0
         target-z 0
         up-x 0
         up-y 1
         up-z 0
         fovy 45
         projection 0}} f]
  (let [p (ffi/alloc 44)]
    (try
      (ffi/write p :float (double pos-x) 0)
      (ffi/write p :float (double pos-y) 4)
      (ffi/write p :float (double pos-z) 8)
      (ffi/write p :float (double target-x) 12)
      (ffi/write p :float (double target-y) 16)
      (ffi/write p :float (double target-z) 20)
      (ffi/write p :float (double up-x) 24)
      (ffi/write p :float (double up-y) 28)
      (ffi/write p :float (double up-z) 32)
      (ffi/write p :float (double fovy) 36)
      (ffi/write p :int (int projection) 40)
      (begin-mode-3d-ptr p)
      (f)
      (end-mode-3d)
      (finally (ffi/free p)))))

;; --- world <-> screen (genuine by-value Camera3D) -----------------------
;; with-camera-3d's Camera3D pointer trick above is correct on AArch64 by
;; accident of the ABI (a struct too large for registers goes via a hidden
;; pointer there) and wrong on x86-64 SysV, where it goes on the stack
;; instead (see this file's Camera2D ABI note above). GetWorldToScreen is new
;; code, not a migration of with-camera-3d, so it uses jolt's real
;; [:by-value [:struct ...]] passing for BOTH the Vector3 and the Camera3D --
;; correct on either ABI, and the pattern the rest of the by-value bindings
;; above already follow.
(def ^:private camera3d-layout
  (ffi/layout [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]))

(ffi/defcfn ^:private get-world-to-screen-raw "GetWorldToScreen"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]]]
  [:by-value [:struct [[:x :float] [:y :float]]]])

(defn world-to-screen
  "GetWorldToScreen. `pos` is [x y z] in world space; `camera` takes the same
  keys as with-camera-3d's opts map (share one map between both calls to
  project a point through the exact camera a frame draws with). Returns
  [screen-x screen-y] as doubles."
  [[px py pz]
   {:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z
           fovy projection]
    :or {pos-x 0
         pos-y 0
         pos-z 0
         target-x 0
         target-y 0
         target-z 0
         up-x 0
         up-y 1
         up-z 0
         fovy 45
         projection 0}}]
  (let [p   (ffi/alloc (ffi/layout-size native/vector3-layout))
        cam (ffi/alloc (ffi/layout-size camera3d-layout))
        out (ffi/alloc (ffi/layout-size native/vector2-layout))]
    (try
      (ffi/write-field p native/vector3-layout :x (double px))
      (ffi/write-field p native/vector3-layout :y (double py))
      (ffi/write-field p native/vector3-layout :z (double pz))
      (ffi/write-field cam camera3d-layout [:position :x] (double pos-x))
      (ffi/write-field cam camera3d-layout [:position :y] (double pos-y))
      (ffi/write-field cam camera3d-layout [:position :z] (double pos-z))
      (ffi/write-field cam camera3d-layout [:target :x] (double target-x))
      (ffi/write-field cam camera3d-layout [:target :y] (double target-y))
      (ffi/write-field cam camera3d-layout [:target :z] (double target-z))
      (ffi/write-field cam camera3d-layout [:up :x] (double up-x))
      (ffi/write-field cam camera3d-layout [:up :y] (double up-y))
      (ffi/write-field cam camera3d-layout [:up :z] (double up-z))
      (ffi/write-field cam camera3d-layout :fovy (double fovy))
      (ffi/write-field cam camera3d-layout :projection (int projection))
      (get-world-to-screen-raw out p cam)
      [(ffi/read-field out native/vector2-layout :x)
       (ffi/read-field out native/vector2-layout :y)]
      (finally
        (ffi/free p)
        (ffi/free cam)
        (ffi/free out)))))

;; --- a persistent native Camera3D, mutated by UpdateCamera (camera-3d-free) --
;; UpdateCamera reads the mouse/wheel/keys itself and writes position/target/up
;; back into the SAME struct, so (unlike with-camera-3d's per-frame map) this
;; buffer has to survive across frames -- allocate it once outside the loop.
(ffi/defcfn update-camera! "UpdateCamera" [:pointer :int] :void)
(ffi/defcfn disable-cursor! "DisableCursor" [] :void)
(ffi/defcfn enable-cursor! "EnableCursor" [] :void)

(def ^:const CAMERA-CUSTOM 0)
(def ^:const CAMERA-FREE 1)
(def ^:const CAMERA-ORBITAL 2)
(def ^:const CAMERA-FIRST-PERSON 3)
(def ^:const CAMERA-THIRD-PERSON 4)

(defn camera3d-alloc
  "A persistent native Camera3D from the same keys with-camera-3d takes.
  Pair with camera3d-free!."
  [& {:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z fovy projection]
      :or {pos-x 0
           pos-y 0
           pos-z 0
           target-x 0
           target-y 0
           target-z 0
           up-x 0
           up-y 1
           up-z 0
           fovy 45
           projection 0}}]
  (let [cam (ffi/alloc (ffi/layout-size camera3d-layout))]
    (ffi/write-field cam camera3d-layout [:position :x] (double pos-x))
    (ffi/write-field cam camera3d-layout [:position :y] (double pos-y))
    (ffi/write-field cam camera3d-layout [:position :z] (double pos-z))
    (ffi/write-field cam camera3d-layout [:target :x] (double target-x))
    (ffi/write-field cam camera3d-layout [:target :y] (double target-y))
    (ffi/write-field cam camera3d-layout [:target :z] (double target-z))
    (ffi/write-field cam camera3d-layout [:up :x] (double up-x))
    (ffi/write-field cam camera3d-layout [:up :y] (double up-y))
    (ffi/write-field cam camera3d-layout [:up :z] (double up-z))
    (ffi/write-field cam camera3d-layout :fovy (double fovy))
    (ffi/write-field cam camera3d-layout :projection (int projection))
    cam))

(defn camera3d-free!
  [cam]
  (ffi/free cam))

(defn camera3d-set-target!
  [cam [x y z]]
  (ffi/write-field cam camera3d-layout [:target :x] (double x))
  (ffi/write-field cam camera3d-layout [:target :y] (double y))
  (ffi/write-field cam camera3d-layout [:target :z] (double z)))
