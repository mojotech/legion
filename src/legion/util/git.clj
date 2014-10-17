(ns legion.util.git
  (:require
    [datomic.api :as d]
    [clojure.string :as string]
    [me.raynes.conch :refer [programs] :as sh]))

(programs git)

(defn call-git [dir & args]
  (try
    (apply git "--git-dir" dir (concat args [{:seq true}]))
    (catch clojure.lang.ExceptionInfo e nil)))

(defn- parse-instant [i]
  (java.util.Date. (* 1000 (Long. i))))

(def rev-format
  "commit HASH
   HASH COMMITED-TIME AUTHORED-TIME"
  "--pretty=format:%H %ct %at")

(defn parse-commit-line [line]
  (let [[hash authored commited] (string/split line #"\s")]
    {:hash hash
     :authored (parse-instant authored)
     :commited (parse-instant commited)}))

;; TODO: caching?
(defn commits
  "takes a path to a .git directory, and returns the commits, as an arrap of maps with the following keys:

    :commit/refs        => a vector of refs associated with a hash
    :commit/hash        => string representing the commit sha
    :commit/authored    => instant the commit was authored
    :commit/commited    => instant the commit was commited

  "
  [dir]
  (let [revs-by-hash (->>
                       (call-git dir "rev-list" "--all" rev-format)
                       ;; for each rev, two lines are returned. we don't care about the first
                       ;;  ...so we just pop off the head and return every other item
                       rest
                       (take-nth 2)
                       (map #(let [commit (parse-commit-line %)]
                              {(:hash commit) commit}))
                       (apply merge))
        refs-by-hash (->> (call-git dir "show-ref")
                          (map #(let [[hash ref] (string/split % #"\s")]
                                  (hash-map hash {:hash hash
                                                  :refs [ref]})))
                          (apply merge-with concat))]
    (vals
      (merge-with merge revs-by-hash refs-by-hash))))

(defn commit [dir id]
  (let [line (second
               (call-git dir "rev-list" "--max-count=1" rev-format id))]
    (parse-commit-line line)))

(defn resolve-ref [dir rev]
  (first (call-git dir "rev-parse" rev)))
