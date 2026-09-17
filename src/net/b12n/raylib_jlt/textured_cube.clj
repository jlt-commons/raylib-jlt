(ns net.b12n.raylib-jlt.textured-cube
  "raylib [models] example - textured cube.

  Two cubes drawn with rlgl immediate mode from one shared atlas texture: the
  left cube maps the whole atlas across each face, the right cube maps only a
  quarter of it (a normalized source rectangle, the DrawCubeTextureRec idea).
  A static camera looks down at both; DrawCube itself never appears, since
  raylib has no textured-cube primitive of its own.

  The atlas is generated on the GPU via `rl/texture-from-fn` rather than
  loaded from a PNG, following this project's no-external-assets convention."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const ATLAS 64)

(defn- atlas-pixel
  "Four solid-color quadrants, so which quarter of the atlas a face samples is
  obvious at a glance. The right cube's source rect picks the bottom-left one."
  [x y]
  (let [left? (< x (/ ATLAS 2))
        top? (< y (/ ATLAS 2))]
    (cond
      (and left? top?) (rl/rgba 230 41 55 255)
      (and (not left?) top?) (rl/rgba 0 158 47 255)
      (and left? (not top?)) (rl/rgba 0 121 241 255)
      :else (rl/rgba 253 249 0 255))))

(defn- tv
  "One textured vertex: rlTexCoord2f(u,v) then rlVertex3f(x,y,z). Stays
  positional (mirrors the two rl* calls it bundles) rather than a kwarg map,
  the same call as every other face-vertex pair below it."
  [u v x y z]
  (rl/rl-tex-coord-2f u v)
  (rl/rl-vertex-3f x y z))

(defn- draw-cube-texture
  "Whole-atlas textured cube centred at (x,y,z), size w*h*l."
  [{:keys [tex-id x y z w h l]}]
  (let [x2 (/ w 2.0) y2 (/ h 2.0) z2 (/ l 2.0)
        xl (- x x2) xr (+ x x2)
        yb (- y y2) yt (+ y y2)
        zb (- z z2) zf (+ z z2)]
    (rl/rl-set-texture tex-id)
    (rl/rl-begin rl/RL-QUADS)
    (rl/rl-color-4ub 255 255 255 255)
    (rl/rl-normal-3f 0.0 0.0 1.0)
    (tv 0.0 0.0 xl yb zf) (tv 1.0 0.0 xr yb zf) (tv 1.0 1.0 xr yt zf) (tv 0.0 1.0 xl yt zf)
    (rl/rl-normal-3f 0.0 0.0 -1.0)
    (tv 1.0 0.0 xl yb zb) (tv 1.0 1.0 xl yt zb) (tv 0.0 1.0 xr yt zb) (tv 0.0 0.0 xr yb zb)
    (rl/rl-normal-3f 0.0 1.0 0.0)
    (tv 0.0 1.0 xl yt zb) (tv 0.0 0.0 xl yt zf) (tv 1.0 0.0 xr yt zf) (tv 1.0 1.0 xr yt zb)
    (rl/rl-normal-3f 0.0 -1.0 0.0)
    (tv 1.0 1.0 xl yb zb) (tv 0.0 1.0 xr yb zb) (tv 0.0 0.0 xr yb zf) (tv 1.0 0.0 xl yb zf)
    (rl/rl-normal-3f 1.0 0.0 0.0)
    (tv 1.0 0.0 xr yb zb) (tv 1.0 1.0 xr yt zb) (tv 0.0 1.0 xr yt zf) (tv 0.0 0.0 xr yb zf)
    (rl/rl-normal-3f -1.0 0.0 0.0)
    (tv 0.0 0.0 xl yb zb) (tv 1.0 0.0 xl yb zf) (tv 1.0 1.0 xl yt zf) (tv 0.0 1.0 xl yt zb)
    (rl/rl-end)
    (rl/rl-set-texture 0)))

(defn- draw-cube-texture-rec
  "Textured cube using only the [su0..su1] x [sv0..sv1] slice of the atlas
  (already normalized to 0..1) on every face."
  [{:keys [tex-id x y z w h l su0 su1 sv0 sv1]}]
  (let [x2 (/ w 2.0) y2 (/ h 2.0) z2 (/ l 2.0)
        xl (- x x2) xr (+ x x2)
        yb (- y y2) yt (+ y y2)
        zb (- z z2) zf (+ z z2)]
    (rl/rl-set-texture tex-id)
    (rl/rl-begin rl/RL-QUADS)
    (rl/rl-color-4ub 255 255 255 255)
    (rl/rl-normal-3f 0.0 0.0 1.0)
    (tv su0 sv1 xl yb zf) (tv su1 sv1 xr yb zf) (tv su1 sv0 xr yt zf) (tv su0 sv0 xl yt zf)
    (rl/rl-normal-3f 0.0 0.0 -1.0)
    (tv su1 sv1 xl yb zb) (tv su1 sv0 xl yt zb) (tv su0 sv0 xr yt zb) (tv su0 sv1 xr yb zb)
    (rl/rl-normal-3f 0.0 1.0 0.0)
    (tv su0 sv0 xl yt zb) (tv su0 sv1 xl yt zf) (tv su1 sv1 xr yt zf) (tv su1 sv0 xr yt zb)
    (rl/rl-normal-3f 0.0 -1.0 0.0)
    (tv su1 sv0 xl yb zb) (tv su0 sv0 xr yb zb) (tv su0 sv1 xr yb zf) (tv su1 sv1 xl yb zf)
    (rl/rl-normal-3f 1.0 0.0 0.0)
    (tv su1 sv1 xr yb zb) (tv su1 sv0 xr yt zb) (tv su0 sv0 xr yt zf) (tv su0 sv1 xr yb zf)
    (rl/rl-normal-3f -1.0 0.0 0.0)
    (tv su0 sv1 xl yb zb) (tv su1 sv1 xl yb zf) (tv su1 sv0 xl yt zf) (tv su0 sv0 xl yt zb)
    (rl/rl-end)
    (rl/rl-set-texture 0)))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [models] example - textured cube"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        atlas (rl/texture-from-fn ATLAS ATLAS atlas-pixel)]
    (loop [frame 0]
      (if-not (rl/keep-running? deadline)
        (rl/unload-texture! atlas)
        (do
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/with-camera-3d {:pos-x 0.0
                              :pos-y 10.0
                              :pos-z 10.0}
            (fn []
              (draw-cube-texture {:tex-id atlas
                                  :x -2.0
                                  :y 2.0
                                  :z 0.0
                                  :w 2.0
                                  :h 4.0
                                  :l 2.0})
              (draw-cube-texture-rec {:tex-id atlas
                                      :x 2.0
                                      :y 1.0
                                      :z 0.0
                                      :w 2.0
                                      :h 2.0
                                      :l 2.0
                                      :su0 0.0
                                      :su1 0.5
                                      :sv0 0.5
                                      :sv1 1.0})
              (rl/draw-grid 10 1.0)))
          (rl/text! "Left: whole atlas. Right: one quarter of it (DrawCubeTextureRec)."
                    {:x 10
                     :y 10
                     :size 18
                     :color rl/DARKGRAY})
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame))))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
