(ns meyvn.maven
  (:require [clojure.java.io :as io])
  (:import [org.apache.maven.shared.invoker DefaultInvoker DefaultInvocationRequest InvocationResult]))


(defn- invoke- [goal]
  (let [invocation-request
        (doto (DefaultInvocationRequest.)
          (.setPomFile (io/file "meyvn-pom.xml"))
          (.setGoals (list goal))
          (.setBatchMode true))        
        invoker (DefaultInvoker.)]
    (.execute invoker invocation-request)))

(defn invoke [args]
  (let [^InvocationResult result (invoke- (first args))]
    (if (zero? (.getExitCode result))
      (println "All done.")
      (println "Build has errors."))))
