(ns net.b12n.raylib-jlt.mesh-generation
  "raylib [models] example - mesh generation (`jolt -M:mesh-generation`).

  Port of raylib's examples/models/models_mesh_generation.c. Eight shapes from
  the GenMesh* family, laid out on a grid and turning together, each drawn with
  DrawMesh under its own transform.

  This is the first example in the suite to use raylib's real mesh API rather
  than an rlgl stand-in. Everything 3D here until now went through immediate
  mode, because Mesh is 120 bytes and Material and Matrix are 40 and 64, and
  jolt could not pass or return a struct by value before 0.7.23. It can now,
  with two rules the spike for this example had to find the hard way:

    * An aggregate-returning call takes a caller-owned destination pointer as
      its FIRST argument. (mesh-cube! m w h l) is four arguments, not three,
      and getting that wrong is an arity error rather than a crash.
    * An aggregate argument is a pointer to the struct bytes, so DrawMesh takes
      three pointers even though its C signature is three values.

  The layouts come from the header that matches the linked library rather than
  from a raylib source checkout. raylib's own tree has moved past the 6.0 tag
  and reorders Mesh's bone fields, so a layout copied from there would silently
  disagree with the struct the library writes.

  GenMesh* uploads to the GPU on its own, so each mesh has a live vao id the
  moment it is generated and there is no UploadMesh call here.

  UP and DOWN change how fast the grid turns; SPACE pauses it."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)

;; name, the fn that fills a mesh buffer, and where it sits on the grid
(def shapes
  [["cube" (fn [m] (rl/mesh-cube! m 1.0 1.0 1.0)) -3.0 1.2 [230 80 70]]
   ["sphere" (fn [m] (rl/mesh-sphere! m 0.6 16 20)) -1.0 1.2 [240 170 60]]
   ["hemisphere" (fn [m] (rl/mesh-hemisphere! m 0.6 12 20)) 1.0 1.2 [235 225 90]]
   ["cylinder" (fn [m] (rl/mesh-cylinder! m 0.5 1.1 20)) 3.0 1.2 [110 210 110]]
   ["cone" (fn [m] (rl/mesh-cone! m 0.6 1.2 20)) -3.0 -1.2 [80 195 215]]
   ["torus" (fn [m] (rl/mesh-torus! m 0.25 0.7 14 20)) -1.0 -1.2 [90 140 235]]
   ["knot" (fn [m] (rl/mesh-knot! m 0.3 0.7 14 20)) 1.0 -1.2 [170 110 230]]
   ["plane" (fn [m] (rl/mesh-plane! m 1.3 1.3 4 4)) 3.0 -1.2 [225 120 190]]])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - mesh generation"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        material (rl/material-default)
        transform (rl/matrix-alloc)
        meshes (mapv (fn [[nm gen x z [r g b]]]
                       (let [m (rl/mesh-alloc)]
                         (gen m)
                         {:name nm
                          :mesh m
                          :x x
                          :z z
                          :color (rl/rgba r g b 255)}))
                     shapes)]
    (loop [frame 0
           angle 0.0
           speed 0.35
           paused? false]
      (if-not (app/keep-running? deadline)
        (do (doseq [{:keys [mesh]} meshes]
              (rl/unload-mesh! mesh)
              (rl/mesh-free! mesh))
            (rl/matrix-free! transform)
            (rl/material-free! material))
        (let [paused?' (if (rl/key-pressed? rl/KEY-SPACE) (not paused?) paused?)
              speed' (cond
                       (rl/key-down? rl/KEY-UP) (min 2.0 (+ speed 0.02))
                       (rl/key-down? rl/KEY-DOWN) (max 0.0 (- speed 0.02))
                       :else speed)
              angle' (if paused?' angle (+ angle (* speed' (rl/get-frame-time))))
              ;; the camera orbits rather than the meshes spinning: DrawMesh
              ;; takes one transform per mesh and a rotation there would need a
              ;; real matrix multiply, which nothing in this suite binds yet
              cam-x (* 9.0 (Math/sin angle'))
              cam-z (* 9.0 (Math/cos angle'))]
          (rl/begin-drawing)
          (rl/clear-background (rl/rgba 26 30 42 255))
          (rl/with-camera-3d
            {:pos-x cam-x
             :pos-y 5.0
             :pos-z cam-z
             :target-x 0.0
             :target-y 0.0
             :target-z 0.0
             :up-x 0.0
             :up-y 1.0
             :up-z 0.0
             :fovy 45.0}
            (fn []
              (rl/draw-grid 12 1.0)
              (doseq [{:keys [mesh x z color]} meshes]
                (rl/matrix-translate! transform x 0.0 z)
                ;; one shared material, re-tinted per mesh. raylib's default
                ;; shader is unlit, so without this every shape is the same
                ;; flat white silhouette and the geometry is unreadable
                (rl/material-diffuse-color! material color)
                (rl/draw-mesh! mesh material transform))))
          (rl/text! "raylib [models] example - mesh generation"
                    {:x 16
                     :y 14
                     :size 20
                     :color rl/RAYWHITE})
          (rl/text! "GenMesh* returns a 120-byte Mesh by value; DrawMesh takes three structs"
                    {:x 16
                     :y 40
                     :size 14
                     :color (rl/rgba 160 175 200 255)})
          (rl/text! (str "UP/DOWN orbit speed " (format "%.2f" speed')
                         (if paused?' "   PAUSED (SPACE)" ""))
                    {:x 16
                     :y (- H 30)
                     :size 16
                     :color (rl/rgba 160 175 200 255)})
          (let [total (reduce + (map (fn [{:keys [mesh]}] (rl/mesh-triangle-count mesh)) meshes))]
            (rl/text! (str (count meshes) " meshes, " total " triangles")
                      {:x (- W 260)
                       :y (- H 30)
                       :size 16
                       :color (rl/rgba 160 175 200 255)}))
          (app/maybe-screenshot! frame 40)
          (rl/end-drawing)
          (recur (inc frame) angle' speed' paused?'))))))
