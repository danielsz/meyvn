(ns meyvn.transient-pom
  (:require
   [meyvn.finders :refer [find-git]]
   [meyvn.plugins :refer [clojure-maven-plugin maven-shade-plugin maven-enforcer-plugin]]
   [clojure.tools.deps.alpha.gen.pom :as gen.pom]
   [clojure.tools.deps.alpha.extensions.pom :as ext.pom]
   [clojure.java.io :as io]
   [clojure.data.xml :as xml])
  (:import [org.apache.maven.model.building FileModelSource]
           [org.apache.maven.model Extension Resource Repository DistributionManagement DeploymentRepository Scm]
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

(defn paths-as-resources [deps-map]
  (into [resource] (for [path (:paths deps-map)]
                     (doto (Resource.)
                       (.setDirectory path)))))

(defn repository [{:keys [id url]}]
  (doto (Repository.)
    (.setId id)
    (.setUrl url)))

(defn source-control [{:keys [tag url connection developer-connection]}]
  (doto (Scm.)
    (.setTag tag)
    (.setUrl url)
    (.setConnection connection)
    (.setDeveloperConnection developer-connection)))

(defn remote-repository [{:keys [id url]}]
  (doto (DeploymentRepository.)
    (.setId id)
    (.setUrl url)))

(defn distribution-management [conf]
  (doto (DistributionManagement.)
    (.setRepository (remote-repository conf))))

(defn write-meyvn-pom [model]
  (let [tmp-file (io/file "meyvn-pom.xml")]
    (with-open [writer (FileWriter. tmp-file)]
      (.write (MavenXpp3Writer.) writer model))
    tmp-file))

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

(defn extend-pom-scm [model conf]
  (when-let [git (and (:enabled conf) (find-git))]
    (let [scm (source-control {:tag (:sha git)
                               :url (:browse git)
                               :connection (str "scm:git:" (:public-clone git))
                               :developer-connection (str "scm:git:" (:dev-clone git))})]
      (.setScm model scm)))
  model)

(defn extend-pom-base- [deps-map conf]
  (let  [pom-file (write-temp-pom deps-map)
         model (ext.pom/read-model (FileModelSource. pom-file) nil)]
    (doto model
      (.setArtifactId (:artifact-id (:pom conf)))
      (.setGroupId (:group-id (:pom conf)))
      (.setVersion (:version (:pom conf))))))

(defn extend-pom-base [deps-map conf]
  (let [model (extend-pom-base- deps-map conf)]
    (extend-pom-scm model (:scm conf))))

(defmulti extend-pom (fn [deps-map conf] (some #(if (:enabled (val %)) (key %)) (:packaging conf))))

(defmethod extend-pom :uberjar [deps-map conf]
  (let [model (extend-pom-base deps-map conf)
        build (doto (.getBuild model)
                (.setPlugins [(maven-shade-plugin (get-in conf [:packaging :uberjar])) (clojure-maven-plugin deps-map) maven-enforcer-plugin])
                (.setExtensions [wagon-extension])
                (.setResources [resource]))]
    (.setPackaging model "clojure")
    (.setBuild model build)
    (.addProperty model "app.main.class" (get-in conf [:packaging :uberjar :main-class]))
    (.setDistributionManagement model (distribution-management (get-in conf [:packaging :uberjar :remote-repository])))
    (write-meyvn-pom model)))

(defmethod extend-pom :jar [deps-map conf]
  (let [deps-map (update-in deps-map [:deps] (fn [deps] (remove #(= (key %) 'org.clojure/clojure) deps)))
        model (extend-pom-base deps-map conf)
        build (.getBuild model)]
    (.setResources build (paths-as-resources deps-map))
    (.setBuild model build)
    (.setDistributionManagement model (distribution-management (get-in conf [:packaging :jar :remote-repository])))
    (write-meyvn-pom model)))

