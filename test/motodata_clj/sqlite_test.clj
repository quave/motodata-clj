(ns motodata-clj.sqlite-test
  (:require [clojure.test :refer :all]
            [motodata-clj.sqlite :refer :all]))

(deftest persist-ride-test
  (testing "Persist ride test"
    (let [res (parse-dir 2015 "../motodata/data")
          ride (->> res first :results first)
          ctx (->> res first :context)
          ]
      (is (> (persist-ride ride ctx) 0)))))
