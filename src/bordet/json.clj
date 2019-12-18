(ns bordet.json
  (:require [jsonista.core :as j]
            [camel-snake-kebab.core :as csk]))

(def mapper
  (j/object-mapper {:encode-key-fn name
                    :decode-key-fn csk/->kebab-case-keyword}))

(defn data->json [data]
  (j/write-value-as-string data mapper))

(defn json->data [json]
  (j/read-value json mapper))

