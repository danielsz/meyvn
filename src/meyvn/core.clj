(ns meyvn.core
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [meyvn.cljs :as cljs]
   [meyvn.configuration :refer [read-conf]]
   [meyvn.transient-pom :as transient]
   [meyvn.maven :as maven]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def cli-options
 [["-g" "--generate" "Write meyvn-pom.xml in current directory."]
  ["-h" "--help" "This help screen."]])

(defn usage [summary]
  (->> ["Meyvn. Better builds for Clojure."
        ""
        "Usage: myvn [options] action"
        ""
        "Options:"
        summary
        ""
        "Action must be a valid Maven action. See `meyvn.edn' in current directory for more options."
        "\n"]
       (str/join "\n")))

(defn exit [msg & {:keys [status] :or {status 0}}]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (when (:help options) (exit (usage summary)))
    (let [[deps-map conf] (read-conf)
          meyvn-pom (transient/extend-pom deps-map conf)]
      (if (:generate options)
        (exit "Pom file written to disk.")
        (do
          (cljs/compile deps-map (:cljs conf))
          (maven/invoke conf arguments)
          (.deleteOnExit meyvn-pom))))))
