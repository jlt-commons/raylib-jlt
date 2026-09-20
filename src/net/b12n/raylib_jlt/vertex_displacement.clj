(ns net.b12n.raylib-jlt.vertex-displacement
  "raylib [shaders] example - vertex displacement (`jolt -M:vertex-displacement`).

  Port of raylib's examples/shaders/shaders_vertex_displacement.c. A flat
  subdivided plane is pushed into rolling terrain entirely on the GPU: the mesh
  never changes and neither does anything on the CPU side of the frame.

  The vertex stage samples a Perlin noise texture and adds the red channel to
  each vertex's y. Because the sample coordinate is itself animated by time,
  the same static mesh and the same static texture produce moving terrain. The
  displaced height is then passed to the fragment stage, which uses it for
  nothing but colour, so peaks come out pale and troughs dark.

  The subdivision is what makes it work. GenMeshPlane's resX and resZ decide
  how many vertices there are to displace, and a plane with four is still a
  plane however good the shader is, because displacement only ever moves
  vertices that exist.

  Sampling a texture from the vertex stage is the one piece of plumbing worth
  noting, and the obvious call is the wrong one. rl/set-uniform-texture! goes
  through SetShaderValueTexture, which raylib applies through its own render
  batch, and DrawMesh draws outside that batch. The sampler then reads zeroes,
  the displacement comes out as nothing, and the plane stays flat with no error
  anywhere. rl/bind-sampler! sets the GL state directly instead, once, the way
  raylib own example does. Slot 0 belongs to the material diffuse map.

  raylib ships the noise as GenImagePerlinNoise and the shaders as two files.
  The noise is generated here the same way; the shaders are inline strings."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const NOISE 512)
(def ^:const PLANE-SIZE 50.0)
(def ^:const PLANE-RES 50)

(def vertex-shader "
#version 330
in vec3 vertexPosition;
in vec2 vertexTexCoord;
in vec3 vertexNormal;
uniform mat4 mvp;
uniform mat4 matModel;
uniform mat4 matNormal;
uniform float time;
uniform sampler2D perlinNoiseMap;
out vec3 fragPosition;
out vec2 fragTexCoord;
out vec3 fragNormal;
out float height;
void main()
{
    vec2 animatedTexCoord = sin(vertexTexCoord + vec2(sin(time + vertexPosition.x*0.1),
                                                      cos(time + vertexPosition.z*0.1))*0.3);
    animatedTexCoord = animatedTexCoord*0.5 + 0.5;
    float displacement = texture(perlinNoiseMap, animatedTexCoord).r*7.0;
    vec3 displacedPosition = vertexPosition + vec3(0.0, displacement, 0.0);
    fragPosition = vec3(matModel*vec4(displacedPosition, 1.0));
    fragTexCoord = vertexTexCoord;
    fragNormal = normalize(vec3(matNormal*vec4(vertexNormal, 1.0)));
    height = displacedPosition.y*0.2;
    gl_Position = mvp*vec4(displacedPosition, 1.0);
}
")

(def fragment-shader "
#version 330
in vec2 fragTexCoord;
in float height;
out vec4 finalColor;
void main()
{
    vec4 deep = vec4(0.0, 0.13, 0.18, 1.0);
    vec4 crest = vec4(1.0, 1.0, 1.0, 1.0);
    finalColor = mix(deep, crest, height);
}
")

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - vertex displacement"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        sh (rl/shader-vf vertex-shader fragment-shader)]
    (if-not sh
      (binding [*out* *err*]
        (println "vertex-displacement: the shader did not link (log above)"))
      (let [noise (rl/image-perlin-noise NOISE NOISE 0 0 1.0)
            material (doto (rl/material-default)
                       (rl/material-shader! sh)
                       (rl/material-diffuse-color! rl/WHITE))
            plane (rl/mesh-alloc)
            transform (rl/matrix-alloc)
            noise-loc (rl/uniform-loc sh "perlinNoiseMap")
            time-loc (rl/uniform-loc sh "time")]
        ;; 50 by 50 subdivisions: the shader can only move vertices that exist,
        ;; and a four-vertex plane stays flat no matter what it samples
        (rl/mesh-plane! plane PLANE-SIZE PLANE-SIZE PLANE-RES PLANE-RES)
        ;; Once, not per frame. set-uniform-texture! would go through
        ;; SetShaderValueTexture, which raylib applies through its render batch
        ;; and DrawMesh does not use, so the vertex stage would sample zeroes
        ;; and the plane would stay resolutely flat. Slot 0 is raylib's, for
        ;; the material's diffuse map
        (rl/bind-sampler! sh noise-loc noise 1)
        (loop [frame 0]
          (if-not (app/keep-running? deadline)
            (do (rl/unload-mesh! plane)
                (rl/mesh-free! plane)
                (rl/matrix-free! transform)
                (rl/material-free! material)
                (rl/unload-texture! noise)
                (rl/unload-shader! sh))
            (let [t (* frame (/ 1.0 60.0))]
              (rl/set-uniform-float! sh time-loc t)
              (rl/begin-drawing)
              (rl/clear-background (rl/rgba 10 16 24 255))
              (rl/with-camera-3d
                {:pos-x 20.0
                 :pos-y 17.0
                 :pos-z -20.0
                 :target-x 0.0
                 :target-y 3.0
                 :target-z 0.0
                 :up-x 0.0
                 :up-y 1.0
                 :up-z 0.0
                 :fovy 60.0}
                (fn []
                  (rl/matrix-translate! transform 0.0 0.0 0.0)
                  (rl/draw-mesh! plane material transform)))
              (rl/text! "raylib [shaders] example - vertex displacement"
                        {:x 16
                         :y 14
                         :size 20
                         :color rl/RAYWHITE})
              (rl/text! (str "one static " PLANE-RES "x" PLANE-RES
                             " plane, displaced in the vertex stage")
                        {:x 16
                         :y 40
                         :size 14
                         :color (rl/rgba 150 185 205 255)})
              (app/maybe-screenshot! frame 40)
              (rl/end-drawing)
              (recur (inc frame)))))))))
