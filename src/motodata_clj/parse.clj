(ns motodata-clj.parse
  (:require [clojure.string :as s]
            [motodata-clj.sqlite :as db]
            [motodata-clj.people-map :as pm]
            [clojure.core.reducers :as r]))

(def people (reduce
              #(into %1 {[(:first_name %2) (:last_name %2)] %2})
              {} (db/get-people)))

(defn find-person
  [{f :first_name l :last_name}]
  (if-let [p (get people (pm/remap f l))]
    p
    (->>
      (keys people)
      (filter #(let [[fir las] %]
              (and (s/starts-with? fir f)
                   (s/starts-with? las l))))
      first
      (get people))))

(defn enrich-rider [rider]
  (if-let [db-rider (find-person rider)]
    (merge rider db-rider)
    rider))

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
  (let [[_ n t c f _ _ l]
         (re-find #"(?ux)
           ^(?<number>\d{1,2})\s+
           (?<team>.*)\s+
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
   {:first_name (-> f (or "") s/trim s/capitalize)
    :last_name (-> l (or "") s/trim s/capitalize)
    :number (Integer/parseInt n)
    :team t
    :country c}))

(defn parse-time [time-str]
  (if (empty? time-str)
    nil
    (let [[_ _ m-str s-str f-str] (re-find #"((\d+)')?(\d+).(\d+)" time-str)
          m (Integer. (or m-str "0"))
          s (Integer. s-str)
          f (Integer. f-str)]
      (+ (* m 60) s
         (->> f Math/log10 Math/ceil (Math/pow 10) (/ f))))))

(defn parse-speed [speed-str]
  (if (empty? speed-str)
    nil
    (Double/parseDouble speed-str)))

(defn parse-lap [line]
  (let [[_ _ t _ unf _ _ _ t1 _ p1 _ _ t2 _ p2 _ _ t3 _ p3 _ _ speed _ _ p4 _ _ t4]
        (re-find #"(?x)
                 ^(
                   (?<time>(\d{1,2}')?\d{1,2}\.\d{4,5}) |
                   (?<unf>unfinished)(1)?
                 )?
                 (\s+)?
                 (
                   (?<t1>(\d{1,2}')?\d{1,2}\.\d{3})
                   (?<pit1>P|PIT)?
                   (b)?
                 )?
                 (\s+
                   (?<t2>(\d{1,2}')?\d{1,2}\.\d{3})
                   (?<pit2>P|PIT)?
                   (b)?
                 )?
                 (\s+
                   (?<t3>(\d{1,2}')?\d{1,2}\.\d{3})
                   (?<pit3>P|PIT)?
                   (b)?
                 )?
                 (
                   \s+(?<speed>\d{2,3}\.\d)?
                   (b(\s+|$))?
                   (?<pit4>(P|PIT)(\s+)?)?
                   (?<t4>(\d{1,2}')?\d{2}\.\d{3})?
                 )?" line)]
    {:time (parse-time t)
     :t1 (parse-time t1)
     :t2 (parse-time t2)
     :t3 (parse-time t3)
     :t4 (parse-time t4)
     :pit (boolean (or p1 p2 p3 p4))
     :finished (not unf)
     :speed (parse-speed speed)}))

(defn parse-rider [rider]
  (let [part (partition-by lap-line? rider)
        hl (s/join " " (first part))
        ls (-> part rest flatten)]
    (try
      (if-let [head (parse-head hl)]
        (merge
          head
          {:raw-head hl
           :laps (map parse-lap ls)}))
      (catch Exception e
        (merge rider {:id nil :error (.getMessage e)})))))

(defn is-ride-valid [ride]
  (boolean (:id ride)))

(defn parse [file-name]
  (->> file-name
    slurp
    split-pages
    (map refine-page)
    flatten
    split-riders
    (r/map (comp enrich-rider parse-rider))
    (into [])
    (group-by is-ride-valid)))
