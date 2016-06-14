(ns motodata-clj.traverse-files
  (:require [clojure.java.io :as io]))

(defn list-files [dir]
  (->> dir
       io/file 
       file-seq 
       (filter #(.isFile %)) 
       (map #(.getPath %))))
