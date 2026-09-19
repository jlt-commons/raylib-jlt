(ns net.b12n.raylib.files
  "File-system helpers: dropped-file and directory listings via raylib's
  FilePathList, plus the working-directory and filename lookups."
  (:require
   [jolt.ffi :as ffi]))

;; --- files: FilePathList, another 16-byte struct returned by value -------
;; FilePathList is {unsigned int count; char **paths;}, the same shape as Shader
;; and so the same binding: 16 bytes, returned by value, handed straight back to
;; its Unload by value. What is new is the char** on the other side of it. raylib
;; owns that array and every string in it until the matching Unload runs, so the
;; helpers below copy the strings out into a Clojure vector and unload inside the
;; same call. Nothing a caller holds points into raylib's memory afterwards.
(def ^:private file-path-list-layout
  (ffi/layout [:struct [[:count :uint] [:paths :pointer]]]))

(ffi/defcfn ^:private file-dropped-raw "IsFileDropped" [] :int)
(ffi/defcfn ^:private load-dropped-files-raw "LoadDroppedFiles" []
  [:by-value [:struct [[:count :uint] [:paths :pointer]]]])
(ffi/defcfn ^:private unload-dropped-files-raw "UnloadDroppedFiles"
  [[:by-value [:struct [[:count :uint] [:paths :pointer]]]]] :void)
(ffi/defcfn ^:private load-directory-files-ex-raw "LoadDirectoryFilesEx"
  [:string :string :bool]
  [:by-value [:struct [[:count :uint] [:paths :pointer]]]])
(ffi/defcfn ^:private unload-directory-files-raw "UnloadDirectoryFiles"
  [[:by-value [:struct [[:count :uint] [:paths :pointer]]]]] :void)
(ffi/defcfn ^:private directory-exists-raw "DirectoryExists" [:string] :int)
(ffi/defcfn get-working-directory  "GetWorkingDirectory"  [] :string)
(ffi/defcfn get-prev-directory-path "GetPrevDirectoryPath" [:string] :string)
(ffi/defcfn get-file-name          "GetFileName"          [:string] :string)

(defn- file-path-list->vec
  "Copy the char** behind a filled FilePathList buffer into a vector of strings."
  [out]
  (let [n (ffi/read-field out file-path-list-layout :count)
        base (ffi/read-field out file-path-list-layout :paths)
        step (ffi/sizeof :pointer)]
    (mapv (fn [i] (ffi/ptr->string (ffi/read base :pointer (* i step))))
          (range n))))

(defn file-dropped?
  "IsFileDropped: whether files were dropped on the window since the last check."
  []
  (not (zero? (bit-and (file-dropped-raw) 0xff))))

(defn directory-exists?
  [path]
  (not (zero? (bit-and (directory-exists-raw path) 0xff))))

(defn dropped-files
  "LoadDroppedFiles as a vector of path strings, unloaded before it returns.
  Only meaningful right after file-dropped? answers true."
  []
  (let [out (ffi/alloc (ffi/layout-size file-path-list-layout))]
    (try
      (load-dropped-files-raw out)
      (let [paths (file-path-list->vec out)]
        (unload-dropped-files-raw out)
        paths)
      (finally (ffi/free out)))))

(defn directory-files
  "LoadDirectoryFilesEx as a vector of path strings, unloaded before it returns.
  `scan-subdirs?` recurses.

  `filter` is raylib's own filter string, and its behaviour is worth stating
  because the header only hints at it. Measured against a directory holding 3
  subdirectories and 2 files: \"*.*\" answers all 5, \"DIRS*\" the 3
  directories, \"FILES*\" the 2 files, and an empty string or nil behaves as
  \"FILES*\" rather than as everything. Extensions work too, \".png;.c\" for
  those two, and they combine with the DIRS/FILES forms over a semicolon."
  [dir filter scan-subdirs?]
  (let [out (ffi/alloc (ffi/layout-size file-path-list-layout))]
    (try
      (load-directory-files-ex-raw out dir (or filter "") (boolean scan-subdirs?))
      (let [paths (file-path-list->vec out)]
        (unload-directory-files-raw out)
        paths)
      (finally (ffi/free out)))))
