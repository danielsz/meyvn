(ns meyvn.plugins
  (:require [clojure.tools.deps.alpha.reader :as tools.reader]
            [clojure.java.io :as io])
  (:import [org.codehaus.plexus.util.xml Xpp3Dom]
           [org.apache.maven.model Plugin PluginExecution Dependency]))

(def data-readers-transformer
  (doto (Dependency.)
    (.setGroupId "org.danielsz")
    (.setArtifactId "shade-edn-data-readers-transformer")
    (.setVersion "1.0.0")))

(defn clojure-maven-plugin-configuration [paths]
  (let [config (Xpp3Dom. "configuration")
        src-directories (Xpp3Dom. "sourceDirectories")]
    (doseq [path paths
          :let [src-directory (Xpp3Dom. "sourceDirectory")]]
      (.setValue src-directory path)
      (.addChild src-directories src-directory))
    (.addChild config src-directories)
    config))

(defn clojure-maven-plugin [deps-map]
  (doto (Plugin.)
    (.setGroupId "com.theoryinpractise")
    (.setArtifactId "clojure-maven-plugin")
    (.setVersion "1.8.1")
    (.setExtensions true)
    (.setConfiguration (clojure-maven-plugin-configuration (:paths deps-map)))))

(defn artifacts [artifacts]
  (let [artifact-set (Xpp3Dom. "artifactSet")
        excludes-set (Xpp3Dom. "excludes")
        children (for [artifact artifacts]
                   (doto (Xpp3Dom. "exclude")
                     (.setValue artifact)))]
    (doseq [child children]
      (.addChild excludes-set child))
    (.addChild artifact-set excludes-set)
    artifact-set))

(defn filters [filters]
  (let [filter-set (Xpp3Dom. "filters")
        filter (Xpp3Dom. "filter")
        artifact (doto (Xpp3Dom. "artifact")
                   (.setValue "*:*"))
        excludes-set (Xpp3Dom. "excludes")
        children (for [filter filters]
                   (doto (Xpp3Dom. "exclude")
                     (.setValue filter)))]
    (doseq [child children]
      (.addChild excludes-set child))
    (.addChild filter artifact)
    (.addChild filter excludes-set)
    (.addChild filter-set filter)
    filter-set))

(defn maven-shade-plugin-configuration [conf]
  (let [config (Xpp3Dom. "configuration")
        transformers (Xpp3Dom. "transformers")
        manifest-transformer (doto (Xpp3Dom. "transformer")
                               (.setAttribute "implementation" "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"))
        manifest-entries (Xpp3Dom. "manifestEntries")
        main-class (doto (Xpp3Dom. "Main-Class")
                     (.setValue "${app.main.class}"))
        appending-transformer (doto (Xpp3Dom. "transformer")
                                (.setAttribute "implementation" "org.danielsz.shade.resource.EdnDataReaderTransformer"))
        resource (doto (Xpp3Dom. "resource")
                   (.setValue "data_readers.clj"))]
    (.addChild manifest-entries main-class) 
    (.addChild manifest-transformer manifest-entries)
    (.addChild appending-transformer resource)
    (.addChild transformers manifest-transformer)
    (.addChild transformers appending-transformer)    
    (.addChild config transformers)
    (.addChild config (filters (get-in conf [:excludes :filters])))
    (.addChild config (artifacts (get-in conf [:excludes :artifacts])))
    config))

(defn maven-shade-plugin [conf]
  (let [execution (doto (PluginExecution.)
                    (.setPhase "package")
                    (.setGoals ["shade"]))
        _ (.addShutdownHook (Runtime/getRuntime)
                            (Thread. #(let [f (io/file "dependency-reduced-pom.xml")]
                                        (when (.exists f) (io/delete-file f)))))]
    (doto (Plugin.)
      (.setGroupId "org.apache.maven.plugins")
      (.setArtifactId "maven-shade-plugin")
      (.setVersion "3.1.1")
      (.addExecution execution)
      (.setConfiguration (maven-shade-plugin-configuration conf))
      (.setDependencies [data-readers-transformer]))))

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
