(ns net.b12n.raylib-jlt.input-actions
  "raylib [core] example - input actions.

  Decodes input as abstract ACTIONS instead of raw keys, so a binding maps
  to a key AND a gamepad button at once and can be remapped freely. Move the
  square with the movement action (WASD by default), SPACE fires (recentres
  + flashes blue for one frame), TAB swaps to the arrow-key set. No new
  bindings: the d-pad is already PAD-UP/DOWN/LEFT/RIGHT and the face buttons
  are already PAD-Y/A/X/B (raylib's LEFT_FACE_* / RIGHT_FACE_* under
  different names, added for input-gamepad.clj), plus one small addition,
  gamepad-released?, mirroring gamepad-down?/gamepad-pressed? exactly.
  Ported from raylib's examples/core/core_input_actions.c."
  (:require
   [net.b12n.raylib-jlt.raylib :as rl]))

(def ^:const W 800)
(def ^:const H 450)
(def ^:const SIZE 40.0)
(def ^:const PAD 0)

;; WASD + the d-pad (raylib's LEFT_FACE_*)
(def ^:private default-keys
  {:up {:key rl/KEY-W
        :button rl/PAD-UP}
   :down {:key rl/KEY-S
          :button rl/PAD-DOWN}
   :left {:key rl/KEY-A
          :button rl/PAD-LEFT}
   :right {:key rl/KEY-D
           :button rl/PAD-RIGHT}
   :fire {:key rl/KEY-SPACE
          :button rl/PAD-B}})

;; arrow keys + the face buttons (raylib's RIGHT_FACE_*)
(def ^:private cursor-keys
  {:up {:key rl/KEY-UP
        :button rl/PAD-Y}
   :down {:key rl/KEY-DOWN
          :button rl/PAD-B}
   :left {:key rl/KEY-LEFT
          :button rl/PAD-X}
   :right {:key rl/KEY-RIGHT
           :button rl/PAD-A}
   :fire {:key rl/KEY-SPACE
          :button rl/PAD-DOWN}})

(defn- action-down?
  [keyset action]
  (let [{:keys [key button]} (get keyset action)]
    (or (rl/key-down? key) (rl/gamepad-down? PAD button))))

(defn- action-pressed?
  [keyset action]
  (let [{:keys [key button]} (get keyset action)]
    (or (rl/key-pressed? key) (rl/gamepad-pressed? PAD button))))

(defn- action-released?
  [keyset action]
  (let [{:keys [key button]} (get keyset action)]
    (or (rl/key-released? key) (rl/gamepad-released? PAD button))))

(defn -main
  [& _]
  (rl/window! {:width W
               :height H
               :title "raylib [core] example - input actions"})
  (rl/set-target-fps 60)
  (let [deadline (rl/auto-quit-deadline)]
    (loop [frame 0 px 400.0 py 200.0 default? true]
      (when (rl/keep-running? deadline)
        (let [ks (if default? default-keys cursor-keys)
              px (cond
                   (action-down? ks :left) (- px 2)
                   (action-down? ks :right) (+ px 2)
                   :else px)
              py (cond
                   (action-down? ks :up) (- py 2)
                   (action-down? ks :down) (+ py 2)
                   :else py)
              fire? (action-pressed? ks :fire)
              px (if fire? (/ (- W SIZE) 2) px)
              py (if fire? (/ (- H SIZE) 2) py)
              release? (action-released? ks :fire)
              default? (if (rl/key-pressed? rl/KEY-TAB) (not default?) default?)]
          (rl/begin-drawing)
          (rl/clear-background rl/GRAY)
          (rl/rect! {:x (int px)
                     :y (int py)
                     :width (int SIZE)
                     :height (int SIZE)
                     :color (if release? rl/BLUE rl/RED)})
          (rl/text! (if default? "Current input set: WASD (default)" "Current input set: Arrow keys")
                    {:x 10
                     :y 10
                     :size 20
                     :color rl/WHITE})
          (rl/text! "Use TAB key to toggle the Actions keyset" {:x 10
                                                                :y 50
                                                                :size 20
                                                                :color rl/GREEN})
          (rl/maybe-screenshot! frame 5)
          (rl/end-drawing)
          (recur (inc frame) px py default?)))))
  (rl/close-window))

;; To run this from your editor
(comment
  (rl/run! -main)
  nil)
