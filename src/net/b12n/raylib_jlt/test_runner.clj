(ns net.b12n.raylib-jlt.test-runner
  "Discovers *_test.clj under test/, requires each, runs clojure.test.
  Wired as the :test alias main in deps.edn. Walks the tree via java.io.File
  interop (verified working on this jolt) and ns-ifies each path relative
  to the test root, so new test namespaces are picked up with no edit here.",
  (:require [clojure.test :as t]))

(defn- files-under
  [dir]
  (let [f (java.io.File. dir)]
    (when (.isDirectory f)
      (mapcat (fn [x]
                (if (.isDirectory x)
                  (files-under (str dir "/" (.getName x)))
                  [(str dir "/" (.getName x))]))
              (.listFiles f)))))

(defn- ns-of
  [test-root path]
  (-> path
      (subs (inc (count test-root)))
      (clojure.string/replace #"\.clj$" "")
      (clojure.string/replace "/" ".")
      (clojure.string/replace "_" "-")))

(defn -main
  [& _args]
  (let [root "test"
        nss (->> (files-under root)
                 (filter #(re-find #"_test\.clj$" %))
                 (map #(ns-of root %))
                 (sort))]
    (println "Testing" (count nss) "namespaces:" (pr-str nss))
    (doseq [n nss] (require (symbol n)))
    (let [{:keys [test pass fail error]} (apply t/run-tests (map symbol nss))]
      (println (format "Ran %d tests: %d assertions passed, %d failures, %d errors"
                       test pass fail error))
      (System/exit (if (zero? (+ fail error)) 0 1)))))
