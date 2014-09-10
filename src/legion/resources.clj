(ns legion.resources
  (:require [legion.git :as git]
            [legion.datomic :as datomic]
            [clojure.pprint :as pprint :refer [pprint]]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [datomic.api :as d :refer [db q]]
            [liberator.core :refer [defresource]]
            [liberator.representation :refer [as-response]]))

(def config (datomic/read-resource "config.edn"))

(defprotocol Output
  "liberator doesn't know how to coerce all output formats...
   most importantly the java.util.HashSets returned by datomic, so we do it ourselves."
  (coerce [this] "coerce output"))

(extend-protocol Output
  java.util.HashSet
  (coerce [this] (vec this))

  Object
  (coerce [this] this))

(defn get-repo [repo-id]
  (-> repo-id keyword config))

(defn get-commit [repo-id commit-id]
  (when-let [repo (get-repo repo-id)]
    (git/resolve-ref (:git-dir repo) commit-id)))

(defn create-task [task]
  ;; this seems janky, need a decent creation abstraction
  (let [{:keys [db-after tx-data tempids]} @(d/transact
                                              (datomic/scratch-conn)
                                              [{:db/id #db/id[:db.part/user -1]
                                                :task/type (:type task)
                                                :task/state :task.state/created
                                                :task/repo (:repo task)
                                                :task/commit (:commit task)}])
        task-id (-> tempids vals first)]
    (first (datomic/find db-after :task :id task-id))))

(defn body-as-json [ctx]
  (-> ctx :request :body slurp json/read-str keywordize-keys))

(def resource-defaults
  {:allowed-methods #{:get}
   :available-media-types #{"text/plain" "application/json"}
   :as-response (fn [d ctx]
                  (as-response (coerce d) ctx))
   :handle-exception pprint})

(defresource default [_] resource-defaults
  :exists? (constantly false))

(defresource status [_] resource-defaults
  :handle-ok (constantly "ok"))

(defresource repos [_] resource-defaults
  :handle-ok (fn [_] config))

(defresource repo [{:keys [repo-id]}] resource-defaults
  :exists? (fn [_]
             (when-let [repo (get-repo repo-id)]
               {::repo repo}))
  :handle-ok ::repo)

(defresource commands [{:keys [repo-id]}] resource-defaults
  :handle-ok (fn [_]
               (when-let [repo (get-repo repo-id)]
                 (:commands repo))))

(defresource commits [{:keys [repo-id]}] resource-defaults
  :handle-ok (fn [_]
               (when-let [db* (-> repo-id get-repo :git-dir git/db)]
                 (datomic/find db* :commit))))

(defresource commit [{:keys [repo-id commit-id]}] resource-defaults
  :exists? (fn [_]
             (when-let [commit (get-commit repo-id commit-id)]
               {::commit commit}))
  :handle-ok ::commit)

(defresource tasks [{:keys [repo-id commit-id]}] resource-defaults
  :allowed-methods #{:get :post}
  :post! (fn [ctx]
           (when-let [commit-id (get-commit repo-id commit-id)]
             (when-let [body (body-as-json ctx)]
               {::task (create-task
                         (assoc body :commit commit-id :repo repo-id))})))
  :handle-ok (fn [_]
               (when-let [commit-id (get-commit repo-id commit-id)]
                 (datomic/find (datomic/scratch-db) :task :commit commit-id :repo repo-id)))
  :handle-created ::task)

(defresource task [{:keys [task-id]}] resource-defaults
  :exists? (fn [_]
             (when-let [task (datomic/find-one (datomic/scratch-db) :task :id (json/read-str task-id))]
               {::task task}))
  :handle-ok ::task)

(comment

  ;; get task
  ;; curl -i localhost:3000/api/v1/repo/bunsen/commit/HEAD/task/17592186045480

  ;; create task
  ;; curl -i -H "Content-Type: application/json" -X POST -d '{"type": "build"}' localhost:3000/api/v1/repo/bunsen/commit/HEAD/task

  (datomic/find (datomic/scratch-db) :task)

  (datomic/find (git/db "../Bunsen/.git") :commit)
  (datomic/find (git/db "../Bunsen/.git") :commit :id "bdb3362773bb7871a0ccd1343e89ae857778a8b3")

  )
