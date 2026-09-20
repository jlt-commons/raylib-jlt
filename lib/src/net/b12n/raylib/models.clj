(ns net.b12n.raylib.models
  "3D geometry drawn inside a with-camera-3d block: rlgl immediate-mode
  stand-ins (cube!, sphere!, draw-grid, the rlgl matrix stack), scalar calls
  with no struct arguments, plus the real Draw{Cube,Sphere,Cylinder,Capsule}*
  calls and DrawPlane, genuinely by value now that jolt's
  [:by-value [:struct ...]] works.

  None of this touches Camera3D itself -- with-camera-3d, camera3d-alloc and
  the persistent-camera helpers live in net.b12n.raylib.camera, and every fn
  here is called from inside a caller-supplied with-camera-3d block rather
  than reading camera state of its own."
  (:require
   [jolt.ffi :as ffi]
   [net.b12n.raylib.color :as color]
   [net.b12n.raylib.native :as native]
   [net.b12n.raylib.rlgl :as rlgl]
   [net.b12n.raylib.shaders :as shaders]))

;; --- Camera3D + 3D geometry --------------------------------------------------
;; Camera3D is 44 bytes (three Vector3 + a float + an int), passed BY VALUE to
;; BeginMode3D, the same >16-byte-struct-by-pointer approach as Camera2D. 3D
;; shape helpers like DrawCube take a Vector3 BY VALUE (a 12-byte float struct
;; passed in FP registers, which the pointer trick does NOT cover), so draw 3D
;; geometry with rlgl immediate mode (rl-vertex-3f) instead. DrawGrid is scalar.
(ffi/defcfn draw-grid    "DrawGrid"    [:int :float] :void)
(ffi/defcfn ^:private rl-vertex-3f-raw "rlVertex3f"  [:float :float :float] :void)

(defn rl-vertex-3f
  "One vertex in 3D.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-vertex-3f-raw (double a0) (double a1) (double a2)))

;; rlgl matrix stack, nested transforms for immediate-mode geometry. rlgl applies
;; the current transform to each rlVertex* at submit time, so push/rotate/translate
;; around a cube! call moves it (used by rlgl-solar-system).
(ffi/defcfn rl-push-matrix "rlPushMatrix" [] :void)
(ffi/defcfn rl-pop-matrix  "rlPopMatrix"  [] :void)
(ffi/defcfn ^:private rl-translatef-raw  "rlTranslatef" [:float :float :float] :void)

(defn rl-translatef
  "Translate the current matrix.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-translatef-raw (double a0) (double a1) (double a2)))

(ffi/defcfn ^:private rl-rotatef-raw     "rlRotatef"    [:float :float :float :float] :void)

(defn rl-rotatef
  "Rotate the current matrix, angle first.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2 a3]
  (rl-rotatef-raw (double a0) (double a1) (double a2) (double a3)))

(ffi/defcfn ^:private rl-scalef-raw      "rlScalef"     [:float :float :float] :void)

(defn rl-scalef
  "Scale the current matrix.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1 a2]
  (rl-scalef-raw (double a0) (double a1) (double a2)))

(defn- shade-color
  "Darken a packed Color by factor f (fakes lighting so cube faces read as 3D)."
  [color f]
  (color/rgba (int (* f (bit-and color 0xff)))
              (int (* f (bit-and (bit-shift-right color 8) 0xff)))
              (int (* f (bit-and (bit-shift-right color 16) 0xff)))
              255))

(defn- quad-3f
  "Two rlgl triangles for a quad, given a shaded color and a vector of its four
  [x y z] corners in a→b→c→d winding order."
  [color [a b c d]]
  (rlgl/rl-color! color)
  (let [[ax ay az] a [bx by bz] b [cx cy cz] c [dx dy dz] d]
    (rl-vertex-3f ax ay az) (rl-vertex-3f bx by bz) (rl-vertex-3f cx cy cz)
    (rl-vertex-3f ax ay az) (rl-vertex-3f cx cy cz) (rl-vertex-3f dx dy dz)))

(defn cube!
  "Draw an axis-aligned box via rlgl immediate mode, its faces shaded from the
  packed `:color` for depth. Must be called inside a BeginMode3D block (see
  with-camera-3d). Keyword args:
    :pos   [x y z] centre           (default [0 0 0])
    :size  a number for a uniform cube, or [sx sy sz]  (default 1)
    :color a packed Color           (default BLACK)"
  [& {:keys [pos size color]
      :or {pos [0.0 0.0 0.0]
           size 1.0
           color color/BLACK}}]
  (let [[cx cy cz] pos
        [sx sy sz] (if (number? size) [size size size] size)
        hx (/ sx 2.0) hy (/ sy 2.0) hz (/ sz 2.0)
        x0 (- cx hx) x1 (+ cx hx) y0 (- cy hy) y1 (+ cy hy) z0 (- cz hz) z1 (+ cz hz)
        ;; the eight corners, named a<x><y><z> by which extreme each axis takes
        a000 [x0 y0 z0] a100 [x1 y0 z0] a010 [x0 y1 z0] a110 [x1 y1 z0]
        a001 [x0 y0 z1] a101 [x1 y0 z1] a011 [x0 y1 z1] a111 [x1 y1 z1]]
    (rlgl/rl-begin rlgl/RL-TRIANGLES)
    (quad-3f (shade-color color 1.0)  [a001 a101 a111 a011])   ; front  +z
    (quad-3f (shade-color color 0.5)  [a100 a000 a010 a110])   ; back   -z
    (quad-3f (shade-color color 0.7)  [a000 a001 a011 a010])   ; left   -x
    (quad-3f (shade-color color 0.85) [a101 a100 a110 a111])   ; right  +x
    (quad-3f (shade-color color 1.0)  [a011 a111 a110 a010])   ; top    +y
    (quad-3f (shade-color color 0.4)  [a000 a100 a101 a001])   ; bottom -y
    (rlgl/rl-end)))

(defn sphere!
  "Draw a sphere via rlgl immediate mode (lat/long tessellation), faces shaded
  from the packed `:color` for depth (brighter toward +y). Must be called inside
  a BeginMode3D block (see with-camera-3d). Keyword args:
    :pos    [x y z] centre        (default [0 0 0])
    :radius a number              (default 0.5)
    :rings  latitude bands        (default 12)
    :slices longitude sectors     (default 16)
    :color  a packed Color        (default BLACK)"
  [& {:keys [pos radius rings slices color]
      :or {pos [0.0 0.0 0.0]
           radius 0.5
           rings 12
           slices 16
           color color/BLACK}}]
  (let [[cx cy cz] pos
        two-pi (* 2.0 Math/PI)]
    (rlgl/rl-begin rlgl/RL-TRIANGLES)
    (dotimes [i rings]
      (let [lat0 (- (* Math/PI (/ (double i) rings)) (/ Math/PI 2.0))
            lat1 (- (* Math/PI (/ (double (inc i)) rings)) (/ Math/PI 2.0))
            y0 (Math/sin lat0) y1 (Math/sin lat1)
            r0 (Math/cos lat0) r1 (Math/cos lat1)
            brightness (+ 0.45 (* 0.55 (/ (+ y0 y1 2.0) 4.0)))
            shaded (shade-color color brightness)]
        (dotimes [j slices]
          (let [lon0 (* two-pi (/ (double j) slices))
                lon1 (* two-pi (/ (double (inc j)) slices))
                s0 (Math/sin lon0) c0 (Math/cos lon0)
                s1 (Math/sin lon1) c1 (Math/cos lon1)
                p00 [(+ cx (* radius r0 c0)) (+ cy (* radius y0)) (+ cz (* radius r0 s0))]
                p01 [(+ cx (* radius r0 c1)) (+ cy (* radius y0)) (+ cz (* radius r0 s1))]
                p10 [(+ cx (* radius r1 c0)) (+ cy (* radius y1)) (+ cz (* radius r1 s0))]
                p11 [(+ cx (* radius r1 c1)) (+ cy (* radius y1)) (+ cz (* radius r1 s1))]]
            (quad-3f shaded [p00 p10 p11 p01])))))
    (rlgl/rl-end)))

;; --- geometric primitives, genuinely by value (geometric-shapes) --------
;; raylib's real Draw{Cube,Sphere,Cylinder,Capsule}* calls, now that
;; [:by-value [:struct ...]] works -- named draw-*! rather than reusing
;; cube!/sphere! (the existing rlgl immediate-mode stand-ins), since these
;; are a genuinely different code path, not a replacement for them.
(defn- vec3->ptr!
  "Allocate a vector3-layout buffer and write [x y z] into it. Caller frees."
  [[x y z]]
  (let [p (ffi/alloc (ffi/layout-size native/vector3-layout))]
    (ffi/write-field p native/vector3-layout :x (double x))
    (ffi/write-field p native/vector3-layout :y (double y))
    (ffi/write-field p native/vector3-layout :z (double z))
    p))

(ffi/defcfn ^:private draw-cube-raw "DrawCube"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :uint]
  :void)
(ffi/defcfn ^:private draw-cube-wires-raw "DrawCubeWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :uint]
  :void)
(ffi/defcfn ^:private draw-sphere-raw "DrawSphere"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :uint]
  :void)
(ffi/defcfn ^:private draw-sphere-wires-raw "DrawSphereWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :int :int :uint]
  :void)
(ffi/defcfn ^:private draw-cylinder-raw "DrawCylinder"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :int :uint]
  :void)
(ffi/defcfn ^:private draw-cylinder-wires-raw "DrawCylinderWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :float :float :int :uint]
  :void)
(ffi/defcfn ^:private draw-capsule-raw "DrawCapsule"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :int :int :uint]
  :void)
(ffi/defcfn ^:private draw-capsule-wires-raw "DrawCapsuleWires"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:x :float] [:y :float] [:z :float]]]] :float :int :int :uint]
  :void)

(defn draw-cube!
  "DrawCube. :pos :width :height :length :color."
  [& {:keys [pos width height length color]
      :or {pos [0.0 0.0 0.0]
           width 1.0
           height 1.0
           length 1.0
           color color/BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cube-raw p (double width) (double height) (double length) color)
         (finally (ffi/free p)))))

(defn draw-cube-wires!
  "DrawCubeWires. :pos :width :height :length :color."
  [& {:keys [pos width height length color]
      :or {pos [0.0 0.0 0.0]
           width 1.0
           height 1.0
           length 1.0
           color color/BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cube-wires-raw p (double width) (double height) (double length) color)
         (finally (ffi/free p)))))

(defn draw-sphere!
  [pos radius color]
  (let [p (vec3->ptr! pos)]
    (try (draw-sphere-raw p (double radius) color)
         (finally (ffi/free p)))))

(defn draw-sphere-wires!
  "DrawSphereWires. :pos :radius :rings :slices :color."
  [& {:keys [pos radius rings slices color]
      :or {pos [0.0 0.0 0.0]
           radius 0.5
           rings 16
           slices 16
           color color/BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-sphere-wires-raw p (double radius) (int rings) (int slices) color)
         (finally (ffi/free p)))))

(defn draw-cylinder!
  "DrawCylinder. :pos :radius-top :radius-bottom :height :slices :color."
  [& {:keys [pos radius-top radius-bottom height slices color]
      :or {pos [0.0 0.0 0.0]
           radius-top 1.0
           radius-bottom 1.0
           height 1.0
           slices 16
           color color/BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cylinder-raw p (double radius-top) (double radius-bottom) (double height)
                            (int slices) color)
         (finally (ffi/free p)))))

(defn draw-cylinder-wires!
  "DrawCylinderWires. :pos :radius-top :radius-bottom :height :slices :color."
  [& {:keys [pos radius-top radius-bottom height slices color]
      :or {pos [0.0 0.0 0.0]
           radius-top 1.0
           radius-bottom 1.0
           height 1.0
           slices 16
           color color/BLACK}}]
  (let [p (vec3->ptr! pos)]
    (try (draw-cylinder-wires-raw p (double radius-top) (double radius-bottom) (double height)
                                  (int slices) color)
         (finally (ffi/free p)))))

(defn draw-capsule!
  "DrawCapsule. :start-pos :end-pos :radius :slices :rings :color."
  [& {:keys [start-pos end-pos radius slices rings color]
      :or {start-pos [0.0 0.0 0.0]
           end-pos [0.0 1.0 0.0]
           radius 0.5
           slices 8
           rings 8
           color color/BLACK}}]
  (let [p1 (vec3->ptr! start-pos)
        p2 (vec3->ptr! end-pos)]
    (try (draw-capsule-raw p1 p2 (double radius) (int slices) (int rings) color)
         (finally (ffi/free p1) (ffi/free p2)))))

(defn draw-capsule-wires!
  "DrawCapsuleWires. :start-pos :end-pos :radius :slices :rings :color."
  [& {:keys [start-pos end-pos radius slices rings color]
      :or {start-pos [0.0 0.0 0.0]
           end-pos [0.0 1.0 0.0]
           radius 0.5
           slices 8
           rings 8
           color color/BLACK}}]
  (let [p1 (vec3->ptr! start-pos)
        p2 (vec3->ptr! end-pos)]
    (try (draw-capsule-wires-raw p1 p2 (double radius) (int slices) (int rings) color)
         (finally (ffi/free p1) (ffi/free p2)))))

;; --- ground plane, genuinely by value (camera-3d-split-screen) ----------
(ffi/defcfn ^:private draw-plane-raw "DrawPlane"
  [[:by-value [:struct [[:x :float] [:y :float] [:z :float]]]]
   [:by-value [:struct [[:x :float] [:y :float]]]]
   :uint]
  :void)

(defn draw-plane!
  "DrawPlane. :pos :size :color."
  [& {:keys [pos size color]
      :or {pos [0.0 0.0 0.0]
           size [1.0 1.0]
           color color/BLACK}}]
  (let [p (vec3->ptr! pos)
        s (native/vec2->ptr! size)]
    (try (draw-plane-raw p s color)
         (finally (ffi/free p) (ffi/free s)))))

;; --- generated meshes --------------------------------------------------------
;; raylib's GenMesh* family returns a Mesh by value, and DrawMesh takes a Mesh,
;; a Material and a Matrix by value. jolt handles both directions, with two
;; rules that are easy to get wrong:
;;
;;   * An aggregate-RETURNING call takes a caller-owned destination pointer as
;;     its FIRST jolt argument, writes the C return there and answers it. So
;;     (gen-mesh-cube-raw dest w h l) is four arguments, not three. The Image
;;     generators in net.b12n.raylib.images have the same shape.
;;   * An aggregate ARGUMENT is a pointer to the struct bytes, so draw-mesh-raw
;;     takes three pointers even though the C signature is three values.
;;
;; The field order below is from /opt/homebrew/include/raylib.h, the header
;; that matches the library this links. Read layouts from that header rather
;; than from a nearby raylib source checkout, which may be any number of
;; commits past the release. For Mesh specifically the two happen to agree
;; today, and an earlier version of this comment wrongly claimed otherwise;
;; the habit still matters, because the same checkout has three times now
;; offered a FUNCTION the released library does not export, each of which
;; compiled and then failed at run time.
(def mesh-layout
  "The `ffi/layout` value for raylib 6.0's Mesh, 120 bytes."
  (ffi/layout [:struct [[:vertex-count :int] [:triangle-count :int]
                        [:vertices :pointer] [:texcoords :pointer]
                        [:texcoords2 :pointer] [:normals :pointer]
                        [:tangents :pointer] [:colors :pointer]
                        [:indices :pointer]
                        [:bone-count :int]
                        [:bone-indices :pointer] [:bone-weights :pointer]
                        [:anim-vertices :pointer] [:anim-normals :pointer]
                        [:vao-id :uint] [:vbo-id :pointer]]]))

(def material-layout
  "The `ffi/layout` value for raylib's Material, 40 bytes: an inlined Shader
  {uint id; int *locs;}, then a MaterialMap pointer, then four float params."
  (ffi/layout [:struct [[:shader-id :uint] [:shader-locs :pointer]
                        [:maps :pointer] [:params [:array :float 4]]]]))

(def matrix-layout
  "The `ffi/layout` value for raylib's Matrix, 64 bytes. raylib declares it in
  the order m0 m4 m8 m12, m1 m5 m9 m13, ... so that is the memory order, and the
  identity diagonal is m0 m5 m10 m15."
  (ffi/layout [:struct [[:m0 :float] [:m4 :float] [:m8 :float] [:m12 :float]
                        [:m1 :float] [:m5 :float] [:m9 :float] [:m13 :float]
                        [:m2 :float] [:m6 :float] [:m10 :float] [:m14 :float]
                        [:m3 :float] [:m7 :float] [:m11 :float] [:m15 :float]]]))

(ffi/defcfn ^:private gen-mesh-cube-raw "GenMeshCube"
  [:float :float :float]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private gen-mesh-sphere-raw "GenMeshSphere"
  [:float :int :int]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private gen-mesh-torus-raw "GenMeshTorus"
  [:float :float :int :int]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private gen-mesh-knot-raw "GenMeshKnot"
  [:float :float :int :int]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private gen-mesh-cylinder-raw "GenMeshCylinder"
  [:float :float :int]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private gen-mesh-cone-raw "GenMeshCone"
  [:float :float :int]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private gen-mesh-plane-raw "GenMeshPlane"
  [:float :float :int :int]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private gen-mesh-hemisphere-raw "GenMeshHemiSphere"
  [:float :int :int]
  [:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                       [:vertices :pointer] [:texcoords :pointer]
                       [:texcoords2 :pointer] [:normals :pointer]
                       [:tangents :pointer] [:colors :pointer]
                       [:indices :pointer] [:bone-count :int]
                       [:bone-indices :pointer] [:bone-weights :pointer]
                       [:anim-vertices :pointer] [:anim-normals :pointer]
                       [:vao-id :uint] [:vbo-id :pointer]]]])

(ffi/defcfn ^:private load-material-default-raw "LoadMaterialDefault"
  [] [:by-value [:struct [[:shader-id :uint] [:shader-locs :pointer]
                          [:maps :pointer] [:params [:array :float 4]]]]])

(ffi/defcfn ^:private draw-mesh-raw "DrawMesh"
  [[:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                        [:vertices :pointer] [:texcoords :pointer]
                        [:texcoords2 :pointer] [:normals :pointer]
                        [:tangents :pointer] [:colors :pointer]
                        [:indices :pointer] [:bone-count :int]
                        [:bone-indices :pointer] [:bone-weights :pointer]
                        [:anim-vertices :pointer] [:anim-normals :pointer]
                        [:vao-id :uint] [:vbo-id :pointer]]]]
   [:by-value [:struct [[:shader-id :uint] [:shader-locs :pointer]
                        [:maps :pointer] [:params [:array :float 4]]]]]
   [:by-value [:struct [[:m0 :float] [:m4 :float] [:m8 :float] [:m12 :float]
                        [:m1 :float] [:m5 :float] [:m9 :float] [:m13 :float]
                        [:m2 :float] [:m6 :float] [:m10 :float] [:m14 :float]
                        [:m3 :float] [:m7 :float] [:m11 :float] [:m15 :float]]]]]
  :void)

(ffi/defcfn ^:private unload-mesh-raw "UnloadMesh"
  [[:by-value [:struct [[:vertex-count :int] [:triangle-count :int]
                        [:vertices :pointer] [:texcoords :pointer]
                        [:texcoords2 :pointer] [:normals :pointer]
                        [:tangents :pointer] [:colors :pointer]
                        [:indices :pointer] [:bone-count :int]
                        [:bone-indices :pointer] [:bone-weights :pointer]
                        [:anim-vertices :pointer] [:anim-normals :pointer]
                        [:vao-id :uint] [:vbo-id :pointer]]]]]
  :void)

(defn mesh-alloc
  "A zeroed 120-byte buffer the GenMesh* fns can write a Mesh into. Caller frees
  with mesh-free! after unload-mesh!."
  []
  (ffi/alloc (ffi/layout-size mesh-layout)))

(defn mesh-free!
  "Free a buffer from mesh-alloc. Does NOT release the GPU-side vertex buffers;
  unload-mesh! does that, and has to run first."
  [m]
  (ffi/free m))

(defn mesh-vertex-count [m] (ffi/read-field m mesh-layout :vertex-count))
(defn mesh-triangle-count [m] (ffi/read-field m mesh-layout :triangle-count))
(defn mesh-vao-id [m] (ffi/read-field m mesh-layout :vao-id))

(defn mesh-cube!
  "GenMeshCube into `m`. raylib uploads the vertex data to the GPU itself, so the
  mesh has a live vao id as soon as this returns."
  [m width height length]
  (gen-mesh-cube-raw m (double width) (double height) (double length)))

(defn mesh-sphere!
  "GenMeshSphere into `m`: a UV sphere of `rings` by `slices`."
  [m radius rings slices]
  (gen-mesh-sphere-raw m (double radius) (int rings) (int slices)))

(defn mesh-hemisphere!
  "GenMeshHemiSphere into `m`: half a sphere, with no bottom cap."
  [m radius rings slices]
  (gen-mesh-hemisphere-raw m (double radius) (int rings) (int slices)))

(defn mesh-torus!
  "GenMeshTorus into `m`. `radius` is the tube, `size` the ring it sweeps."
  [m radius size rad-seg sides]
  (gen-mesh-torus-raw m (double radius) (double size) (int rad-seg) (int sides)))

(defn mesh-knot!
  "GenMeshKnot into `m`: a trefoil, same parameters as the torus."
  [m radius size rad-seg sides]
  (gen-mesh-knot-raw m (double radius) (double size) (int rad-seg) (int sides)))

(defn mesh-cylinder!
  "GenMeshCylinder into `m`."
  [m radius height slices]
  (gen-mesh-cylinder-raw m (double radius) (double height) (int slices)))

(defn mesh-cone!
  "GenMeshCone into `m`."
  [m radius height slices]
  (gen-mesh-cone-raw m (double radius) (double height) (int slices)))

(defn mesh-plane!
  "GenMeshPlane into `m`, subdivided `res-x` by `res-z`."
  [m width length res-x res-z]
  (gen-mesh-plane-raw m (double width) (double length) (int res-x) (int res-z)))

(defn material-default
  "LoadMaterialDefault into a fresh 40-byte buffer: raylib's default shader with
  a white diffuse map. Caller frees with material-free!. Do NOT pass this to
  UnloadMaterial, which would free the shared default shader out from under
  every other user of it."
  []
  (let [p (ffi/alloc (ffi/layout-size material-layout))]
    (load-material-default-raw p)
    p))

(defn material-free! [mat] (ffi/free mat))

(defn matrix-alloc
  "A 64-byte Matrix buffer set to the identity."
  []
  (let [p (ffi/alloc (ffi/layout-size matrix-layout))]
    (doseq [k [:m0 :m5 :m10 :m15]]
      (ffi/write-field p matrix-layout k 1.0))
    p))

(defn matrix-free! [mtx] (ffi/free mtx))

(defn matrix-translate!
  "Set `mtx` to a translation, leaving the rotation block as the identity.
  raylib's translation components are m12 m13 m14, the fourth COLUMN, which sit
  at the end of the first three declared rows rather than contiguously."
  [mtx x y z]
  (doseq [[k v] [[:m0 1.0] [:m5 1.0] [:m10 1.0] [:m15 1.0]
                 [:m12 (double x)] [:m13 (double y)] [:m14 (double z)]]]
    (ffi/write-field mtx matrix-layout k v))
  mtx)

(defn draw-mesh!
  "DrawMesh: draw `m` with `material` under `transform`. All three cross by
  value, which here means jolt is handed a pointer to each. Call inside a
  with-camera-3d block."
  [m material transform]
  (draw-mesh-raw m material transform))

(defn unload-mesh!
  "UnloadMesh: release the GPU-side vertex buffers. The 120-byte host buffer is
  separate and still needs mesh-free!."
  [m]
  (unload-mesh-raw m))

(defn material-diffuse-color!
  "Tint a material's diffuse map. raylib's default shader multiplies this into
  whatever the map's texture carries, and the default map is a single white
  texel, so on a default material this is simply the colour the mesh comes out.

  MaterialMap is {Texture2D texture; Color color; float value;} and Texture2D is
  five 4-byte fields, so the colour of map 0 sits 20 bytes into the maps array."
  [mat color]
  (let [maps (ffi/read-field mat material-layout :maps)]
    (ffi/write maps :uint color 20))
  mat)

(defn material-shader!
  "Point a material at `sh`, the way C does with `model.materials[0].shader`.

  This is not optional plumbing, and BeginShaderMode is not a substitute for
  it. DrawMesh reads the shader out of the MATERIAL it is handed; the shader
  mode rlgl tracks applies to the default batch, which is what the 2D calls and
  the immediate-mode 3D helpers go through. Wrapping a DrawMesh in
  with-shader therefore does nothing at all: the mesh still draws with whatever
  shader its material carries, which for a default material is raylib's unlit
  one, and the result is a flat silhouette that looks like the shader failed."
  [mat sh]
  (ffi/write-field mat material-layout :shader-id
                   (ffi/read-field sh shaders/shader-layout :id))
  (ffi/write-field mat material-layout :shader-locs
                   (ffi/read-field sh shaders/shader-layout :locs))
  mat)

(defn material-diffuse-texture!
  "Put an rlgl texture id into a material's diffuse map, so meshes drawn with it
  sample that texture instead of the single white texel LoadMaterialDefault
  leaves there.

  MaterialMap begins with a Texture2D, which is five 4-byte fields (id, width,
  height, mipmaps, format) at offset 0 of the maps array. raylib reads all of
  them, not just the id: the size and format decide how it binds and samples,
  so a map carrying the right id and a zero size draws nothing."
  [mat tex-id w h]
  (let [maps (ffi/read-field mat material-layout :maps)]
    (ffi/write maps :uint tex-id 0)
    (ffi/write maps :int (int w) 4)
    (ffi/write maps :int (int h) 8)
    (ffi/write maps :int 1 12)
    (ffi/write maps :int native/PIXELFORMAT-R8G8B8A8 16))
  mat)
