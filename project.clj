(defproject legion "0.1.0-SNAPSHOT"
  :description "Legion: for we are many"
  :url "https://github.com/mojotech/legion"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [bidi "1.10.4"]
                 [liberator "0.12.1"]
                 [me.raynes/conch "0.8.0"]
                 [com.datomic/datomic-free "0.9.4880"]]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]
                   :plugins [[lein-ring "0.8.11"]]
                   :ring {:handler legion.handler/app
                          :auto-reload? true
                          :auto-refresh? true
                          :stacktraces? false}}})
