(ns legion.model.task
  (:refer-clojure :exclude [read])
  (:require [clojure.set :refer [rename-keys]]
            [schema.core :as s]
            [datomic.api :as d :refer [q]]))

(def schema
  {(s/optional-key :id) (s/either
                          s/Num
                          datomic.Entity
                          datomic.db.DbId
                          [(s/one s/Keyword "attribute")
                           (s/one s/Str "value")])
   :type s/Str
   :state (s/enum :starting :started :stopping :stopped :crashed)
   :repo s/Str
   :commit s/Str
   (s/optional-key :params) (s/maybe {s/Keyword s/Str})})

(defn create
  [t]
  (let [t
        id (or (:id t) (d/tempid :db.part/user))]
    (concat
      (when (:params t)
        (map
          (fn [[k v]]
            {:db/id (d/tempid :db.part/user)
             :task/_params id
             :task.params/key k
             :task.params/val v})
          (:params t)))
      (map
        (fn [[k v]]
          [:db/add id (keyword "task" (name k)) v])
        (-> t
            (dissoc :id :params)
            (assoc :state :starting))))))

(defn read
  "takes an entity and returns a model"
  [e]
  (let [i (:db/id e)
        t (->> (d/touch e)
               (map
                 (fn [[k v]] [(-> k name keyword) v]))
               (into {:id i}))]
    (update-in t [:params] #(->> %
                                 (map
                                   (juxt :task.params/key :task.params/val))
                                 (into {})))))

(defn claim [t]
  [[:db.fn/cas (:id t) :task/state :starting :started]])

(comment

  (d/delete-database (:database-uri ((-> user/*app* :config :env))))

  (let [t {:type "build"
           :repo "a repo"
           :commit "a commit"
           :params {:name "value"}}
        c (-> user/*app* :database :conn)
        d (d/db c)
        e (first (first
                   (q '[:find ?t
                        :where [?t :task/type "build"]] d)))]
    ;; (./aprint (create t))
    ;;
    ;; @(d/transact c (create t))
    ;; (./aprint i)

    ;; (d/touch (d/entity d e))

    (read (d/entity d e))
    ;; @(d/transact c (claim (read (d/entity d e))))

    )

  )
