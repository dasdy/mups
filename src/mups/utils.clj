(ns mups.utils
  (:require [clojure.java.io :refer [file]]))

(defn map-into-table
  "map hashtable, put result of calling function on each entry into hashtable and return it"
  ([f & colls] (into {} (apply map f colls))))

(defn file-exists [path]
  (and path (.exists (file path))))
