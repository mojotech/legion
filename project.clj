(defproject legion "0.1.0-SNAPSHOT"
  :description "Server Management with Configuration as Data"
  :url "http://github.com/mojotech/legion"
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot legion.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
