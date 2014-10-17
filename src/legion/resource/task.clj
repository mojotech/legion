(ns legion.resource.task
  (:require [legion.util.resource :as r]
            [legion.presenter.api :refer [create-task read-task update-task delete-task]]
            [liberator.core :refer [defresource]]))

(defresource task [{:keys [task-id]}] r/defaults
  :exists? (fn [_]
             #_(when-let [task (datomic/find-one (datomic/scratch-db) :task :id (json/read-str task-id))]
               {::task task}))
  :handle-ok ::task)
