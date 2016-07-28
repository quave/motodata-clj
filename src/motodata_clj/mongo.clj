(ns motodata-clj.mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:use clojure.set)
  (:import org.bson.types.ObjectId))

(def conn (mg/connect {:host "172.17.0.5"}))
(def db (mg/get-db conn "motodata"))

(defn persist-ride [rider context]
  (boolean
    (mc/insert db "analysis"
      (merge {:_id (ObjectId.)}
             context 
             (rename-keys
               (select-keys rider [:_id :laps])
               {:_id :person_id})))))

(defn get-people []
  (mc/find-maps db "people"))
