(ns ^:bordet.loader/task bordet.task.github.notifications
  (:require [bordet.task.github :as hub]
            [bordet.config :as config]
            [bordet.control :as control]
            [bordet.channels :as channels]
            [bordet.http :refer [route]]
            [taoensso.timbre :refer [info debug]]))


(defn get-notifications []
  (hub/request {:url (hub/core "notifications")}))

(defn add-subject-data [notification]
 (-> notification
     (update :subject #(merge % (select-keys (hub/request (select-keys % [:url]))
                                                      [:state :html-url :comments])))
     (update :subject dissoc :latest-comment-url :url)))

(defn ->notification [data]
  (letfn [(get-> [& path] (get-in data path))]
    (merge
      (:subject data)
      {:repo (get-> :repository :full-name)
       :reason (get-> :reason)
       :id (get-> :id)})))

(defn mark-as-read [id]
  (hub/request {:url (hub/core "notifications/threads" id)
                :method :patch}))

(config/set-config-when-missing ::refresh-rate 5)
(defmethod channels/xform ::dashboard [_]
  (info "Got message")
  (map (fn [_] (into []
                      (comp
                        (map add-subject-data)
                        (map ->notification))
                      (get-notifications)))))

(defmethod control/message-handler ::mark-as-read [msg]
  (let [thread-id (last msg)]
    (mark-as-read thread-id)
    (channels/force-tick ::refresh)))
