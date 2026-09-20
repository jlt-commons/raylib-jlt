(ns net.b12n.raylib-jlt.mesh-instancing
  "raylib [shaders] example - mesh instancing (`jolt -M:mesh-instancing`).

  Port of raylib's examples/shaders/shaders_mesh_instancing.c. Ten thousand
  lit cubes in a single draw call.

  Instancing moves the per-copy transform out of the draw loop and into a
  vertex attribute. One mesh and one material go to the GPU, alongside an
  array of ten thousand matrices, and the vertex stage reads its own matrix
  per instance from `in mat4 instanceTransform`. The alternative, a DrawMesh
  per cube, is ten thousand draw calls a frame.

  Nothing has to be wired up for that attribute. raylib 6.0 resolves it by
  name when the shader loads, filling SHADER_LOC_VERTEX_INSTANCETRANSFORM from
  the attribute called `instanceTransform`. This is worth saying because
  raylib's example on master assigns a locs slot by hand, and assigns a
  different one: that code targets a later raylib than the 6.0 here, and
  copying it would set a slot 6.0 never reads while leaving the one it does
  read already correct.

  The lighting is the shader from net.b12n.raylib-jlt.basic-lighting, unchanged
  except that the vertex stage multiplies by the instance matrix. Distance
  fades the far cubes the same way net.b12n.raylib-jlt.fog-rendering does.

  The matrices are built once. Rebuilding ten thousand of them per frame would
  cost more than the draw call it saves, so the field turns by orbiting the
  camera instead."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const INSTANCES 10000)
(def ^:const SPREAD 52.0)

(def vertex-shader "
#version 330
in vec3 vertexPosition;
in vec2 vertexTexCoord;
in vec3 vertexNormal;
in mat4 instanceTransform;
uniform mat4 mvp;
uniform mat4 matNormal;
out vec3 fragPosition;
out vec2 fragTexCoord;
out vec3 fragNormal;
void main()
{
    fragPosition = vec3(instanceTransform*vec4(vertexPosition, 1.0));
    fragTexCoord = vertexTexCoord;
    fragNormal = normalize(vec3(matNormal*vec4(vertexNormal, 1.0)));
    gl_Position = mvp*instanceTransform*vec4(vertexPosition, 1.0);
}
")

(def fragment-shader "
#version 330
in vec3 fragPosition;
in vec2 fragTexCoord;
in vec3 fragNormal;
uniform vec4 colDiffuse;
out vec4 finalColor;

#define MAX_LIGHTS 4

struct Light {
    int enabled;
    int type;
    vec3 position;
    vec3 target;
    vec4 color;
};

uniform Light lights[MAX_LIGHTS];
uniform vec4 ambient;
uniform vec3 viewPos;
uniform vec4 fogColor;
uniform float fogDensity;

void main()
{
    vec3 lightDot = vec3(0.0);
    vec3 normal = normalize(fragNormal);
    vec3 viewD = normalize(viewPos - fragPosition);
    vec3 specular = vec3(0.0);

    for (int i = 0; i < MAX_LIGHTS; i++)
    {
        if (lights[i].enabled == 1)
        {
            vec3 light = vec3(0.0);
            if (lights[i].type == 0) light = -normalize(lights[i].target - lights[i].position);
            else light = normalize(lights[i].position - fragPosition);

            float NdotL = max(dot(normal, light), 0.0);
            lightDot += lights[i].color.rgb*NdotL;

            float specCo = 0.0;
            if (NdotL > 0.0) specCo = pow(max(0.0, dot(viewD, reflect(-light, normal))), 16.0);
            specular += specCo;
        }
    }

    finalColor = (colDiffuse + vec4(specular, 1.0))*vec4(lightDot, 1.0);
    finalColor += (ambient/10.0);
    finalColor = pow(finalColor, vec4(1.0/2.2));

    float dist = length(viewPos - fragPosition);
    float fogFactor = clamp(1.0/exp((dist*fogDensity)*(dist*fogDensity)), 0.0, 1.0);
    finalColor = mix(fogColor, finalColor, fogFactor);
}
")

(defn- light-uniforms!
  [sh i kind [px py pz] [r g b]]
  (rl/set-uniform-int! sh (rl/uniform-loc sh (str "lights[" i "].enabled")) 1)
  (rl/set-uniform-int! sh (rl/uniform-loc sh (str "lights[" i "].type")) kind)
  (rl/set-uniform-vec3! sh (rl/uniform-loc sh (str "lights[" i "].position")) px py pz)
  (rl/set-uniform-vec3! sh (rl/uniform-loc sh (str "lights[" i "].target")) 0.0 0.0 0.0)
  (rl/set-uniform-vec4! sh (rl/uniform-loc sh (str "lights[" i "].color"))
                        (/ r 255.0) (/ g 255.0) (/ b 255.0) 1.0))

(defn- fill-transforms!
  "One matrix per instance, scattered through a cube of side SPREAD and each
  turned to its own angle about its own axis. Built once at startup."
  [buf]
  (dotimes [i INSTANCES]
    (let [x (- (rand SPREAD) (/ SPREAD 2.0))
          y (- (rand SPREAD) (/ SPREAD 2.0))
          z (- (rand SPREAD) (/ SPREAD 2.0))
          ;; a random unit-ish axis: exact normalisation does not matter for
          ;; a scatter, but a zero-length axis would collapse the rotation
          ax (+ 0.1 (rand 1.0))
          ay (+ 0.1 (rand 1.0))
          az (+ 0.1 (rand 1.0))
          len (Math/sqrt (+ (* ax ax) (* ay ay) (* az az)))]
      (rl/matrix-array-set! buf i
                            [(/ ax len) (/ ay len) (/ az len)]
                            (rand (* 2.0 Math/PI))
                            [x y z])))
  buf)

(defn -main
  [& _]
  (rl/set-config-flags rl/FLAG-MSAA-4X-HINT)
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - mesh instancing"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader-vf vertex-shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "mesh-instancing: the instancing shader did not link (log above)"))
      (let [material (doto (rl/material-default)
                       (rl/material-shader! sh)
                       (rl/material-diffuse-color! (rl/rgba 210 215 235 255)))
            cube (rl/mesh-alloc)
            transforms (rl/matrix-array-alloc INSTANCES)
            view-pos (rl/uniform-loc sh "viewPos")]
        (rl/mesh-cube! cube 0.85 0.85 0.85)
        (fill-transforms! transforms)
        (rl/set-uniform-vec4! sh (rl/uniform-loc sh "ambient") 0.35 0.35 0.4 1.0)
        (rl/set-uniform-vec4! sh (rl/uniform-loc sh "fogColor") 0.09 0.10 0.14 1.0)
        (rl/set-uniform-float! sh (rl/uniform-loc sh "fogDensity") 0.028)
        (light-uniforms! sh 0 1 [22.0 26.0 22.0] [255 244 220])
        (light-uniforms! sh 1 1 [-24.0 -14.0 -18.0] [90 150 255])
        (dotimes [i 2]
          (rl/set-uniform-int! sh (rl/uniform-loc sh (str "lights[" (+ i 2) "].enabled")) 0))
        (loop [frame 0]
          (if-not (app/keep-running? deadline)
            (do (rl/unload-mesh! cube)
                (rl/mesh-free! cube)
                (rl/matrix-free! transforms)
                (rl/material-free! material)
                (rl/unload-shader! sh))
            (let [t (* frame 0.004)
                  cam-x (* 46.0 (Math/sin t))
                  cam-z (* 46.0 (Math/cos t))
                  ft (rl/get-frame-time)]
              (rl/set-uniform-vec3! sh view-pos cam-x 12.0 cam-z)
              (rl/begin-drawing)
              (rl/clear-background (rl/rgba 23 26 36 255))
              (rl/with-camera-3d
                {:pos-x cam-x
                 :pos-y 12.0
                 :pos-z cam-z
                 :target-x 0.0
                 :target-y 0.0
                 :target-z 0.0
                 :up-x 0.0
                 :up-y 1.0
                 :up-z 0.0
                 :fovy 45.0}
                (fn []
                  (rl/draw-mesh-instanced! cube material transforms INSTANCES)))
              (rl/text! "raylib [shaders] example - mesh instancing"
                        {:x 16
                         :y 14
                         :size 20
                         :color rl/RAYWHITE})
              (rl/text! (str INSTANCES " lit cubes in one draw call")
                        {:x 16
                         :y 40
                         :size 14
                         :color (rl/rgba 165 180 210 255)})
              (rl/text! (str "fps " (if (pos? ft) (int (/ 1.0 ft)) 0))
                        {:x (- W 90)
                         :y (- H 30)
                         :size 16
                         :color (rl/rgba 165 180 210 255)})
              (app/maybe-screenshot! frame 40)
              (rl/end-drawing)
              (recur (inc frame)))))))))
