(ns legion.resource.commit
  (:require [legion.util.resource :as r]
            [legion.presenter.api :refer [read-commit]]
            [liberator.core :refer [defresource]]))

(defresource commit [{:keys [config]} {:keys [repo-id commit-id]}] r/defaults
  :exists? (fn [_]
             (when-let [commit (read-commit config repo-id commit-id)]
               {::commit commit}))
  :handle-ok ::commit)
