(ns meyvn.cljs
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.deps.alpha :as tools.deps]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import [java.lang ProcessBuilder]))

(defn delete-recursively [path]
  (doseq [f (reverse (file-seq (io/file path)))]
     (io/delete-file f)))

(defn cleanup [conf]
  (let [opts (:compiler-opts conf)
        opts (condp #(%1 %2) opts
               string? (edn/read-string (slurp opts))
               map? opts
               nil)]
    (when (and (= :advanced (:optimizations opts)) (:output-dir opts))
      (println "Cleaning up" (:output-dir opts) "directory")
      (delete-recursively (:output-dir opts)))))

(defn- compile- [deps-map conf]
  (let [cljs-map (get-in deps-map [:aliases (:deps.edn-alias conf)])
        lib-map (tools.deps/resolve-deps deps-map cljs-map)
        cp (tools.deps/make-classpath lib-map (:paths deps-map) cljs-map)
        pb (ProcessBuilder. ["clj" "-Scp" cp "--main" "cljs.main" "--compile-opts" (str (:compiler-opts conf)) "--compile" (:main-class conf)])]
    (.waitFor (-> pb .inheritIO .start))))

(defn compile [deps-map conf]
  (when (:enabled conf)
    (println "Compiling Clojurescript sources. This may take a while...")
    (let [rc (compile- deps-map conf)]
      (if (zero? rc)
        (do (println "Clojurescript compilation succeeded.")
            (cleanup conf))
        (do (println "Clojurescript compilation failed.")
            (System/exit 1))))))

