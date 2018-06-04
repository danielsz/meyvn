(ns meyvn.core
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [meyvn.cljs :as cljs]
   [meyvn.sanitation :as sanitation]
   [meyvn.transient-pom :as transient]
   [meyvn.maven :as maven]
   [clojure.java.io :as io]
   [clojure.tools.deps.alpha.reader :as tools.reader]
   [clojure.string :as str]))

(def conf)

(def deps-map
  (let [config-dir (or (System/getenv "CLJ_CONFIG")
                       (System/getenv "XDG_CONFIG_HOME")
                       (str (System/getProperty "user.home") "/.clojure"))]
    (tools.reader/read-deps [(io/file "/usr/local/lib/clojure/deps.edn")
                             (io/file (str config-dir "/deps.edn"))
                             (io/file "deps.edn")])))

(def cli-options
 [["-g" "--generate" "Write meyvn-pom.xml in current directory."]
  ["-h" "--help"]])

(defn usage [summary]
  (->> ["Meyvn. Better builds for Clojure."
        ""
        "Usage: myvn [options] action"
        ""
        "Options:"
        summary
        ""
        "Action must be a valid Maven action."
        "Configuration can be found in meyvn.edn"]
       (str/join "\n")))

(defn exit [msg & {:keys [status] :or {status 0}}]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (when (:help options) (exit (usage summary)))
    (sanitation/checks)
    (let [meyvn-pom (transient/extend-pom deps-map conf)]
      (if (:generate options)
        (exit "Pom file written to disk.")
        (do
          (cljs/compile deps-map (:cljs conf))
          (maven/invoke arguments)
          (.deleteOnExit meyvn-pom))))))
