(ns user
  (:require [legion.app :refer [-main]]
            [clojure.java.io :as io]
            [clj-time.core :refer [now]]
            [clj-time.format :as f]
            [camel-snake-kebab.core :refer [->snake_case ->kebab-case]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [com.stuartsierra.component :as component]))

(def ^:dynamic *app*)

(defn start []
  (alter-var-root #'*app*
                  (constantly
                    (component/start (-main)))))

(defn stop []
  (alter-var-root #'*app*
                  #(when % (component/stop %))))

(defn restart []
  (stop)
  (refresh :after 'user/start))
