(ns meyvn.plugins
  (:require [clojure.tools.deps.alpha.reader :as tools.reader]
            [clojure.java.io :as io])
  (:import [org.codehaus.plexus.util.xml Xpp3Dom]
           [org.apache.maven.model Plugin PluginExecution]))

(def clojure-maven-plugin-configuration 
  (let [config (Xpp3Dom. "configuration")
        src-directories (Xpp3Dom. "sourceDirectories")
        paths (:paths (tools.reader/slurp-deps "deps.edn"))]
    (doseq [path paths
          :let [src-directory (Xpp3Dom. "sourceDirectory")]]
      (.setValue src-directory path)
      (.addChild src-directories src-directory))
    (.addChild config src-directories)
    config))

(def clojure-maven-plugin
  (doto (Plugin.)
    (.setGroupId "com.theoryinpractise")
    (.setArtifactId "clojure-maven-plugin")
    (.setVersion "1.8.1")
    (.setExtensions true)
    (.setConfiguration clojure-maven-plugin-configuration)))

(def maven-shade-plugin-configuration 
  (let [config (Xpp3Dom. "configuration")
        transformers (Xpp3Dom. "transformers")
        transformer (doto (Xpp3Dom. "transformer")
                      (.setAttribute "implementation" "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"))
        manifest-entries (Xpp3Dom. "manifestEntries")
        main-class (doto (Xpp3Dom. "Main-Class")
                     (.setValue "${app.main.class}"))
        filters (Xpp3Dom. "filters")
        filter (Xpp3Dom. "filter")
        artifact (doto (Xpp3Dom. "artifact")
                   (.setValue "*:*"))
        excludes (Xpp3Dom. "excludes")
        exclude1 (doto (Xpp3Dom. "exclude")
                  (.setValue "META-INF/*.SF"))
        exclude2 (doto (Xpp3Dom. "exclude")
                  (.setValue "META-INF/*.DSA"))
        exclude3 (doto (Xpp3Dom. "exclude")
                  (.setValue "META-INF/*.RSA"))]
    (.addChild manifest-entries main-class) 
    (.addChild transformer manifest-entries)
    (.addChild transformers transformer)
    (.addChild excludes exclude1)
    (.addChild excludes exclude2)
    (.addChild excludes exclude3)
    (.addChild filter artifact)
    (.addChild filter excludes)
    (.addChild filters filter)
    (.addChild config transformers)
    (.addChild config filters)
    config))

(def maven-shade-plugin
  (let [execution (doto (PluginExecution.)
                    (.setPhase "package")
                    (.setGoals ["shade"]))
        _ (.addShutdownHook (Runtime/getRuntime)
                            (Thread. #(io/delete-file (io/file "dependency-reduced-pom.xml"))))]
    (doto (Plugin.)
      (.setGroupId "org.apache.maven.plugins")
      (.setArtifactId "maven-shade-plugin")
      (.setVersion "3.1.1")
      (.addExecution execution)
      (.setConfiguration maven-shade-plugin-configuration))))

(def maven-enforcer-plugin-configuration
  (let [config (Xpp3Dom. "configuration")
        rules (Xpp3Dom. "rules")
        dependency-convergence (Xpp3Dom. "dependencyConvergence")]
    (.addChild rules dependency-convergence)
    (.addChild config rules)
    config))

(def maven-enforcer-plugin
  (doto (Plugin.)
    (.setGroupId "org.apache.maven.plugins")
    (.setArtifactId "maven-enforcer-plugin")
    (.setVersion "3.0.0-M1")
    (.setConfiguration maven-enforcer-plugin-configuration)))
