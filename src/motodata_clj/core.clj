(ns motodata-clj.core
  (:require [motodata-clj.parse :as parser]
            [motodata-clj.traverse-files :as files]
            [motodata-clj.mongo :as db]
            [clojure.core.reducers :as r])
  (:use clojure.pprint
        [clojure.java.io :only [output-stream]]))

(def errors-file "src/motodata_clj/errors.clj")
(defn extract-context [file-name]
  (let [[found year number ev-name category session] 
        (re-find #"(?x)
                 (\d{4})\/
                 (\d{2})_([A-Z]{3})\/
                 [A-Z]{3}_
                 ([MotoGP01235c]+)_
                 ([A-Za-z0-9]{2,3}(\d)?)_analysis"
                 file-name)]
    (and found
         {:year year
          :event-number number
          :event-name ev-name
          :category category
          :session session})))

(defn process-file [file]
  (println file)
  (let [context (extract-context file)
        data (parser/parse file)]
    {:context context
     :errors (data false)
     :results (data true)}))

(defn parse-dir [dir-name] 
  (map process-file (files/list-files dir-name)))
(defn persist-result [{:keys [context results]}]
   (map #(db/persist-ride % context) results))

(defn get-err-names [res]
  (->> res
       (map :errors)
       flatten
       (map #(select-keys % [:first_name :last_name]))
       set))

(defn -main []
  (with-open [o (clojure.java.io/writer "process-log")]
    (doseq [res (->> "data"
                     parse-dir
                     (map persist-result))]
      (.write o (str res)))))

