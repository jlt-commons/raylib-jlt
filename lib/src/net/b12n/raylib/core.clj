(ns net.b12n.raylib.core
  "Window lifecycle (init/close, frame begin/end, config flags and window
  state), monitor and clipboard queries, window placement and hi-DPI
  diagnostics, take-screenshot, window-should-close?, and run!, the entry
  point that marshals an example's -main onto the process main thread macOS
  requires for AppKit to open a window at all."
  ;; run! is the REPL entry point below; the shadowed clojure.core/run! is a
  ;; reducing form this suite never uses.
  (:refer-clojure :exclude [run!])
  (:require
   [jolt.ffi :as ffi]
   [jolt.host]
   [net.b12n.raylib.native :as native]))

;; --- window / lifecycle ------------------------------------------------------
(ffi/defcfn init-window    "InitWindow"   [:int :int :string] :void)
(ffi/defcfn set-target-fps "SetTargetFPS" [:int] :void)
;; Reassigns the key that closes the window. Passing KEY-NULL takes ESC away, so
;; an example can put its own confirmation in front of a close request rather
;; than being closed out from under it. See window-should-close.
(ffi/defcfn set-exit-key   "SetExitKey"   [:int] :void)
(ffi/defcfn close-window   "CloseWindow"  [] :void)
(ffi/defcfn ^:private should-close-raw "WindowShouldClose" [] :int)

;; --- frame -------------------------------------------------------------------
(ffi/defcfn begin-drawing      "BeginDrawing"     [] :void)
(ffi/defcfn end-drawing        "EndDrawing"       [] :void)
(ffi/defcfn clear-background   "ClearBackground"  [:uint] :void)       ; Color
(ffi/defcfn get-frame-time     "GetFrameTime"     [] :float)           ; seconds since last frame
(ffi/defcfn begin-scissor-mode "BeginScissorMode" [:int :int :int :int] :void)
(ffi/defcfn end-scissor-mode   "EndScissorMode"   [] :void)

;; --- take-screenshot / window-should-close? (lifted out of screenshot hook
;; plumbing) --------------------------------------------------------------
;; Neither is about headless smoke tests, the banner they sat under in
;; raylib.clj. See net.b12n.raylib.rlgl (flush-batch) and net.b12n.raylib.input
;; (the four key / mouse predicates) for the rest of that banner's contents.
(ffi/defcfn take-screenshot "TakeScreenshot" [:string] :void)

(defn window-should-close?
  "WindowShouldClose. C-bool returns arrive in the low byte; mask so only 0/1
  counts."
  []
  (not (zero? (bit-and (should-close-raw) 0xff))))

;; --- window state / config flags ---------------------------------------------
;; SetConfigFlags must be called BEFORE InitWindow; SetWindowState/ClearWindowState
;; take the same FLAG-* bits at runtime.
(ffi/defcfn set-config-flags   "SetConfigFlags"   [:uint] :void)
(ffi/defcfn set-window-state   "SetWindowState"   [:uint] :void)
(ffi/defcfn clear-window-state "ClearWindowState" [:uint] :void)
(ffi/defcfn toggle-fullscreen  "ToggleFullscreen" [] :void)
(ffi/defcfn get-screen-width   "GetScreenWidth"   [] :int)
(ffi/defcfn get-screen-height  "GetScreenHeight"  [] :int)
(ffi/defcfn get-time           "GetTime"          [] :double)
(ffi/defcfn ^:private window-state-raw   "IsWindowState"   [:uint] :int)
(ffi/defcfn ^:private window-resized-raw "IsWindowResized" [] :int)

(def ^:const FLAG-WINDOW-RESIZABLE   0x00000004)
(def ^:const FLAG-WINDOW-UNDECORATED 0x00000008)
(def ^:const FLAG-MSAA-4X-HINT       0x00000020)
(def ^:const FLAG-VSYNC-HINT         0x00000040)
(def ^:const FLAG-WINDOW-TOPMOST     0x00001000)
(def ^:const FLAG-WINDOW-HIGHDPI     0x00002000)

(defn window-state?
  "IsWindowState, is this FLAG-* bit currently set?"
  [flag]
  (not (zero? (bit-and (window-state-raw flag) 0xff))))

(defn window-resized?
  "IsWindowResized, did the window change size on the last frame?"
  []
  (not (zero? (bit-and (window-resized-raw) 0xff))))

;; --- monitors ----------------------------------------------------------------
;; GetMonitorPosition returns a Vector2 by value and so has no binding; the
;; scalar width/height/refresh/name queries cover what a monitor listing needs.
(ffi/defcfn get-monitor-count        "GetMonitorCount"       [] :int)
(ffi/defcfn get-current-monitor      "GetCurrentMonitor"     [] :int)
(ffi/defcfn get-monitor-width        "GetMonitorWidth"       [:int] :int)
(ffi/defcfn get-monitor-height       "GetMonitorHeight"      [:int] :int)
(ffi/defcfn get-monitor-refresh-rate "GetMonitorRefreshRate" [:int] :int)
(ffi/defcfn get-monitor-name         "GetMonitorName"        [:int] :string)

;; --- clipboard ---------------------------------------------------------------
;; GetClipboardText returns a const char* raylib owns; :string copies it out.
(ffi/defcfn set-clipboard-text "SetClipboardText" [:string] :void)
(ffi/defcfn get-clipboard-text "GetClipboardText" [] :string)

;; --- REPL entry point --------------------------------------------------------
;; InitWindow reaches AppKit through GLFW, and macOS only lets NSApplication
;; initialize on the process main thread. An nREPL eval runs on a worker thread,
;; so calling an example's -main straight from a connected editor traps the whole
;; process: EXC_BREAKPOINT inside -[NSApplication run], with no Clojure exception
;; to catch and nothing in the REPL but a dropped connection.
;;
;; jolt.host/call-on-main-thread-async marshals the call onto the thread `jolt
;; nrepl-server` parks in its main pump, and invokes it inline when no pump is
;; running, which is what `bb <example>` does. So the one call is right from both.

(defn run!
  "Run an example's entry point `f` on the process main thread, the only thread
  macOS lets raylib open a window from. This is how an example starts from a
  connected editor:

      (comment
        (rl/run! -main))

  Calling `(-main)` directly over nREPL instead kills the whole jolt process,
  editor connection included, because the eval runs on a worker thread and macOS
  traps any thread but the main one initializing AppKit.

  Returns immediately under `jolt nrepl-server`: the window loop takes the main
  thread and the REPL stays free. Under `bb <example>` there is no pump and the
  caller is already the main thread, so `f` runs inline and this wrapper changes
  nothing."
  [f]
  (jolt.host/call-on-main-thread-async f))

;; --- window/monitor diagnostics, genuinely by value (highdpi-testbed) ---
(ffi/defcfn toggle-borderless-windowed! "ToggleBorderlessWindowed" [] :void)

(ffi/defcfn ^:private get-window-scale-dpi-raw "GetWindowScaleDPI"
  []
  [:by-value [:struct [[:x :float] [:y :float]]]])

(defn get-window-scale-dpi
  "GetWindowScaleDPI. Returns [x y]."
  []
  (let [out (ffi/alloc (ffi/layout-size native/vector2-layout))]
    (try
      (get-window-scale-dpi-raw out)
      [(ffi/read-field out native/vector2-layout :x)
       (ffi/read-field out native/vector2-layout :y)]
      (finally (ffi/free out)))))

(ffi/defcfn ^:private get-window-position-raw "GetWindowPosition"
  []
  [:by-value [:struct [[:x :float] [:y :float]]]])

(defn get-window-position
  "GetWindowPosition. Returns [x y]."
  []
  (let [out (ffi/alloc (ffi/layout-size native/vector2-layout))]
    (try
      (get-window-position-raw out)
      [(ffi/read-field out native/vector2-layout :x)
       (ffi/read-field out native/vector2-layout :y)]
      (finally (ffi/free out)))))

;; --- window placement ----------------------------------------------------
;; Both scalar, and both only meaningful after init-window. SetWindowMinSize
;; needs FLAG_WINDOW_RESIZABLE to have any effect, since a fixed-size window has
;; no minimum to enforce.
(ffi/defcfn set-window-min-size "SetWindowMinSize" [:int :int] :void)
(ffi/defcfn set-window-monitor  "SetWindowMonitor" [:int] :void)
