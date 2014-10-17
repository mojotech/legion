(ns legion.app
  (:gen-class)
  (:require [legion.service.config :refer [config]]
            [legion.service.server :refer [server]]
            [legion.service.database :refer [database]]
            [legion.service.scheduler :refer [scheduler]]
            [com.stuartsierra.component :as component :refer [start stop]]))

(defn app [config]
  (-> (component/system-map
        :config (config)
        :server (server)
        :database (database)
        :scheduler (scheduler))
      (component/system-using
        {:database [:config]
         :server [:config :database]})))

(defn -main [& args]
  (start (app config)))
