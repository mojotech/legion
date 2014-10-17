(ns legion.resource.tasks
  (:require [legion.util.resource :as r]
            [legion.presenter.api :refer [list-tasks]]
            [liberator.core :refer [defresource]]))

(defresource tasks [{:keys [config database]} {:keys [repo-id commit-id]}] r/defaults
  :allowed-methods #{:get :post}
  :post! (fn [ctx]
           #_(let [body (r/extract-body ctx)
                 repo (repo/find config repo-id)
                 commit (commit/find repo commit-id)]
             (when-let [task (task/create database repo commit body)]
               {::task task})))
  :handle-ok (fn [_]
               #_(let [repo (repo/find config repo-id)
                     commit (commit/find repo commit-id)]
                 (task/list database commit)))
  :handle-created ::task)

(comment

;; curl -v -X POST --header "Content-Type:application/json" -d '{"type":"build"}' localhost:8080/api/v1/repo/bunsen/commit/1c18c274c0897bd4efb0aa073bb84408f13ecf93/task

  )
