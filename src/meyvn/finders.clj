(ns meyvn.finders
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [meyvn.utils :refer [exit]])
  (:import [java.nio.file FileSystems]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.lib Constants]
           [org.eclipse.jgit.transport URIish]
           [org.eclipse.jgit.errors RepositoryNotFoundException]))

(defn find-env []
  (when-not (System/getenv "M2_HOME")
    (exit "M2_HOME not set in environment" :status 1)))

(defn find-exec []
  (let [rc (sh "which" "mvn")]
    (if (not (zero? (:exit rc)))
      (exit "Cannot find Maven executable!" :status 1)
      (io/file (str/trim (:out (sh "which" "mvn")))))))

(defn find-git []
  (when-let [git (try (Git/open (io/file "."))
                      (catch RepositoryNotFoundException e nil))]
    (let [repo (.getRepository git)
          config (.getConfig repo)
          last-commit (.resolve repo Constants/HEAD)
          uri (URIish. (.getString config "remote" "origin" "url"))]
      {:sha (.getName last-commit)
       :branch (.getBranch repo)
       :state (.getDescription (.getRepositoryState repo))
       :uri (str uri)
       :browse (str "https://" (.getHost uri) "/" (first (str/split (.getPath uri) #"\.")))})))

(defn find-file [s]
  (let [f (io/file s)]
    (when (.exists f )
      f)))

(defn find-user-deps []
  (let [user-dir (or (System/getenv "CLJ_CONFIG")
                     (System/getenv "XDG_CONFIG_HOME")
                     (str (System/getProperty "user.home") "/.clojure"))]
    (find-file (str user-dir "/deps.edn"))))

(defn find-local-deps []
  (find-file "deps.edn"))

(defn find-global-deps []
  (let [linux (io/file "/usr/local/lib/clojure/deps.edn")
        mac (io/file "/usr/local/Cellar/clojure")]
    (cond
      (.exists linux)  linux
      (.exists mac)  (let [grammar (.getPathMatcher
                                    (FileSystems/getDefault)
                                    "glob:/usr/local/Cellar/clojure/*/deps.edn")
                           files (file-seq mac)]
                       (first (filter #(.matches grammar (.toPath %)) files))))))
