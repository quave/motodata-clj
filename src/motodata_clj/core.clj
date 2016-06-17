(ns motodata-clj.core
  (:require [motodata-clj.parse :as parser]
            [motodata-clj.traverse-files :as files]
            [motodata-clj.mongo :as db]))

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
          :category category
          :event-number number
          :event-name ev-name
          :session session})))

(defn parse-dir [dir-name]
  (->> dir-name
    files/list-files
    (map parser/parse)))

(defn -main []
  (parse-dir "data"))
