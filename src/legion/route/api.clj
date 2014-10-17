(ns legion.route.api)

(def routes
  ["" [["/api/v1" {"/status" :status
                   "/repo" {"" :repos
                            ["/" :repo-id] {"" :repo
                                            "/command" :commands
                                            "/commit" {"" :commits
                                                       ["/" :commit-id] {"" :commit
                                                                         "/task" {"" :tasks
                                                                                  ["/" :task-id] :task}}}}}}]
       [#".*" :default]]])