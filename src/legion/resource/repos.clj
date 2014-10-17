(ns legion.resource.repos
  (:require [legion.util.resource :as r]
            [legion.presenter.api :refer [list-repos]]
            [liberator.core :refer [defresource]]))

(defresource repos [{:keys [config]} _] r/defaults
  :handle-ok (fn [_] (list-repos config)))
