(ns motodata-clj.core
  (:require [motodata-clj.parse :as parser]
            [motodata-clj.traverse-files :as dir]
            [motodata-clj.mongo :as db]))

(defn -main []
  (->>
    (dir/get-parsibles)
    (map #(-> %
           parser/parse
           db/persist-parsed))))
