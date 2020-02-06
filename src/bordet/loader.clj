(ns bordet.loader
  (:require [clojure.java.classpath :as cp]
            [bordet.config :as config]
            [clojure.tools.namespace.find :as find]))

(defn tasks []
  (into []
        (comp (filter (comp :bordet.loader/task
                            meta))
              (filter (fn [ns-]
                        (config/get-config (keyword (name ns-)
                                                    "enabled")
                                           false))))
        (find/find-namespaces (cp/classpath))))


