(ns legion.plugin.docker
  (:require [legion.util.docker :as docker]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

;; TODO
;;  - build task
;;  - clean task

(def config-file-name "deploy.json")

(defn parse-config [path]
  (json/read-str
    (slurp (io/file path config-file-name)) :key-fn keyword))

#_(defmethod l/command :build [{:keys [path hash]}]
  (let [images (-> path parse-config :images)]
    (doseq [image images]
      (-> {:hash hash
           :image image}
          (agent)
          (send build-image)
          (send push-image)))))

#_(defmethod l/command :clean [{:keys [path hash]}])
