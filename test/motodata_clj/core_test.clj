(ns motodata-clj.core-test
  (:require [clojure.test :refer :all]
            [motodata-clj.core :refer :all]
            [motodata-clj.parse :refer :all]
            [motodata-clj.sqlite :refer :all])
  (:use clojure.pprint))

(deftest parse-test
  (testing "Parse file"
    (let [file "../motodata/data/2000/09_GBR/GBR_500cc_WUP_analysis.txt"
          res (process-file file)]
      (is (->> res :results empty? not))
      (is (->> res :errors empty?)))))
