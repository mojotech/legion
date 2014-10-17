(ns legion.util.resource
  (:require [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.data.json :as json :refer [JSONWriter -write]]
            [liberator.dev :refer [wrap-trace]]
            [liberator.core :refer [defresource]])
  (:import (java.io PrintWriter)))

(extend-protocol JSONWriter
  java.util.Date
  (-write [date #^PrintWriter out]
    (.print out (json/json-str (str date)))))

(defn middleware [config handler]
  (if (:trace? config)
    (wrap-trace handler :ui :header) identity))

(defn extract-body [ctx]
  (-> ctx :request :body slurp json/read-str keywordize-keys))

(def defaults
  {:allowed-methods #{:get}
   :available-media-types #{"text/plain" "application/json"}
   :handle-exception clojure.pprint/pprint})
