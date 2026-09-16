(ns net.b12n.raylib-jlt.undo-redo
  "raylib [core] example - undo redo (`jolt -M:undo-redo`).

  Port of raylib's examples/core/core_undo_redo.c. Drive a square around a grid
  with the arrows, recolour it with SPACE, and step through the history with
  CTRL-Z and CTRL-Y. Visited cells stay on screen as a trail, and the strip along
  the bottom shows the history itself: how many states are held, and where the
  cursor sits inside them.

  The C keeps a 26-slot ring buffer of PlayerState and moves three indices around
  it. That does not port straight across, because the interesting part is the
  bounded history rather than the pointer arithmetic, and Clojure already has a
  better shape for it: a vector of states plus a cursor. Recording past the cap
  drops the oldest entry, which is what the ring achieves by overwriting.

  The one behaviour worth preserving exactly is that a new move after an undo
  discards whatever was ahead. The C gets that by assigning lastUndoIndex after
  it writes, so the redo tail becomes unreachable; here the tail is dropped
  outright."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)

(def ^:const MAX-STATES 26)
(def ^:const CELL 24)
(def ^:const CELLS-X 30)
(def ^:const CELLS-Y 13)
(def ^:const GRID-X 40)
(def ^:const GRID-Y 60)

;; Cycled by SPACE. raylib's example flips between a small set rather than
;; picking at random, so the trail stays readable.
(def ^:private palette
  [[230 41 55 255]     ; RED
   [0 121 241 255]     ; BLUE
   [0 158 47 255]      ; LIME
   [255 161 0 255]     ; ORANGE
   [200 122 255 255]]) ; PURPLE

(defn- clamp [v lo hi] (max lo (min hi v)))

(defn- record
  "Appends state to the history at the cursor, dropping anything ahead of it and
  the oldest entry once the cap is reached. Returns [history cursor]."
  [history cursor state]
  (let [kept (conj (subvec history 0 (inc cursor)) state)]
    (if (> (count kept) MAX-STATES)
      [(subvec kept 1) (dec (count kept))]
      [kept (dec (count kept))])))

(defn- draw-history!
  "The strip along the bottom: one slot per held state, the cursor filled in."
  [x y history cursor]
  (let [slot 12]
    (rl/text! "HISTORY" {:x 40
                         :y (- y 4)
                         :size 10
                         :color rl/GRAY})
    (dotimes [i (count history)]
      (let [sx (+ x (* i slot))]
        (if (= i cursor)
          (rl/rect! {:x sx
                     :y y
                     :width (- slot 2)
                     :height 16
                     :color rl/DARKGRAY})
          (rl/rect-lines! {:x sx
                           :y y
                           :width (- slot 2)
                           :height 16
                           :color rl/LIGHTGRAY}))))
    (rl/text! (str (inc cursor) " / " (count history))
              {:x (+ x (* MAX-STATES slot) 12)
               :y y
               :size 14
               :color rl/GRAY})))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - undo redo"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)
        start    {:x 10
                  :y 10
                  :color 0}]
    (loop [frame   0
           player  start
           history [start]
           cursor  0
           ticks   0]
      (when (rl/keep-running? deadline)
        (let [ctrl?   (rl/key-down? rl/KEY-LEFT-CONTROL)
              undo?   (and ctrl? (rl/key-pressed? rl/KEY-Z))
              redo?   (and ctrl? (rl/key-pressed? rl/KEY-Y))
              ;; Arrows and SPACE only act when CTRL is up, so CTRL-Z does not
              ;; also nudge the player.
              moved   (cond-> player
                        (and (not ctrl?) (rl/key-pressed? rl/KEY-RIGHT)) (update :x inc)
                        (and (not ctrl?) (rl/key-pressed? rl/KEY-LEFT))  (update :x dec)
                        (and (not ctrl?) (rl/key-pressed? rl/KEY-UP))    (update :y dec)
                        (and (not ctrl?) (rl/key-pressed? rl/KEY-DOWN))  (update :y inc)
                        (and (not ctrl?) (rl/key-pressed? rl/KEY-SPACE))
                        (update :color (fn [c] (mod (inc c) (count palette)))))
              moved   (-> moved
                          (update :x clamp 0 (dec CELLS-X))
                          (update :y clamp 0 (dec CELLS-Y)))
              ;; The C samples every second frame rather than every frame, so a
              ;; held arrow key does not fill the history with duplicates.
              ticks   (inc ticks)
              sample? (>= ticks 2)
              changed? (and sample? (not= moved (nth history cursor)))
              [history cursor] (if changed?
                                 (record history cursor moved)
                                 [history cursor])
              ticks   (if sample? 0 ticks)
              ;; Undo and redo move the cursor and adopt whatever sits there.
              cursor  (cond
                        (and undo? (pos? cursor))                    (dec cursor)
                        (and redo? (< cursor (dec (count history)))) (inc cursor)
                        :else                                        cursor)
              player  (if (or undo? redo?) (nth history cursor) moved)]
          (rl/begin-drawing)
          (rl/clear-background rl/RAYWHITE)
          (rl/text! "[ARROWS] MOVE - [SPACE] COLOR - [CTRL+Z] UNDO - [CTRL+Y] REDO"
                    {:x 40
                     :y 20
                     :size 20
                     :color rl/DARKGRAY})
          ;; The trail: every state up to the cursor, so an undo visibly shortens it.
          (dotimes [i (inc cursor)]
            (let [{:keys [x y]} (nth history i)]
              (rl/rect! {:x (+ GRID-X (* x CELL))
                         :y (+ GRID-Y (* y CELL))
                         :width CELL
                         :height CELL
                         :color rl/LIGHTGRAY})))
          ;; Grid lines over the trail, so the cells stay legible.
          (dotimes [i (inc CELLS-X)]
            (rl/line! {:x1 (+ GRID-X (* i CELL))
                       :y1 GRID-Y
                       :x2 (+ GRID-X (* i CELL))
                       :y2 (+ GRID-Y (* CELLS-Y CELL))
                       :color (rl/rgba 220 220 220 255)}))
          (dotimes [i (inc CELLS-Y)]
            (rl/line! {:x1 GRID-X
                       :y1 (+ GRID-Y (* i CELL))
                       :x2 (+ GRID-X (* CELLS-X CELL))
                       :y2 (+ GRID-Y (* i CELL))
                       :color (rl/rgba 220 220 220 255)}))
          (let [{:keys [x y color]} player
                [r g b a] (nth palette color)]
            (rl/rect! {:x (+ GRID-X (* x CELL))
                       :y (+ GRID-Y (* y CELL))
                       :width CELL
                       :height CELL
                       :color (rl/rgba r g b a)}))
          (draw-history! 110 400 history cursor)
          (rl/maybe-screenshot! frame 10)
          (rl/end-drawing)
          (recur (inc frame) player history cursor ticks)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
