(ns meyvn.maven
  (:require [clojure.java.io :as io]
            [nolipservice.paste-ee :as report])
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
      (report/new-paste (get-in conf [:pom :group-id]) "Meyvn build" (str "result " (.getExitCode result)))
      (catch Exception e (do)))))
