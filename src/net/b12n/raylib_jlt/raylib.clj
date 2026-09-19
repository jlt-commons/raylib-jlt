(ns net.b12n.raylib-jlt.raylib
  "Shared jolt.ffi bindings for raylib, the surface used by the example programs
  in this project (net.b12n.raylib-jlt.core, net.b12n.raylib-jlt.input, net.b12n.raylib-jlt.bounce, net.b12n.raylib-jlt.colors, net.b12n.raylib-jlt.mouse,
  net.b12n.raylib-jlt.wheel, net.b12n.raylib-jlt.shapes, net.b12n.raylib-jlt.text, net.b12n.raylib-jlt.logo, net.b12n.raylib-jlt.gradient, net.b12n.raylib-jlt.stars, net.b12n.raylib-jlt.camera2d).

  raylib is the upstream C game library (raysan5/raylib); jolt calls it directly
  over its C ABI. The system libraylib is declared as a :jolt/native lib in
  deps.edn. Almost every call here uses raylib's scalar-argument variants, so the
  only by-value struct that crosses the FFI boundary is `Color`, a 4-byte
  {u8 r,g,b,a} packed into a :uint (see `rgba` and README.md). The one exception,
  Camera2D (24 bytes, passed by pointer), lives in net.b12n.raylib-jlt.camera2d, not here."
  ;; run! is the REPL entry point (see the bottom of this file); the shadowed
  ;; clojure.core/run! is a reducing form this suite never uses.
  (:refer-clojure :exclude [run!])
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.audio :as audio]
   [net.b12n.raylib.camera :as camera]
   [net.b12n.raylib.color :as color]
   [net.b12n.raylib.core :as core]
   [net.b12n.raylib.files :as files]
   [net.b12n.raylib.images :as images]
   [net.b12n.raylib.input :as input]
   [net.b12n.raylib.kwargs :as kwargs]
   [net.b12n.raylib.log :as log]
   [net.b12n.raylib.models :as models]
   [net.b12n.raylib.native :as native]
   [net.b12n.raylib.rays :as rays]
   [net.b12n.raylib.rlgl :as rlgl]
   [net.b12n.raylib.shaders :as shaders]
   [net.b12n.raylib.shapes :as shapes]
   [net.b12n.raylib.splines :as splines]
   [net.b12n.raylib.text :as text]
   [net.b12n.raylib.textures :as textures]
   [net.b12n.raylib.util :as util]))

;; --- Color -------------------------------------------------------------------
;; Moved to net.b12n.raylib.color. Re-exported here so every example that says
;; rl/rgba or rl/RAYWHITE keeps working unchanged. This whole file becomes
;; aliases like these, and is then replaced by the generated
;; net.b12n.raylib.all.
(def rgba color/rgba)

(def LIGHTGRAY color/LIGHTGRAY)   (def GRAY       color/GRAY)
(def DARKGRAY  color/DARKGRAY)    (def YELLOW     color/YELLOW)
(def GOLD      color/GOLD)        (def ORANGE     color/ORANGE)
(def PINK      color/PINK)        (def RED        color/RED)
(def MAROON    color/MAROON)      (def GREEN      color/GREEN)
(def LIME      color/LIME)        (def DARKGREEN  color/DARKGREEN)
(def SKYBLUE   color/SKYBLUE)     (def BLUE       color/BLUE)
(def DARKBLUE  color/DARKBLUE)    (def PURPLE     color/PURPLE)
(def VIOLET    color/VIOLET)      (def DARKPURPLE color/DARKPURPLE)
(def BEIGE     color/BEIGE)       (def BROWN      color/BROWN)
(def DARKBROWN color/DARKBROWN)   (def WHITE      color/WHITE)
(def BLACK     color/BLACK)       (def MAGENTA    color/MAGENTA)
(def RAYWHITE  color/RAYWHITE)

;; --- window / lifecycle ------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/init-window or rl/close-window keeps working unchanged.
(def init-window core/init-window)
(def set-target-fps core/set-target-fps)
(def set-exit-key core/set-exit-key)
(def close-window core/close-window)

;; --- frame -------------------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/begin-drawing or rl/clear-background keeps working unchanged.
(def begin-drawing core/begin-drawing)
(def end-drawing core/end-drawing)
(def clear-background core/clear-background)
(def get-frame-time core/get-frame-time)
(def begin-scissor-mode core/begin-scissor-mode)
(def end-scissor-mode core/end-scissor-mode)

;; --- 2D shapes + text ----------------------------------------------------------
;; text moved to net.b12n.raylib.text, shapes to net.b12n.raylib.shapes.
;; Re-exported here so every example that says rl/draw-text or rl/draw-circle
;; keeps working unchanged.
(def draw-text text/draw-text)
(def draw-fps text/draw-fps)
(def measure-text text/measure-text)
(def draw-pixel shapes/draw-pixel)
(def draw-line shapes/draw-line)
(def draw-rectangle shapes/draw-rectangle)
(def draw-rectangle-lines shapes/draw-rectangle-lines)
(def draw-rectangle-grad-v shapes/draw-rectangle-grad-v)
;; #region draw-circle-binding
(def draw-circle shapes/draw-circle)
;; #endregion
(def draw-circle-lines shapes/draw-circle-lines)
(def draw-ellipse shapes/draw-ellipse)

;; --- rlgl immediate mode (all scalar), for triangles / points ---------------
;; Moved to net.b12n.raylib.rlgl. Re-exported here so every example that says
;; rl/rl-begin or rl/rl-color! keeps working unchanged.
(def rl-begin rlgl/rl-begin)
(def rl-end rlgl/rl-end)
(def rl-vertex-2f rlgl/rl-vertex-2f)
(def rl-color-4ub rlgl/rl-color-4ub)
(def RL-LINES rlgl/RL-LINES)
(def RL-TRIANGLES rlgl/RL-TRIANGLES)
(def rl-color! rlgl/rl-color!)

;; --- Camera2D: a struct passed BY VALUE --------------------------------------
;; Moved to net.b12n.raylib.camera. Re-exported here so every example that says
;; rl/with-camera-2d keeps working unchanged.
;; #region camera2d-by-value
(def end-mode-2d camera/end-mode-2d)
(def with-camera-2d camera/with-camera-2d)
;; #endregion

;; --- Camera3D + 3D geometry --------------------------------------------------
;; Camera3D plumbing moved to net.b12n.raylib.camera; the geometry a
;; with-camera-3d block draws (draw-grid, rl-vertex-3f, the rlgl matrix
;; stack, cube!, sphere!) moved to net.b12n.raylib.models. Re-exported here
;; so every example that says rl/cube! or rl/with-camera-3d keeps working
;; unchanged.
(def draw-grid models/draw-grid)
(def rl-vertex-3f models/rl-vertex-3f)

;; moved to net.b12n.raylib.camera
(def begin-mode-3d-ptr camera/begin-mode-3d-ptr)
(def end-mode-3d camera/end-mode-3d)

(def rl-push-matrix models/rl-push-matrix)
(def rl-pop-matrix models/rl-pop-matrix)
(def rl-translatef models/rl-translatef)
(def rl-rotatef models/rl-rotatef)
(def rl-scalef models/rl-scalef)

;; moved to net.b12n.raylib.camera
(def with-camera-3d camera/with-camera-3d)

(def cube! models/cube!)
(def sphere! models/sphere!)

;; --- input -------------------------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/get-mouse-x or rl/get-random-value keeps working unchanged.
(def get-mouse-x input/get-mouse-x)
(def get-mouse-y input/get-mouse-y)
(def get-mouse-wheel input/get-mouse-wheel)
(def get-random-value input/get-random-value)
(def get-char-pressed input/get-char-pressed)
(def get-key-pressed input/get-key-pressed)

;; --- libc time (the one NON-raylib FFI) --------------------------------------
;; Moved to net.b12n.raylib.util. Re-exported here so every example that says
;; rl/local-time keeps working unchanged.
(def local-time util/local-time)

;; --- screenshot hook plumbing (headless smoke tests) -------------------------
;; take-screenshot and window-should-close? moved to net.b12n.raylib.core, the
;; four predicates to net.b12n.raylib.input and flush-batch to
;; net.b12n.raylib.rlgl -- none of which this banner ever described.
(def take-screenshot core/take-screenshot)

(def window-should-close? core/window-should-close?)

(def key-down? input/key-down?)
(def key-pressed? input/key-pressed?)
(def mouse-down? input/mouse-down?)
(def mouse-pressed? input/mouse-pressed?)

;; --- constants (raylib KeyboardKey / MouseButton) ----------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/KEY-SPACE or rl/MOUSE-LEFT keeps working unchanged.
(def KEY-NULL input/KEY-NULL)
(def KEY-SPACE input/KEY-SPACE)
(def KEY-R input/KEY-R)
(def KEY-W input/KEY-W)
(def KEY-A input/KEY-A)
(def KEY-S input/KEY-S)
(def KEY-D input/KEY-D)
(def KEY-RIGHT input/KEY-RIGHT)
(def KEY-LEFT input/KEY-LEFT)
(def KEY-DOWN input/KEY-DOWN)
(def KEY-UP input/KEY-UP)
(def MOUSE-LEFT input/MOUSE-LEFT)
(def MOUSE-RIGHT input/MOUSE-RIGHT)
(def MOUSE-MIDDLE input/MOUSE-MIDDLE)
(def KEY-BACKSPACE input/KEY-BACKSPACE)
(def KEY-ENTER input/KEY-ENTER)

;; --- keyword-argument drawing API ---------------------------------------------
;; Moved to net.b12n.raylib.kwargs, merging what were two banners here (an
;; "ergonomic keyword-argument drawing API" and a "keyword-argument drawing
;; API") describing the same layer. Re-exported here so every example that
;; says rl/rect! or rl/text! keeps working unchanged.
(def window! kwargs/window!)
(def text! kwargs/text!)
(def text-width kwargs/text-width)
(def fps! kwargs/fps!)

(def rect! kwargs/rect!)
(def rect-lines! kwargs/rect-lines!)
(def rect-gradient! kwargs/rect-gradient!)
(def circle! kwargs/circle!)
(def circle-lines! kwargs/circle-lines!)
(def ellipse! kwargs/ellipse!)
(def line! kwargs/line!)
(def pixel! kwargs/pixel!)
(def sector! kwargs/sector!)
(def ring! kwargs/ring!)
(def line-ex! kwargs/line-ex!)

;; --- smoke-test loop guards --------------------------------------------------
;; Moved to net.b12n.raylib-jlt.app. It is the example suite's own harness, not
;; a raylib binding, so it does not go in the library.
(def auto-quit-deadline app/auto-quit-deadline)
(def keep-running?      app/keep-running?)
(def maybe-screenshot!  app/maybe-screenshot!)

;; =============================================================================
;; Scalar extensions
;; =============================================================================
;; Everything below is appended rather than slotted into the sections above on
;; purpose: since jolt 0.4.0 a symbol must be defined before its first use in the
;; file, and the ordering of the sections above is load-bearing (see the Color
;; note at the top). Appending cannot disturb it. Nothing above refers to
;; anything here.

;; --- window state / config flags ---------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/set-config-flags or rl/window-state? keeps working unchanged.
(def set-config-flags core/set-config-flags)
(def set-window-state core/set-window-state)
(def clear-window-state core/clear-window-state)
(def toggle-fullscreen core/toggle-fullscreen)
(def get-screen-width core/get-screen-width)
(def get-screen-height core/get-screen-height)
(def get-time core/get-time)
(def FLAG-WINDOW-RESIZABLE core/FLAG-WINDOW-RESIZABLE)
(def FLAG-WINDOW-UNDECORATED core/FLAG-WINDOW-UNDECORATED)
(def FLAG-MSAA-4X-HINT core/FLAG-MSAA-4X-HINT)
(def FLAG-VSYNC-HINT core/FLAG-VSYNC-HINT)
(def FLAG-WINDOW-TOPMOST core/FLAG-WINDOW-TOPMOST)
(def FLAG-WINDOW-HIGHDPI core/FLAG-WINDOW-HIGHDPI)
(def window-state? core/window-state?)
(def window-resized? core/window-resized?)

;; --- monitors ----------------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/get-monitor-count or rl/get-monitor-name keeps working unchanged.
(def get-monitor-count core/get-monitor-count)
(def get-current-monitor core/get-current-monitor)
(def get-monitor-width core/get-monitor-width)
(def get-monitor-height core/get-monitor-height)
(def get-monitor-refresh-rate core/get-monitor-refresh-rate)
(def get-monitor-name core/get-monitor-name)

;; --- clipboard ---------------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/set-clipboard-text or rl/get-clipboard-text keeps working unchanged.
(def set-clipboard-text core/set-clipboard-text)
(def get-clipboard-text core/get-clipboard-text)

;; --- gamepad -----------------------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/gamepad-down? or rl/PAD-A keeps working unchanged.
(def get-gamepad-axis-count input/get-gamepad-axis-count)
(def get-gamepad-axis-movement input/get-gamepad-axis-movement)
(def get-gamepad-name input/get-gamepad-name)
(def gamepad-available? input/gamepad-available?)
(def gamepad-down? input/gamepad-down?)
(def gamepad-pressed? input/gamepad-pressed?)
(def gamepad-released? input/gamepad-released?)
(def PAD-UP input/PAD-UP)
(def PAD-RIGHT input/PAD-RIGHT)
(def PAD-DOWN input/PAD-DOWN)
(def PAD-LEFT input/PAD-LEFT)
(def PAD-Y input/PAD-Y)
(def PAD-B input/PAD-B)
(def PAD-A input/PAD-A)
(def PAD-X input/PAD-X)
(def PAD-L1 input/PAD-L1)
(def PAD-L2 input/PAD-L2)
(def PAD-R1 input/PAD-R1)
(def PAD-R2 input/PAD-R2)
(def PAD-SELECT input/PAD-SELECT)
(def PAD-MENU input/PAD-MENU)
(def PAD-START input/PAD-START)
(def AXIS-LEFT-X input/AXIS-LEFT-X)
(def AXIS-LEFT-Y input/AXIS-LEFT-Y)
(def AXIS-RIGHT-X input/AXIS-RIGHT-X)
(def AXIS-RIGHT-Y input/AXIS-RIGHT-Y)

;; --- touch / gestures --------------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/get-touch-x or rl/GESTURE-TAP keeps working unchanged.
(def get-touch-point-count input/get-touch-point-count)
(def get-touch-point-id input/get-touch-point-id)
(def get-touch-x input/get-touch-x)
(def get-touch-y input/get-touch-y)
(def get-gesture-detected input/get-gesture-detected)
(def set-gestures-enabled input/set-gestures-enabled)
(def GESTURE-NONE input/GESTURE-NONE)
(def GESTURE-TAP input/GESTURE-TAP)
(def GESTURE-DOUBLETAP input/GESTURE-DOUBLETAP)
(def GESTURE-HOLD input/GESTURE-HOLD)
(def GESTURE-DRAG input/GESTURE-DRAG)
(def GESTURE-SWIPE-RIGHT input/GESTURE-SWIPE-RIGHT)
(def GESTURE-SWIPE-LEFT input/GESTURE-SWIPE-LEFT)
(def GESTURE-SWIPE-UP input/GESTURE-SWIPE-UP)
(def GESTURE-SWIPE-DOWN input/GESTURE-SWIPE-DOWN)
(def GESTURE-PINCH-IN input/GESTURE-PINCH-IN)
(def GESTURE-PINCH-OUT input/GESTURE-PINCH-OUT)

;; --- remaining input predicates ----------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/set-mouse-cursor or rl/key-released? keeps working unchanged.
(def set-mouse-cursor input/set-mouse-cursor)
(def key-released? input/key-released?)
(def mouse-released? input/mouse-released?)

;; --- more KeyboardKey constants ----------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/KEY-ESCAPE or rl/KEY-Z keeps working unchanged.
(def KEY-ESCAPE input/KEY-ESCAPE)
(def KEY-TAB input/KEY-TAB)
(def KEY-DELETE input/KEY-DELETE)
(def KEY-HOME input/KEY-HOME)
(def KEY-END input/KEY-END)
(def KEY-F1 input/KEY-F1)
(def KEY-F2 input/KEY-F2)
(def KEY-F3 input/KEY-F3)
(def KEY-LEFT-SHIFT input/KEY-LEFT-SHIFT)
(def KEY-LEFT-CONTROL input/KEY-LEFT-CONTROL)
(def KEY-LEFT-SUPER input/KEY-LEFT-SUPER)
(def KEY-ZERO input/KEY-ZERO)
(def KEY-ONE input/KEY-ONE)
(def KEY-TWO input/KEY-TWO)
(def KEY-THREE input/KEY-THREE)
(def KEY-FOUR input/KEY-FOUR)
(def KEY-FIVE input/KEY-FIVE)
(def KEY-SIX input/KEY-SIX)
(def KEY-SEVEN input/KEY-SEVEN)
(def KEY-EIGHT input/KEY-EIGHT)
(def KEY-NINE input/KEY-NINE)
(def KEY-B input/KEY-B)
(def KEY-C input/KEY-C)
(def KEY-E input/KEY-E)
(def KEY-F input/KEY-F)
(def KEY-G input/KEY-G)
(def KEY-H input/KEY-H)
(def KEY-M input/KEY-M)
(def KEY-N input/KEY-N)
(def KEY-P input/KEY-P)
(def KEY-Q input/KEY-Q)
(def KEY-T input/KEY-T)
(def KEY-V input/KEY-V)
(def KEY-X input/KEY-X)
(def KEY-Y input/KEY-Y)
(def KEY-Z input/KEY-Z)

;; --- extra scalar drawing ------------------------------------------------------
;; Moved to net.b12n.raylib.shapes. Re-exported here so every example that says
;; rl/circle-gradient! or rl/rect-pro! keeps working unchanged.
(def draw-rectangle-grad-h shapes/draw-rectangle-grad-h)
(def begin-blend-mode shapes/begin-blend-mode)
(def end-blend-mode shapes/end-blend-mode)
(def BLEND-ALPHA shapes/BLEND-ALPHA)
(def BLEND-ADDITIVE shapes/BLEND-ADDITIVE)
(def BLEND-MULTIPLIED shapes/BLEND-MULTIPLIED)
(def BLEND-ADD-COLORS shapes/BLEND-ADD-COLORS)
(def BLEND-SUBTRACT-COLORS shapes/BLEND-SUBTRACT-COLORS)
(def BLEND-CUSTOM shapes/BLEND-CUSTOM)
(def set-blend-factors shapes/set-blend-factors)
(def GL-SRC-ALPHA shapes/GL-SRC-ALPHA)
(def GL-MIN shapes/GL-MIN)
(def GL-MAX shapes/GL-MAX)
(def circle-gradient! shapes/circle-gradient!)
(def rect-pro! shapes/rect-pro!)
(def rect-gradient-h! shapes/rect-gradient-h!)

;; --- rlgl textures -----------------------------------------------------------
;; Moved to net.b12n.raylib.textures. Re-exported here so every example that
;; says rl/texture! or rl/texture-from-fn keeps working unchanged.
(def rl-load-texture textures/rl-load-texture)
(def rl-unload-texture textures/rl-unload-texture)
(def rl-update-texture textures/rl-update-texture)
(def rl-texture-parameters textures/rl-texture-parameters)
(def rl-set-texture textures/rl-set-texture)
(def rl-tex-coord-2f textures/rl-tex-coord-2f)
(def rl-normal-3f textures/rl-normal-3f)
(def RL-QUADS textures/RL-QUADS)
(def PIXELFORMAT-R8G8B8A8 native/PIXELFORMAT-R8G8B8A8)
(def PIXELFORMAT-R8G8B8 native/PIXELFORMAT-R8G8B8)
(def RL-TEXTURE-WRAP-S textures/RL-TEXTURE-WRAP-S)
(def RL-TEXTURE-WRAP-T textures/RL-TEXTURE-WRAP-T)
(def RL-TEXTURE-WRAP-REPEAT textures/RL-TEXTURE-WRAP-REPEAT)
(def RL-TEXTURE-WRAP-CLAMP textures/RL-TEXTURE-WRAP-CLAMP)
(def RL-TEXTURE-MAG-FILTER textures/RL-TEXTURE-MAG-FILTER)
(def RL-TEXTURE-MIN-FILTER textures/RL-TEXTURE-MIN-FILTER)
(def RL-TEXTURE-FILTER-NEAREST textures/RL-TEXTURE-FILTER-NEAREST)
(def RL-TEXTURE-FILTER-LINEAR textures/RL-TEXTURE-FILTER-LINEAR)
(def texture-filter! textures/texture-filter!)
(def texture-wrap! textures/texture-wrap!)
(def texture-from-fn textures/texture-from-fn)
(def update-texture-from-fn! textures/update-texture-from-fn!)
(def unload-texture! textures/unload-texture!)
(def texture! textures/texture!)

;; --- rlgl framebuffers (render textures) -------------------------------------
;; Moved to net.b12n.raylib.textures. Re-exported here so every example that
;; says rl/render-texture or rl/with-render-texture keeps working unchanged.
(def rl-load-framebuffer textures/rl-load-framebuffer)
(def rl-framebuffer-attach textures/rl-framebuffer-attach)
(def rl-enable-framebuffer textures/rl-enable-framebuffer)
(def rl-disable-framebuffer textures/rl-disable-framebuffer)
(def rl-unload-framebuffer textures/rl-unload-framebuffer)
(def rl-load-texture-depth textures/rl-load-texture-depth)
(def rl-viewport textures/rl-viewport)
(def rl-matrix-mode textures/rl-matrix-mode)
(def rl-load-identity textures/rl-load-identity)
(def rl-ortho textures/rl-ortho)
(def rl-set-framebuffer-width textures/rl-set-framebuffer-width)
(def rl-set-framebuffer-height textures/rl-set-framebuffer-height)
(def rl-get-framebuffer-width textures/rl-get-framebuffer-width)
(def rl-get-framebuffer-height textures/rl-get-framebuffer-height)
(def rl-mult-matrix-f textures/rl-mult-matrix-f)
(def get-render-width textures/get-render-width)
(def get-render-height textures/get-render-height)
(def RL-PROJECTION textures/RL-PROJECTION)
(def RL-MODELVIEW textures/RL-MODELVIEW)
(def RL-ATTACHMENT-COLOR-CHANNEL0 textures/RL-ATTACHMENT-COLOR-CHANNEL0)
(def RL-ATTACHMENT-DEPTH textures/RL-ATTACHMENT-DEPTH)
(def RL-ATTACHMENT-TEXTURE2D textures/RL-ATTACHMENT-TEXTURE2D)
(def RL-ATTACHMENT-RENDERBUFFER textures/RL-ATTACHMENT-RENDERBUFFER)
(def render-texture textures/render-texture)
(def unload-render-texture! textures/unload-render-texture!)
(def with-render-texture textures/with-render-texture)

;; --- shaders -----------------------------------------------------------------
;; Moved to net.b12n.raylib.shaders. Re-exported here so every example that says
;; rl/shader or rl/with-shader keeps working unchanged.
(def end-shader-mode shaders/end-shader-mode)
(def shader-layout shaders/shader-layout)
(def UNIFORM-FLOAT shaders/UNIFORM-FLOAT)
(def UNIFORM-VEC2 shaders/UNIFORM-VEC2)
(def UNIFORM-VEC3 shaders/UNIFORM-VEC3)
(def UNIFORM-VEC4 shaders/UNIFORM-VEC4)
(def UNIFORM-INT shaders/UNIFORM-INT)
(def UNIFORM-IVEC2 shaders/UNIFORM-IVEC2)
(def UNIFORM-IVEC3 shaders/UNIFORM-IVEC3)
(def UNIFORM-IVEC4 shaders/UNIFORM-IVEC4)
(def UNIFORM-SAMPLER2D shaders/UNIFORM-SAMPLER2D)
(def shader shaders/shader)
(def unload-shader! shaders/unload-shader!)
(def uniform-loc shaders/uniform-loc)
(def with-shader shaders/with-shader)
(def set-uniform-float! shaders/set-uniform-float!)
(def set-uniform-vec2! shaders/set-uniform-vec2!)
(def set-uniform-vec3! shaders/set-uniform-vec3!)
(def set-uniform-vec4! shaders/set-uniform-vec4!)
(def set-uniform-int! shaders/set-uniform-int!)
(def set-uniform-ivec3-array! shaders/set-uniform-ivec3-array!)
(def set-uniform-texture! shaders/set-uniform-texture!)

;; --- REPL entry point --------------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; (rl/run! -main) keeps working unchanged.
(def run! core/run!)

;; --- backface culling --------------------------------------------------------
;; Moved to net.b12n.raylib.rlgl. Re-exported here so every example that says
;; rl/rl-disable-backface-culling keeps working unchanged.
(def rl-disable-backface-culling rlgl/rl-disable-backface-culling)
(def rl-enable-backface-culling rlgl/rl-enable-backface-culling)

;; --- mouse position / cursor -------------------------------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/set-mouse-position or rl/hide-cursor keeps working unchanged.
(def set-mouse-position input/set-mouse-position)
(def hide-cursor input/hide-cursor)
(def show-cursor input/show-cursor)

;; --- audio (raudio) -----------------------------------------------------
;; Moved to net.b12n.raylib.audio. Re-exported here so every example that says
;; rl/load-audio-stream or rl/play-audio-stream keeps working unchanged.
(def init-audio-device audio/init-audio-device)
(def close-audio-device audio/close-audio-device)
(def set-audio-stream-buffer-size-default audio/set-audio-stream-buffer-size-default)
(def load-audio-stream audio/load-audio-stream)
(def unload-audio-stream audio/unload-audio-stream)
(def play-audio-stream audio/play-audio-stream)
(def audio-stream-processed? audio/audio-stream-processed?)
(def update-audio-stream audio/update-audio-stream)
(def set-audio-stream-pan audio/set-audio-stream-pan)

;; --- world <-> screen ------------------------------------------------------
;; Moved to net.b12n.raylib.camera. Re-exported here so every example that
;; says rl/world-to-screen keeps working unchanged.
(def world-to-screen camera/world-to-screen)

;; --- more keyboard constants (keyboard-testbed) --------------------------
;; Moved to net.b12n.raylib.input. Re-exported here so every example that says
;; rl/KEY-I or rl/KEY-RIGHT-ALT keeps working unchanged.
(def KEY-I input/KEY-I)
(def KEY-J input/KEY-J)
(def KEY-K input/KEY-K)
(def KEY-L input/KEY-L)
(def KEY-O input/KEY-O)
(def KEY-U input/KEY-U)
(def KEY-APOSTROPHE input/KEY-APOSTROPHE)
(def KEY-COMMA input/KEY-COMMA)
(def KEY-MINUS input/KEY-MINUS)
(def KEY-PERIOD input/KEY-PERIOD)
(def KEY-SLASH input/KEY-SLASH)
(def KEY-SEMICOLON input/KEY-SEMICOLON)
(def KEY-EQUAL input/KEY-EQUAL)
(def KEY-LEFT-BRACKET input/KEY-LEFT-BRACKET)
(def KEY-BACKSLASH input/KEY-BACKSLASH)
(def KEY-RIGHT-BRACKET input/KEY-RIGHT-BRACKET)
(def KEY-GRAVE input/KEY-GRAVE)
(def KEY-INSERT input/KEY-INSERT)
(def KEY-PAGE-UP input/KEY-PAGE-UP)
(def KEY-PAGE-DOWN input/KEY-PAGE-DOWN)
(def KEY-CAPS-LOCK input/KEY-CAPS-LOCK)
(def KEY-PRINT-SCREEN input/KEY-PRINT-SCREEN)
(def KEY-PAUSE input/KEY-PAUSE)
(def KEY-F4 input/KEY-F4)
(def KEY-F5 input/KEY-F5)
(def KEY-F6 input/KEY-F6)
(def KEY-F7 input/KEY-F7)
(def KEY-F8 input/KEY-F8)
(def KEY-F9 input/KEY-F9)
(def KEY-F10 input/KEY-F10)
(def KEY-F11 input/KEY-F11)
(def KEY-F12 input/KEY-F12)
(def KEY-LEFT-ALT input/KEY-LEFT-ALT)
(def KEY-RIGHT-SHIFT input/KEY-RIGHT-SHIFT)
(def KEY-RIGHT-CONTROL input/KEY-RIGHT-CONTROL)
(def KEY-RIGHT-ALT input/KEY-RIGHT-ALT)

;; --- geometric primitives, genuinely by value (geometric-shapes) --------
;; Moved to net.b12n.raylib.models, along with the ground plane below.
;; Re-exported here so every example that says rl/draw-cube! or
;; rl/draw-plane! keeps working unchanged.
(def draw-cube! models/draw-cube!)
(def draw-cube-wires! models/draw-cube-wires!)
(def draw-sphere! models/draw-sphere!)
(def draw-sphere-wires! models/draw-sphere-wires!)
(def draw-cylinder! models/draw-cylinder!)
(def draw-cylinder-wires! models/draw-cylinder-wires!)
(def draw-capsule! models/draw-capsule!)
(def draw-capsule-wires! models/draw-capsule-wires!)

;; --- ground plane, genuinely by value (camera-3d-split-screen) ----------
(def draw-plane! models/draw-plane!)

;; --- window/monitor diagnostics, genuinely by value (highdpi-testbed) ---
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/toggle-borderless-windowed! or rl/get-window-scale-dpi keeps working
;; unchanged.
(def toggle-borderless-windowed! core/toggle-borderless-windowed!)
(def get-window-scale-dpi core/get-window-scale-dpi)
(def get-window-position core/get-window-position)

;; --- hashing + base64 (compute-hash) --------------------------------------
;; Moved to net.b12n.raylib.util. Re-exported here so every example that says
;; rl/compute-md5 or rl/base64-encode keeps working unchanged.
(def compute-crc32 util/compute-crc32)
(def compute-md5 util/compute-md5)
(def compute-sha1 util/compute-sha1)
(def compute-sha256 util/compute-sha256)
(def base64-encode util/base64-encode)

;; --- a persistent native Camera3D, mutated by UpdateCamera ------------------
;; Moved to net.b12n.raylib.camera. Re-exported here so every example that
;; says rl/camera3d-alloc or rl/update-camera! keeps working unchanged.
(def update-camera! camera/update-camera!)
(def disable-cursor! camera/disable-cursor!)
(def enable-cursor! camera/enable-cursor!)
(def CAMERA-CUSTOM camera/CAMERA-CUSTOM)
(def CAMERA-FREE camera/CAMERA-FREE)
(def CAMERA-ORBITAL camera/CAMERA-ORBITAL)
(def CAMERA-FIRST-PERSON camera/CAMERA-FIRST-PERSON)
(def CAMERA-THIRD-PERSON camera/CAMERA-THIRD-PERSON)
(def camera3d-alloc camera/camera3d-alloc)
(def camera3d-free! camera/camera3d-free!)
(def camera3d-set-target! camera/camera3d-set-target!)

;; --- splines by Vector2, genuinely by value (splines-drawing) -----------
;; Moved to net.b12n.raylib.splines. Re-exported here so every example that
;; says rl/spline-segment-linear! keeps working unchanged.
(def spline-segment-linear! splines/spline-segment-linear!)
(def spline-segment-basis! splines/spline-segment-basis!)
(def spline-segment-catmull-rom! splines/spline-segment-catmull-rom!)
(def spline-segment-bezier-cubic! splines/spline-segment-bezier-cubic!)

;; --- rays: picking a point in the 3D scene, all by value -----------------
;; Moved to net.b12n.raylib.rays. Re-exported here so every example that
;; says rl/screen-to-world-ray or rl/cursor-hidden? keeps working unchanged.
(def cursor-hidden? rays/cursor-hidden?)
(def screen-to-world-ray rays/screen-to-world-ray)
(def ray-collision-box rays/ray-collision-box)
(def draw-ray! rays/draw-ray!)

;; --- files: FilePathList, another 16-byte struct returned by value -------
;; Moved to net.b12n.raylib.files. Re-exported here so every example that says
;; rl/dropped-files or rl/directory-files keeps working unchanged.
(def get-working-directory files/get-working-directory)
(def get-prev-directory-path files/get-prev-directory-path)
(def get-file-name files/get-file-name)
(def file-dropped? files/file-dropped?)
(def directory-exists? files/directory-exists?)
(def dropped-files files/dropped-files)
(def directory-files files/directory-files)

;; --- the trace log, and the suite's first callback INTO jolt -------------
;; Moved to net.b12n.raylib.log. Re-exported here so every example that says
;; rl/on-trace-log! or rl/LOG-WARNING keeps working unchanged.
(def set-trace-log-callback log/set-trace-log-callback)
(def LOG-TRACE log/LOG-TRACE)
(def LOG-DEBUG log/LOG-DEBUG)
(def LOG-INFO log/LOG-INFO)
(def LOG-WARNING log/LOG-WARNING)
(def LOG-ERROR log/LOG-ERROR)
(def LOG-FATAL log/LOG-FATAL)
(def TRACE-LOG-BUFFER log/TRACE-LOG-BUFFER)
(def on-trace-log! log/on-trace-log!)
(def free-callable! log/free-callable!)

;; --- the audio stream callback, on a thread jolt never started ----------
;; Moved to net.b12n.raylib.audio. Re-exported here so every example that says
;; rl/on-audio-stream! or rl/clear-audio-stream-callback! keeps working
;; unchanged.
(def on-audio-stream! audio/on-audio-stream!)
(def clear-audio-stream-callback! audio/clear-audio-stream-callback!)

;; --- window placement ----------------------------------------------------
;; Moved to net.b12n.raylib.core. Re-exported here so every example that says
;; rl/set-window-min-size or rl/set-window-monitor keeps working unchanged.
(def set-window-min-size core/set-window-min-size)
(def set-window-monitor core/set-window-monitor)

;; --- Image: raylib's CPU-side pixel buffer, by value ---------------------
;; Moved to net.b12n.raylib.images. Re-exported here so every example that says
;; rl/image-color or rl/image-text keeps working unchanged.
(def image-color images/image-color)
(def image-checked images/image-checked)
(def image-gradient-linear images/image-gradient-linear)
(def image-gradient-radial images/image-gradient-radial)
(def image-gradient-square images/image-gradient-square)
(def image-white-noise images/image-white-noise)
(def image-perlin-noise images/image-perlin-noise)
(def image-cellular images/image-cellular)
(def image-text images/image-text)

;; --- Image processing: raylib's own pixel operations ---------------------
;; Moved to net.b12n.raylib.images. Re-exported here so every example that says
;; rl/image-from-texture! or rl/image->texture keeps working unchanged.
(def image-from-texture! images/image-from-texture!)
(def image-copy! images/image-copy!)
(def unload-image! images/unload-image!)
(def image->texture images/image->texture)
(def image-format! images/image-format!)
(def image-color-invert! images/image-color-invert!)
(def image-color-grayscale! images/image-color-grayscale!)
(def image-color-tint! images/image-color-tint!)
(def image-color-contrast! images/image-color-contrast!)
(def image-color-brightness! images/image-color-brightness!)
(def image-flip-horizontal! images/image-flip-horizontal!)
(def image-flip-vertical! images/image-flip-vertical!)
(def image-blur-gaussian! images/image-blur-gaussian!)

;; --- Image geometry and convolution --------------------------------------
;; Moved to net.b12n.raylib.images. Re-exported here so every example that says
;; rl/image-crop! or rl/image-convolve! keeps working unchanged.
(def image-crop! images/image-crop!)
(def image-resize! images/image-resize!)
(def image-convolve! images/image-convolve!)
