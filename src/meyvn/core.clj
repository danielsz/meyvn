(ns meyvn.core
  (:require
   [meyvn.cljs :as cljs]
   [meyvn.sanitation :as sanitation]
   [meyvn.transient-pom :as transient]
   [meyvn.maven :as maven]
   [clojure.tools.deps.alpha.reader :as tools.reader]
   [clojure.java.io :as io]))

(def conf)

(def deps-map (tools.reader/read-deps [(io/file "/usr/local/lib/clojure/deps.edn")
                                       (io/file "/home/arch/daniel/.clojure/deps.edn")
                                       (io/file "deps.edn")]))


(defn -main [& args] 
  (sanitation/checks)
  (cljs/compile deps-map (:cljs conf))
  (transient/extend-pom deps-map conf)
  (maven/invoke args))

