(ns bordet.redis
  (:require [bordet.control :as control]
            [bordet.config :as config]
            [bordet.loader :as loader]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.timbre :refer [info debug]]))

(def localhost {:pool {} :spec {:uri (config/get-config ::uri "redis://127.0.0.1:6379")}})

(defmacro wcar* [& body]
  `(wcar localhost ~@body))

(defn start-listener! []
  (info "Starting redis listener for the following handlers" (keys (control/handlers)))
    (car/with-new-pubsub-listener
      (:spec localhost)
      {"bordet*" control/message-handler}
      (car/psubscribe "bordet*")))

(def stop-listener! car/close-listener)

(defn set-value [key- value-]
  (wcar*
    (car/set key- value-)))

(defmethod control/message-handler ::wipe [_]
  (wcar*
    (->> (loader/tasks)
         (eduction
           (map name)
           (map #(str % "/dashboard")))
         (run! car/del))))
