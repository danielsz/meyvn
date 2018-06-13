(ns meyvn.utils
  (:require
   [nolipservice.core :as nlp]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.java.browse :refer [browse-url]])
  (:import [java.util Scanner InputMismatchException]
           [java.util UUID]))

(def meyvn-dir (io/file (str (System/getProperty "user.home") "/.meyvn")))

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

(def report (let [uuid (.getName (first (.listFiles meyvn-dir)))]
              (partial nlp/report uuid)))

(defn opt-in []
  (when-not (.exists meyvn-dir)
    (println "Meyvn sends the POM’s group ID and success result of each execution back to an analytics server. Do you accept? (Y/N)")
    (let [resp (str/lower-case (prompt))]
      (case resp
        "y" (let [uuid-file (str meyvn-dir "/" (UUID/randomUUID))]
              (and (io/make-parents uuid-file) (spit uuid-file "")))
        "n" (do (browse-url "https://github.com/danielsz/meyvn#sustainable-open-source")
                (exit "Sorry, currently there is no other way to make this work."))))))
