(ns legion.util.model
  (:require [clojure.walk :refer [postwalk]]))

(defn unnamespace-key [k]
  (keyword (name k)))

(defn unnamespace-entry [[k v]]
  (if (keyword? k) [(unnamespace-key k) v] [k v]))

(defn unnamespace-keys-1 [m]
  (if (map? m)
    (into {} (map unnamespace-entry m)) m))

(defn unnamespace-keys [m]
  (postwalk unnamespace-keys-1 m))

(defn namespace-key [ns k]
  (keyword ns (name k)))

(defn namespace-entry [ns [k v]]
  (if (keyword? k) [(namespace-key k) v] [k v]))

(defn namespace-keys-1 [ns m]
  (if (map? m)
    (into {}
          (map
            (partial namespace-entry ns) m)) m))

(defn namespace-keys [ns m]
  (postwalk (partial namespace-keys-1 ns) m))

(defn update-entry [f [k v]]
  [(f k) v])

(defn update-keys-1 [f m]
  (if (map? m)
    (into {}
          (map
            (partial update-entry f) m)) m))

(defn update-keys [f m]
  (postwalk (partial update-keys-1 f) m))
