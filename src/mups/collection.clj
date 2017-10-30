(ns mups.collection
  (:require [cheshire.core :refer :all]))

(defmulti save-collection (fn [type _ _] type))
(defmulti read-collection (fn [type _] type))
(defmulti make-name (fn [type _] type))

(defmethod make-name :default [type title]
  (str title "." (name type)))

(defmethod save-collection :json [_ collection path]
  (let [my-pretty-printer (create-pretty-printer
                           (assoc default-pretty-print-options
                                  :indent-arrays? true))]
    (spit path (generate-string
                (into (sorted-map) collection)
                {:pretty my-pretty-printer}))
    collection))

(defmethod read-collection :json [_ path]
  (parse-string (slurp path)))
