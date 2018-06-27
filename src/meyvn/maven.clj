(ns meyvn.maven
  (:require [clojure.java.io :as io]
            [nolipservice.core :refer [report]])
  (:import [org.apache.maven.shared.invoker DefaultInvoker DefaultInvocationRequest InvocationResult]))

(defn- invoke- [goals]
  (let [invocation-request
        (doto (DefaultInvocationRequest.)
          (.setPomFile (io/file "meyvn-pom.xml"))
          (.setGoals goals)
          (.setBatchMode true))        
        invoker (DefaultInvoker.)]
    (.execute invoker invocation-request)))

(defn invoke [conf args]
  (let [^InvocationResult result (invoke- args)]
    (try
      (report (get-in conf [:pom :group-id]) (.getExitCode result))
      (catch Exception e (do)))))
