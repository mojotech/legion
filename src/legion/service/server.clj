(ns legion.service.server
  "connects routes to resources"
  (:require [legion.util.route :as route]
            [legion.route.api :as api]
            [legion.resource.status :refer [status]]
            [legion.resource.repo :refer [repo]]
            [legion.resource.repos :refer [repos]]
            [legion.resource.commands :refer [commands]]
            [legion.resource.commit :refer [commit]]
            [legion.resource.commits :refer [commits]]
            [legion.resource.task :refer [task]]
            [legion.resource.tasks :refer [tasks]]
            [legion.resource.default :refer [default]]
            [liberator.dev :refer  [wrap-trace]]
            [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as component :refer [start stop]]))

(def routes
  api/routes)

(def handlers
  {:status status
   :repo repo
   :repos repos
   :commands commands
   :commit commit
   :commits commits
   :task task
   :tasks tasks
   :default default})

(defrecord Server [config database]
  component/Lifecycle

  (start [server]
    (if (:jetty server)
      server
      (let [env ((:env config))
            handler (->
                      (route/make-handler
                        #(let [resource (% handlers)]
                           (fn [request]
                             ((resource server (:route-params request)) request)))
                        routes)
                      (wrap-trace :header :ui))]
        (assoc server
               :jetty (run-jetty handler {:port (:server-port env)
                                          :host (:server-host env) :join? false})))))

  (stop [server]
    (when-let [jetty (:jetty server)]
      (.stop jetty))
    (dissoc server :jetty)))

(defn server [] (map->Server {}))
