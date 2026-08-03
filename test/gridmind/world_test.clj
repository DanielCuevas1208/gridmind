(ns gridmind.world-test
  (:require [clojure.test :refer [deftest is]]
            [gridmind.world :as world]
            [gridmind.worlds :as worlds]))

(deftest step-moves-and-rewards
  (is (= {:state [0 1] :reward -0.01 :done false}
         (world/step worlds/small [0 0] :right)))
  (is (= {:state [0 3] :reward 1.0 :done true}
         (world/step worlds/small [0 2] :right)))
  (is (= {:state [1 1] :reward -1.0 :done true}
         (world/step worlds/small [1 0] :right))))

(deftest borders-block-movement
  (is (= [0 0] (world/move worlds/small [0 0] :up)))
  (is (= [0 0] (world/move worlds/small [0 0] :left)))
  (is (= [2 3] (world/move worlds/small [2 3] :down)))
  (is (= [2 3] (world/move worlds/small [2 3] :right))))

(deftest wind-pushes-the-agent-up
  (is (= [3 1] (:state (world/step worlds/windy [3 0] :right))))
  (is (= [2 4] (:state (world/step worlds/windy [3 3] :right))))
  (is (= [1 7] (:state (world/step worlds/windy [3 6] :right))))
  (is (= [2 8] (:state (world/step worlds/windy [3 7] :right)))))

(deftest wind-stops-at-the-top-border
  (is (= [0 6] (:state (world/step worlds/windy [0 5] :right)))))

(deftest states-exclude-walls
  (let [world (-> (world/empty-world 2 2 [0 0])
                  (world/place [1 0] :wall))]
    (is (= #{[0 0] [0 1] [1 1]} (set (world/states world))))))

(deftest world-validation-finds-problems
  (is (empty? (world/problems worlds/small)))
  (is (seq (world/problems (assoc worlds/small :start [9 9]))))
  (is (seq (world/problems (assoc worlds/small :tiles {}))))
  (is (thrown? clojure.lang.ExceptionInfo (world/validate! (assoc worlds/small :start [9 9])))))

(deftest load-world-from-resource
  (is (= "cliff" (:name worlds/cliff)))
  (is (world/terminal? worlds/cliff [3 11]))
  (is (world/hazard? worlds/cliff [3 5])))

(deftest worlds-load-by-name
  (is (= worlds/small (worlds/load "small"))))
