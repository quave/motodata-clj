(ns motodata-clj.core
  (:require [motodata-clj.parse :as parser]
            [motodata-clj.traverse-files :as files]
            [motodata-clj.mongo :as db]))

(defn -main []
  (->>
    (files/list-files "data")
    (map #(-> %
           parser/parse
           db/persist-parsed))))
