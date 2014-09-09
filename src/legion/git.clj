(ns legion.git
  (:require
    [clojure.string :as string]
    [me.raynes.conch :refer [programs] :as sh]))

(def ^:dynamic default-dir ".git")

(programs git)

(defn call-git [dir & args]
  (try
    (apply git "--git-dir" dir (concat args [{:seq true}]))
    (catch clojure.lang.ExceptionInfo e nil)))

(defn db
  "takes a path to a .git directory, and returns an (almost) datomic database (E/A/V/T minus the transaction id):

    [hash :commit/ref #string] (refs associated with a commit hash, cardinality many)
    [hash :commit/authored #instant] (instant the commit was authored)
    [hash :commit/commited #instant] (instant the commit was commited)

    example query, find all commits pointed to by a ref containing master, before 3 days ago (using datomic and clj-time libraries):

    (q '[:find ?e ?r
        :in $ ?before
        :where
        [?e :commit/ref ?r]
        [(.contains ?r \"master\")]
        [?e :commit/authored ?t]
        [(.before ?t ?before)]]
      (db \"../Bunsen/.git\")
      (-> 3 t/days t/ago c/to-date))

  "
  ([] (db default-dir))
  ([dir]
   (let [revs (->>
                ;; for each rev, two lines are returned:
                ;;
                ;;   commit HASH
                ;;   HASH COMMITED-TIME AUTHORED-TIME
                ;;
                ;; we don't care about the first line...
                (call-git dir "rev-list" "--all" "--pretty=format:%H %ct %at")
                ;;  ...so we just pop off the head and return every other item
                rest
                (take-nth 2))
         refs (call-git dir "show-ref")]
     (vec (concat
            (mapcat #(let [[id ref] (string/split % #"\s")]
                       [[id :commit/ref ref]]) refs)
            (mapcat #(let [[id authored commited] (string/split % #"\s")]
                       [[id :commit/authored (java.util.Date. (* 1000 (Long. authored)))]
                        [id :commit/commited (java.util.Date. (* 1000 (Long. commited)))]]) revs))))))

(defn resolve-ref
  ([rev] (resolve-ref default-dir rev))
  ([dir rev]
   (first (call-git dir "rev-parse" rev))))

(comment

  (q '[:find ?e ?r
       :in $ ?before
       :where
       [?e :commit/ref ?r]
       [(.contains ?r "master")]
       [?e :commit/authored ?t]
       [(.before ?t ?before)]]
     (legion.server.git/db "../Bunsen/.git")
     (-> 3 t/days t/ago c/to-date))

  )
