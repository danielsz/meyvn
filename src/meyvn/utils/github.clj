(ns meyvn.utils.github)

(defn parse-github-url
  "Parses a GitHub URL returning a [username repo] pair."
  [url]
  (if url
    (next
     (or (re-matches #"(?:[A-Za-z-]{2,}@)?github.com:([^/]+)/([^/]+).git" url)
         (re-matches #"[^:]+://(?:[A-Za-z-]{2,}@)?github.com/([^/]+)/([^/]+).git" url)))))

(defn github-urls [url]
  (if-let [[user repo] (parse-github-url url)]
    {:public-clone (str "git://github.com/" user "/" repo ".git")
     :dev-clone (str "ssh://git@github.com/" user "/" repo ".git")
     :browse (str "https://github.com/" user "/" repo)}))
