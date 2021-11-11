(ns bordet.core
  (:require [taoensso.timbre :refer [info debug]]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [bordet.loader :as loader]
            [bordet.control :as control]
            [bordet.channels :as channels]
            [bordet.redis :as redis]
            [bordet.config :as config])
  (:gen-class))

(def on? (atom true))

(defmethod control/message-handler ::close [_]
  (info "Shutting down bordet")
  (reset! on? false))


(defn require-tasks! [tasks]
  (info "Loading namespaces for the following tasks" tasks)
  (run! require tasks))

(defn -main [& args]
  (config/load-config!)
  (channels/start-sink!)
  (channels/start-ticker!)
  (channels/start-tasks!
    (map name (doto (loader/tasks)
                (require-tasks!))))

  (let [listener (redis/start-listener!)]
    (a/go-loop []
               (a/<! (a/timeout 5000))
               (if-not @on?
                 (do
                   (channels/stop-ticker!)
                   (channels/stop-sink!)
                   (channels/stop-tasks!)
                   (redis/stop-listener! listener))
                 (recur)))))
