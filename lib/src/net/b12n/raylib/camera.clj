(ns net.b12n.raylib.camera
  "Camera2D and Camera3D: the scoped with-camera-2d / with-camera-3d forms,
  the persistent native Camera3D UpdateCamera writes into directly
  (camera3d-alloc / camera3d-set-target! / camera3d-free!, paired with
  update-camera!), and world-to-screen (GetWorldToScreen).

  Every Camera2D/Camera3D crosses the boundary with jolt's
  [:by-value [:struct ...]] passing, which follows the platform ABI: correct
  on AArch64 and x86-64 alike. (These used to bind BeginMode2D/BeginMode3D
  as [:pointer], which only worked because AArch64 happens to pass a struct
  over 16 bytes through a hidden pointer; x86-64 SysV copies it onto the
  stack. See docs/guide/struct-by-value-pointer-trick.md for that history.)

  camera3d-layout stays private: net.b12n.raylib.rays, the one other reader
  of the Camera3D shape in this library, spells the struct out inline in its
  own signatures rather than sharing this layout.

  The 3D geometry a with-camera-3d block draws -- cube!, sphere!, draw-grid,
  the rlgl matrix stack -- is not here; it lives in net.b12n.raylib.models."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.native :as native]))

;; #region camera2d-by-value
;; --- Camera2D: a struct passed BY VALUE ---------------------------------------
;; raylib's BeginMode2D(Camera2D) takes {Vector2 offset; Vector2 target; float
;; rotation; float zoom}, 24 bytes, by value. The struct is staged in native
;; memory and the binding's [:by-value [:struct ...]] argument copies it the way
;; the ABI wants: through a hidden pointer on AArch64, on the stack on x86-64.
(def ^:private camera2d-layout
  (ffi/layout [:struct [[:offset [:struct [[:x :float] [:y :float]]]]
                        [:target [:struct [[:x :float] [:y :float]]]]
                        [:rotation :float]
                        [:zoom :float]]]))

(ffi/defcfn ^:private begin-mode-2d-raw "BeginMode2D"
  [[:by-value [:struct [[:offset [:struct [[:x :float] [:y :float]]]]
                        [:target [:struct [[:x :float] [:y :float]]]]
                        [:rotation :float]
                        [:zoom :float]]]]]
  :void)
;; #endregion
(ffi/defcfn end-mode-2d "EndMode2D" [] :void)

(defn with-camera-2d
  "Run (f) with a Camera2D active (BeginMode2D -> f -> EndMode2D). Keys:
  :offset-x/y :target-x/y :rotation :zoom."
  [{:keys [offset-x offset-y target-x target-y rotation zoom]
    :or {offset-x 0
         offset-y 0
         target-x 0
         target-y 0
         rotation 0
         zoom 1.0}} f]
  (let [p (ffi/alloc (ffi/layout-size camera2d-layout))]
    (try
      (ffi/write-field p camera2d-layout [:offset :x] (double offset-x))
      (ffi/write-field p camera2d-layout [:offset :y] (double offset-y))
      (ffi/write-field p camera2d-layout [:target :x] (double target-x))
      (ffi/write-field p camera2d-layout [:target :y] (double target-y))
      (ffi/write-field p camera2d-layout :rotation (double rotation))
      (ffi/write-field p camera2d-layout :zoom (double zoom))
      (begin-mode-2d-raw p)
      (f)
      (end-mode-2d)
      (finally (ffi/free p)))))

;; --- Camera3D: three Vector3 + fovy + projection, 44 bytes, by value ----------
(def ^:private camera3d-layout
  (ffi/layout [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]))

(ffi/defcfn ^:private begin-mode-3d-raw "BeginMode3D"
  [[:by-value [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]]]
  :void)
(ffi/defcfn end-mode-3d "EndMode3D" [] :void)

(defn begin-mode-3d-ptr
  "BeginMode3D with the native Camera3D at pointer `p` (the buffer
  camera3d-alloc below builds). The struct is passed by value; `p` only says
  where to copy it from. Public because UpdateCamera mutates a Camera3D in
  place across frames (see the persistent-buffer section below): a caller
  driving that persistent camera has no with-camera-3d-equivalent wrapper to
  go through, and has to open and close 3D mode around it directly."
  [p]
  (begin-mode-3d-raw p))

(defn- write-camera3d!
  [cam {:keys [pos-x pos-y pos-z target-x target-y target-z up-x up-y up-z fovy projection]
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
  cam)

(defn with-camera-3d
  "Run (f) with a Camera3D active (BeginMode3D -> f -> EndMode3D). Keys:
  :pos-x/y/z :target-x/y/z :up-x/y/z :fovy :projection (0 = perspective)."
  [opts f]
  (let [p (ffi/alloc (ffi/layout-size camera3d-layout))]
    (try
      (write-camera3d! p opts)
      (begin-mode-3d-raw p)
      (f)
      (end-mode-3d)
      (finally (ffi/free p)))))

;; --- world <-> screen -----------------------------------------------------
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
  [[px py pz] camera]
  (let [p   (ffi/alloc (ffi/layout-size native/vector3-layout))
        cam (ffi/alloc (ffi/layout-size camera3d-layout))
        out (ffi/alloc (ffi/layout-size native/vector2-layout))]
    (try
      (ffi/write-field p native/vector3-layout :x (double px))
      (ffi/write-field p native/vector3-layout :y (double py))
      (ffi/write-field p native/vector3-layout :z (double pz))
      (write-camera3d! cam camera)
      (get-world-to-screen-raw out p cam)
      [(double (ffi/read-field out native/vector2-layout :x))
       (double (ffi/read-field out native/vector2-layout :y))]
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
  [& {:as opts}]
  (write-camera3d! (ffi/alloc (ffi/layout-size camera3d-layout)) opts))

(defn camera3d-free!
  [cam]
  (ffi/free cam))

(defn camera3d-set-target!
  [cam [x y z]]
  (ffi/write-field cam camera3d-layout [:target :x] (double x))
  (ffi/write-field cam camera3d-layout [:target :y] (double y))
  (ffi/write-field cam camera3d-layout [:target :z] (double z)))
