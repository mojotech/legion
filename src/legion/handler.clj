(ns legion.handler
  (:require
            [bidi.bidi :as bidi]
            [liberator.dev :as liberator]
            [legion.datomic :as datomic]
            [legion.routes :refer [routes] :as routes]
            [legion.resources :as resources]))

(def handlers
  "map named routes to resources"
  {:default resources/default
   :status resources/status
   :repos resources/repos
   :repo resources/repo
   :commands resources/commands
   :commits resources/commits
   :commit resources/commit
   :tasks resources/tasks
   :task resources/task})

(defn wrap-resource-handler [resource]
  "wrap the resource function to take advantage of paramaterized resource definitions"
  (fn [request]
    ((resource (:route-params request)) request)))

(defn attach-resource-handlers
  "take the routes structure, and attach handler functions directly, so that make-handler can create a proper ring handler"
  [routes handlers]
  (routes/transform routes #(wrap-resource-handler (handlers %))))

(defn wrap-with-default-handler
  "add a default resource handler, to avoid the ugly default 404 from the underlying ring server adapter (i.e. jetty)"
  [handler default]
  (let [default (wrap-resource-handler (:default handlers))]
    (fn [request]
      (or
        (handler request)
        (default request)))))

(def app
  (do
    ;; hm, probably a better place (and way) to do this
    @(datomic/migrate datomic/schema)
    (-> routes
        (attach-resource-handlers handlers)
        bidi/make-handler
        (wrap-with-default-handler (:default handlers))
        (liberator/wrap-trace :ui :header)
        )))
