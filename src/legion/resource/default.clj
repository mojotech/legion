(ns legion.resource.default
  (:require [legion.util.resource :as r]
            [liberator.core :refer [defresource]]))

(defresource default [_ _] r/defaults
  :exists? (constantly false))
