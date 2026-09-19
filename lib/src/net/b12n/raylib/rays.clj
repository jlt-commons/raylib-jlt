(ns net.b12n.raylib.rays
  "Picking a point in the 3D scene: GetScreenToWorldRay (screen-to-world-ray),
  GetRayCollisionBox (ray-collision-box), DrawRay (draw-ray!), and
  IsCursorHidden (cursor-hidden?), all genuinely by value.

  screen-to-world-ray builds the Camera3D argument via
  net.b12n.raylib.camera/camera3d-alloc rather than its own layout: the
  GetScreenToWorldRay C signature below spells the Camera3D struct out inline
  instead of sharing net.b12n.raylib.camera's private camera3d-layout, so this
  is the one place in the library with two independent descriptions of the
  same 44-byte struct. See that namespace's docstring."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.camera :as camera]
   [net.b12n.raylib.native :as native]))

;; --- rays: picking a point in the 3D scene, all by value -----------------
;; GetScreenToWorldRay is the exact inverse of GetWorldToScreen above, and it is
;; bound the same way: a by-value Vector2 in, a by-value Camera3D in, a by-value
;; Ray out. GetRayCollisionBox then takes that Ray with an axis-aligned
;; BoundingBox and hands back a RayCollision, whose first field is a one-byte C
;; _Bool rather than an int. `:bool` is what reads that correctly, and it is also
;; what makes the layout land `distance` at offset 4 instead of 1. Sizes are
;; asserted at load rather than assumed, since a wrong offset here reads a
;; plausible float out of the wrong bytes and never errors.
(def ^:private ray-layout
  (ffi/layout [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]))

(def ^:private ray-collision-layout
  (ffi/layout [:struct [[:hit :bool]
                        [:distance :float]
                        [:point  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:normal [:struct [[:x :float] [:y :float] [:z :float]]]]]]))

(assert (= 24 (ffi/layout-size ray-layout)) "Ray is two Vector3s, 24 bytes")
(assert (= 32 (ffi/layout-size ray-collision-layout))
        "RayCollision is bool + pad + float + two Vector3s, 32 bytes")

(ffi/defcfn ^:private get-screen-to-world-ray-raw "GetScreenToWorldRay"
  [[:by-value [:struct [[:x :float] [:y :float]]]]
   [:by-value [:struct [[:position [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:target   [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:up       [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:fovy :float]
                        [:projection :int32]]]]]
  [:by-value [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                       [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]])

(ffi/defcfn ^:private get-ray-collision-box-raw "GetRayCollisionBox"
  [[:by-value [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]]
   [:by-value [:struct [[:min [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:max [:struct [[:x :float] [:y :float] [:z :float]]]]]]]]
  [:by-value [:struct [[:hit :bool]
                       [:distance :float]
                       [:point  [:struct [[:x :float] [:y :float] [:z :float]]]]
                       [:normal [:struct [[:x :float] [:y :float] [:z :float]]]]]]])

(ffi/defcfn ^:private draw-ray-raw "DrawRay"
  [[:by-value [:struct [[:position  [:struct [[:x :float] [:y :float] [:z :float]]]]
                        [:direction [:struct [[:x :float] [:y :float] [:z :float]]]]]]]
   :uint]
  :void)

(ffi/defcfn ^:private cursor-hidden-raw "IsCursorHidden" [] :int)

(defn cursor-hidden?
  "IsCursorHidden, so an example can ask whether it currently owns the pointer
  rather than tracking a flag of its own alongside disable-cursor!."
  []
  (not (zero? (bit-and (cursor-hidden-raw) 0xff))))

(defn- ray->ptr!
  "Allocate a ray-layout buffer from {:position [x y z] :direction [x y z]}.
  Caller frees."
  [{:keys [position direction]}]
  (let [[px py pz] position
        [dx dy dz] direction
        p (ffi/alloc (ffi/layout-size ray-layout))]
    (ffi/write-field p ray-layout [:position :x] (double px))
    (ffi/write-field p ray-layout [:position :y] (double py))
    (ffi/write-field p ray-layout [:position :z] (double pz))
    (ffi/write-field p ray-layout [:direction :x] (double dx))
    (ffi/write-field p ray-layout [:direction :y] (double dy))
    (ffi/write-field p ray-layout [:direction :z] (double dz))
    p))

(defn screen-to-world-ray
  "GetScreenToWorldRay. `screen` is [x y] in window coordinates and `camera`
  takes the same keys as with-camera-3d's opts map, so one map can be shared
  between the ray and the frame it is picking in. Returns
  {:position [x y z] :direction [x y z]}, the unit direction included."
  [[sx sy] camera]
  (let [pos (ffi/alloc (ffi/layout-size native/vector2-layout))
        cam (camera/camera3d-alloc camera)
        out (ffi/alloc (ffi/layout-size ray-layout))]
    (try
      (ffi/write-field pos native/vector2-layout :x (double sx))
      (ffi/write-field pos native/vector2-layout :y (double sy))
      (get-screen-to-world-ray-raw out pos cam)
      {:position [(ffi/read-field out ray-layout [:position :x])
                  (ffi/read-field out ray-layout [:position :y])
                  (ffi/read-field out ray-layout [:position :z])]
       :direction [(ffi/read-field out ray-layout [:direction :x])
                   (ffi/read-field out ray-layout [:direction :y])
                   (ffi/read-field out ray-layout [:direction :z])]}
      (finally
        (ffi/free pos)
        (camera/camera3d-free! cam)
        (ffi/free out)))))

(defn ray-collision-box
  "GetRayCollisionBox against the axis-aligned box spanning `lo` to `hi`, both
  [x y z]. Returns {:hit? :distance :point :normal}; everything but :hit? is
  meaningless when :hit? is false, exactly as in the C."
  [ray lo hi]
  (let [r (ray->ptr! ray)
        box (ffi/alloc (ffi/layout-size ray-layout))     ; BoundingBox is two Vector3s too
        out (ffi/alloc (ffi/layout-size ray-collision-layout))
        [lx ly lz] lo
        [hx hy hz] hi]
    (try
      (ffi/write-field box ray-layout [:position :x] (double lx))
      (ffi/write-field box ray-layout [:position :y] (double ly))
      (ffi/write-field box ray-layout [:position :z] (double lz))
      (ffi/write-field box ray-layout [:direction :x] (double hx))
      (ffi/write-field box ray-layout [:direction :y] (double hy))
      (ffi/write-field box ray-layout [:direction :z] (double hz))
      (get-ray-collision-box-raw out r box)
      {:hit? (ffi/read-field out ray-collision-layout :hit)
       :distance (ffi/read-field out ray-collision-layout :distance)
       :point [(ffi/read-field out ray-collision-layout [:point :x])
               (ffi/read-field out ray-collision-layout [:point :y])
               (ffi/read-field out ray-collision-layout [:point :z])]
       :normal [(ffi/read-field out ray-collision-layout [:normal :x])
                (ffi/read-field out ray-collision-layout [:normal :y])
                (ffi/read-field out ray-collision-layout [:normal :z])]}
      (finally
        (ffi/free r)
        (ffi/free box)
        (ffi/free out)))))

(defn draw-ray!
  "DrawRay: the ray drawn as a long line from its origin. Inside a BeginMode3D
  block, like the other draw-*! calls."
  [ray color]
  (let [r (ray->ptr! ray)]
    (try (draw-ray-raw r color)
         (finally (ffi/free r)))))
