(ns net.b12n.raylib.input
  "Keyboard, mouse, gamepad and touch/gesture input: the IsXDown/IsXPressed
  family and their KeyboardKey/MouseButton/GamepadButton/GamepadAxis
  constants, plus GetRandomValue, which lives here because it always has."
  (:require
   [jolt.ffi :as ffi]))

;; --- input -------------------------------------------------------------------
(ffi/defcfn ^:private key-down-raw     "IsKeyDown"          [:int] :int)
(ffi/defcfn ^:private key-pressed-raw  "IsKeyPressed"       [:int] :int)
(ffi/defcfn ^:private mouse-down-raw   "IsMouseButtonDown"  [:int] :int)
(ffi/defcfn ^:private mouse-pressed-raw "IsMouseButtonPressed" [:int] :int)
(ffi/defcfn get-mouse-x      "GetMouseX"         [] :int)
(ffi/defcfn get-mouse-y      "GetMouseY"         [] :int)
(ffi/defcfn get-mouse-wheel  "GetMouseWheelMove" [] :float)
(ffi/defcfn get-random-value "GetRandomValue"    [:int :int] :int)
(ffi/defcfn get-char-pressed "GetCharPressed"    [] :int)   ; unicode codepoint; 0 = queue empty
(ffi/defcfn get-key-pressed  "GetKeyPressed"     [] :int)   ; keycode; 0 = queue empty

;; --- key / mouse predicates (lifted out of screenshot hook plumbing) --------
;; They sat under a banner about headless smoke tests and have nothing to do
;; with screenshots. C-bool returns arrive in the low byte; mask so only 0/1
;; counts.
(defn key-down?
  [k]
  (not (zero? (bit-and (key-down-raw k) 0xff))))

(defn key-pressed?
  [k]
  (not (zero? (bit-and (key-pressed-raw k) 0xff))))

(defn mouse-down?
  [b]
  (not (zero? (bit-and (mouse-down-raw b) 0xff))))

(defn mouse-pressed?
  [b]
  (not (zero? (bit-and (mouse-pressed-raw b) 0xff))))

;; --- constants (raylib KeyboardKey / MouseButton) ----------------------------
;; Merged from three sections of the old single-file raylib.clj: this one,
;; "more KeyboardKey constants" and "more keyboard constants
;; (keyboard-testbed)". Each block's own order is preserved and nothing is
;; deduplicated or re-sorted.
(def ^:const KEY-NULL  0)   ; not a key: "nothing closes the window"
(def ^:const KEY-SPACE 32)  (def ^:const KEY-R     82)
(def ^:const KEY-W     87)  (def ^:const KEY-A     65)
(def ^:const KEY-S     83)  (def ^:const KEY-D     68)
(def ^:const KEY-RIGHT 262) (def ^:const KEY-LEFT  263)
(def ^:const KEY-DOWN  264) (def ^:const KEY-UP    265)
(def ^:const MOUSE-LEFT 0)
(def ^:const MOUSE-RIGHT 1)
(def ^:const MOUSE-MIDDLE 2)
(def ^:const KEY-BACKSPACE 259) (def ^:const KEY-ENTER 257)

(def ^:const KEY-ESCAPE 256) (def ^:const KEY-TAB   258)
(def ^:const KEY-DELETE 261) (def ^:const KEY-HOME  268)
(def ^:const KEY-END    269) (def ^:const KEY-F1    290)
(def ^:const KEY-F2     291) (def ^:const KEY-F3    292)
(def ^:const KEY-LEFT-SHIFT 340) (def ^:const KEY-LEFT-CONTROL 341)
(def ^:const KEY-LEFT-SUPER 343)
(def ^:const KEY-ZERO 48) (def ^:const KEY-ONE   49) (def ^:const KEY-TWO   50)
(def ^:const KEY-THREE 51) (def ^:const KEY-FOUR 52) (def ^:const KEY-FIVE  53)
(def ^:const KEY-SIX  54) (def ^:const KEY-SEVEN 55) (def ^:const KEY-EIGHT 56)
(def ^:const KEY-NINE 57)
(def ^:const KEY-B 66) (def ^:const KEY-C 67) (def ^:const KEY-E 69)
(def ^:const KEY-F 70)
(def ^:const KEY-G 71) (def ^:const KEY-H 72) (def ^:const KEY-M 77)
(def ^:const KEY-N 78) (def ^:const KEY-P 80) (def ^:const KEY-Q 81)
(def ^:const KEY-T 84) (def ^:const KEY-V 86) (def ^:const KEY-X 88)
(def ^:const KEY-Y 89) (def ^:const KEY-Z 90)

(def ^:const KEY-I 73) (def ^:const KEY-J 74) (def ^:const KEY-K 75)
(def ^:const KEY-L 76) (def ^:const KEY-O 79) (def ^:const KEY-U 85)
(def ^:const KEY-APOSTROPHE 39) (def ^:const KEY-COMMA 44)
(def ^:const KEY-MINUS 45) (def ^:const KEY-PERIOD 46)
(def ^:const KEY-SLASH 47) (def ^:const KEY-SEMICOLON 59)
(def ^:const KEY-EQUAL 61) (def ^:const KEY-LEFT-BRACKET 91)
(def ^:const KEY-BACKSLASH 92) (def ^:const KEY-RIGHT-BRACKET 93)
(def ^:const KEY-GRAVE 96) (def ^:const KEY-INSERT 260)
(def ^:const KEY-PAGE-UP 266) (def ^:const KEY-PAGE-DOWN 267)
(def ^:const KEY-CAPS-LOCK 280) (def ^:const KEY-PRINT-SCREEN 283)
(def ^:const KEY-PAUSE 284) (def ^:const KEY-F4 293)
(def ^:const KEY-F5 294) (def ^:const KEY-F6 295) (def ^:const KEY-F7 296)
(def ^:const KEY-F8 297) (def ^:const KEY-F9 298) (def ^:const KEY-F10 299)
(def ^:const KEY-F11 300) (def ^:const KEY-F12 301)
(def ^:const KEY-LEFT-ALT 342) (def ^:const KEY-RIGHT-SHIFT 344)
(def ^:const KEY-RIGHT-CONTROL 345) (def ^:const KEY-RIGHT-ALT 346)

;; --- gamepad -----------------------------------------------------------------
(ffi/defcfn get-gamepad-axis-count    "GetGamepadAxisCount"    [:int] :int)
(ffi/defcfn get-gamepad-axis-movement "GetGamepadAxisMovement" [:int :int] :float)
(ffi/defcfn get-gamepad-name          "GetGamepadName"         [:int] :string)
(ffi/defcfn ^:private gamepad-available-raw "IsGamepadAvailable"     [:int] :int)
(ffi/defcfn ^:private gamepad-down-raw      "IsGamepadButtonDown"    [:int :int] :int)
(ffi/defcfn ^:private gamepad-pressed-raw   "IsGamepadButtonPressed" [:int :int] :int)
(ffi/defcfn ^:private gamepad-released-raw  "IsGamepadButtonReleased" [:int :int] :int)

(defn gamepad-available?
  [pad]
  (not (zero? (bit-and (gamepad-available-raw pad) 0xff))))

(defn gamepad-down?
  [pad button]
  (not (zero? (bit-and (gamepad-down-raw pad button) 0xff))))

(defn gamepad-pressed?
  [pad button]
  (not (zero? (bit-and (gamepad-pressed-raw pad button) 0xff))))

(defn gamepad-released?
  [pad button]
  (not (zero? (bit-and (gamepad-released-raw pad button) 0xff))))

;; raylib GamepadButton / GamepadAxis
(def ^:const PAD-UP     1)  (def ^:const PAD-RIGHT  2)
(def ^:const PAD-DOWN   3)  (def ^:const PAD-LEFT   4)
(def ^:const PAD-Y      5)  (def ^:const PAD-B      6)
(def ^:const PAD-A      7)  (def ^:const PAD-X      8)
(def ^:const PAD-L1     9)  (def ^:const PAD-L2    10)
(def ^:const PAD-R1    11)  (def ^:const PAD-R2    12)
(def ^:const PAD-SELECT 13) (def ^:const PAD-MENU  14)
(def ^:const PAD-START 15)
(def ^:const AXIS-LEFT-X 0) (def ^:const AXIS-LEFT-Y 1)
(def ^:const AXIS-RIGHT-X 2) (def ^:const AXIS-RIGHT-Y 3)

;; --- touch / gestures --------------------------------------------------------
;; On desktop raylib synthesises touch point 0 from the mouse, so these read as a
;; one-finger stream with no touchscreen attached.
(ffi/defcfn get-touch-point-count "GetTouchPointCount" [] :int)
(ffi/defcfn get-touch-point-id    "GetTouchPointId"    [:int] :int)
(ffi/defcfn get-touch-x           "GetTouchX"          [] :int)
(ffi/defcfn get-touch-y           "GetTouchY"          [] :int)
(ffi/defcfn get-gesture-detected  "GetGestureDetected" [] :int)
(ffi/defcfn set-gestures-enabled  "SetGesturesEnabled" [:uint] :void)

(def ^:const GESTURE-NONE 0)        (def ^:const GESTURE-TAP 1)
(def ^:const GESTURE-DOUBLETAP 2)   (def ^:const GESTURE-HOLD 4)
(def ^:const GESTURE-DRAG 8)        (def ^:const GESTURE-SWIPE-RIGHT 16)
(def ^:const GESTURE-SWIPE-LEFT 32) (def ^:const GESTURE-SWIPE-UP 64)
(def ^:const GESTURE-SWIPE-DOWN 128)
(def ^:const GESTURE-PINCH-IN 256)  (def ^:const GESTURE-PINCH-OUT 512)

;; --- remaining input predicates ----------------------------------------------
(ffi/defcfn set-mouse-cursor "SetMouseCursor" [:int] :void)
(ffi/defcfn ^:private key-released-raw   "IsKeyReleased"          [:int] :int)
(ffi/defcfn ^:private mouse-released-raw "IsMouseButtonReleased"  [:int] :int)

(defn key-released?
  [k]
  (not (zero? (bit-and (key-released-raw k) 0xff))))

(defn mouse-released?
  [b]
  (not (zero? (bit-and (mouse-released-raw b) 0xff))))

;; --- mouse position / cursor -------------------------------------------------
;; SetMousePosition warps the pointer; HideCursor and ShowCursor toggle whether
;; it is drawn. Together they are mouse-look: read the offset from the window
;; centre, turn by it, warp back to the centre, and the pointer can turn forever
;; without leaving the window or being visible while it does (doom).
(ffi/defcfn set-mouse-position "SetMousePosition" [:int :int] :void)
(ffi/defcfn hide-cursor        "HideCursor"       [] :void)
(ffi/defcfn show-cursor        "ShowCursor"       [] :void)
