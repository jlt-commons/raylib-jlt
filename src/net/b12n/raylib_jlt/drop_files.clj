(ns net.b12n.raylib-jlt.drop-files
  "raylib [core] example - drop files (`jolt -M:drop-files`).

  Port of raylib's examples/core/core_drop_files.c. Drag files or folders onto
  the window and their paths pile up in a striped list. The list keeps growing
  across drops until BACKSPACE clears it.

  New FFI: `IsFileDropped` plus `LoadDroppedFiles`, which returns a
  `FilePathList` by value. That struct is `{unsigned int count; char **paths;}`,
  the same 16-byte shape as `Shader`, so the binding is the one the shader
  section already uses. The `char **` on the other side is what is new here:
  raylib owns that array and every string in it until the matching
  `UnloadDroppedFiles` runs, so `rl/dropped-files` copies the strings into a
  Clojure vector and unloads inside the same call. Nothing a caller holds points
  into raylib's memory afterwards.

  This one cannot record itself. A drop is an operating-system event, not
  something the app polls for, so no synthetic input reaches it and the gallery
  frame shows the empty state. Run it and drag something in."
  (:require
   [net.b12n.raylib-jlt.app :as app]
   [net.b12n.raylib.all :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const ROW-H 24)
(def ^:const MAX-ROWS 13)

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - drop files"})
  (rl/set-target-fps 60)
  (let [deadline (app/auto-quit-deadline)]
    (loop [frame 0
           paths []]
      (when (app/keep-running? deadline)
        (let [paths (cond
                      (rl/file-dropped?) (into paths (rl/dropped-files))
                      (rl/key-pressed? rl/KEY-BACKSPACE) []
                      :else paths)
              ;; Only the tail fits, so a long pile shows its most recent end.
              shown (vec (take-last MAX-ROWS paths))]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (if (empty? paths)
            (rl/text! "drop your files on this window" {:x 100
                                                        :y 40
                                                        :size 20
                                                        :color rl/DARKGRAY})
            (do
              (rl/text! (str "dropped files (" (count paths) ")") {:x 100
                                                                   :y 40
                                                                   :size 20
                                                                   :color rl/DARKGRAY})
              (doseq [[i path] (map-indexed vector shown)]
                (let [y (+ 85 (* ROW-H i))]
                  (rl/rect! {:x 0
                             :y y
                             :width W
                             :height ROW-H
                             :color (rl/rgba 200 200 200 (if (even? i) 128 77))})
                  ;; The full path can outrun the window, so the name leads and
                  ;; the directory it came from follows in a lighter grey.
                  (rl/text! (rl/get-file-name path) {:x 16
                                                     :y (+ y 7)
                                                     :size 10
                                                     :color rl/DARKGRAY})
                  (rl/text! path {:x 220
                                  :y (+ y 7)
                                  :size 10
                                  :color rl/GRAY})))
              (rl/text! "drop more, or BACKSPACE to clear"
                        {:x 100
                         :y (+ 95 (* ROW-H (count shown)))
                         :size 20
                         :color rl/DARKGRAY})))
          (app/maybe-screenshot! frame 20)
          (rl/end-drawing)
          (recur (inc frame) paths)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
