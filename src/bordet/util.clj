(ns bordet.util
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn split-first [re s]
  (str/split s re 2))

(defn split-last [re s]
  (let [pattern (re-pattern (str re "(?!.*" re ")"))]
    (split-first pattern s)))

(defn map-keys
  ([fn-] (map (juxt (comp fn- key) val)))
  ([fn- coll] (into {} (map-keys fn-) coll)))

(defn map-vals
  ([fn-] (map (juxt key (comp fn- val))))
  ([fn- coll] (into {} (map-keys fn-) coll)))
