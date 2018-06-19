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
   (map #(db/persist-ride % context) results))

(def cli-options
  ;; An option with a required argument
  [["-dry" nil "Dry run" :id :dry, :default true]
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

(defn -main [& args]
  (let [{{dry :dry year :year}
         :options info :summary} (parse-opts args cli-options)]
    (with-open [o (clojure.java.io/writer "process-log")]
      (doseq [res (->> "data"
                       (parse-dir year)
                       #(if dry
                          %
                          (map persist-result %))
                       )]
        (.write o (str res))))))

