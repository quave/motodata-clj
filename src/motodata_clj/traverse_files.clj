(ns motodata-clj.traverse-files
  (:require [clojure.java.io :as io]))

(defn is-file [f] (.isFile f))
(defn get-path [f] (.getPath f))
(defn is-analysis [name]
  (re-find #"analysis" name))

(defn parse-int [number-string]
  (try (Integer/parseInt number-string)
    (catch Exception e nil)))

(defn after-start-year [name year]
  (let [[_ y] (re-find #"\/(\d{4})\/" name)
        py (parse-int y)] (and py (>= py year))))

(defn list-files [dir start-year]
  (->> dir
       io/file
       file-seq
       (filter is-file)
       (map get-path)
       (filter #(after-start-year % start-year))
       (filter is-analysis)))
