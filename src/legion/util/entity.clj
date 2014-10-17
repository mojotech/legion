(ns legion.util.entity
  (:refer-clojure :exclude [find read ensure])
  (:require [schema.core :as s]
            [datomic.api :as d :refer [q]])
  (:import (datomic.db DbId)))

(def ref-schema
  (s/either
    s/Num
    datomic.Entity
    datomic.db.DbId
    [(s/one s/Keyword "attribute")
     (s/one s/Str "value")]))

(defprotocol Entity
  (exists? [this db])
  (create [this])
  (read [this db])
  (update [this])
  (delete [this]))

(defn entity? [e]
  (instance? datomic.Entity e))

(defn entity-exists? [e db]
  (let [id (:db/id
             (d/entity db (:db/id e)))]
    (and id
         (not
           (empty?
             (q '[:find ?id
                  :in $ ?id
                  :where [?id]] db id))))))

(defn read-entity
  ([e]
   (into {} (d/touch e)))
  ([e db]
   (read-entity (d/entity db e))))

(defn create-entity
  ([e] [e])
  ([f* e] [[f* e]]))

(defn update-entity
  ([e] [e])
  ([f* e] [[f* e]]))

(defn delete-entity
  ([e] [[:db.fn/retractEntity (:db/id e)]])
  ([f* e] [[f* e]]))

(defn identify [x]
  (try
    (s/validate ref-schema x)
    (catch Exception e
      (d/tempid :db.part/user))))

(defn construct-model [k construct convert data]
  (let [e (cond
            (map? data) (construct data)
            (instance? legion.util.entity.Entity data) data
            (entity? data) (construct
                             (-> data read-entity convert))
            :else (construct {k data}))]
    (update-in e [k] identify)))

(defn id-query [constraints & vars]
  [:find '?id
   :in (apply vector '$ '% vars)
   :where constraints])

(defn find* [query db & args]
  (map first
       (apply q query db args)))

(def find
  (comp first find*))

(defn find-by* [attr db value]
  (find* '[:find ?id
           :in $ ?attr ?val
           :where [?id ?attr ?val]] db attr value))

(def find-by
  (comp first find-by*))

(defn finder*
  ([query] (finder* query []))
  ([query rules]
   (cond
     (keyword? query) (partial find-by* query)
     (sequential? query) (fn [db & args]
                           (map first
                                (apply q
                                       (apply id-query query) db rules args))))))

(def finder
  (comp #(comp first %) finder*))

(defn ensure [e db]
  (if (exists? e db) [] (create e)))
