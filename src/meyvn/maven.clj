(ns meyvn.maven
  (:require [clojure.java.io :as io]
            [nolipservice.core :as report])
  (:import [org.apache.maven.shared.invoker DefaultInvoker DefaultInvocationRequest InvocationResult]))


(defn- invoke- [goal]
  (let [invocation-request
        (doto (DefaultInvocationRequest.)
          (.setPomFile (io/file "meyvn-pom.xml"))
          (.setGoals (list goal))
          (.setBatchMode true))        
        invoker (DefaultInvoker.)]
    (.execute invoker invocation-request)))

(defn invoke [conf args]
  (let [^InvocationResult result (invoke- (first args))]
    (try
      (report/pastebin (get-in conf [:pom :group-id]) (.getExitCode result))
      (catch Exception e (do)))))
