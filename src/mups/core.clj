(ns mups.core
     (:require [clojure.java.io :refer [file]]
               [mups.libscan :refer [build-collection remove-ignored
                                     only-listened-authors get-all-mp3-tags-in-dir
                                     diff-collections]]
               [mups.collection :refer [save-collection read-collection]]
               [mups.lastfm :refer [fetch-album-details get-authors-from-lastfm
                                    remove-singles]]
               [mups.utils :refer [file-exists]]))

(def collection-writer :json)
(def collection-reader :json)
(def diff-writer :html)

(def write-coll (partial save-collection collection-writer))

(defn build-full-collection [music-path cache-path]
  (let [id3-tags (if music-path
                   (get-all-mp3-tags-in-dir music-path)
                   [])
        cached-collection (if (file-exists cache-path)
                            (read-collection collection-reader cache-path)
                            {})]
    (build-collection id3-tags cached-collection)))

(defn build-user-collection [mpath cachepath ignored-stuff]
  (-> (build-full-collection mpath cachepath)
      only-listened-authors
      (remove-ignored ignored-stuff)
      (write-coll cachepath)))

(defn fetch-details-in-scanned-collection [user-collection ignored-stuff]
  (-> user-collection
      get-authors-from-lastfm
      (remove-ignored ignored-stuff)
      fetch-album-details))

(defn build-diff [user-collection ignored-stuff lastfmpath]
  (-> user-collection
      (fetch-details-in-scanned-collection ignored-stuff)
      remove-singles
      (write-coll lastfmpath)
      (diff-collections user-collection)))
