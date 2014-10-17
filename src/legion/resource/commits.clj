(ns legion.resource.commits
  (:require [legion.util.resource :as r]
            [legion.presenter.api :refer [list-commits]]
            [liberator.core :refer [defresource]]))

(defresource commits [{:keys [config]} {:keys [repo-id]}] r/defaults
  :handle-ok (fn [_] (list-commits config repo-id)))
