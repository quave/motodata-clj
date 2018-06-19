(defproject motodata-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.novemberain/monger "3.0.2"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.xerial/sqlite-jdbc "3.23.1"]
                 [org.clojure/tools.cli "0.3.7"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [clj-time "0.14.4"]]
  :main ^:skip-aot motodata-clj.core)
