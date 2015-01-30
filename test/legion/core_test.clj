(ns legion.core-test
  (:require [clojure.test :refer :all]
            [vmfest.manager :as vm]
            [clojure.string :refer [split]]
            [vmfest.virtualbox.image :refer [setup-model]]
            [legion.core :refer :all]))

(defn wait-for [f timeout]
  (when (and (> timeout 0)
             (not (f)))
    (do (Thread/sleep 1000)
        (recur f (- timeout 1000)))))

(defn get-port [machine-name]
  (Integer.
    (str "3022"
         (second
           (split machine-name #"-")))))

(defn alive? [machines]
  (not-any?
    clojure.string/blank?
    (pmap vm/get-ip machines)))

(defn get-vm-hardware [host-port]
  {:memory-size 512
   :cpu-count 1
   :network [{:attachment-type :host-only
              :host-only-interface "vboxnet0"}
             {:attachment-type :nat
              :nat-rules [{:name "ssh", :protocol :tcp,
                           :host-ip "", :host-port host-port,
                           :guest-ip "", :guest-port 22}]}]
   :storage [{:name "IDE Controller"
              :bus :ide
              :devices [nil nil {:device-type :dvd} nil]}]
   :boot-mount-point ["IDE Controller" 0]})

(defn vm->server [vm]
  {:host "localhost"
   :default-user "vmfest"
   :password "vmfest"
   :port (get-port
           (vm/get-machine-attribute vm :name))})

(def my-server
  (try
    (let [server (vm/server)]
      (setup-model
        "https://s3.amazonaws.com/vmfest-images/debian-6.0.2.1-64bit-v0.3.vdi.gz"
        server)
      server)
    (catch Exception e (str "caught exception: " (.getMessage e)))))

(defn create-machines [number]
  (pmap
    #(do (vm/start %) %)
    (pmap
      #(vm/instance my-server
                    %
                    :debian-6.0.2.1-64bit-v0.3
                    (get-vm-hardware (get-port %)))
      (map #(str "machine-" %) (range number)))))

(defn run-on-vms [number f]
  (let [machines (create-machines number)]
    (wait-for #(alive? machines) 10000)
    (let [result (f machines)]
      (dorun (pmap vm/nuke machines))
      result)))

(defn run-command-on-vms
  [command vms]
  (run-commands-on-servers
    command
    (map vm->server vms)))

(deftest
  ^:integration
  run-single-command
  (testing "Run a command on a server"
    (is
      (.contains
        (->> (run-on-vms
               1
               (partial run-command-on-vms "ls -la"))
             (into {})
             :out)
        ".bashrc"))))

(deftest
  ^:integration
  run-command-on-multiple-servers
  (testing "Run a command on multiple servers"
    (is
      (apply distinct?
             (run-on-vms
               3
               (partial run-command-on-vms
                        "ip -4 -o addr show eth0 | awk '{print $4}'"))))))
