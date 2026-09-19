(ns net.b12n.raylib.check
  "Headless load-check for the library (`jolt -M:check` from lib/).

  Requires every library namespace including net.b12n.raylib.all, so this is
  what compiles the generated aggregator too, WITHOUT opening a window, so
  the bindings can be verified with no display attached. Requiring a
  namespace catches an unresolved symbol within it, and an unknown alias or
  an unqualified unknown symbol at a call site -- but it does NOT catch a
  wrong arity, or a var that does not exist reached through a real module
  alias: planting either of those and running this gate still exits 0. `bb
  lint:strict` (clj-kondo, seeing through defcfn via the project's hook)
  checks the call graph statically and catches both. This gate also does not
  exercise rendering; that needs a real window, and the example suite's own
  screenshot smoke does it."
  (:require
   [net.b12n.raylib.all]
   [net.b12n.raylib.audio]
   [net.b12n.raylib.camera]
   [net.b12n.raylib.color]
   [net.b12n.raylib.core]
   [net.b12n.raylib.files]
   [net.b12n.raylib.images]
   [net.b12n.raylib.input]
   [net.b12n.raylib.kwargs]
   [net.b12n.raylib.log]
   [net.b12n.raylib.models]
   [net.b12n.raylib.native]
   [net.b12n.raylib.rays]
   [net.b12n.raylib.rlgl]
   [net.b12n.raylib.shaders]
   [net.b12n.raylib.shapes]
   [net.b12n.raylib.splines]
   [net.b12n.raylib.text]
   [net.b12n.raylib.textures]
   [net.b12n.raylib.util]))

(defn -main [& _]
  (println "library namespaces loaded OK"))
