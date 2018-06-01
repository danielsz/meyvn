(ns meyvn.cljs
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.deps.alpha :as tools.deps])
  (:import [java.lang ProcessBuilder]))

(defn- compile- [deps-map conf]
  (let [cljs-map (get-in deps-map [:aliases (:tools-deps-alias conf)])
        lib-map (tools.deps/resolve-deps deps-map cljs-map)
        cp (tools.deps/make-classpath lib-map (:paths deps-map) cljs-map)
        pb (ProcessBuilder. ["clj" "-Scp" cp "--main" "cljs.main" "--compile-opts" (:compiler-opts conf) "--compile" (:main-class conf)])]
    (.waitFor (-> pb .inheritIO .start))))

(defn compile [deps-map conf]
  (println "Compiling Clojurescript sources. This may take a while...")
  (let [rc (compile- deps-map conf)]
    (if (zero? rc)
      (println "Clojurescript compilation succeeded")
      (println "Clojurescript compilation failed"))))

