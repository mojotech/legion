(ns legion.resource.repo
  (:require [legion.util.resource :as r]
            [legion.presenter.api :refer [read-repo]]
            [liberator.core :refer [defresource]]))

(defresource repo [{:keys [config]} {:keys [repo-id]}] r/defaults
  :exists? (fn [_]
             (when-let [repo (read-repo config repo-id)]
               {::repo repo}))
  :handle-ok ::repo)
