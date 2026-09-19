(ns net.b12n.raylib.check
  "Headless load-check for the library (`jolt -M:check` from lib/).

  Requires every library namespace, which compiles each one (macro expansion,
  var resolution, arity checks) WITHOUT opening a window, so the bindings can be
  verified with no display attached. It does not exercise rendering; that needs
  a real window, and the example suite's own screenshot smoke does it."
  (:require
   [net.b12n.raylib.color]
   [net.b12n.raylib.files]
   [net.b12n.raylib.log]
   [net.b12n.raylib.native]
   [net.b12n.raylib.util]))

(defn -main [& _]
  (println "library namespaces loaded OK"))
