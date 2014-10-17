(ns legion.util.datomic
  #_(:refer-clojure :exclude [keyword boolean long bigint float double bigdec ref bytes defn])
  (:refer-clojure :exclude [defn])
  (:require [datomic.api :as d :refer [db q]]))

;; (clojure.core/defn entity [& {:as attrs}]
;;   [(assoc attrs
;;           :db/id (d/tempid :db.part/user))])
;;
;; (clojure.core/defn attribute [type ident & {:as options}]
;;   (let [tx {:db/id (d/tempid :db.part/db)
;;             :db/ident ident
;;             :db/valueType type
;;             :db/cardinality (if (:many? options)
;;                               :db.cardinality/many
;;                               :db.cardinality/one)
;;             :db.install/_attribute :db.part/db}]
;;     [(reduce
;;        (clojure.core/fn [tx [k v]]
;;          (condp = k
;;            :doc (assoc tx :db/doc v)
;;            :unique? (if v (assoc tx :db/unique :db.unique/value) tx)
;;            :identity? (if v (assoc tx :db/unique :db.unique/identity) tx)
;;            :index? (if v (assoc tx :db/index true) tx)
;;            :fulltext? (if v (assoc tx :db/fulltext true) tx)
;;            :component? (if v (assoc tx :db/isComponent true) tx)
;;            :history? (if (not v) (assoc tx :db/noHistory true) tx)
;;            :many? tx
;;            (assoc tx k v)))
;;        tx options)]))
;;
;; (def string (partial attribute :db.type/string))
;; (def keyword (partial attribute :db.type/keyword))
;; (def boolean (partial attribute :db.type/boolean))
;; (def long (partial attribute :db.type/long))
;; (def bigint (partial attribute :db.type/bigint))
;; (def float (partial attribute :db.type/float))
;; (def double (partial attribute :db.type/double))
;; (def bigdec (partial attribute :db.type/bigdec))
;; (def ref (partial attribute :db.type/ref))
;; (def instant (partial attribute :db.type/instant))
;; (def uuid (partial attribute :db.type/uuid))
;; (def uri (partial attribute :db.type/uri))
;; (def bytes (partial attribute :db.type/bytes))
;;
;; (clojure.core/defn enum [ident values & rest]
;;   (concat
;;     (apply attribute :db.type/ref ident rest)
;;     (map #(hash-map :db/id (d/tempid :db.part/user)
;;                     :db/ident %)
;;       values)))
;;
;; (defmacro defn [name doc bindings & body]
;;   (let [[doc bindings body] (if (string? doc)
;;                               [doc bindings body]
;;                               [(clojure.core/name name)
;;                                doc
;;                                (list* bindings body)])]
;;     `[{:db/id (d/tempid :db.part/user)
;;        :db/doc ~doc
;;        :db/ident ~name
;;        :db/fn (d/function
;;                 {:lang "clojure"
;;                  :params '~bindings
;;                  :code '(do ~@body)})}]))
;;

(defmacro defn [name & body]
  (let [decl []
        [decl body] (if (string? (first body))
                      [(conj decl (first body)) (rest body)]
                      [decl body])
        [decl body] (if (map? (first body))
                      [(conj decl (first body)) (rest body)]
                      [decl body])
        [decl body] [(conj decl (first body)) (rest body)]
        source `(quote (do ~@body))]
    `(clojure.core/defn
       ~(vary-meta name assoc :source source) ~@decl ~@body)))

(clojure.core/defn ->tx [sym]
  (let [{:keys [ns name doc source arglists requires imports]} (meta sym)
        fn-map {:lang "clojure"
                :params (first arglists)
                :code source}
        fn-map (if imports
                 (assoc fn-map :imports imports) fn-map)
        fn-map (if requires
                 (assoc fn-map :requires requires) fn-map)
        tx-map {:db/id (d/tempid :db.part/user)
                :db/ident (keyword (str ns) (clojure.core/name name))
                :db/fn (d/function fn-map)}
        tx-map (if doc
                 (assoc tx-map :db/doc doc) tx-map)]
    tx-map))

#_(datomic/defn hello
  "hello there"
  {:requires '[[clojure.string :as str]]}
  [x y z]
  (println "hello"))
#_(let [ns 'legion.presenter.api]
    (->> (keys (ns-publics ns))
         (map (comp meta (partial ns-resolve ns)))
         (filter ::source)
         (map (partial ->tx ns))))

#_(map ->tx (filter ::source (map (comp meta (partial ns-resolve 'legion.presenter.api)) (keys (ns-publics 'legion.presenter.api)))))
