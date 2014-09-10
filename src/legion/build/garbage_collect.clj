(ns legion.build.garbage-collect
  (require [me.raynes.conch :refer [programs with-programs let-programs] :as sh]
           [taoensso.timbre :as timbre]
           [legion.build.debug-repl :refer [debug-repl] :as debug])
  (use [clojure.string :only [split trim]]))

(timbre/refer-timbre) ; Provides useful Timbre aliases

(programs docker git)

(defn slurp-docker-images-line
  "Add to a mapping of imageid->[tags], given a line from docker images command"
  [mapping, line]
  (let [[repository _ img] (split line  #"\s+")
        tags (get mapping img [])]
    (assoc mapping img (conj tags repository))))

(defn raw-docker-images
  "Returns a vector of vectors of all image id's known to Docker"
  []
  (subvec (split (docker "images") #"\n") 1))

(defn docker-tags-by-image
  "Returns a map of docker image id's to all tags associated with the id"
  []
  (reduce slurp-docker-images-line {} (raw-docker-images)))

(defn sha-from-tag
  "Given a docker tag, I extract the git sha from it (if any)"
  [tag]
  (get (re-matches #"legion_([a-z0-9]+)_[a-z0-9]+" tag) 1))

(defn commit-occurred-at
  "Given a sha, I return the date at which it occurred"
  [sha]
  (trim (git "log" "-1" "--format=%ct" sha)))

(defn still-need-sha?
  "Given a sha, I determine whether it mandates keeping any associated image"
  [heads, sha]
  (contains? heads sha))

(defn still-need-image?
  "I determine if a given image is exempt from garbage collection"
  [heads [image-id tags]]
  (let [legion-tags (filter identity (map sha-from-tag tags))]
    (or (empty? legion-tags)
        (some (partial still-need-sha? heads) legion-tags)
        )))

(defn remove-image!
  "I delete a docker image permanently"
  [image-id]
  (info "removing docker image" image-id)
  (docker "rmi" "-f" image-id {:throw false}))

(defn git-heads
  "I retrieve a map of the heads of all local branches in a given repo (sha->branch)"
  [dir]
  (let [raw-git-results  (git "show-ref" "--heads" {:dir dir})]
    (apply hash-map (split raw-git-results  #"\s+"))))

(defn garbage-collect!
  "Given a git repo dir, I remove docker images uneeded for heads of that repo"
  [dir]
  (println "entry to garbage-collect" )
  (let [heads (git-heads dir),
        images (docker-tags-by-image)
        garbage (remove (partial still-need-image? heads) images)]
    (debug "git heads:" heads)
    (debug "all docker images and tags:" images)
    (map remove-image! (keys garbage))))
