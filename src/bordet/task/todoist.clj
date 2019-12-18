(ns ^:bordet.loader/task bordet.task.todoist
  (:require [bordet.http :as http]
            [bordet.config :as config]
            [bordet.control :as control]
            [bordet.channels :as channels]
            [taoensso.timbre :refer [info debug]]))

(def api (partial http/route "https://api.todoist.com/rest/v1"))
(defn request [data]
  (http/request (merge
                  {:headers {:authorization (str "Bearer "
                                                 (config/get-config ::token))}
                   :method :get}
                  data)))

(defn all-projects []
  (info "Fetching projects")
  (->> (request {:url (api "projects")})
       :body
       (into {}
             (map (juxt :id :name)))))

(defn all-tasks []
  (let [projects (all-projects)]
    (info "Fetching open tasks")
    (->> (request {:url (api "tasks")})
         :body
         (into []
               (comp
                 (filter (complement :completed))
                 (map #(select-keys % [:id :content :url :project-id]))
                 (map #(update % :project-id projects)))))))

(defmethod control/message-handler ::delete-task [msg]
  (let [task-id (last msg)]
    (request {:method :delete
              :url (api "tasks" task-id)})
    (channels/force-tick ::refresh)))

(defmethod control/message-handler ::close-task [msg]
  (let [task-id (last msg)]
    (request {:method :post
              :url (api "tasks" task-id "close")})
    (channels/force-tick ::refresh)))


(config/set-config-when-missing ::refresh-rate 40)
(defmethod channels/xform ::dashboard [_]
  (map (fn [_] (all-tasks))))
