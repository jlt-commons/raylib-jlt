(ns net.b12n.raylib-jlt.shader-hot-reload
  "raylib [shaders] example - hot-reloading a shader (`jolt -M:shader-hot-reload`).

  Swap the running fragment shader without restarting. 1 and 2 compile two working
  shaders; 3 compiles one that is deliberately broken, which is the case worth
  showing: a shader that fails to compile must leave the previous program running
  and say so, not take the frame down with it.

  That is the whole reason a live shader loop is usable at all. Editing GLSL means
  compiling invalid GLSL most of the time - a half-typed expression is a syntax
  error, and a syntax error every few keystrokes has to be survivable or the loop
  is not a loop.

  rl/shader returns nil on a failed link rather than throwing, so the failure is a
  value the caller decides about. Here the decision is: keep the old program, keep
  drawing, put the compiler's own message on screen. raylib prints the full log to
  stderr, which is where the actual line number lives.

  raylib's own version of this example watches a .fs file's mtime and reloads on
  change. This one keeps its GLSL in the namespace - the same reason every other
  shader here does: nothing reads from disk, so the example runs from anywhere and
  the demo recorder has no working directory to get wrong."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:private prelude "
#version 330
in vec4 fragColor;
out vec4 finalColor;
uniform vec2  uResolution;
uniform float uTime;
")

(def ^:private variants
  [["1  rings"
    (str prelude "
void main() {
  vec2 p = (vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y) - 0.5 * uResolution)
           / uResolution.y;
  float d = length(p);
  float v = 0.5 + 0.5 * sin(d * 34.0 - uTime * 2.2);
  vec3 col = mix(vec3(0.08, 0.10, 0.20), vec3(0.30, 0.85, 0.95), v);
  finalColor = vec4(col, 1.0) * fragColor;
}")]
   ["2  checker"
    (str prelude "
void main() {
  vec2 p = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y) / 44.0;
  p.x += uTime * 1.5;
  float c = mod(floor(p.x) + floor(p.y), 2.0);
  vec3 col = mix(vec3(0.95, 0.75, 0.25), vec3(0.55, 0.15, 0.35), c);
  finalColor = vec4(col, 1.0) * fragColor;
}")]
   ["3  deliberately broken"
    ;; `vec3 col = ` with nothing after it: a plain syntax error, and exactly the
    ;; shape a half-typed line has while you are still typing it.
    (str prelude "
void main() {
  vec3 col = ;
  finalColor = vec4(col, 1.0);
}")]])

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [shaders] example - hot-reloading a shader"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           sh (rl/shader (second (first variants)))
           label (first (first variants))
           reloads 0
           failures 0
           note ""]
      (if-not (rl/keep-running? deadline)
        (when sh (rl/unload-shader! sh))
        (let [want (cond
                     (rl/key-pressed? rl/KEY-ONE) 0
                     (rl/key-pressed? rl/KEY-TWO) 1
                     (rl/key-pressed? rl/KEY-THREE) 2
                     :else nil)
              ;; The compile happens here, mid-loop, with a live program already
              ;; bound - which is the situation the nil return has to survive.
              [sh label reloads failures note]
              (if (nil? want)
                [sh label reloads failures note]
                (let [[new-label src] (nth variants want)
                      candidate (rl/shader src)]
                  (if candidate
                    (do (when sh (rl/unload-shader! sh))
                        [candidate new-label (inc reloads) failures
                         (str "compiled " new-label)])
                    ;; Keep the old program. Dropping it here would leave nothing
                    ;; to draw with, which is the failure mode this guards.
                    [sh label reloads (inc failures)
                     (str "compile FAILED for " new-label " - kept the previous shader")])))]
          (rl/begin-drawing)
          (rl/clear-background rl/BLACK)
          (if sh
            (do (rl/set-uniform-vec2! sh (rl/uniform-loc sh "uResolution") (double W) (double H))
                (rl/set-uniform-float! sh (rl/uniform-loc sh "uTime") (rl/get-time))
                (rl/with-shader sh (fn [] (rl/rect! {:x 0
                                                     :y 0
                                                     :width W
                                                     :height H
                                                     :color rl/WHITE}))))
            (rl/text! "no shader compiled" {:x 20
                                            :y 200
                                            :size 30
                                            :color rl/MAROON}))
          (rl/rect! {:x 0
                     :y 0
                     :width W
                     :height 86
                     :color (rl/rgba 0 0 0 165)})
          (rl/text! (str "running: " label) {:x 14
                                             :y 10
                                             :size 18
                                             :color rl/RAYWHITE})
          (rl/text! (str reloads " reloads   ·   " failures " rejected")
                    {:x 14
                     :y 34
                     :size 14
                     :color rl/LIGHTGRAY})
          (rl/text! note {:x 14
                          :y 56
                          :size 14
                          :color (if (pos? failures) rl/GOLD rl/GREEN)})
          (rl/text! "1 rings   ·   2 checker   ·   3 broken (stays on the last good one)"
                    {:x 14
                     :y (- H 28)
                     :size 14
                     :color rl/LIGHTGRAY})
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) sh label reloads failures note)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
