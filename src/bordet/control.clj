(ns bordet.control
  (:require [taoensso.timbre :refer [info]]))

(defmulti message-handler
  (fn [msg]
    (info "Got message" msg)
    (when-not (#{"subscribe" "psubscribe"} (first msg))
      (keyword (last (butlast msg))))))

(defmethod message-handler nil [msg]
  (info :subscribed msg))

(defn handlers []
  (into {}
        (filter (comp some? key))
        (methods message-handler)))
