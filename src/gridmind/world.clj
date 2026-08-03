(ns gridmind.world
  "Deterministic grid world model.

  A world is a map that describes a rectangular grid. It stores the start
  position, the goal, the hazards, the walls, and the rewards. The model
  provides deterministic transitions. Wind can push an agent up a fixed
  number of cells per column."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def actions
  "The four movement actions."
  [:up :down :left :right])

(def ^:private deltas
  {:up [-1 0] :down [1 0] :left [0 -1] :right [0 1]})

(defn in-bounds?
  "True when position `pos` lies inside the grid."
  [world [r c]]
  (and (<= 0 r) (< r (:rows world))
       (<= 0 c) (< c (:cols world))))

(defn tile
  "The tile type at `pos`. Returns :empty when nothing is placed there."
  [world pos]
  (get-in world [:tiles pos] :empty))

(defn wall?
  "True when `pos` is a wall."
  [world pos]
  (= :wall (tile world pos)))

(defn goal?
  "True when `pos` is a goal."
  [world pos]
  (= :goal (tile world pos)))

(defn hazard?
  "True when `pos` is a hazard."
  [world pos]
  (= :hazard (tile world pos)))

(defn terminal?
  "True when the episode ends at `pos`."
  [world pos]
  (or (goal? world pos) (hazard? world pos)))

(defn states
  "All positions an agent can occupy. Walls are excluded."
  [world]
  (for [r (range (:rows world))
        c (range (:cols world))
        :let [pos [r c]]
        :when (not (wall? world pos))]
    pos))

(defn empty-world
  "Create a blank world with `rows` rows and `cols` columns.
  The agent starts at `start`."
  ([]
   (empty-world 3 4 [0 0]))
  ([rows cols start]
   {:name "world"
    :rows rows
    :cols cols
    :start start
    :tiles {}
    :wind nil
    :rewards {:goal 1.0 :hazard -1.0 :step -0.01}
    :max-steps 200}))

(defn place
  "Place a tile of type `type` (one of :goal, :hazard, :wall) at `pos`."
  [world pos type]
  (assoc-in world [:tiles pos] type))

(defn place-all
  "Place tiles of type `type` at every position in `positions`."
  [world type positions]
  (reduce #(place %1 %2 type) world positions))

(defn set-rewards
  "Override the reward map. See the rewards of a world for keys."
  [world rewards]
  (assoc world :rewards rewards))

(defn set-wind
  "Set the wind vector. Entry `i` gives the wind strength of column `i`.
  Wind pushes the agent up that many cells after each move."
  [world wind]
  (assoc world :wind wind))

(defn set-name
  "Set the display name of the world."
  [world name]
  (assoc world :name name))

(defn move
  "Move one step from `pos` along `action`.
  A wall or a grid border blocks the move and keeps the agent in place."
  [world [r c] action]
  (let [[dr dc] (deltas action)
        target [(+ r dr) (+ c dc)]
        target (if (in-bounds? world target) target [r c])]
    (if (wall? world target) [r c] target)))

(defn wind-push
  "Apply the wind of the current column to `pos`.
  The wind pushes the agent up and stops at the top border."
  [world [r c]]
  (if-let [w (and (:wind world) (get (:wind world) c))]
    (let [target [(max 0 (- r w)) c]]
      (if (wall? world target) [r c] target))
    [r c]))

(defn step
  "Take `action` from `state`.
  Returns a map with the next state, the reward, and a `done` flag."
  [world state action]
  (let [next (wind-push world (move world state action))
        rewards (:rewards world)
        goal? (goal? world next)
        hazard? (hazard? world next)
        reward (cond goal? (:goal rewards)
                     hazard? (:hazard rewards)
                     :else (:step rewards))]
    {:state next :reward reward :done (or goal? hazard?)}))

(defn follow-policy
  "Walk from the start position by applying `policy` to each visited state.
  `policy` maps a state to an action. Returns the terminal state reached,
  or nil when the walk does not end within a generous step budget."
  [world policy]
  (let [budget (* 20 (:rows world) (:cols world))]
    (loop [s (:start world) steps 0]
      (cond
        (goal? world s) s
        (hazard? world s) s
        (>= steps budget) nil
        :else (recur (:state (step world s (policy s))) (inc steps))))))

;; World loading from EDN

(defn- tile-entry
  "Expand one entry of a `:tiles` map into [position type] pairs.
  Accepts both the grouped form {:goal [[r c] ...]} and the
  coordinate form {[r c] :goal}."
  [k v]
  (cond
    (keyword? v) [[k v]]
    (and (vector? v) (= 2 (count v)) (every? integer? v)) [[v k]]
    :else (map (fn [p] [p k]) v)))

(defn- coord-tiles
  "Normalize the `:tiles` map to the coordinate form."
  [tiles]
  (into {} (mapcat (fn [[k v]] (tile-entry k v)) tiles)))

(defn ->world
  "Build a world map from a descriptor.
  See the EDN files under resources/gridmind/worlds for the shape."
  [m]
  {:name (:name m)
   :rows (:rows m)
   :cols (:cols m)
   :start (:start m)
   :tiles (coord-tiles (:tiles m))
   :wind (when (:wind m) (vec (:wind m)))
   :rewards (merge {:goal 1.0 :hazard -1.0 :step -0.01} (:rewards m))
   :max-steps (or (:max-steps m) 200)})

(defn problems
  "Return a vector of problems found in `world`. The vector is empty
  when the world is valid."
  [world]
  (vec
   (remove nil?
           [(when-not (and (integer? (:rows world)) (integer? (:cols world))
                           (pos? (:rows world)) (pos? (:cols world)))
              "rows and cols must be positive integers")
            (when-not (and (vector? (:start world)) (= 2 (count (:start world))))
              "start must be a two-element vector")
            (when (and (:start world) (not (in-bounds? world (:start world))))
              "start position is out of bounds")
            (when (and (:start world) (wall? world (:start world)))
              "start position is a wall")
            (when (and (:start world) (terminal? world (:start world)))
              "start position must not be a goal or a hazard")
            (when-not (some #(goal? world %) (states world))
              "world must contain at least one goal")
            (when-let [bad (seq (remove (partial in-bounds? world) (keys (:tiles world))))]
              (str "tile positions out of bounds: " (pr-str (vec bad))))
            (when-let [bad (seq (keep (fn [[_ t]]
                                        (when-not (contains? #{:goal :hazard :wall} t) t))
                                      (:tiles world)))]
              (str "unknown tile types: " (pr-str (vec bad))))
            (when (and (:wind world) (not= (count (:wind world)) (:cols world)))
              "wind vector length must equal the number of columns")])))

(defn validate!
  "Throw an exception when `world` is invalid. Otherwise return `world`."
  [world]
  (let [bad (problems world)]
    (when (seq bad)
      (throw (ex-info (str "invalid world: " (str/join "; " bad))
                      {:world world :problems bad})))
    world))

(defn load-world
  "Load and validate a world from a classpath resource."
  [resource]
  (if-let [url (io/resource resource)]
    (validate! (->world (edn/read-string (slurp url))))
    (throw (ex-info (str "world resource not found: " resource)
                    {:resource resource}))))
