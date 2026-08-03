(ns gridmind.run-tests
  "Entry point for the Clojure CLI test alias.
  Runs every test namespace in the project."

  (:require [clojure.test :as test]
            [gridmind.agent-test]
            [gridmind.world-test]))

(defn -main
  "Run the full test suite. Exit with 0 on success, 1 on failure."
  [& _]
  (let [result (test/run-tests 'gridmind.agent-test 'gridmind.world-test)
        failures (+ (:fail result) (:error result))]
    (shutdown-agents)
    (System/exit (if (pos? failures) 1 0))))
