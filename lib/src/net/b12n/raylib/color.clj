(ns net.b12n.raylib.color
  "raylib's `Color` as the packed little-endian uint32 that crosses the FFI
  boundary, plus the named palette from `src/raylib.h`.

  Every drawing binding in this library takes a colour as a `:uint` rather than
  a 4-byte struct, because that is what fits in one register and what jolt could
  express long before it could pass structs by value. `rgba` is the only way
  those integers are built.")

;; #region rgba
(defn rgba
  "Pack an RGBA color into the little-endian uint32 that raylib's `Color` struct
  is (r | g<<8 | b<<16 | a<<24), so it can cross the FFI boundary as a :uint."
  [r g b a]
  (bit-or (int r) (bit-shift-left (int g) 8)
          (bit-shift-left (int b) 16) (bit-shift-left (int a) 24)))
;; #endregion

;; raylib's named color palette (values from src/raylib.h).
(def LIGHTGRAY (rgba 200 200 200 255))   (def GRAY       (rgba 130 130 130 255))
(def DARKGRAY  (rgba 80 80 80 255))      (def YELLOW     (rgba 253 249 0 255))
(def GOLD      (rgba 255 203 0 255))     (def ORANGE     (rgba 255 161 0 255))
(def PINK      (rgba 255 109 194 255))   (def RED        (rgba 230 41 55 255))
(def MAROON    (rgba 190 33 55 255))     (def GREEN      (rgba 0 228 48 255))
(def LIME      (rgba 0 158 47 255))      (def DARKGREEN  (rgba 0 117 44 255))
(def SKYBLUE   (rgba 102 191 255 255))   (def BLUE       (rgba 0 121 241 255))
(def DARKBLUE  (rgba 0 82 172 255))      (def PURPLE     (rgba 200 122 255 255))
(def VIOLET    (rgba 135 60 190 255))    (def DARKPURPLE (rgba 112 31 126 255))
(def BEIGE     (rgba 211 176 131 255))   (def BROWN      (rgba 127 106 79 255))
(def DARKBROWN (rgba 76 63 47 255))      (def WHITE      (rgba 255 255 255 255))
(def BLACK     (rgba 0 0 0 255))         (def MAGENTA    (rgba 255 0 255 255))
(def RAYWHITE  (rgba 245 245 245 255))
