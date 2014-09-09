(ns legion.datomic
  (:refer-clojure :exclude [find])
  (:require
            [clojure.java.io :as io]
            [clojure.string :as string]
            [datomic.api :as d :refer [db q]]))

(defn read-resource [r]
  (read-string (slurp (io/resource r))))

(def rules (read-resource "rules.edn"))
(def schema (read-resource "schema.edn"))
(def config (read-resource "database.edn"))

(defn migrate [schema]
  (let [uri (:uri config)]
    (d/create-database uri)
    (d/transact (d/connect uri) schema)))

(defn scratch-conn []
  (d/connect (:uri config)))

(defn scratch-db []
  (db (scratch-conn)))

(defn installed-attributes [db]
  (apply concat
    (q '[:find ?v
         :where
         [_ :db.install/attribute ?a]
         [?a :db/ident ?v]]
     db)))

(defn- keyword->symbol [k]
  (->> k name (str "?") symbol))

(defn- symbol->keyword [s]
  (-> s
      (string/replace-first #"\?" "")
      keyword))

(defn find
  "given a database, a rule name, and map of attribute name/value pairs...
  return a list of entities as a map of attribute name/value pairs"
  [db rule-name & {:as attrs}]
  (let [attrs (or attrs {})
        rule (->>
               rules
               (map first)
               (filter
                 #(= (-> % first)
                     (-> rule-name name symbol)))
               first)
        vars (rest rule)
        query (concat
                (list* :find vars)
                (list* :in '$ '% (map keyword->symbol (keys attrs)))
                (list* :where [rule]))
        attr-names (map symbol->keyword vars)]
    (->>
      (apply q query db rules (vals attrs))
      (map
        #(apply assoc
                {}
                (interleave attr-names %))))))

(defn find-one [& args]
  (first
    (apply find args)))

(comment
  "some example queries"

  (migrate schema)

  (find (scratch-db) :task)
  (find (scratch-db) :task :id 17592186045425)
  (find (scratch-db) :task :type "build" :id 17592186045425)

)
