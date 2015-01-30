(defproject legion "0.1.0-SNAPSHOT"
  :description "Server Management with Configuration as Data"
  :url "http://github.com/mojotech/legion"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [vmfest "0.4.0-alpha.1"]
                 [org.clojars.tbatchelli/vboxjws "4.3.4"]
                 [clj-ssh "0.5.11"]]
  :main ^:skip-aot legion.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts [~(str "-Dvbox.home=" (System/getenv "VBOX_HOME"))]
  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)})
