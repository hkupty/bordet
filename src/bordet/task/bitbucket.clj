(ns ^:bordet.loader/task bordet.task.bitbucket
  (:require [bordet.http :as http]
            [bordet.config :as config]
            [bordet.channels :as channels]
            [taoensso.timbre :refer [info debug]]))

(defn core [& routes] (apply http/route (config/get-config ::host) "rest/api/1.0" routes))

(defn build-status [& routes]
  (apply http/route (config/get-config ::host) "/rest/build-status/1.0" routes))

(defn request [data]
  (http/request (merge
                  {:headers {:authorization (str "Bearer "
                                                 (config/get-config ::token))}
                   :method :get}
                  data)))

(defn my-prs []
  (info "Fetching Pull requests")
  (-> (request {:url (core "dashboard/pull-requests?state=open&role=author")})
      :body
      :values))

(defn commit-status [commit]
  (request {:url (build-status "commits" commit)}))

(defn check-build-status [commit]
  (info "Fetching build status for commit" commit)
  (-> commit
      commit-status
      :body
      :values
      last))

(defn pr->info [pull-request]
  (merge
    (select-keys pull-request [:title :state :description])
    {:mergeable? (= "CLEAN" (get-in pull-request [:properties :merge-result :outcome]))
     :comments (get-in pull-request [:properties :comment-count] 0)
     :open-tasks (get-in pull-request [:properties :open-task-count])
     :url (get-in pull-request [:links :self 0 :href])
     :approvals (->> pull-request
                     :reviewers
                     (filter :approved)
                     (count))
     :build-status (-> pull-request
                       :from-ref
                       :latest-commit
                       (check-build-status)
                       (select-keys [:url :state]))}))

;; Reflectable API
(config/set-config-when-missing ::refresh-rate 1)
(defmethod channels/xform ::dashboard [_]
 (map (fn [_] (map pr->info (my-prs)))))
