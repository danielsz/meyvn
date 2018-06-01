(ns meyvn.core
  (:require
   [meyvn.cljs :as cljs]
   [meyvn.sanitation :as sanitation]
   [meyvn.transient-pom :as transient]
   [clojure.tools.deps.alpha.reader :as tools.reader]
   [clojure.java.io :as io])
  (:import [org.apache.maven.shared.invoker DefaultInvoker DefaultInvocationRequest InvocationResult]))

(def conf)

(def deps-map (tools.reader/read-deps [(io/file "/usr/local/lib/clojure/deps.edn")
                                       (io/file "/home/arch/daniel/.clojure/deps.edn")
                                       (io/file "deps.edn")]))

(defn invoke [goal]
  (let [invocation-request
        (doto (DefaultInvocationRequest.)
          (.setPomFile (io/file "meyvn-pom.xml"))
          (.setGoals (list goal))
          (.setBatchMode true))        
        invoker (DefaultInvoker.)]
    (.execute invoker invocation-request)))

(defn invoke-maven [args]
  (let [^InvocationResult result (case (first args)
                                   "validate"  (invoke "validate")
                                   "compile" (invoke "compile")
                                   "test" (invoke "test")
                                   "package" (invoke "package")
                                   "verify" (invoke "verify")
                                   "install" (invoke "install")
                                   "deploy" (invoke "deploy")
                                   "clean" (invoke "clean")
                                   (println "done"))]
    (if (zero? (.getExitCode result))
      (println "All done.")
      (println "Build has errors."))))

(defn -main [& args] 
  (sanitation/checks)
  (cljs/compile deps-map (:cljs conf))
  (transient/extend-pom deps-map conf)
  (invoke-maven args))

