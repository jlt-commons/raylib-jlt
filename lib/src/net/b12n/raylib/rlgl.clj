(ns net.b12n.raylib.rlgl
  "rlgl, raylib's lower-level immediate-mode drawing layer: scalar
  triangle/point drawing, backface culling, and flush-batch, the call that
  forces raylib's deferred batch to the GPU before it would otherwise flush
  on its own."
  (:require
   [jolt.ffi :as ffi]))

;; --- rlgl immediate mode (all scalar), for triangles / points ---------------
(ffi/defcfn rl-begin     "rlBegin"     [:int] :void)   ; RL-LINES / RL-TRIANGLES
(ffi/defcfn rl-end       "rlEnd"       [] :void)
(ffi/defcfn ^:private rl-vertex-2f-raw "rlVertex2f"  [:float :float] :void)

(defn rl-vertex-2f
  "One vertex, in whatever space the current matrix defines.

  Coerces to double, because the C takes floats and an integer argument
  aborts the process on the first draw. The mirror of the int coercion
  the kwarg drawing API does."
  [a0 a1]
  (rl-vertex-2f-raw (double a0) (double a1)))

(ffi/defcfn rl-color-4ub "rlColor4ub"  [:int :int :int :int] :void)  ; u8 args
(def ^:const RL-LINES 1)
(def ^:const RL-TRIANGLES 4)

(defn rl-color!
  "rlColor4ub from a packed rgba Color, so rlgl immediate mode can use the same
  Color values as the rest of the API."
  [color]
  (rl-color-4ub (bit-and color 0xff)
                (bit-and (bit-shift-right color 8) 0xff)
                (bit-and (bit-shift-right color 16) 0xff)
                (bit-and (bit-shift-right color 24) 0xff)))

;; --- flush-batch (lifted out of screenshot hook plumbing, promoted public) ---
;; It sat under a banner about headless smoke tests and was private, but it is
;; an rlgl call with nothing screenshot-specific about it, and both the example
;; harness and net.b12n.raylib.textures need it.
(ffi/defcfn ^:private flush-batch-raw "rlDrawRenderBatchActive" [] :void)

(defn flush-batch
  "rlDrawRenderBatchActive. raylib batches geometry and defers the actual draw
  calls until EndDrawing, so anything that reads pixels mid-frame, a
  screenshot or a render-texture readback, must flush the pending batch
  first or see stale or partial content."
  []
  (flush-batch-raw))

;; --- backface culling --------------------------------------------------------
;; raylib culls back faces by default, which is why the fans and quads above are
;; wound to raylib's front-facing order (see the note in sector!). An example
;; that decides visibility ITSELF needs the cull switched off, because a
;; screen-space test is not a winding rule and the two disagree: helitorus keeps
;; a triangle when the 2D cross product of its edges is positive, which is
;; exactly the orientation raylib treats as back-facing, so with culling on the
;; faces it keeps are the faces raylib drops and the surface renders inside-out.
;; Disable it, do the test, and both windings reach the rasterizer.
(ffi/defcfn rl-disable-backface-culling "rlDisableBackfaceCulling" [] :void)
(ffi/defcfn rl-enable-backface-culling  "rlEnableBackfaceCulling"  [] :void)
