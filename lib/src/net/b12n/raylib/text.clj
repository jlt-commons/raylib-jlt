(ns net.b12n.raylib.text
  "DrawText, DrawFPS and MeasureText: raylib's scalar text-drawing surface.
  These bindings use raylib's scalar-argument variants, so nothing but the
  packed colour crosses the FFI boundary by value here. Font binding is a
  later porting batch's business, not this module's."
  (:require
   [jolt.ffi :as ffi]))

;; --- text ----------------------------------------------------------------
(ffi/defcfn draw-text            "DrawText"            [:string :int :int :int :uint] :void)
(ffi/defcfn draw-fps             "DrawFPS"             [:int :int] :void)
(ffi/defcfn measure-text         "MeasureText"         [:string :int] :int)
