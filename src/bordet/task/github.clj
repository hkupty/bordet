(ns bordet.task.github
  (:require [bordet.http :as http]
            [clojure.string :as str]
            [bordet.config :as config]
            [bordet.channels :as channels]
            [taoensso.timbre :refer [info debug]]))



(defn core [& routes] (apply http/route (config/get-config ::host
                                                           "https://api.github.com")
                             routes))

(defn request [data]
  (:body (http/request (merge
                  {:headers {:authorization (str "Bearer "
                                                 (config/get-config ::token))}
                   :method :get}
                  data))))


(def no-url (filter (comp
                      (complement #(str/ends-with? % "-url"))
                      name
                      key)))

(def no-count (filter (comp
                      (complement #(str/ends-with? % "-count"))
                      name
                      key)))

