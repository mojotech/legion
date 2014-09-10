(ns legion.build.logging
  (require [taoensso.timbre :as timbre]
           [clojure.string :as str]
           [riemann.client :as riemann]))

(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "legion.log")
(timbre/set-config!
 [:fmt-output-fn]
 (fn [{:keys [level throwable message timestamp hostname ns]}
      ;; Any extra appender-specific opts:
      & [{:keys [nofonts?] :as appender-fmt-output-opts}]]
      (format "%s [%s] %s - %s%s"
              timestamp
              ns
              (-> level name str/upper-case)
              (or message "")
              (or (timbre/stacktrace throwable "\n" (when nofonts? {})) ""))))

