(ns hooks.jolt-ffi
  "clj-kondo hook for jolt.ffi/defcfn.

  `defcfn` binds a C symbol to a Clojure var:

      (ffi/defcfn draw-text \"DrawText\" [:string :int :int :int :uint] :void)

  clj-kondo cannot see through the macro, so without this hook every bound name
  is an `Unresolved symbol` inside its own library module and an
  `Unresolved var: rl/…` at each of the ~460 call sites in the examples,
  enough noise to make the linter useless as a gate.

  The hook rewrites the form into a `defn` of the same name whose parameter count
  matches the C argument-type vector, and whose body is a literal of the declared
  C return type. That buys three things clj-kondo could not otherwise know:

    * the var exists (kills the false positives),
    * its arity, passing the wrong number of arguments to a binding is exactly
      the FFI mistake that otherwise surfaces only as a native crash,
    * its return type, so `(+ 1 (rl/measure-text …))` type-checks.

  Two shapes carry an argument the C signature does not show, and both are
  handled below or the arity check reports a false positive:

    * a `[:by-value [:struct …]]` RETURN. jolt writes the struct through a
      caller-supplied destination pointer passed as the FIRST argument, so the
      Clojure arity is one MORE than the argtype vector.
    * a `:varargs` marker inside the argtype vector. It is a boundary marker,
      not a parameter, so the arity is one LESS than the vector's length.

  Return-type mapping is deliberately conservative: numeric C types become a
  number, `:string` a string, and everything else (`:void`, `:pointer`,
  an aggregate) nil.
  `:pointer` is an opaque handle that only ever gets passed back into other
  `ffi/*` calls, which are themselves untyped here, so calling it nil costs
  nothing today. If a pointer ever flows somewhere type-checked, widen this map
  rather than deleting the hook."
  (:require [clj-kondo.hooks-api :as api]))

(def ^:private numeric-ret
  #{:int :uint :long :ulong :short :ushort :byte :ubyte :float :double :size-t})

(defn- ret-node
  "A literal whose inferred type matches the declared C return type."
  [ret]
  (let [k (when ret (api/sexpr ret))]
    (cond
      (contains? numeric-ret k) (api/token-node 0)
      (= :string k)             (api/string-node "")
      :else                     (api/token-node nil))))

(defn- aggregate?
  "A [:by-value [:struct …]] type node."
  [n]
  (and n (api/vector-node? n)
       (= :by-value (some-> n :children first api/sexpr))))

(defn- varargs-marker?
  [n]
  (= :varargs (api/sexpr n)))

(defn defcfn
  [{:keys [node]}]
  (let [[_defcfn name-node _c-symbol arg-types ret] (:children node)]
    ;; Only rewrite the shape we understand; anything else falls through to the
    ;; default analysis rather than silently interning a wrong var.
    (if (and name-node arg-types (api/vector-node? arg-types))
      (let [;; :varargs marks the fixed/variadic boundary and is not itself a
            ;; parameter, so it does not count toward the arity.
            arg-count (count (remove varargs-marker? (:children arg-types)))
            ;; An aggregate return is written through a destination pointer that
            ;; jolt takes as the first argument, so the callable is one wider
            ;; than the C signature reads.
            n (cond-> arg-count (aggregate? ret) inc)
            params (map (fn [i] (api/token-node (symbol (str "_arg" i)))) (range n))
            expanded (api/list-node
                      [(api/token-node 'clojure.core/defn)
                       name-node
                       (api/vector-node (vec params))
                       (ret-node ret)])]
        {:node (with-meta expanded (meta node))})
      {:node node})))
