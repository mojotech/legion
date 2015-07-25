(ns legion.core
  (:gen-class)
  (:require [clj-ssh.ssh :as ssh]
            [clojure.set :refer [rename-keys]]))

(def servers
  [{:name "styx"
    :host "104.236.232.144"
    :default-user "root"}
   {:name "cocytus"
    :host "104.236.232.151"
    :default-user "root"}])

(defn run-command [server command]
  (let [agent (ssh/ssh-agent {})
        passed-config (select-keys
                        (rename-keys server {:default-user :username})
                        [:username :password :port])
        session-config (merge {:strict-host-key-checking :no}
                              passed-config)
        session (ssh/session
                  agent
                  (:host server)
                  session-config)]

    (ssh/with-connection session
      (second (ssh/ssh session {:cmd command})))))

(defn run-commands-on-servers [command servers]
  (doall (pmap #(run-command % command) servers)))

(defn -main [command]
  (run-commands-on-servers command (servers)))
