(ns gridmind.agent-test
  (:require [clojure.test :refer [deftest is testing]]
            [gridmind.agent :as agent]
            [gridmind.world :as world]))

(def small (world/load-world "gridmind/worlds/small.edn"))
(def windy (world/load-world "gridmind/worlds/windy.edn"))

(deftest q-table-starts-empty
  (let [q (agent/q-table)]
    (is (= 0.0 (agent/q-value q [0 0] :up)))))

(deftest update-q-moves-toward-target
  (let [q (agent/q-table)]
    (agent/update-q! q [0 0] :up 4.0 0.5)
    (is (= 2.0 (agent/q-value q [0 0] :up)))
    (agent/update-q! q [0 0] :up 10.0 0.25)
    (is (= 4.0 (agent/q-value q [0 0] :up)))))

(deftest greedy-action-picks-the-maximum
  (let [q (agent/q-table)]
    (agent/update-q! q [0 0] :right 5.0 1.0)
    (agent/update-q! q [0 0] :down 2.0 1.0)
    (is (= :right (agent/greedy-action q [0 0])))))

(deftest greedy-action-ties-resolve-to-action-order
  (let [q (agent/q-table)]
    (is (= :up (agent/greedy-action q [0 0])))
    (agent/update-q! q [0 0] :left 0.0 1.0)
    (agent/update-q! q [0 0] :right 0.0 1.0)
    (is (= :up (agent/greedy-action q [0 0])))))

(deftest epsilon-zero-is-greedy
  (let [q (agent/q-table)
        rng (java.util.Random. 1)]
    (agent/update-q! q [0 0] :right 9.0 1.0)
    (is (= :right (agent/epsilon-greedy q [0 0] rng 0.0)))))

(deftest epsilon-greedy-is-reproducible
  (let [q (agent/q-table)]
    (is (= (agent/epsilon-greedy q [0 0] (java.util.Random. 42) 0.5)
           (agent/epsilon-greedy q [0 0] (java.util.Random. 42) 0.5)))))

(deftest run-episode-is-reproducible
  (let [q1 (agent/q-table)
        q2 (agent/q-table)
        options {:epsilon 0.1}]
    (is (= (agent/run-episode small q1 (java.util.Random. 7) options)
           (agent/run-episode small q2 (java.util.Random. 7) options)))
    (is (= @q1 @q2))))

(deftest run-episode-tracks-reward-and-steps
  (let [q (agent/q-table)
        result (agent/run-episode small q (java.util.Random. 3) {:epsilon 1.0})]
    (is (number? (:reward result)))
    (is (pos? (:steps result)))
    (is (world/terminal? small (:terminal result)))))

(deftest step-budget-exhausts-gracefully
  (let [world (-> (world/empty-world 1 4 [0 0])
                  (world/set-rewards {:goal 1.0 :step 0.0})
                  (assoc :max-steps 3)
                  (world/place [0 3] :goal))
        q (agent/q-table)
        result (agent/run-episode world q (java.util.Random. 0) {:epsilon 0.0})]
    (is (= :exhausted (:terminal result)))
    (is (= 3 (:steps result)))))

(deftest q-learning-reaches-goal-on-small-world
  (let [{:keys [q-table rewards]} (agent/train small {:episodes 100 :seed 0})]
    (is (= [0 3] (agent/evaluate small q-table)))
    (is (= 100 (count rewards)))
    (is (every? number? rewards))
    (is (> (last rewards) (first rewards)))))

(deftest q-learning-improves-with-training
  (let [{:keys [q-table rewards]} (agent/train small {:episodes 200 :seed 0 :epsilon-decay 0.99})]
    (is (= [0 3] (agent/evaluate small q-table)))
    (is (> (reduce + (take-last 20 rewards))
           (reduce + (take 20 rewards))))))

(deftest sarsa-reaches-goal-on-small-world
  (let [{:keys [q-table]} (agent/train small
                                       {:episodes 200 :seed 1
                                        :algorithm :sarsa :epsilon 0.1})]
    (is (= [0 3] (agent/evaluate small q-table)))))

(deftest q-learning-learns-the-windy-world
  (let [{:keys [q-table]} (agent/train windy
                                       {:episodes 300 :seed 0
                                        :epsilon 0.15 :epsilon-decay 0.995})]
    (is (= [3 7] (agent/evaluate windy q-table)))))

(deftest sarsa-learns-the-windy-world
  (let [{:keys [q-table]} (agent/train windy
                                       {:episodes 300 :seed 2
                                        :algorithm :sarsa
                                        :epsilon 0.15 :epsilon-decay 0.995})]
    (is (= [3 7] (agent/evaluate windy q-table)))))

(deftest training-with-the-same-seed-matches
  (let [a (agent/train small {:episodes 30 :seed 5})
        b (agent/train small {:episodes 30 :seed 5})]
    (is (= @(:q-table a) @(:q-table b)))
    (is (= (:rewards a) (:rewards b)))))

(deftest greedy-policy-respects-the-q-table
  (let [q (agent/q-table)]
    (agent/update-q! q [0 0] :down 3.0 1.0)
    (is (= :down ((agent/greedy-policy q) [0 0])))
    (is (fn? (agent/greedy-policy q)))))
