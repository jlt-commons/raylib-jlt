#!/usr/bin/env bb
(ns check-demos
  "Gate the demo gallery against the registry, so the counts in prose cannot
  drift away from the GIFs on disk.

  `bb check:registration` already gates the example count across source,
  deps.edn, check.clj and bb.edn. Nothing gated the gallery, which is how
  `demos.md` once claimed to show every example while showing 91 of them.

  This checks CONSISTENCY, not completeness. An example with no recording is
  fine and must stay fine: `bb record` drives an internal capture tool, and
  CONTRIBUTING.md tells contributors they don't need to record anything, so a
  gate demanding a GIF per example would turn every outside example PR red on
  a step its author was told to skip. What is not fine is the galleries and
  the prose disagreeing about which examples have one.

  So an unrecorded example must be absent from the full-size gallery, carry a
  `*not recorded yet*` row in the catalog, and be excluded from every stated
  count. Record it and all three flip together.

  Every expected value is derived from the registry or from the file itself,
  so adding an example never means editing a number in here. Read-only, and
  needs no jolt, libraylib or capture tool, so it runs on a bare checkout."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]))

(load-file (str (io/file (or (System/getProperty "bb.file.dir") "scripts")
                         "examples_registry.clj")))

(def ^:private ids (mapv first examples-registry/examples))

(defn- gif? [id] (.exists (io/file (str "docs/demos/" id ".gif"))))

(defn- problems []
  (let [demos    (slurp (io/file "docs/guide/demos.md"))
        catalog  (slurp (io/file "docs/guide/example-catalog.md"))
        flat     (slurp (io/file "docs/demos/README.md"))
        ledger   (read-string (slurp (io/file "docs/demos/ledger.edn")))
        id-set   (set ids)
        recorded (set (filter gif? ids))
        pending  (set/difference id-set recorded)
        probs    (atom [])
        add!     (fn [what] (swap! probs conj what))]

    ;; 1. The ledger describes recordings, so it tracks the recorded set
    ;;    exactly. Then `bb record` reporting nothing to do means something.
    (let [lk (set (keys ledger))]
      (doseq [id (sort (set/difference recorded lk))] (add! (str "ledger has no entry for " id ", which has a GIF")))
      (doseq [k (sort (set/difference lk id-set))] (add! (str "ledger entry is not an example: " k))))

    ;; 2. The full-size gallery carries exactly the recorded examples.
    (let [in-demos (set (map second (re-seq #"(?m)^### (\S+)" demos)))]
      (doseq [id (sort (set/difference recorded in-demos))]
        (add! (str "docs/guide/demos.md has no entry for " id ", which has a GIF")))
      (doseq [id (sort (set/intersection pending in-demos))]
        (add! (str "docs/guide/demos.md has an entry for " id ", which has no GIF"))))

    ;; 3. The catalog carries every example, recorded or not, and marks which.
    (let [rows  (set (map second (re-seq #"\| `([a-z0-9-]+)` \|" catalog)))
          noted (set (map second (re-seq #"\| \*not recorded yet\* \| `([a-z0-9-]+)` \|" catalog)))]
      (doseq [id (sort (set/difference id-set rows))]
        (add! (str "docs/guide/example-catalog.md has no row for " id)))
      (doseq [id (sort (set/difference pending noted))]
        (add! (str id " has no GIF, so its catalog row should read *not recorded yet*")))
      (doseq [id (sort (set/intersection recorded noted))]
        (add! (str id " has a GIF, but its catalog row still reads *not recorded yet*"))))

    ;; 4. Every image reference resolves. A gallery pointing at a deleted still
    ;;    renders as a broken image on the published site, silently.
    (doseq [[file txt pat] [["docs/guide/demos.md" demos #"\.\./demos/([A-Za-z0-9_.-]+)"]
                            ["docs/guide/example-catalog.md" catalog #"\.\./demos/([A-Za-z0-9_.-]+)"]
                            ["docs/demos/README.md" flat #"\]\(([A-Za-z0-9_.-]+\.gif)\)"]]
            [_ f] (re-seq pat txt)]
      (when-not (.exists (io/file (str "docs/demos/" f)))
        (add! (str file " references a missing docs/demos/" f))))

    ;; 5. Catalog thumbnails link into demos.md by anchor.
    (let [anchors (set (map second (re-seq #"(?m)^### (\S+)" demos)))]
      (doseq [a (sort (set (remove anchors (map second (re-seq #"demos\.md#([a-z0-9-]+)" catalog)))))]
        (add! (str "docs/guide/example-catalog.md links to demos.md#" a ", which has no heading"))))

    ;; 6. Each "## <group> (N)" heading states the entries beneath it, and the
    ;;    headings together account for every recording.
    (doseq [s (rest (str/split demos #"(?m)^## "))]
      (when-let [[_ g n] (re-find #"^(\S+) \((\d+)\)" s)]
        (let [actual (count (re-seq #"(?m)^### " s))]
          (when (not= (parse-long n) actual)
            (add! (str "docs/guide/demos.md heading \"## " g " (" n ")\" but the section holds "
                       actual " entries"))))))
    (let [stated (reduce + 0 (map (fn [m] (parse-long (second m)))
                                  (re-seq #"(?m)^## \S+ \((\d+)\)" demos)))]
      (when (not= stated (count recorded))
        (add! (str "docs/guide/demos.md group headings total " stated ", but "
                   (count recorded) " examples have a GIF"))))

    ;; 7. Prose counting GIFs or recordings must say what is on disk. Scoped to
    ;;    a number immediately followed by a gallery word, so it does not fire
    ;;    on a resolution, a year or an example count.
    (doseq [file ["README.md" "docs/site.edn" "docs/guide/demos.md"
                  "docs/guide/index.md" "docs/templates/home.html"]
            :when (.exists (io/file file))
            [whole num] (re-seq #"(\d+)\s+(?:animated\s+)?(?:recorded\s+)?(?:GIFs?|recordings)\b"
                                (slurp (io/file file)))]
      (when (not= (parse-long num) (count recorded))
        (add! (str file " says \"" (str/trim whole) "\", but docs/demos holds "
                   (count recorded) " GIFs"))))

    [@probs pending]))

(defn -main [& _]
  (let [[probs pending] (problems)]
    (when (seq pending)
      (println (str (count pending) " example(s) not recorded yet: "
                    (str/join ", " (sort pending))))
      (println "That is allowed. A maintainer records them with `bb record`.")
      (println))
    (if (seq probs)
      (binding [*out* *err*]
        (println "demo gallery gaps:")
        (doseq [p probs] (println (str "  " p)))
        (println)
        (println "The galleries and the prose must agree about which examples have a")
        (println "GIF. See AGENTS.md \"Counts live in several places\".")
        (System/exit 1))
      (println (str "demo gallery ok, " (count ids) " examples, "
                    (- (count ids) (count pending)) " recorded, galleries and counts agree")))))

(when (= *file* (System/getProperty "babashka.file")) (-main))
