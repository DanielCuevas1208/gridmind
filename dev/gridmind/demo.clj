(ns gridmind.demo
  "Train an agent on each built-in world.
  Prints the reward curve and the final greedy path."

  (:require [gridmind.agent :as agent]
            [gridmind.world :as world]
            [gridmind.worlds :as worlds]))

(defn- greedy-path
  "Return the positions visited by the greedy policy, up to a terminal state."
  [world q]
  (let [policy (agent/greedy-policy q)]
    (loop [pos (:start world)
           seen []]
      (if (or (world/terminal? world pos) (some #{pos} seen))
        (conj seen pos)
        (recur (:state (world/step world pos (policy pos)))
               (conj seen pos))))))

(defn- train-and-report
  "Train on `world` and print one line per metric."
  [world options]
  (let [{:keys [q-table rewards]} (agent/train world options)
        path (greedy-path world q-table)
        end (peek path)
        end-label (cond (= end :exhausted) "budget exhausted"
                        (world/goal? world end) "goal"
                        (world/hazard? world end) "hazard"
                        :else "cycle")]
    (println (str "== " (:name world) " =="))
    (println (str "  reward, first episode: " (format "%.3f" (first rewards))))
    (println (str "  reward, last episode:  " (format "%.3f" (last rewards))))
    (println (str "  greedy path: " (pr-str path) " -> " end-label))))

(defn -main
  "Train each built-in world and print the results."
  [& _]
  (doseq [world worlds/all]
    (train-and-report world {:episodes 200 :seed 0 :epsilon 0.15 :epsilon-decay 0.995}))
  (shutdown-agents))
