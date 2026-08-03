(ns gridmind.worlds
  "Built-in worlds shipped with Gridmind.

  Each world is loaded from an EDN file on the classpath. The files
  live under resources/gridmind/worlds."
  (:require [gridmind.world :as world]))

(def small
  "A tiny grid with one goal and one hazard."
  (world/load-world "gridmind/worlds/small.edn"))

(def windy
  "The classic windy grid world. Wind pushes the agent up per column."
  (world/load-world "gridmind/worlds/windy.edn"))

(def cliff
  "The classic cliff walking world. Falling into the cliff costs -100."
  (world/load-world "gridmind/worlds/cliff.edn"))

(def all
  "All built-in worlds in the order they appear in the demo."
  [small windy cliff])

(def by-name
  "Look up a built-in world by name."
  {"small" small "windy" windy "cliff" cliff})

(defn load
  "Load a built-in world by name, or a classpath resource by full path."
  [name-or-resource]
  (if-let [builtin (get by-name name-or-resource)]
    builtin
    (world/load-world name-or-resource)))
