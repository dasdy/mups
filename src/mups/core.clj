(ns mups.core
     (:require [clojure.java.io :refer [file]]
               [swiss.arrows :refer [-<>]]
               [mups.libscan :refer [build-collection remove-ignored
                                     only-listened-authors get-all-mp3-tags-in-dir
                                     diff-collections ]]
               [mups.collection :refer [save-collection read-collection]]
               [mups.lastfm :refer [fetch-album-details get-authors-from-lastfm
                                    remove-singles]]))

(def collection-writer :json)
(def collection-reader :json)
(def diff-writer :html)

(defn build-user-collection
  [mpath cachepath ignored-stuff]
  (-<> (if mpath (get-all-mp3-tags-in-dir mpath) [])
      (build-collection (if (and cachepath (.exists (file cachepath)))
                          (read-collection collection-reader cachepath)
                          {}))
      (only-listened-authors)
      (remove-ignored ignored-stuff)
      (save-collection collection-writer <> cachepath)))

(defn fetch-details-in-scanned-collection [user-collection ignored-stuff]
  (-> user-collection
      (get-authors-from-lastfm)
      (remove-ignored ignored-stuff)
      (fetch-album-details)))

(defn build-diff
  [user-collection ignored-stuff lastfmpath]
  (-<> user-collection
           (fetch-details-in-scanned-collection ignored-stuff)
           (remove-singles)
           (save-collection collection-writer <> lastfmpath)
           (diff-collections user-collection)))
