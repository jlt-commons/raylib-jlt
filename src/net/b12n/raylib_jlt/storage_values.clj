(ns net.b12n.raylib-jlt.storage-values
  "raylib [core] example - storage save/load values (`jolt -M:storage-values`).

  Port of raylib's examples/core/core_storage_values.c. R rolls a new score and
  hi-score, ENTER writes them to disk, SPACE reads them back. Quit and run it
  again to see the values survive.

  raylib's SaveStorageValue and LoadStorageValue treat a file as an array of
  ints addressed by position, growing it as needed. Neither is bound, and neither
  needs to be: the same contract is a few lines over an EDN map, which is also
  inspectable, so `cat storage.edn` shows what was saved instead of a wall of
  bytes.

  The failure modes are the interesting part, and both are handled the way the C
  handles them. Loading before anything was ever saved returns zero rather than
  an error, because a missing file is a legitimate first run. A corrupt or
  unreadable file does the same instead of taking the example down, since a
  demonstration of persistence should not crash on a bad file.

  storage.edn is written to the working directory, matching where raylib puts
  storage.data. It is gitignored."
  (:require
   [clojure.edn :as edn]
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const STORAGE-FILE "storage.edn")

(defn- load-values
  "The saved map, or an empty one. A missing file is an ordinary first run, and
  an unreadable one is not worth crashing over."
  []
  (try
    (if (.exists (java.io.File. STORAGE-FILE))
      (or (edn/read-string (slurp STORAGE-FILE)) {})
      {})
    (catch Exception _ {})))

(defn- save-values!
  "Returns a message for the screen rather than throwing, so a read-only
  directory reports itself instead of ending the run."
  [m]
  (try
    (spit STORAGE-FILE (pr-str m))
    (str "saved to " STORAGE-FILE)
    (catch Exception e
      (str "save failed: " (.getMessage e)))))

(defn -main
  [& _]
  (rl/window! :width W :height H :title "raylib [core] example - storage save/load values")
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0
           score 0
           hiscore 0
           status "press R to roll, ENTER to save, SPACE to load"]
      (when (rl/keep-running? deadline)
        (let [roll?  (rl/key-pressed? rl/KEY-R)
              save?  (rl/key-pressed? rl/KEY-ENTER)
              load?  (rl/key-pressed? rl/KEY-SPACE)
              score   (if roll? (rl/get-random-value 1000 2000) score)
              hiscore (if roll? (rl/get-random-value 2000 4000) hiscore)
              loaded  (when load? (load-values))
              score   (if load? (get loaded :score 0) score)
              hiscore (if load? (get loaded :hiscore 0) hiscore)
              status  (cond
                        roll? "rolled new values, not saved yet"
                        save? (save-values! {:score score
                                             :hiscore hiscore})
                        load? (str "loaded from " STORAGE-FILE)
                        :else status)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! (str "SCORE: " score) :x 280 :y 130 :size 40 :color rl/MAROON)
          (rl/text! (str "HI-SCORE: " hiscore) :x 210 :y 200 :size 50 :color rl/BLACK)
          (rl/text! "[R] roll  -  [ENTER] save  -  [SPACE] load"
                    :x 250 :y 300 :size 20 :color rl/GRAY)
          (rl/text! status :x 250 :y 330 :size 20 :color rl/DARKGRAY)
          (rl/text! "values survive a restart" :x 250 :y 360 :size 20 :color rl/LIGHTGRAY)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) score hiscore status)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
