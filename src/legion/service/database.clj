(ns legion.service.database
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [io.rkn.conformity :as c]
            [datomic.api :as d :refer [q]]
            [com.stuartsierra.component :as component :refer [start stop]]))

(defn migrations [file]
  (->> file io/resource io/file slurp (edn/read-string {:readers *data-readers*})))

(defn migrate [conn file & ks]
  (if (empty? ks)
    (c/ensure-conforms conn (migrations file))
    (c/ensure-conforms conn (migrations file) ks)))

(defrecord Database [config]
  component/Lifecycle

  (start [database]
    (if (:conn database)
      database
      (let [env ((:env config))
            uri (:database-uri env)]
        (d/create-database uri)
        (let [conn (d/connect uri)]
          (migrate conn "migrations.edn")
          (assoc database :conn conn)))))

  (stop [database]
    (when-let [conn (:conn database)]
      (d/release conn))
    (dissoc database :conn)))

(defn database [] (map->Database {}))
