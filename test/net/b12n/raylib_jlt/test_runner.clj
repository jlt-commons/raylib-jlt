(ns net.b12n.raylib-jlt.test-runner
  "Entry point for `jolt -M:test`. Requires each test namespace and runs
  clojure.test against it. Exits non-zero on any failure."
  (:require [clojure.test :as t]))

(defn- exit
  "Terminate the process with `code`.

  Call `System/exit` DIRECTLY. It is a static-method interop FORM, not a var,
  so `(resolve 'System/exit)` is ALWAYS nil, under jolt and on the JVM alike.
  A cond guarded on that resolve never fires and falls through to nil, which
  makes a suite print its failures and still exit 0. Same bug and same fix as
  glitter-gl.test-runner/exit."
  [code]
  (System/exit code))

(defn -main [& _]
  (let [namespaces '[net.b12n.raylib-jlt.ffi-write-test]
        ;; A namespace that fails to REQUIRE used to be printed and forgotten.
        ;; run-tests only ever sees what loaded, so its counters cannot tell a
        ;; namespace that does not exist from one that would not compile.
        ;; Measured on jolt v0.7.29, whose ffi/write takes its last two
        ;; arguments the other way round: glitter-uikit's runner exited 0 on 19
        ;; of its 37 tests and the whole fleet reported green against a runtime
        ;; the code cannot run on.
        broken (atom [])]
    (doseq [ns namespaces]
      (try (require ns :reload)
           (catch Throwable e
             (swap! broken conj ns)
             (println "ERROR requiring" ns ":" (pr-str e)))))
    (let [loaded  (remove (set @broken) namespaces)
          ;; (apply t/run-tests '()) is (t/run-tests), which tests the CURRENT
          ;; namespace and reports a cheerful zero. Guard the empty case.
          results (if (seq loaded)
                    (apply t/run-tests loaded)
                    {:test 0
                     :pass 0
                     :fail 0
                     :error 0})
          failed  (+ (:fail results 0) (:error results 0) (count @broken))]
      (println "----")
      (when (seq @broken)
        (println "FAILED TO LOAD:" (count @broken) "of" (count namespaces)
                 "namespaces:" (pr-str @broken))
        (println "  a namespace that will not load is a failure, not an absence"))
      (println "tests:" (:test results 0)
               "assertions:" (:pass results 0) "passed /"
               failed "failed")
      (when (pos? failed) (exit 1)))))
