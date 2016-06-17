(ns motodata-clj.traverse-files
  (:require [clojure.java.io :as io]))

(defn is-file [f] (.isFile f))
(defn get-path [f] (.getPath f))
(defn is-analysis [name]
  (re-find #"analysis" name))

(defn list-files [dir]
  (->> dir
       io/file 
       file-seq 
       (filter is-file) 
       (map get-path)
       (filter is-analysis)))
