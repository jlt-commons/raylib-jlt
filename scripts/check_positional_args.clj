#!/usr/bin/env bb
;; Find functions with 3+ positional arguments.
;; A leading 1-2 "subject" args before a {:keys [...]} map are allowed 
;; only genuinely-positional 3+ arg lists are flagged.
;;
;; Usage: bb scripts/check_positional_args.clj [--strict]
;;        --strict: exit non-zero if any functions are found
;;
;; Adapted from glitter's scripts/check_positional_args.clj, same script,
;; source-dirs pointed at this project's flat src/net/b12n/raylib_jlt layout.

(ns check-positional-args
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

;; ============================================================================
;; Configuration
;; ============================================================================

(def min-positional-args
  "Minimum number of positional arguments to flag"
  3)

(def source-dirs
  "Directories to scan"
  ;; lib/src holds the binding library now that the extraction arc has moved
  ;; every module (kwargs included) out of src; without it this gate would
  ;; stop seeing the functions it is meant to check.
  ["src" "lib/src"])

(def file-pattern
  "Glob pattern for files to check. \"**\" requires at least one directory
  level, so it alone would miss files sitting directly in source-dirs;
  \"{*,**/*}.clj\" matches both flat and nested files."
  "{*,**/*}.clj")

;; Known exceptions - functions that intentionally use positional args
;; (e.g., protocol implementations, interop, or mirroring a raylib C
;; function's exact positional signature).
(def exceptions
  #{})

;; ============================================================================
;; Parsing Logic
;; ============================================================================

(defn extract-defn-forms
  "Extract all defn/defn- forms from file content with line numbers"
  [content]
  (let [lines (str/split-lines content)]
    (->> (map-indexed vector lines)
         (keep (fn [[idx line]]
                 (when-let [[_ form-type fn-name] (re-find #"^\(defn(-?)?\s+([a-zA-Z_<>!?*-][a-zA-Z0-9_<>!?*-]*)" line)]
                   {:line-num (inc idx)
                    :fn-name fn-name
                    :is-private? (= form-type "-")
                    :line-content line})))
         vec)))

(defn find-arg-vector
  "Find the argument vector for a defn, handling multi-line definitions.
   Properly skips over docstrings to find the actual argument vector."
  [content start-line]
  (let [lines (str/split-lines content)
        ;; Look at the start line and subsequent lines (increased to handle longer docstrings)
        search-lines (->> lines
                          (drop (dec start-line))
                          (take 50))
        ;; Join lines for parsing
        search-text (str/join "\n" search-lines)
        ;; First, remove the defn/defn- fn-name part
        after-name (str/replace-first search-text #"^\(defn-?\s+[a-zA-Z_<>!?*-][a-zA-Z0-9_<>!?*-]*\s*" "")
        ;; Check if there's a docstring (starts with ")
        trimmed (str/trim after-name)
        has-docstring? (str/starts-with? trimmed "\"")
        ;; If docstring exists, skip it completely
        ;; Match the docstring opening quote, everything until closing quote, then find the [
        after-docstring (if has-docstring?
                          ;; Match everything from first " to the last " before a [
                          ;; Look for pattern: "...(any content including newlines)..."  [args]
                          (if-let [[_ rest] (re-find #"(?s)\"(?:[^\"\\]|\\.)*\"\s*(\[.*)" trimmed)]
                            rest
                            ;; Fallback: if multiline docstring pattern fails, skip lines starting with "
                            after-name)
                          after-name)
        ;; Also handle metadata like ^:private
        cleaned (str/replace after-docstring #"^\^[^\[]*" "")]
    ;; Find first [ ... ] that's the argument vector
    (when-let [[_ args] (re-find #"\[(.*?)\]" cleaned)]
      args)))

(defn parse-arg-vector
  "Parse an argument vector string into individual arguments"
  [arg-str]
  (when arg-str
    (let [;; Remove comments
          cleaned (str/replace arg-str #";.*" "")
          ;; Split on whitespace, handling nested structures
          tokens (re-seq #"[^\s\[\]\{\}]+" cleaned)]
      tokens)))

(defn uses-keyword-destructuring?
  "Check if the arg vector uses {:keys [...]} destructuring"
  [arg-str]
  (when arg-str
    (boolean (re-find #"\{:keys" arg-str))))

(defn count-positional-args
  "Count the number of positional arguments (excluding destructured maps)"
  [arg-str]
  (if (uses-keyword-destructuring? arg-str)
    ;; If using {:keys}, only count args before it
    (let [before-keys (first (str/split arg-str #"\{:keys"))]
      (count (parse-arg-vector before-keys)))
    ;; Otherwise count all args
    (count (parse-arg-vector arg-str))))

(defn analyze-function
  "Analyze a single function definition"
  [content {:keys [line-num fn-name is-private?]}]
  (let [arg-str (find-arg-vector content line-num)
        positional-count (count-positional-args arg-str)
        uses-keywords? (uses-keyword-destructuring? arg-str)]
    {:fn-name fn-name
     :line-num line-num
     :is-private? is-private?
     :arg-str arg-str
     :positional-count positional-count
     :uses-keywords? uses-keywords?
     :needs-refactor? (and (>= positional-count min-positional-args)
                           (not uses-keywords?)
                           (not (contains? exceptions fn-name)))}))

(defn analyze-file
  "Analyze a single file for functions with too many positional args"
  [file-path]
  (let [content (slurp (str file-path))
        defn-forms (extract-defn-forms content)
        analyses (map #(analyze-function content %) defn-forms)
        flagged (filter :needs-refactor? analyses)]
    (when (seq flagged)
      {:file (str file-path)
       :functions flagged})))

;; ============================================================================
;; Reporting
;; ============================================================================

(defn format-function-report
  "Format a single function finding"
  [{:keys [fn-name line-num arg-str positional-count is-private?]}]
  (let [visibility (if is-private? "private " "")]
    (format "  Line %d: %s%s [%d args]\n           Args: [%s]"
            line-num visibility fn-name positional-count
            (str/trim (or arg-str "?")))))

(defn format-file-report
  "Format findings for a single file"
  [{:keys [file functions]}]
  (let [rel-path (str/replace file #"^.*?src/" "src/")]
    (str "\n📄 " rel-path "\n"
         (str/join "\n" (map format-function-report functions)))))

(defn print-summary
  "Print summary statistics"
  [results]
  (let [total-functions (reduce + 0 (map #(count (:functions %)) results))
        total-files (count results)]
    (println "\n" (str/join "" (repeat 60 "━")))
    (println (format "📊 Summary: %d function(s) in %d file(s) have 3+ positional args"
                     total-functions total-files))
    (println (str/join "" (repeat 60 "━")))
    (println)
    (println "💡 Recommendation: Convert to keyword arguments, e.g.:")
    (println "   Before: (defn foo [client model prompt] ...)")
    (println "   After:  (defn foo [{:keys [client model prompt]}] ...)")
    (println)
    (println "   Or with required subject + options map:")
    (println "   (defn foo [client {:keys [model prompt]}] ...)")))

;; ============================================================================
;; Main
;; ============================================================================

(defn find-all-files
  "Find all Clojure files to check"
  []
  (->> source-dirs
       (mapcat #(fs/glob % file-pattern))
       (map str)
       sort))

(defn run-check
  "Run the positional arguments check"
  [strict?]
  (println)
  (println "🔍 Checking for functions with 3+ positional arguments...")
  (println "   (These should use keyword argument maps instead)")
  (println)

  (let [files (find-all-files)
        results (->> files
                     (keep analyze-file)
                     (sort-by :file))]
    (if (empty? results)
      (do
        (println "✅ No functions found with 3+ positional arguments!")
        0)
      (do
        (doseq [result results]
          (println (format-file-report result)))
        (print-summary results)
        (if strict? 1 0)))))

(defn -main [& args]
  (let [strict? (some #{"--strict"} args)
        exit-code (run-check strict?)]
    (System/exit exit-code)))

;; Run if executed directly
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
