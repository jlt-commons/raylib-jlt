#!/usr/bin/env bb
;; Convert flat keyword-argument calls into an explicit map literal.
;;
;; net.b12n.raylib.kwargs's drawing/window functions are defined `[& {:keys [...]}]`,
;; which lets a caller write either
;;
;;   (rl/window! :width W :height H :title "hi")
;;   (rl/window! {:width W :height H :title "hi"})
;;
;; and get the same result -- Clojure's rest-arg map destructuring accepts a
;; flat run of keyword/value pairs OR a single map. The project's preferred
;; style is the explicit map: it reads as one argument instead of a run of
;; tokens, and it is what `check_positional_args.clj`'s own recommendation
;; already shows for definitions. This script brings call sites into line.
;;
;; Structural, not textual: it walks each file with rewrite-clj (bundled with
;; babashka), so it only ever touches an actual argument list of an actual
;; call to one of `kwarg-fns` below, and it preserves every comment, blank
;; line and multi-line layout inside the wrapped region byte-for-byte. A
;; regex over the raw text cannot make that guarantee once values are
;; arbitrary nested forms (`:color (nth row-colors r)`, multi-line maps, ...).
;;
;; Usage: bb scripts/kwarg_calls_to_maps.clj [--fix] [--strict]
;;        (no flags)  report non-conforming call sites, exit 0
;;        --strict    same report, exit non-zero if any are found
;;        --fix       rewrite files in place; report what changed, exit 0
;;
;; Re-run with no flags (or --strict, e.g. in a pre-commit hook) any time
;; after this to confirm no new flat-kwarg call sites crept back in.

(ns kwarg-calls-to-maps
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [rewrite-clj.node :as n]
            [rewrite-clj.zip :as z]))

;; ============================================================================
;; Configuration
;; ============================================================================

;; fn-name (no namespace) -> number of leading positional args before the
;; kwargs, e.g. `text!`'s `[s & {:keys [...]}]` has one (`s`). Matched by
;; bare name, so both `rl/window!` and an unaliased `window!` are caught.
(def kwarg-fns
  {"window!"          0
   "text!"            1
   "fps!"             0
   "rect!"            0
   "rect-lines!"      0
   "rect-gradient!"   0
   "circle!"          0
   "circle-lines!"    0
   "ellipse!"         0
   "line!"            0
   "pixel!"           0
   "sector!"          0
   "ring!"            0
   "line-ex!"         0
   "circle-gradient!" 0
   "rect-pro!"        0
   "rect-gradient-h!" 0
   "texture!"         1
   "cube!"            0
   "sphere!"          0})

;; "src" is the example suite, "lib/src" is the binding library where the
;; kwargs definitions themselves now live. Without lib/src this checker finds
;; no kwargs definitions at all and reports zero violations from a clean exit,
;; which looks exactly like success. "lib/test" replaces "test" once P5.T3
;; moves the unit suite; listing a directory that does not exist yet is
;; harmless, since the glob simply matches nothing.
(def source-dirs ["src" "lib/src" "lib/test"])

(def file-pattern
  "\"**\" requires at least one directory level, so it alone would miss files
  sitting directly in source-dirs; \"{*,**/*}.clj\" matches both."
  "{*,**/*}.clj")

;; ============================================================================
;; Structural walk
;; ============================================================================

(defn- significant?
  [node]
  (not (n/whitespace-or-comment? node)))

(defn- nth-significant-index
  "Index into `children` of the k-th significant (non-whitespace/comment)
  node, 0-based, or nil if there are fewer than k+1."
  [children k]
  (->> children
       (map-indexed vector)
       (filter (fn [[_ node]] (significant? node)))
       (map first)
       (drop k)
       first))

(defn- call-head-name
  "The bare (namespace-stripped) name of a list's head symbol, or nil."
  [zloc]
  (let [head (z/down zloc)]
    (when (and head (= :token (z/tag head)))
      (let [v (z/sexpr head)]
        (when (symbol? v) (name v))))))

(defn- kwarg-call?
  [zloc]
  (and (= :list (z/tag zloc))
       (contains? kwarg-fns (call-head-name zloc))))

(defn- tail-children
  "The raw children of the call's argument list (including whitespace/comment
  nodes) that come after the fn symbol and its `n-pos` positional args, or nil
  if the call doesn't even have that many args."
  [node n-pos]
  (let [children (vec (n/children node))]
    (when-let [boundary (nth-significant-index children n-pos)]
      (let [tail-start (inc boundary)]
        (when (<= tail-start (count children))
          (subvec children tail-start (count children)))))))

(defn- sig-indexes
  [children]
  (keep-indexed (fn [i node] (when (significant? node) i)) children))

;; nil return means "leave this call alone" -- either it already uses an
;; explicit map, it has no trailing args at all, or its trailing args aren't a
;; clean flat run of keyword/value pairs (a shape worth a human look, not a
;; silent skip: `report!` records why).
(defn- flat-kwarg-tail
  [tail report!]
  (let [sigs (vec (sig-indexes tail))]
    (cond
      (empty? sigs) nil

      (and (= 1 (count sigs))
           (= :map (n/tag (nth tail (first sigs)))))
      nil ;; already `{...}` -- nothing to do

      (odd? (count sigs))
      (do (report! "odd number of trailing args (not plain key/value pairs)")
          nil)

      (not (every? (fn [i]
                     (let [node (nth tail (nth sigs i))]
                       (and (= :token (n/tag node)) (keyword? (n/sexpr node)))))
                   (range 0 (count sigs) 2)))
      (do (report! "a key position isn't a literal keyword")
          nil)

      :else
      {:first-sig (first sigs)
       :last-sig (last sigs)})))

(defn- wrap-in-map
  [node n-pos report!]
  (let [children (vec (n/children node))]
    (when-let [tail (tail-children node n-pos)]
      (when-let [{:keys [first-sig last-sig]} (flat-kwarg-tail tail report!)]
        (let [tail-start   (- (count children) (count tail))
              leading-ws   (subvec tail 0 first-sig)
              map-body     (subvec tail first-sig (inc last-sig))
              trailing-ws  (subvec tail (inc last-sig) (count tail))
              new-tail     (vec (concat leading-ws [(n/map-node map-body)] trailing-ws))
              new-children (vec (concat (subvec children 0 tail-start) new-tail))]
          (n/replace-children node new-children))))))

(defn fix-string
  "Rewrite every matching call site in `content`. Returns [new-content
  findings], where findings are {:line :col :fn :reason} maps for call sites
  the walk left untouched because their shape needed a human, not a script."
  [content]
  (let [findings (atom [])
        report!  (fn [zloc reason]
                   (let [[row col] (z/position zloc)]
                     (swap! findings conj {:line row
                                           :col col
                                           :fn (call-head-name zloc)
                                           :reason reason})))
        zloc     (z/of-string* content {:track-position? true})
        zloc'    (z/prewalk zloc kwarg-call?
                            (fn [loc]
                              (let [n-pos (get kwarg-fns (call-head-name loc))
                                    new-node (wrap-in-map (z/node loc) n-pos
                                                          (fn [reason] (report! loc reason)))]
                                (when new-node (z/replace loc new-node)))))]
    [(z/root-string zloc') @findings]))

;; ============================================================================
;; Report-only walk (no file mutation) -- used when `--fix` is not passed.
;; ============================================================================

(defn find-call-sites
  "Every flat-kwarg call site in `content`, as {:line :col :fn} maps."
  [content]
  (let [sites (atom [])
        zloc  (z/of-string* content {:track-position? true})]
    (z/prewalk zloc kwarg-call?
               (fn [loc]
                 (let [n-pos (get kwarg-fns (call-head-name loc))
                       tail  (tail-children (z/node loc) n-pos)]
                   (when (and tail (flat-kwarg-tail tail (fn [_])))
                     (let [[row col] (z/position loc)]
                       (swap! sites conj {:line row
                                          :col col
                                          :fn (call-head-name loc)}))))
                 nil)) ;; report-only: never edit
    @sites))

;; ============================================================================
;; File plumbing
;; ============================================================================

(defn- find-all-files
  []
  (->> source-dirs
       (mapcat #(fs/glob % file-pattern))
       (map str)
       sort))

(defn- rel
  [path]
  (str/replace path #"^.*?/(src|test)/" "$1/"))

(defn run-check
  [strict?]
  (println)
  (println "Checking for flat keyword-argument calls to raylib drawing fns...")
  (println "  (these should pass an explicit {} map instead)")
  (println)
  (let [results (for [f (find-all-files)
                      :let [sites (find-call-sites (slurp f))]
                      :when (seq sites)]
                  {:file f
                   :sites sites})]
    (if (empty? results)
      (do (println "No flat keyword-argument call sites found.")
          0)
      (do
        (doseq [{:keys [file sites]} results]
          (println (str "\n" (rel file)))
          (doseq [{:keys [line col fn]} sites]
            (println (format "  %d:%d  (%s ...)" line col fn))))
        (println)
        (println (format "%d call site(s) in %d file(s)"
                         (reduce + 0 (map (comp count :sites) results))
                         (count results)))
        (println)
        (println "Fix with: bb fix:kwarg-calls")
        (if strict? 1 0)))))

(defn run-fix
  []
  (println)
  (println "Rewriting flat keyword-argument calls into explicit maps...")
  (println)
  (let [changed  (atom [])
        warnings (atom [])]
    (doseq [f (find-all-files)]
      (let [content        (slurp f)
            [new findings] (fix-string content)]
        (when (seq findings)
          (swap! warnings conj {:file f
                                :findings findings}))
        (when (not= content new)
          (spit f new)
          (swap! changed conj f))))
    (doseq [f @changed]
      (println "  fixed:" (rel f)))
    (when (seq @warnings)
      (println)
      (println "Left untouched (needs a human look):")
      (doseq [{:keys [file findings]} @warnings]
        (println (str "\n" (rel file)))
        (doseq [{:keys [line col fn reason]} findings]
          (println (format "  %d:%d  (%s ...) -- %s" line col fn reason)))))
    (println)
    (println (format "%d file(s) changed, %d call site(s) flagged for review"
                     (count @changed)
                     (reduce + 0 (map (comp count :findings) @warnings))))
    0))

;; ============================================================================
;; Main
;; ============================================================================

(defn -main
  [& args]
  (let [fix?    (some #{"--fix"} args)
        strict? (some #{"--strict"} args)
        exit-code (if fix? (run-fix) (run-check strict?))]
    (System/exit exit-code)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
