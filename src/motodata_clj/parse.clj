(ns motodata-clj.parse
  (:require [clojure.string :as s]
            [motodata-clj.mongo :as db]))

(def people (reduce 
              #(into %1 {[(:first_name %2) (:last_name %2)] %2})
              {} (db/get-people)))

(defn find-person [f l]
  (if-let [p (people [f l])]
    p
    (first 
      (filter #(let [[fir las] %]
                (and (s/starts-with? fir f)
                     (s/starts-with? las l)))
              (keys people)))))

(defn head-start? [line] 
  (and (re-find #"^\d{1,2}(\s|$)" line) 
       (not (re-find #"^18 Garage" line))))
(defn lap-line? [line] 
  (re-find #"^[\d'.\sbPITunfinished]+\.[\d'.\sPITb]+$" line))

(defn split-pages [text]
  (s/split text #"\f"))

(defn refine-page [page]
  (->> page
       s/split-lines 
       (drop-while #(not (head-start? %)))
       (take-while #(not (re-find #"^Page" %)))))

(defn split-riders [lines]
  (->> lines 
       (partition-by head-start?)
       (partition 2 2)
       (map flatten)))

(defn parse-head [head]
  (let [[_ c f _ _ l] 
         (re-find #"(?ux)
           \b(?<country>[A-Z]{3})
           (?<first>
             (
               ([A-Z]{1})?
               [\u00C0-\u01FFa-z'\-]+
               \b\s?
             )+\s?
           )\s
           (?<last>
           [\u00C0-\u01FF'\-A-Z\s]+
           )\b" head)]
    (find-person 
       (-> f s/trim s/capitalize) 
       (-> l s/trim s/capitalize))))

(defn parse-lap [line]
  line)

(defn parse-rider [rider]
  (let [part (partition-by lap-line? rider)
        hl (s/join " " (first part))
        ls (-> part rest flatten)]
    (merge 
      (parse-head hl)
      {:laps (map parse-lap ls)})))

(defn parse [file-name] 
  (->>
    file-name
    slurp
    split-pages
    (map refine-page)
    flatten
    split-riders
    (map parse-rider)))

