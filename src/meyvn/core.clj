(ns meyvn.core
  (:require
   [meyvn.plugins :refer [clojure-maven-plugin maven-shade-plugin maven-enforcer-plugin]]
   [clojure.tools.deps.alpha.gen.pom :as gen.pom]
   [clojure.tools.deps.alpha.extensions.pom :as ext.pom]
   [clojure.tools.deps.alpha :as tools.deps :refer [print-tree]]
   [clojure.tools.deps.alpha.reader :as tools.reader]
   [clojure.java.io :as io]
   [clojure.data.xml :as xml]
   [clojure.java.shell :refer [sh]]
   [clojure.edn :as edn]
   [clojure.string :as str])
  (:import [org.apache.maven.model.building FileModelSource]
           [org.apache.maven.model Extension Resource DistributionManagement DeploymentRepository]
           [org.apache.maven.model.io.xpp3 MavenXpp3Writer]
           [java.io File FileWriter]
           [org.apache.maven.shared.invoker DefaultInvoker DefaultInvocationRequest InvocationResult]
           [java.lang ProcessBuilder]))

(defn find-env []
  (when (not (System/getenv "M2_HOME"))
    (println "M2_HOME not set in environment")
    (System/exit 1)))

(defn find-exec []
  (let [rc (sh "which" "mvn")]
    (if (not (zero? (:exit rc)))
      (do (println "Cannot find Maven executable!")
          (System/exit 1))
      (io/file (str/trim (:out (sh "which" "mvn")))))))

(defn find-conf []
  (when (not (.exists (io/file "meyvn.edn")))
    (println "No config found")
    (System/exit 1)))

(def conf
  (-> "meyvn.edn"
      slurp
      edn/read-string))

(def gen-pom #'gen.pom/gen-pom)

(def deps-map (tools.reader/read-deps [(io/file "/usr/local/lib/clojure/deps.edn")
                                       (io/file "/home/arch/daniel/.clojure/deps.edn")
                                       (io/file "deps.edn")]))

(defn cljs-compile []
  (let [lib-map (tools.deps/resolve-deps deps-map (:cljs (:aliases deps-map)))
        cp (tools.deps/make-classpath lib-map (:paths deps-map) (:cljs (:aliases deps-map)))
        pb (ProcessBuilder. ["clj" "-Scp" cp "--main" "cljs.main" "--compile-opts" "cljsc_opts.edn" "--compile" (:main-class (:cljs conf))])
        rc (.waitFor (-> pb .inheritIO .start))]
    (if (zero? rc)
      (println "Clojurescript compilation succeeded")
      (println "Clojurescript compilation failed"))))

(def wagon-extension
  (doto (Extension.)
    (.setGroupId "org.apache.maven.wagon")
    (.setArtifactId "wagon-ssh-external")
    (.setVersion "3.0.0")))

(def resource
  (doto (Resource.)
    (.setDirectory "resources")))

(def remote-repository
  (doto (DeploymentRepository.)
    (.setUrl "scpexe://danielsz@gad.tuppu.net:/home/danielsz/.m2/repository")
    (.setId "ssh-repository")))

(def distribution-management
  (doto (DistributionManagement.)
    (.setRepository remote-repository)))

(defn write-meyvn-pom [model]
  (with-open [writer (FileWriter. (io/file "meyvn-pom.xml"))]
    (.write (MavenXpp3Writer.) writer model)))

(defn write-temp-pom []
  (let [temp-file (File/createTempFile "pom" ".xml")
        {:keys [deps paths :mvn/repos]} deps-map
        repos (remove #(= "https://repo1.maven.org/maven2/" (-> % val :url)) repos)
        project-name (.. (io/file ".") getCanonicalFile getName)
        pom (gen-pom deps paths repos project-name)]
    (with-open [writer (io/writer temp-file)]
      (binding [*out* writer]
        (println (xml/indent-str pom))))
    (.deleteOnExit temp-file)
    (println (.getAbsolutePath temp-file))
    temp-file))

(defn invoke [goal]
  (find-env)
  (let [invocation-request
        (doto (DefaultInvocationRequest.)
          (.setPomFile (io/file "meyvn-pom.xml"))
          (.setGoals (list goal))
          (.setBatchMode true))        
        invoker (DefaultInvoker.)]
    (.execute invoker invocation-request)))

(defn extend-pom []
  (let  [pom-file (write-temp-pom)
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
    (.setDistributionManagement model distribution-management)
    (write-meyvn-pom model)))

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
  ;(cljs-compile)
  (extend-pom)
  (invoke-maven args))

(comment
  (let [deps-map (tools.reader/slurp-deps "deps.edn")
        lib-map (tools.deps/resolve-deps deps-map {} )
        class-path (tools.deps/make-classpath lib-map [] {})]
    (println class-path)
    (println (type model))
    (println deps-map)
    (print-tree lib-map)))
