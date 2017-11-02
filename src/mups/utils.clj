(ns mups.utils)

(defn map-into-table
  "map hashtable, put result of calling function on each entry into hashtable and return it"
  ([f & colls] (into {} (apply map f colls))))
