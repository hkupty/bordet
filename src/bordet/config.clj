(ns bordet.config
  (:require [bordet.util :refer [map-keys split-last]]
            [bordet.control :as control]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.io PushbackReader)))

(def current-config (atom {}))

(def defaults (atom {}))

(defn from-java-config []
 (into {}
       (comp
         (filter #(str/starts-with? % "bordet."))
         (map-keys (comp
                     (partial apply keyword)
                     (partial split-last #"\."))))
   (System/getProperties)))

(defn from-edn-file []
  (-> (System/getenv "HOME")
      (io/file ".config" "bordet" "config.edn")
      (io/reader)
      (PushbackReader.)
      (edn/read)))

(defn load-config! []
  (reset! current-config
          (merge
            @defaults
            (from-edn-file)
            (from-java-config))))

(defn set-config-when-missing [key- value-]
  (letfn [(assoc-if-nil [m- k- v-]
            (cond-> m-
              (nil? (get m- k-)) (assoc k- v-)))]
    (swap! defaults assoc-if-nil key- value-)
    (swap! current-config assoc-if-nil key- value-)))

(defn get-config
  ([key-] (get-config key- nil))
  ([key- default] (get @current-config key- default)))

(defn config-keys [] (keys @current-config))

(defmethod control/message-handler ::reload [_] (load-config!))
