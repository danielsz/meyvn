(ns meyvn.configuration
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.tools.deps.alpha.reader :as tools.reader]
            [meyvn.finders :refer [find-env find-local-deps find-global-deps]]))

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
        global-deps (find-global-deps)]
    (tools.reader/read-deps [global-deps
                             (io/file (str user-dir "/deps.edn"))
                             (io/file "deps.edn")])))

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
  (find-local-deps)
  (find-conf)
  [deps-map (read-conf-)])
