(ns net.b12n.raylib-jlt.directory-files
  "raylib [core] example - directory files (`jolt -M:directory-files`).

  Port of raylib's examples/core/core_directory_files.c: a file browser that
  starts in the working directory. UP and DOWN move the selection, ENTER or a
  click on a folder goes into it, BACKSPACE goes back up, and the mouse wheel
  scrolls. Directories sort first and are marked; files show their size.

  `rl/directory-files` and `rl/directory-exists?` are new here, though the
  FilePathList binding behind them landed with `drop-files`. The filter passed
  is `\"*.*\"`, which is the one that answers directories AND files; an empty
  filter quietly means files only, which is measured in that function's
  docstring rather than inferred from raylib's header.

  The one real deviation is the whole interface. The C draws its browser with
  raygui, a separate single-header library this suite does not bind: it binds
  the system libraylib and nothing else. So the list, the selection highlight,
  the scrollbar and the path bar are drawn here from `rect!` and `text!`, which
  is a fair trade, since a list view over a vector of strings is not what makes
  the C example interesting.

  Like `drop-files`, this one needs a person. No synthetic input actuates a
  raylib window (see the note in scripts/demo_manifest.edn), so the gallery
  frame shows the opening listing rather than a walk through a tree."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [clojure.string :as str]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const ROW-H 22)
(def ^:const LIST-TOP 70)
(def ^:const VISIBLE 16)

(defn- listing
  "Everything in `dir`, directories first and then files, each sorted by name."
  [dir]
  (->> (rl/directory-files dir "*.*" false)
       (mapv (fn [p] {:path p
                      :name (rl/get-file-name p)
                      :dir? (rl/directory-exists? p)}))
       (sort-by (fn [e] [(if (:dir? e) 0 1) (str/lower-case (:name e))]))
       vec))

(defn- clamp
  [x lo hi]
  (-> x (max lo) (min hi)))

(defn- keep-visible
  "Scroll just far enough that the selected row is on screen."
  [scroll sel]
  (cond
    (< sel scroll) sel
    (>= sel (+ scroll VISIBLE)) (inc (- sel VISIBLE))
    :else scroll))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - directory files"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)
        start (rl/get-working-directory)]
    (loop [frame 0
           dir start
           entries (listing start)
           sel 0
           scroll 0]
      (when (app/keep-running? deadline)
        (let [n (count entries)
              my (rl/get-mouse-y)
              row (when (and (>= my LIST-TOP) (< my (+ LIST-TOP (* ROW-H VISIBLE))))
                    (+ scroll (quot (- my LIST-TOP) ROW-H)))
              hover (when (and row (< row n)) row)
              sel (cond
                    (rl/key-pressed? rl/KEY-DOWN) (clamp (inc sel) 0 (max 0 (dec n)))
                    (rl/key-pressed? rl/KEY-UP) (clamp (dec sel) 0 (max 0 (dec n)))
                    (and hover (rl/mouse-pressed? rl/MOUSE-LEFT)) hover
                    :else (clamp sel 0 (max 0 (dec n))))
              chosen (get entries sel)
              ;; A click on a folder walks into it; ENTER does the same for the
              ;; keyboard. BACKSPACE walks back up, and stops at the root, where
              ;; GetPrevDirectoryPath answers the path it was given.
              descend? (and chosen (:dir? chosen)
                            (or (rl/key-pressed? rl/KEY-ENTER)
                                (and hover (= hover sel) (rl/mouse-pressed? rl/MOUSE-LEFT))))
              up (when (rl/key-pressed? rl/KEY-BACKSPACE)
                   (rl/get-prev-directory-path dir))
              next-dir (cond
                         descend? (:path chosen)
                         (and up (not= up dir) (rl/directory-exists? up)) up
                         :else dir)
              moved? (not= next-dir dir)
              [dir entries sel scroll] (if moved?
                                         [next-dir (listing next-dir) 0 0]
                                         [dir entries sel
                                          (-> (+ scroll (* -3 (int (rl/get-mouse-wheel))))
                                              (clamp 0 (max 0 (- n VISIBLE)))
                                              (keep-visible sel))])]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/rect! {:x 0
                     :y 0
                     :width W
                     :height 60
                     :color (rl/rgba 230 232 236 255)})
          (rl/text! dir {:x 12
                         :y 14
                         :size 20
                         :color rl/DARKGRAY})
          (rl/text! (str (count entries) " entries")
                    {:x 12
                     :y 40
                     :size 10
                     :color rl/GRAY})
          (doseq [i (range VISIBLE)]
            (let [idx (+ scroll i)
                  e (get entries idx)
                  y (+ LIST-TOP (* ROW-H i))]
              (when e
                (when (= idx sel)
                  (rl/rect! {:x 0
                             :y y
                             :width W
                             :height ROW-H
                             :color (rl/rgba 102 191 255 90)}))
                (when (and hover (= idx hover) (not= idx sel))
                  (rl/rect! {:x 0
                             :y y
                             :width W
                             :height ROW-H
                             :color (rl/rgba 200 200 200 70)}))
                (rl/text! (if (:dir? e) "[dir]" "     ") {:x 12
                                                          :y (+ y 6)
                                                          :size 10
                                                          :color rl/SKYBLUE})
                (rl/text! (:name e) {:x 60
                                     :y (+ y 6)
                                     :size 10
                                     :color (if (:dir? e) rl/DARKBLUE rl/DARKGRAY)}))))
          ;; A scrollbar, because a list that runs past the window with no hint
          ;; of it reads as the whole directory.
          (when (> n VISIBLE)
            (let [track-h (* ROW-H VISIBLE)
                  thumb-h (max 20 (int (* track-h (/ (double VISIBLE) n))))
                  thumb-y (+ LIST-TOP (int (* (- track-h thumb-h)
                                              (/ (double scroll) (max 1 (- n VISIBLE))))))]
              (rl/rect! {:x (- W 8)
                         :y LIST-TOP
                         :width 6
                         :height track-h
                         :color (rl/rgba 220 220 220 255)})
              (rl/rect! {:x (- W 8)
                         :y thumb-y
                         :width 6
                         :height thumb-h
                         :color rl/GRAY})))
          (rl/text! "UP/DOWN select - ENTER or click a folder to enter - BACKSPACE up - wheel scrolls"
                    {:x 12
                     :y (- H 18)
                     :size 10
                     :color rl/GRAY})
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) dir entries sel scroll)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
