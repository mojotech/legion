(defproject legion "0.1.0-SNAPSHOT"
  :description "Legion: for we are many"
  :url "https://github.com/mojotech/legion"
  :main legion.app
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [bidi "1.10.4"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [liberator "0.12.1"]
                 [jarohen/nomad "0.7.0"]
                 [clj-time "0.8.0"]
                 [camel-snake-kebab "0.2.4"]
                 [io.rkn/conformity "0.3.2"]
                 [jarohen/chime "0.1.6"]
                 [me.raynes/conch "0.8.0"]
                 [com.h2database/h2 "1.4.181"]
                 [clojurewerkz/quartzite "1.3.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [com.datomic/datomic-free "0.9.4880"]
                 [prismatic/schema "0.2.6"]]
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [org.clojure/tools.namespace "0.2.7"]]
                   :jvm-opts ["-Dnomad.env=dev"]}})
