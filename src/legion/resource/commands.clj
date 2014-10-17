(ns legion.resource.commands
  (:require [legion.util.resource :as r]
            [legion.presenter.api :refer [list-commands]]
            [liberator.core :refer [defresource]]))

(defresource commands [{:keys [config]} {:keys [repo-id]}] r/defaults
  :handle-ok (fn [_] (list-commands config repo-id)))
