(ns legion.build.docker-push
  (require [me.raynes.conch :refer [programs with-programs let-programs] :as sh]
           [taoensso.timbre :as timbre]
           [legion.build.debug-repl :refer [debug-repl] :as debug]
           [legion.build.core :as core]))

(timbre/refer-timbre) ; Provides useful Timbre aliases

(programs docker)

(defn remote-tag
  "Given the server url, repo name, service name, and sha, I return the remote tag"
  [url repo service sha]
  (str url "/" repo "/" service ":" sha)
  )

 (defn push-image
   "I push the given tagged to remote"
   [local-tag remote-tag]
   (info "tagging and pushing" local-tag remote-tag)
   (docker "tag" local-tag remote-tag)
   (docker "push" remote-tag))

(defn push-all!
  "I push all images to a docker remote"
  [dir]
  (let [config (core/parse-build-file dir)
        sha (core/head-sha dir)]
    (map (fn [service]
           (let [name (:name service)
                 local (core/unique-tag sha name)
                 remote (remote-tag
                         (:dockerPushUrl config)
                         (:repoName config)
                         name
                         sha)]
             (push-image local remote)))
         (:build config))
    ))
