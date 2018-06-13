(ns meyvn.utils
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.browse :refer [browse-url]])
  (:import [java.util Scanner InputMismatchException]))

(defn exit [msg & {:keys [status] :or {status 0}}]
  (println msg)
  (System/exit status))

(defn prompt []
  (let [sc (Scanner. (System/in))
        resp (try (-> sc
                      (.next (re-pattern "[Yy]|[Nn]")))
                  (catch InputMismatchException e
                    (println "Please answer [Y]es or [N]o")
                    nil))]
    (or resp (recur))))

(defn opt-in []
  (let [meyvn-dir (io/file (str (System/getProperty "user.home") "/.meyvn"))]
    (when-not (.exists meyvn-dir)
      (println "Meyvn sends the POM’s group ID and success result of each execution back to an analytics server. Do you accept? (Y/N)")
      (let [resp (str/lower-case (prompt))]
        (case resp
          "y" (.mkdir meyvn-dir)
          "n" (do (browse-url "https://github.com/danielsz/meyvn#sustainable-open-source")
                  (exit "Sorry, currently there is no other way to make this work.")))))))
