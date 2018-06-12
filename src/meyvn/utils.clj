(ns meyvn.utils
  (:import [java.util Scanner]))

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
  
  )
