(ns bordet.loader
  (:require [clojure.java.classpath :as cp]
            [clojure.tools.namespace.find :as find]))

(defn tasks []
  (into []
        (filter (comp :bordet.loader/task meta))
        (find/find-namespaces (cp/classpath))))
