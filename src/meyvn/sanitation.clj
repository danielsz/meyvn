(ns meyvn.sanitation
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.pprint :as pprint]))

(def defaults
  (let [name (.. (io/file ".") getCanonicalFile getName)]
    {:pom {:group-id name
           :artifact-id name
           :version "1.0.0"
           :name name}
     :main-class "main.core"
     :cljs {:main-class "main.core"
            :compiler-opts "cljsc_opts.edn"
            :tools-deps-alias :cljs}}))

(defn find-env []
  (when (not (System/getenv "M2_HOME"))
    (println "M2_HOME not set in environment")
    (System/exit 1)))

(defn find-exec []
  (let [rc (sh "which" "mvn")]
    (if (not (zero? (:exit rc)))
      (do (println "Cannot find Maven executable!")
          (System/exit 1))
      (io/file (str/trim (:out (sh "which" "mvn")))))))

(defn write-conf [defaults]
  (with-open [writer (io/writer (io/file "meyvn.edn"))]
    (binding [*out* writer]
      (pprint/pprint defaults))))

(defn find-conf []
  (when (not (.exists (io/file "meyvn.edn")))
    (write-conf defaults)))

(defn checks []
  (find-env)
  (find-conf))
