(ns legion.service.config
  (:require [clojure.java.io :as io]
            [nomad :refer [defconfig]]))

(defconfig env (io/resource "config/env.edn"))
(defconfig repo (io/resource "config/repo.edn"))

(defn config []
  {:env env
   :repo repo})
