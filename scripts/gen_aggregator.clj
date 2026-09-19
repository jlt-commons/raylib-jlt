#!/usr/bin/env bb
;; Regenerates lib/src/net/b12n/raylib/all.clj, the one door back into the
;; library's module namespaces. The module count is not stated here on
;; purpose -- this is a hand-written comment, not generated output, so a
;; literal count here can drift from reality the moment a module is added
;; or removed. The generated file's own header states the real count,
;; computed at generation time.
;;
;; Usage: bb scripts/gen_aggregator.clj          write lib/src/net/b12n/raylib/all.clj
;;        bb scripts/gen_aggregator.clj --check  compare against the committed file;
;;                                                exit non-zero with a diff if they differ
;;
;; Needs jolt + libraylib on this machine, same as `bb check:lib`: the list of
;; public vars per module comes from `ns-publics` against a live jolt process,
;; not from parsing the module source as text. Verified against jolt v0.8.9:
;; `def` re-export plus `alter-meta!` to copy `:doc`/`:arglists` onto the
;; alias works, and `ns-publics` is available there, returning exactly the
;; expected public symbols with no private leakage. Asking jolt directly
;; sidesteps every trap a source-text parser would hit here:
;; `def`/`defn`/`defn-`/`ffi/defcfn` are all different
;; shapes, several modules pack more than one `def` on a line, and comment
;; banners are not code.
;;
;; `*file*` is resolved to an absolute path by babashka, so this script finds
;; its own directory (and the repo root one level up) regardless of the
;; caller's cwd -- it works the same run as `bb scripts/gen_aggregator.clj`
;; from the repo root or as `bb ../scripts/gen_aggregator.clj` from lib/,
;; which is how lib/bb.edn's own gen:all task reaches it.

(ns gen-aggregator
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def repo-root (str (fs/parent (fs/parent (fs/normalize (fs/absolutize *file*))))))
(def lib-dir (str (fs/file repo-root "lib")))
(def module-dir (str lib-dir "/src/net/b12n/raylib"))
(def all-file (str module-dir "/all.clj"))

;; Excluded BY NAME, not by any "starts with check" convention -- so a future
;; module whose name happens to start with those letters is never silently
;; dropped. `all` is this generator's own output: it cannot require itself.
;; `check` is net.b12n.raylib.check, the library's own headless load gate,
;; not part of its public surface.
(def excluded-basenames #{"all" "check"})

(defn jolt-cmd [] (if (fs/which "jolt") "jolt" "joltc"))

(defn module-basenames
  "Sorted basenames of every module file under module-dir, minus
  excluded-basenames. Sorting here is what makes the :require list and the
  per-module sections in the generated file deterministic."
  []
  (->> (fs/glob module-dir "*.clj")
       (map (comp fs/file-name str))
       (map #(str/replace % #"\.clj$" ""))
       (remove excluded-basenames)
       sort))

(defn ns-sym [basename] (symbol (str "net.b12n.raylib." basename)))

(defn fetch-publics
  "Shell out to a live jolt process from lib/ (so lib/deps.edn's :jolt/native
  resolves, same as `bb check:lib`) and ask it, per module, for every public
  symbol sorted, each tagged with whether it carries ^:const. Returns
  {basename [[sym const?] ...]}. This is ns-publics driving the generator,
  verified against jolt v0.8.9."
  [basenames]
  (let [nss (mapv ns-sym basenames)
        code (str "(doseq [m '" (pr-str nss) "] (require m))"
                  "(prn (into {} (map (fn [m] [m (mapv (fn [s] [s (boolean (:const (meta (get (ns-publics m) s))))])"
                  "                                     (sort (keys (ns-publics m))))]))"
                  "           '" (pr-str nss) "))")
        {:keys [exit out err]} (p/shell {:dir lib-dir
                                         :out :string
                                         :err :string
                                         :continue true}
                                        (jolt-cmd) "-M" "-e" code)]
    (when-not (zero? exit)
      (binding [*out* *err*]
        (println "jolt failed while collecting ns-publics for the aggregator:")
        (println err))
      (System/exit 1))
    (let [by-ns (edn/read-string out)]
      (into {} (map (fn [b] [b (get by-ns (ns-sym b))])) basenames))))

(defn assert-no-collisions!
  "Aborts (non-zero exit, naming both modules) if any symbol is public in more
  than one module. A collision would make `all` silently pick one module's
  var over the other's -- this generator refuses instead."
  [by-module]
  (let [owner (fn [[basename pairs]]
                (map (fn [[sym _]] [sym basename]) pairs))
        by-sym (group-by first (mapcat owner by-module))
        collisions (->> by-sym
                        (filter (fn [[_ owners]] (> (count owners) 1)))
                        (sort-by first))]
    (when (seq collisions)
      (binding [*out* *err*]
        (println "gen_aggregator: name collision(s) across modules -- aborting:")
        (doseq [[sym owners] collisions]
          (println (str "  " sym " is public in: " (str/join ", " (map second owners))))))
      (System/exit 1))))

(defn const-families
  "Family prefixes among the vars that carry ^:const, derived from the
  symbol names themselves (the segment before the first '-'), not hand
  enumerated. A hand-written list drifts the moment a module adds a new
  ^:const family and nobody remembers to update the comment -- this one
  can't drift, because it's read off the same live metadata that produces
  const-count."
  [by-module]
  (->> (mapcat val by-module)
       (filter second)
       (map first)
       (map name)
       (map (fn [s] (first (str/split s #"-"))))
       distinct
       sort))

(defn ns-form
  [basenames total const-count families]
  (str
   "(ns net.b12n.raylib.all\n"
   "  \"Every public var of this library's " total " concrete-module surface, re-exported\n"
   "  under net.b12n.raylib.all so a caller can (:require [net.b12n.raylib.all :as rl])\n"
   "  and keep calling rl/rect! instead of requiring all " (count basenames) " modules by hand.\n"
   "  net.b12n.raylib.check is not aggregated: it is the library's own headless load\n"
   "  gate, not part of its public surface.\n\n"
   "  " const-count " of these vars carry ^:const in their source module (the raylib/rlgl enum\n"
   "  families: " (str/join ", " (map (fn [f] (str f "-*")) families)) "). alter-meta! below\n"
   "  copies :const alongside :doc and :arglists onto every alias, so it is not lost the\n"
   "  way the hand-written shim this file replaces silently dropped it. The value read\n"
   "  through the alias is correct either way; :const only affects whether a caller's\n"
   "  own compile can inline the constant at the call site.\"\n"
   "  (:refer-clojure :exclude [run!]) ; core/run! shadows clojure.core/run!\n"
   "  (:require\n"
   (str/join "\n" (map (fn [b] (str "   [net.b12n.raylib." b " :as " b "]")) basenames))
   "))\n"))

(defn module-section
  [basename pairs]
  (str "\n;; --- " basename " (" (count pairs) ") "
       (apply str (repeat (max 0 (- 72 (count basename) 10)) "-")) "\n"
       (str/join "\n"
                 (map (fn [[sym _const?]]
                        (str "(def " sym " " basename "/" sym ")\n"
                             "(alter-meta! #'" sym " merge (select-keys (meta #'" basename "/" sym ")"
                             " [:doc :arglists :const]))"))
                      pairs))
       "\n"))

(defn generated-content
  [basenames by-module]
  (let [total (reduce + (map (comp count val) by-module))
        const-count (reduce + (map (fn [[_ pairs]] (count (filter second pairs))) by-module))
        families (const-families by-module)]
    (str ";; GENERATED FILE. Do not edit by hand.\n"
         ";; Produced by scripts/gen_aggregator.clj -- regenerate with `bb gen:all`\n"
         ";; (repo root) or `bb gen:all` from lib/. `bb check:aggregator` fails if this\n"
         ";; file no longer matches what the generator produces from the " (count basenames)
         " concrete\n;; module namespaces.\n"
         ";;\n"
         ";; " total " public vars total across " (count basenames) " modules, zero name collisions,\n"
         ";; computed by asking a live jolt process for each module's ns-publics -- the\n"
         ";; same mechanism verified against jolt v0.8.9 for def re-export + alter-meta! metadata\n"
         ";; copy, not a parse of the module source text.\n"
         (ns-form basenames total const-count families)
         (str/join "" (map (fn [b] (module-section b (get by-module b))) basenames)))))

(defn write-file! [content]
  (spit all-file content)
  (println (str "wrote " all-file)))

(defn check-mode [content]
  (let [committed (if (fs/exists? all-file) (slurp all-file) "")]
    (if (= committed content)
      (do (println (str all-file " matches the generator output")) 0)
      (let [tmp (fs/create-temp-file {:suffix ".clj"})]
        (spit (str tmp) content)
        (println (str all-file " does NOT match the generator output. Diff (committed vs generated):"))
        (let [{:keys [out]} (p/shell {:out :string
                                      :continue true} "diff" "-u" all-file (str tmp))]
          (println out))
        (fs/delete (str tmp))
        1))))

(defn -main [& args]
  (let [basenames (module-basenames)
        by-module (fetch-publics basenames)]
    (assert-no-collisions! by-module)
    (let [content (generated-content basenames by-module)]
      (if (some #{"--check"} args)
        (System/exit (check-mode content))
        (write-file! content)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
