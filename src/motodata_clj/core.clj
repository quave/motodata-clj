(ns motodata-clj.core
  (:require [motodata-clj.parse :as parser]
            [motodata-clj.traverse-files :as files]
            [motodata-clj.sqlite :as db]
            [clojure.core.reducers :as r]
            [clojure.tools.cli :refer [parse-opts]])

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
         {:year (Integer/parseInt year)
          :event-number (Integer/parseInt number)
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

(defn parse-dir [start-year dir-name]
  (map process-file (files/list-files dir-name start-year)))

(defn persist-result [{:keys [context results]}]
  (println (str "persist " (count results) context))
  (map #(db/persist-ride % context) results))

(def cli-options
  ;; An option with a required argument
  [["-dry" "--dry" "Dry run" :id :dry]
   ["-f" nil "File to parse" :id :file, :default nil]
   ["-y" "--year YEAR" "Start year"
    :id :year
    :default 2000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 2000 % 2030) "Must be a number between 2000 and 2030"]]])

(defn get-err-names [res]
  (->> res
       (map :errors)
       flatten
       (map #(select-keys % [:first_name :last_name]))
       set))

(defn cond-persist-result [dry]
  (if dry identity persist-result))

(defn process [dry year]
  (with-open [o (clojure.java.io/writer "process-log" :append true)]
    (let [path "../motodata/data"
          pipeline (comp
                     (partial map #(str % "\n"))
                     (partial map (cond-persist-result dry))
                     (partial parse-dir year))]
      (doseq [r (pipeline path)] (.write o r) r))))

(defn -main [& args]
  (let [{options :options info :summary} (parse-opts args cli-options)]
    (pprint options)
    (process (:dry options) (:year options))))

