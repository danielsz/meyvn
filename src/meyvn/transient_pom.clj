(ns meyvn.transient-pom
  (:require [meyvn.plugins :refer [clojure-maven-plugin maven-shade-plugin maven-enforcer-plugin]]
            [clojure.tools.deps.alpha.gen.pom :as gen.pom]
            [clojure.tools.deps.alpha.extensions.pom :as ext.pom]
            [clojure.java.io :as io]
            [clojure.data.xml :as xml])
  (:import [org.apache.maven.model.building FileModelSource]
           [org.apache.maven.model Extension Resource DistributionManagement DeploymentRepository]
           [org.apache.maven.model.io.xpp3 MavenXpp3Writer]
           [java.io File FileWriter]))

(def gen-pom #'gen.pom/gen-pom)

(def wagon-extension
  (doto (Extension.)
    (.setGroupId "org.apache.maven.wagon")
    (.setArtifactId "wagon-ssh-external")
    (.setVersion "3.0.0")))

(def resource
  (doto (Resource.)
    (.setDirectory "resources")))

(defn remote-repository [conf]
  (doto (DeploymentRepository.)
    (.setUrl (:url conf))
    (.setId "ssh-repository")))

(defn distribution-management [conf]
  (doto (DistributionManagement.)
    (.setRepository (remote-repository conf))))

(defn write-meyvn-pom [model]
  (let [tmp-file (io/file "meyvn-pom.xml")]
    (with-open [writer (FileWriter. tmp-file)]
      (.write (MavenXpp3Writer.) writer model))
    (.deleteOnExit tmp-file)))

(defn write-temp-pom [deps-map]
  (let [tmp-file (File/createTempFile "pom" ".xml")
        {:keys [deps paths :mvn/repos]} deps-map
        repos (remove #(= "https://repo1.maven.org/maven2/" (-> % val :url)) repos)
        project-name (.. (io/file ".") getCanonicalFile getName)
        pom (gen-pom deps paths repos project-name)]
    (with-open [writer (io/writer tmp-file)]
      (binding [*out* writer]
        (println (xml/indent-str pom))))
    (.deleteOnExit tmp-file)
    (comment (println (.getAbsolutePath tmp-file)))
    tmp-file))

(defn extend-pom [deps-map conf]
  (let  [pom-file (write-temp-pom deps-map)
         model (ext.pom/read-model (FileModelSource. pom-file) nil)
         build (.getBuild model)]
    (.setArtifactId model (:artifact-id (:pom conf)))
    (.setGroupId model (:group-id (:pom conf)))
    (.setVersion model (:version (:pom conf)))
    (.setPackaging model "clojure")
    (.addPlugin build maven-shade-plugin)
    (.addPlugin build clojure-maven-plugin)
    (.addPlugin build maven-enforcer-plugin) 
    (.setExtensions build [wagon-extension])
    (.setResources build [resource])
    (.setBuild model build)
    (.addProperty model "app.main.class" (:main-class conf))
    (.setDistributionManagement model (distribution-management (:remote-repository conf)))
    (write-meyvn-pom model)))
