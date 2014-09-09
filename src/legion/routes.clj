(ns legion.routes
  (:require [clojure.java.io :as io]))

(def routes
  (read-string (slurp (io/resource "routes.edn"))))

(defprotocol Routes
  (transform [this f] "applies a function f to all route handlers and returns the new routes structure"))

(extend-protocol Routes
  clojure.lang.Sequential
  (transform [this f]
    (let [[p m] this] [p (transform m f)]))

  clojure.lang.MapEquivalence
  (transform [this f]
    (into {}
          (for [[p m] this] [p (transform m f)])))

  Object
  (transform [this f] (f this)))
