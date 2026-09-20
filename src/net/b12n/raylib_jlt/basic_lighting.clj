(ns net.b12n.raylib-jlt.basic-lighting
  "raylib [shaders] example - basic lighting (`jolt -M:basic-lighting`).

  Port of raylib's examples/shaders/shaders_basic_lighting.c. Four coloured
  point lights over a few generated meshes, shaded per fragment with a
  Blinn-Phong term.

  This is the first example here with a custom VERTEX shader. Every other
  shader in the suite is fragment-only, running against raylib's default
  vertex stage, which is enough when you are transforming colours that are
  already on screen. Lighting is not: the fragment stage needs the surface
  position in world space and the normal after the model transform, and
  raylib's default vertex shader passes neither. Hence rl/shader-vf, which
  takes both sources, beside the fragment-only rl/shader.

  One non-obvious step makes or breaks it, and it is easy to get wrong in a
  way that still renders. The shader needs the eye position to compute a
  specular term, and raylib does not supply it. SHADER_LOC_VECTOR_VIEW exists
  in raylib's enum and nowhere else in its source: the C examples use that slot
  purely to stash a location they then pass to SetShaderValue themselves every
  frame. So this example keeps the location in an ordinary binding and writes
  it each frame. Skip that write and viewPos stays at the origin, which leaves
  the diffuse term untouched and only moves the highlights, so the scene still
  looks lit and is quietly wrong.

  raylib ships this as resources/shaders/glsl330/lighting.{vs,fs} plus a
  rlights.h helper. No files ship here, so both shaders are inline strings and
  the rlights Light struct is a plain Clojure map whose uniforms are pushed by
  light-uniforms!. Nothing in that header needed FFI: it is uniform plumbing."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const MAX-LIGHTS 4)

(def vertex-shader "
#version 330
in vec3 vertexPosition;
in vec2 vertexTexCoord;
in vec3 vertexNormal;
in vec4 vertexColor;
uniform mat4 mvp;
uniform mat4 matModel;
uniform mat4 matNormal;
out vec3 fragPosition;
out vec2 fragTexCoord;
out vec4 fragColor;
out vec3 fragNormal;
void main()
{
    fragPosition = vec3(matModel*vec4(vertexPosition, 1.0));
    fragTexCoord = vertexTexCoord;
    fragColor = vertexColor;
    fragNormal = normalize(vec3(matNormal*vec4(vertexNormal, 1.0)));
    gl_Position = mvp*vec4(vertexPosition, 1.0);
}
")

(def fragment-shader "
#version 330
in vec3 fragPosition;
in vec2 fragTexCoord;
in vec4 fragColor;
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

void main()
{
    vec4 texelColor = texture(texture0, fragTexCoord);
    vec3 lightDot = vec3(0.0);
    vec3 normal = normalize(fragNormal);
    vec3 viewD = normalize(viewPos - fragPosition);
    vec3 specular = vec3(0.0);
    vec4 tint = colDiffuse*fragColor;

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

    finalColor = (texelColor*((tint + vec4(specular, 1.0))*vec4(lightDot, 1.0)));
    finalColor += texelColor*(ambient/10.0)*tint;
    finalColor = pow(finalColor, vec4(1.0/2.2));
}
")

;; rlights.h's CreateLight, as data. The shader indexes its uniforms by name,
;; so a light is just the five values plus the positions raylib will write them
;; to, looked up once at startup rather than per frame.
(defn- make-light
  [sh i kind [px py pz] [tx ty tz] color]
  {:kind kind
   :position [px py pz]
   :target [tx ty tz]
   :color color
   :enabled-loc (rl/uniform-loc sh (str "lights[" i "].enabled"))
   :type-loc (rl/uniform-loc sh (str "lights[" i "].type"))
   :position-loc (rl/uniform-loc sh (str "lights[" i "].position"))
   :target-loc (rl/uniform-loc sh (str "lights[" i "].target"))
   :color-loc (rl/uniform-loc sh (str "lights[" i "].color"))})

(defn- light-uniforms!
  "rlights.h's UpdateLightValues. The colour crosses as four floats in 0..1,
  not as a packed Color: the shader declares it vec4."
  [sh {:keys [kind position target color enabled-loc type-loc position-loc
              target-loc color-loc]}
   enabled?]
  (rl/set-uniform-int! sh enabled-loc (if enabled? 1 0))
  (rl/set-uniform-int! sh type-loc kind)
  (apply rl/set-uniform-vec3! sh position-loc position)
  (apply rl/set-uniform-vec3! sh target-loc target)
  (let [[r g b] color]
    (rl/set-uniform-vec4! sh color-loc (/ r 255.0) (/ g 255.0) (/ b 255.0) 1.0)))

(def lights-spec
  [[[-2.0 1.0 -2.0] [235 225 90]]
   [[2.0 1.0 2.0] [230 60 50]]
   [[-2.0 1.0 2.0] [70 210 100]]
   [[2.0 1.0 -2.0] [70 130 240]]])

(defn -main
  [& _]
  (rl/set-config-flags rl/FLAG-MSAA-4X-HINT)
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - basic lighting"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader-vf vertex-shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "basic-lighting: the lighting shader did not link (log above)"))
      (let [;; without this raylib never writes the camera position and every
            ;; specular highlight is computed as though the eye were at 0,0,0
            view-pos (rl/uniform-loc sh "viewPos")
            amb (rl/uniform-loc sh "ambient")
            _ (rl/set-uniform-vec4! sh amb 0.1 0.1 0.1 1.0)
            lights (vec (map-indexed (fn [i [pos color]]
                                       (make-light sh i 1 pos [0.0 0.0 0.0] color))
                                     lights-spec))
            material (doto (rl/material-default)
                       (rl/material-shader! sh)
                       (rl/material-diffuse-color! rl/WHITE))
            transform (rl/matrix-alloc)
            ground (rl/mesh-alloc)
            ball (rl/mesh-alloc)
            post (rl/mesh-alloc)]
        (rl/mesh-plane! ground 9.0 9.0 3 3)
        (rl/mesh-sphere! ball 0.9 24 32)
        (rl/mesh-cylinder! post 0.3 1.4 20)
        (loop [frame 0
               enabled (vec (repeat MAX-LIGHTS true))]
          (if-not (app/keep-running? deadline)
            (do (doseq [m [ground ball post]]
                  (rl/unload-mesh! m)
                  (rl/mesh-free! m))
                (rl/matrix-free! transform)
                (rl/material-free! material)
                (rl/unload-shader! sh))
            (let [enabled' (reduce (fn [acc [i k]]
                                     (if (rl/key-pressed? k)
                                       (update acc i not)
                                       acc))
                                   enabled
                                   (map-indexed vector [rl/KEY-Y rl/KEY-R rl/KEY-G rl/KEY-B]))
                  t (* frame 0.012)
                  cam-x (* 8.5 (Math/sin t))
                  cam-z (* 8.5 (Math/cos t))]
              ;; raylib does NOT write this for you. SHADER_LOC_VECTOR_VIEW
              ;; appears nowhere in raylib's source except its own enum; the C
              ;; examples use that slot only to stash the location and then
              ;; push the value themselves, exactly as here. Leave this out and
              ;; viewPos stays at the origin: the diffuse term is unaffected,
              ;; so the scene still looks lit and only the specular highlights
              ;; are wrong, which is easy to ship without noticing
              (rl/set-uniform-vec3! sh view-pos cam-x 6.0 cam-z)
              (dotimes [i MAX-LIGHTS]
                (light-uniforms! sh (nth lights i) (nth enabled' i)))
              (rl/begin-drawing)
              (rl/clear-background (rl/rgba 20 24 34 255))
              (rl/with-camera-3d
                {:pos-x cam-x
                 :pos-y 6.0
                 :pos-z cam-z
                 :target-x 0.0
                 :target-y 0.8
                 :target-z 0.0
                 :up-x 0.0
                 :up-y 1.0
                 :up-z 0.0
                 :fovy 45.0}
                (fn []
                  ;; NOT with-shader. DrawMesh reads the shader out of the
                  ;; material it is handed, and the shader mode rlgl tracks
                  ;; applies to the default batch instead, so wrapping these
                  ;; calls in with-shader draws everything unlit and looks
                  ;; exactly like a shader that failed to link
                  (rl/matrix-translate! transform 0.0 0.0 0.0)
                  (rl/draw-mesh! ground material transform)
                  (rl/matrix-translate! transform 0.0 0.9 0.0)
                  (rl/draw-mesh! ball material transform)
                  (doseq [[dx dz] [[-3.2 -3.2] [3.2 -3.2] [-3.2 3.2] [3.2 3.2]]]
                    (rl/matrix-translate! transform dx 0.7 dz)
                    (rl/draw-mesh! post material transform))
                  ;; the lamps themselves are drawn unshaded, outside the shader,
                  ;; so each one shows the colour it is casting
                  (dotimes [i MAX-LIGHTS]
                    (let [{:keys [position color]} (nth lights i)
                          [px py pz] position
                          [r g b] color]
                      (if (nth enabled' i)
                        (rl/draw-sphere! [px py pz] 0.2 (rl/rgba r g b 255))
                        (rl/draw-sphere-wires! {:pos [px py pz]
                                                :radius 0.2
                                                :rings 8
                                                :slices 8
                                                :color (rl/rgba r g b 90)}))))))
              (rl/text! "raylib [shaders] example - basic lighting"
                        {:x 16
                         :y 14
                         :size 20
                         :color rl/RAYWHITE})
              (rl/text! "custom vertex shader: lighting needs the world position and normal"
                        {:x 16
                         :y 40
                         :size 14
                         :color (rl/rgba 160 175 200 255)})
              (rl/text! "Y R G B toggle the four lights"
                        {:x 16
                         :y (- H 30)
                         :size 16
                         :color (rl/rgba 160 175 200 255)})
              (app/maybe-screenshot! frame 40)
              (rl/end-drawing)
              (recur (inc frame) enabled'))))))))
