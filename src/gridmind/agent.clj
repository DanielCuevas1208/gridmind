(ns gridmind.agent
  "Tabular reinforcement learning for grid worlds.

  An agent stores one value per state-action pair in a Q-table.
  Q-learning and SARSA update the table while the agent explores.
  A seeded random source makes every run deterministic."

  (:require [gridmind.world :as world]))

(def ^:private default-options
  {:alpha 0.5
   :gamma 1.0
   :epsilon 0.1
   :algorithm :q-learning})

(defn q-table
  "Create an empty Q-table."
  []
  (atom {}))

(defn q-value
  "Read the value of `state` and `action`.
  Unknown pairs have a value of 0.0."
  [q state action]
  (get-in @q [state action] 0.0))

(defn update-q!
  "Move the value of `state` and `action` toward `target` by `alpha`."
  [q state action target alpha]
  (let [current (q-value q state action)]
    (swap! q assoc-in [state action]
           (+ current (* alpha (- target current))))))

(defn greedy-action
  "Return the action with the highest value for `state`.
  Equal values resolve to the earlier action in world/actions."
  [q state]
  (reduce (fn [best action]
            (if (> (q-value q state action) (q-value q state best))
              action
              best))
          (first world/actions)
          world/actions))

(defn epsilon-greedy
  "Pick an action for `state` using epsilon-greedy exploration.
  Pick a random action with probability `epsilon`.
  Otherwise pick the greedy action. `rng` must be a java.util.Random."
  [q state rng epsilon]
  (if (< (.nextDouble rng) epsilon)
    (nth world/actions (.nextInt rng (count world/actions)))
    (greedy-action q state)))

(defn run-episode
  "Run one learning episode from the start of `world`.

  The agent picks actions with the epsilon-greedy policy.
  It updates `q` after every step with Q-learning or SARSA.
  Returns the total reward, the step count, and the terminal state.
  When the step budget runs out, the terminal value is :exhausted."
  [world q rng options]
  (let [{:keys [alpha gamma epsilon algorithm]}
        (merge default-options options)
        first-action (epsilon-greedy q (:start world) rng epsilon)]
    (loop [state (:start world)
           action first-action
           reward-total 0.0
           steps 0]
      (cond
        (world/terminal? world state)
        {:reward reward-total :steps steps :terminal state}

        (>= steps (:max-steps world))
        {:reward reward-total :steps steps :terminal :exhausted}

        :else
        (let [result (world/step world state action)
              next-state (:state result)
              reward (:reward result)
              done (:done result)
              next-action (when-not done
                            (if (= algorithm :sarsa)
                              (epsilon-greedy q next-state rng epsilon)
                              (greedy-action q next-state)))
              target (if done
                       reward
                       (+ reward (* gamma (q-value q next-state next-action))))]
          (update-q! q state action target alpha)
          (recur next-state next-action
                 (+ reward-total reward)
                 (inc steps)))))))

(defn train
  "Train an agent on `world` for `episodes` episodes.

  Epsilon falls toward 0.01 by `epsilon-decay` after each episode.
  A `seed` makes the whole run reproducible.
  Returns a map with the final Q-table and one reward per episode."
  [world {:keys [episodes seed epsilon-decay] :as options}]
  (let [{:keys [epsilon] :as merged} (merge default-options options)
        rng (java.util.Random. (or seed 0))
        q (q-table)
        episodes (or episodes 100)
        decay (or epsilon-decay 1.0)]
    (loop [n 0
           eps epsilon
           rewards []]
      (if (>= n episodes)
        {:q-table q :rewards rewards}
        (recur (inc n)
               (max 0.01 (* eps decay))
               (conj rewards
                     (:reward
                      (run-episode world q rng
                                   (assoc merged :epsilon eps)))))))))

(defn greedy-policy
  "Return a deterministic policy for `q`.
  The policy maps a state to its highest-value action."
  [q]
  (fn [state] (greedy-action q state)))

(defn evaluate
  "Walk `world` from its start with the greedy policy for `q`.
  Returns the terminal state reached, or nil when the walk never ends."
  [world q]
  (world/follow-policy world (greedy-policy q)))
