(ns bordet.http
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [bordet.json :as json]))

(defn update-when [obj key- fn-]
  (cond-> obj
    (some? (key- obj)) (update key- fn-)))

(defn route [& parts]
  (str/join "/" parts))

(defn request [req]
  (-> req
      (update-when :body json/data->json)
      (client/request)
      (update-when :body json/json->data)))
