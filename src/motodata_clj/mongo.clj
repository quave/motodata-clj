(ns motodata-clj.mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc]))

(def conn (mg/connect {:host "172.17.0.5"}))
(def db (mg/get-db conn "motodata"))

(defn persist-parsed [parsed]
  parsed)

(defn get-people []
  (mc/find-maps db "people"))
