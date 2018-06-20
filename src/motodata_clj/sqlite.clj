(ns motodata-clj.sqlite
  (:require [clojure.java.jdbc :refer [query insert-multi!]  :as jdbc])
  (:use [clojure.pprint :only (pprint)]
        [clj-time.format :only (unparse formatter)]
        [clj-time.core :only (now)]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname "../motodata/db/development.sqlite3"})

(def get-result (comp (keyword "last_insert_rowid()") first))

(def date-formatter (formatter "yyyy-MM-dd HH:mm:ss.SSS"))

(defn add-dates [obj]
  (let [n (unparse date-formatter (now))]
    (merge obj {:created_at n :updated_at n})))

(defn insert [table objects]
  (let [wrapped (if (seq? objects) objects (vector objects))
        enriched (map add-dates wrapped)]
    (comment println (str "insert " table))
    (comment println enriched)
    (get-result
      (jdbc/insert-multi! db-spec table enriched))))

(defn db-query [sql]
  (comment println (str "query " sql))
  (jdbc/query db-spec sql))

(defn extract-rider [ride context]
  (merge
    {:person_id (:id ride)
     :category (:category context)}
    (select-keys ride [:number :team])))

(defn find-circuit-id [name]
  (->> (str "select id from circuits where name like '" name "'")
       db-query
       first
       :id))

(defn insert-event [year number name]
  (let [cid (find-circuit-id name)
        now (java.util.Date.)]
    (if (not cid)
      nil
      (insert :events
        {:year year
         :number number
         :circuit_id cid}))))

(defn find-event-id [year number name]
  (let [sql (str "select id from events where year=" year " and number=" number)
        found (first (db-query sql))]
    (if found
      (:id found)
      (insert-event year number name))))

(defn get-laps [ride context rider_id]
  (map-indexed
    (fn [i l] (merge l
            {:sequence (inc i)
             :position 0
             :session (:session context)
             :event_id (find-event-id
                         (:year context)
                         (:event-number context)
                         (:event-name context))
             :rider_id rider_id}))
    (:laps ride)))

(defn persist-ride [ride context]
  (let [rider_id (insert :riders (extract-rider ride context))]
    (insert :laps (get-laps ride context rider_id))))

(defn get-people []
  (db-query "select * from people"))
