(ns legion.resource.status
  (:require [legion.util.resource :as r]
            [liberator.core :refer [defresource]]))

(defresource status [_ _] r/defaults
  :handle-ok (constantly "ok"))
