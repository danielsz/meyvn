(ns meyvn.utils)

(defn exit [msg & {:keys [status] :or {status 0}}]
  (println msg)
  (System/exit status))
