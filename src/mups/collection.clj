(ns mups.collection
  (:require [cheshire.core :refer :all]))

(defmulti save-collection (fn [type collection path] type))
(defmulti read-collection (fn [type path] type))

(defmethod save-collection :json [dispatcher collection path]
  (let [my-pretty-printer (create-pretty-printer
                           (assoc default-pretty-print-options
                                  :indent-arrays? true))]
    (spit path (generate-string
                (into (sorted-map) collection)
                {:pretty my-pretty-printer}))
    collection))

(defmethod read-collection :json [dispatcher path]
  (parse-string (slurp path)))
