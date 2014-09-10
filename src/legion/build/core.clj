(ns legion.build.core
  (require [me.raynes.conch :refer [programs with-programs let-programs] :as sh]
           [clojure.data.json :as json]
           [taoensso.timbre :as timbre]
           [legion.build.debug-repl :refer [debug-repl] :as debug]
           legion.build.logging)
  (use [clojure.string :only [split trim]]))

(timbre/refer-timbre) ; Provides useful Timbre aliases

(extend-type nil
  sh/Redirectable
  (redirect [s options k proc]
      (->>
        (k proc)
        (reduce
          (fn [acc b]
            (print b)
            (.flush *out*)
            (debug b)
            (str acc b))
          ""))))

(programs docker git mktemp)

(defn docker-inspect
  "I inspect a docker image with the specified cid into a Clojure data structure."
  [cid]
  (debug "docker inspect" cid)
  (json/read-str (docker "inspect" cid)
                 :key-fn keyword))

(defn parse-build-file
  "I parse the json registry of containers to be built."
  [repo-path]
  (json/read-str (slurp (str repo-path "/deploy.json")) :key-fn keyword))

(defn head-sha
  "Given a git project directory, I return the sha of current checked out HEAD"
  [dir]
  (debug "getting head sha of" dir)
  (trim (git "log" "-1" "--format=%H" {:dir dir})))

(defn unique-tag
  "Given a sha and a logical container name, I return the legion unique tag"
  [sha name]
  (str "legion_" sha "_" name))

(defn build-container!
  "I build a docker image given a project path and a set of image-specific options."
  [project-dir options]
  (let [{name :name path :path} options]
    (info "building docker container" name "at" path)
    (docker "build" "-t" name (str project-dir "/" path))
    (let [unique (unique-tag (head-sha project-dir) name)]
      (info "tagging container" name "with" unique)
      (docker "tag" name unique)
      )))

(defn checkout-repo!
  "I check out a sha from a git repo into the specified directory"
  [url sha dir]
  (info "checking out sha" sha "of repo" url "at" dir)
  (git "clone" url dir)
  (git "checkout" sha {:dir dir}))

(defn create-temp-dir!
  "I create a temporary directory and return its path"
  []
  (trim (mktemp "-d" "-t" "repo")))

(defn build-containers!
  "Given a git repo path, I build its docker containers"
  [path]
  (let [build-list (:build (parse-build-file path))]
    (info "build list for " path build-list)
    (map (partial build-container! path) build-list)))

(defn prepare-repo!
  "Given a repo URL and a sha, I check it out into a temp dir"
  [url sha]
  (let [temp-dir (create-temp-dir!)]
    (info "temp dir for repo" url "and sha" sha ":" temp-dir)
    (checkout-repo! url sha temp-dir)
    (temp-dir)))
