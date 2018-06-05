(ns meyvn.configuration
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.tools.deps.alpha.reader :as tools.reader]))

(def defaults
  (let [name (.. (io/file ".") getCanonicalFile getName)]
    {:pom {:group-id name
           :artifact-id name
           :version "1.0.0"
           :name name}
     :packaging {:uberjar {:enabled true
                           :main-class "main.core"
                           :remote-repository {:id "ssh-repository"
                                               :url "scpexe://user@domain:/home/.m2/repository"}
                           :excludes {:artifacts ["org.clojure:google-closure-library"]
                                      :filters ["META-INF/*.SF" "META-INF/*.DSA" "META-INF/*.RSA"]}}
                 :jar {:enabled false
                       :remote-repository {:id "clojars"
                                           :url "https://clojars.org/repo"}}}
     :cljs {:enabled false
            :main-class "main.core"
            :compiler-opts {:optimizations :advanced
                            :output-wrapper true
                            :infer-externs true
                            :parallel-build true
                            :aot-cache true
                            :output-dir "resources/js/compiled"
                            :output-to "resources/js/main.js"
                            :source-map "resources/js/main.js.map"}
            :tools-deps-alias :cljs}}))


(def deps-map
  (let [user-dir (or (System/getenv "CLJ_CONFIG")
                     (System/getenv "XDG_CONFIG_HOME")
                     (str (System/getProperty "user.home") "/.clojure"))
        installation-dir (some (fn [path] (when (.exists (io/file path)) path))
                               ["/usr/local/lib/clojure"
                                "/usr/local/Cellar/clojure/1.9.0.381"])]
    (tools.reader/read-deps [(io/file (str installation-dir "/deps.edn"))
                             (io/file (str user-dir "/deps.edn"))
                             (io/file "deps.edn")])))

(defn find-deps-edn []
  (when-not (.exists (io/file "deps.edn"))
    (println "No deps.edn found in current directory.")))

(defn find-env []
  (when-not (System/getenv "M2_HOME")
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

(defn- read-conf- []
  (-> "meyvn.edn"
      slurp
      edn/read-string))

(defn find-conf []
  (when-not (.exists (io/file "meyvn.edn"))
    (write-conf defaults)))

(defn read-conf []
  (find-env)
  (find-deps-edn)
  (find-conf)
  [deps-map (read-conf-)])
