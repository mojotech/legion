(ns legion.service.scheduler
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [com.stuartsierra.component :as component :refer [start stop]]))

(defrecord Scheduler [config]
  component/Lifecycle

  (start [scheduler]
    (if (:started scheduler)
      scheduler
      (do
        (qs/initialize)
        (qs/start)
        (assoc scheduler :started true))))

  (stop [scheduler]
    (when (:started scheduler)
      (qs/shutdown))
    (dissoc scheduler :started)))

(defn scheduler [] (map->Scheduler {}))
