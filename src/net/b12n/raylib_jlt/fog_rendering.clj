(ns net.b12n.raylib-jlt.fog-rendering
  "raylib [shaders] example - fog rendering (`jolt -M:fog-rendering`).

  Port of raylib's examples/shaders/shaders_fog_rendering.c. The lighting
  shader from net.b12n.raylib-jlt.basic-lighting with two uniforms added, so
  surfaces fade toward a fog colour with distance from the eye.

  The fog itself is four lines of GLSL. What makes it work is that the vertex
  stage already hands the fragment stage a world-space position, which is the
  same reason lighting needs a custom vertex shader at all. Distance is then
  just `length(viewPos - fragPosition)`, and the falloff is exponential:
  `1/exp((d*density)^2)`, which thickens fast and never quite reaches zero.

  The eye position is written every frame from here. raylib does not push it:
  SHADER_LOC_VECTOR_VIEW appears nowhere in its source but its own enum. Get
  that wrong and the fog is measured from the origin rather than the camera,
  which still fades things plausibly and is still wrong.

  Upstream textures the cubes with texel_checker.png. No files ship here, so
  the checker is generated and pushed into the material's diffuse map with
  rl/material-diffuse-texture!. The texture matters more than decoration: on
  flat colour the fog reads as a wash, and on a repeating pattern you can see
  which row it has reached.

  UP and DOWN change the density. At zero the far cubes come back."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const GRID 7)
(def ^:const SPACING 2.4)
(def ^:const CHECK 64)

(def vertex-shader "
#version 330
in vec3 vertexPosition;
in vec2 vertexTexCoord;
in vec3 vertexNormal;
uniform mat4 mvp;
uniform mat4 matModel;
uniform mat4 matNormal;
out vec3 fragPosition;
out vec2 fragTexCoord;
out vec3 fragNormal;
void main()
{
    fragPosition = vec3(matModel*vec4(vertexPosition, 1.0));
    fragTexCoord = vertexTexCoord;
    fragNormal = normalize(vec3(matNormal*vec4(vertexNormal, 1.0)));
    gl_Position = mvp*vec4(vertexPosition, 1.0);
}
")

(def fragment-shader "
#version 330
in vec3 fragPosition;
in vec2 fragTexCoord;
in vec3 fragNormal;
uniform sampler2D texture0;
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
    vec4 texelColor = texture(texture0, fragTexCoord);
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

    finalColor = (texelColor*((colDiffuse + vec4(specular, 1.0))*vec4(lightDot, 1.0)));
    finalColor += texelColor*(ambient/10.0);
    finalColor = pow(finalColor, vec4(1.0/2.2));

    float dist = length(viewPos - fragPosition);
    float fogFactor = 1.0/exp((dist*fogDensity)*(dist*fogDensity));
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    finalColor = mix(fogColor, finalColor, fogFactor);
}
")

(defn- checker-pixel
  [x y]
  (if (even? (+ (quot x 16) (quot y 16)))
    (rl/rgba 225 225 235 255)
    (rl/rgba 120 125 140 255)))

(defn- light-uniforms!
  "One light's worth of uniforms. Only one light here, straight overhead, since
  the subject is the fog rather than the shading."
  [sh i kind [px py pz] [r g b]]
  (rl/set-uniform-int! sh (rl/uniform-loc sh (str "lights[" i "].enabled")) 1)
  (rl/set-uniform-int! sh (rl/uniform-loc sh (str "lights[" i "].type")) kind)
  (rl/set-uniform-vec3! sh (rl/uniform-loc sh (str "lights[" i "].position")) px py pz)
  (rl/set-uniform-vec3! sh (rl/uniform-loc sh (str "lights[" i "].target")) 0.0 0.0 0.0)
  (rl/set-uniform-vec4! sh (rl/uniform-loc sh (str "lights[" i "].color"))
                        (/ r 255.0) (/ g 255.0) (/ b 255.0) 1.0))

(defn -main
  [& _]
  (rl/set-config-flags rl/FLAG-MSAA-4X-HINT)
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - fog rendering"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader-vf vertex-shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "fog-rendering: the fog shader did not link (log above)"))
      (let [checker (rl/texture-from-fn CHECK CHECK checker-pixel)
            material (doto (rl/material-default)
                       (rl/material-shader! sh)
                       (rl/material-diffuse-color! rl/WHITE)
                       (rl/material-diffuse-texture! checker CHECK CHECK))
            transform (rl/matrix-alloc)
            cube (rl/mesh-alloc)
            ground (rl/mesh-alloc)
            view-pos (rl/uniform-loc sh "viewPos")
            fog-density-loc (rl/uniform-loc sh "fogDensity")]
        (rl/mesh-cube! cube 1.6 1.6 1.6)
        (rl/mesh-plane! ground 40.0 40.0 12 12)
        (rl/set-uniform-vec4! sh (rl/uniform-loc sh "ambient") 0.22 0.22 0.25 1.0)
        (rl/set-uniform-vec4! sh (rl/uniform-loc sh "fogColor") 0.42 0.45 0.52 1.0)
        (light-uniforms! sh 0 1 [0.0 14.0 0.0] [255 250 235])
        (dotimes [i 3]
          (rl/set-uniform-int! sh (rl/uniform-loc sh (str "lights[" (inc i) "].enabled")) 0))
        (loop [frame 0
               density 0.13]
          (if-not (app/keep-running? deadline)
            (do (doseq [m [cube ground]]
                  (rl/unload-mesh! m)
                  (rl/mesh-free! m))
                (rl/matrix-free! transform)
                (rl/material-free! material)
                (rl/unload-texture! checker)
                (rl/unload-shader! sh))
            (let [density' (cond
                             (rl/key-down? rl/KEY-UP) (min 0.5 (+ density 0.002))
                             (rl/key-down? rl/KEY-DOWN) (max 0.0 (- density 0.002))
                             :else density)
                  t (* frame 0.006)
                  cam-x (* 13.0 (Math/sin t))
                  cam-z (* 13.0 (Math/cos t))]
              (rl/set-uniform-vec3! sh view-pos cam-x 5.0 cam-z)
              (rl/set-uniform-float! sh fog-density-loc density')
              (rl/begin-drawing)
              (rl/clear-background (rl/rgba 107 115 133 255))
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
                  (rl/matrix-translate! transform 0.0 -0.85 0.0)
                  (rl/draw-mesh! ground material transform)
                  (let [half (/ (dec GRID) 2.0)]
                    (dotimes [ix GRID]
                      (dotimes [iz GRID]
                        (rl/matrix-translate! transform
                                              (* SPACING (- ix half))
                                              0.0
                                              (* SPACING (- iz half)))
                        (rl/draw-mesh! cube material transform))))))
              (rl/text! "raylib [shaders] example - fog rendering"
                        {:x 16
                         :y 14
                         :size 20
                         :color rl/RAYWHITE})
              (rl/text! "exponential fog on distance from the eye, in the same shader as the lighting"
                        {:x 16
                         :y 40
                         :size 14
                         :color (rl/rgba 225 230 240 255)})
              (rl/text! (str "UP/DOWN fog density " (format "%.3f" density'))
                        {:x 16
                         :y (- H 30)
                         :size 16
                         :color (rl/rgba 225 230 240 255)})
              (app/maybe-screenshot! frame 40)
              (rl/end-drawing)
              (recur (inc frame) density'))))))))
