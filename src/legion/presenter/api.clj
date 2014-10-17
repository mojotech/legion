(ns legion.presenter.api
  (:require [legion.util.git :as git]
            [legion.util.datomic :as datomic]
            [legion.model.task :as task]
            [datomic.api :as d :refer [q]]))

(defn list-repos [config]
  (-> ((:repo config)) :repos keys))

(defn read-repo [config repo-id]
  (let [name (keyword repo-id)]
    (-> ((:repo config)) :repos name)))

;; FIXME: why not just use read-repo to get commands
(defn list-commands [config repo-id]
  (-> (read-repo config repo-id) :commands))

(defn list-commits [config repo-id]
  (-> (read-repo config repo-id) :path git/commits))

(defn read-commit [config repo-id commit-id]
  (-> (read-repo config repo-id) :path (git/commit commit-id)))

(defn list-tasks [config repo-id commit-id])

(defn create-task [config database repo-id commit-id data]
  (let [repo-name (-> (read-repo config repo-id) :name)
        commit-hash (-> (read-commit config repo-id commit-id) :hash)
        task-data (assoc data
                         :repo repo-name
                         :commit commit-hash)
        create-tx (task/create task-data)
        {:keys [db-after tempids]} (d/transact (:conn database) create-tx)
        id (d/resolve-tempid db-after (first tempids))]
    (assoc task-data :id id)))

(defn read-task [config repo-id commit-id task-id])

(defn update-task [config repo-id commit-id task-id data])

(defn delete-task [config repo-id commit-id task-id])
