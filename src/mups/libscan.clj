(ns mups.libscan
  (:require claudio.id3
            [clojure.java.io :refer [file]]
            [clojure.data :refer [diff]]
            [mups.utils :refer [map-into-table]]
            [mups.data :refer [->Album ->Artist ->DiffItem]]
            [cheshire.core :refer :all]))

(defn add-author-info [mp3-tags collection]
  (let [base-artist-name (get mp3-tags :artist "Unknown Artist")
        artist-name (.toLowerCase base-artist-name)
        base-album-name (get mp3-tags :album "Unknown Album")
        album-name (.toLowerCase base-album-name)
        artist-info (get collection
                         artist-name
                         (->Artist base-artist-name {} nil))
        update-func (fn [album-desc]
                      (if album-desc
                        (update-in album-desc [:song-count] inc)
                        (->Album 1 base-album-name nil nil)))
        artist-albums (:albums artist-info)
        new-albums (update-in artist-albums [album-name] update-func)
        new-artist-info (assoc artist-info :albums new-albums)]
    (assoc collection artist-name new-artist-info)))

(defn author-song-count
  ([author-info]
   (reduce + (map #(:song-count % 0) (vals (:albums author-info)))))
  ([collection author-name]
   (let [author-info (get collection author-name)]
       (author-song-count author-info))))

(defn walk [dirpath pattern]
  (doall (filter #(re-matches pattern (.getName %))
                 (file-seq (file dirpath)))))

(defn get-all-mp3-in-dir [dir-path]
  (walk dir-path #".*\.mp3"))

(defn get-all-mp3-tags-in-dir [dir-path]
  (map #(try
          (.setLevel (java.util.logging.Logger/getLogger "org.jaudiotagger")
                     java.util.logging.Level/OFF)
          (claudio.id3/read-tag %)
          (catch Exception e
            (println (str "file: " % "\ncaught: " (.getMessage e)))))
       (get-all-mp3-in-dir dir-path)))

(defn build-collection [mp3-info coll]
  (reduce (fn [acc x] (add-author-info x acc)) coll mp3-info))

(defn author-is-listened
  "check if user truly listens to this author
   (to filter out those where there is only 1-2 songs)"
  [[_ author-info]]
  (let [total-songs (author-song-count author-info)
        album-count (count (:albums author-info))
        albums (vals (:albums author-info))]
    (or (> total-songs 5)
        (and (> album-count 1)
             (every? #(> % 1) (map :song-count albums))))))

(defn only-listened-authors [collection]
  (into {} (filter author-is-listened collection)))

(defn find-author-missing-albums [local-author-info lastfm-author-info]
  (let [[user-added missing common] (diff (set (keys (:albums local-author-info)))
                                          (set (keys (:albums lastfm-author-info))))
        mapper (fn [albums-map]
                 "since diff is splitting collections by key, three collections are
                  just sets of album names. This is a function generator that associates
                  actual albums with keys from diff resultsets."
                 (fn [album-title] (get albums-map album-title)))
        map-not-nil (fn [author-info album-titles]
                      (if album-titles
                        (map (mapper (:albums author-info)) album-titles)
                        []))]
    (->DiffItem (:display-name local-author-info)
                (map-not-nil lastfm-author-info common)
                (map-not-nil local-author-info user-added)
                (map-not-nil lastfm-author-info missing))))

(defn diff-collections [lastfm-collection user-collection]
  (map-into-table
   (fn [author]
     (let [local-author-info (get user-collection author)
           lastfm-author-info (get lastfm-collection author)]
       [author (find-author-missing-albums
                local-author-info
                lastfm-author-info)]))
   (keys user-collection)))

(defn remove-ignored [collection ignore-collection]
  (if ignore-collection
    (let [authors (:ignored-authors ignore-collection)
          albums (:ignored-albums ignore-collection)
          author_albums (:ignored-author-albums ignore-collection)
          collection-without-ignored-authors (apply dissoc collection authors)]
      (map-into-table
       (fn [[k v]]
         (let [current-albums (:albums v)
               removed-globals (apply dissoc current-albums albums)
               removed-author-albums (apply dissoc removed-globals (get author_albums k))]
           [k (assoc v :albums removed-author-albums)]))
       collection-without-ignored-authors))
    collection))

(defn map-collection [collection collection-mapping]
  (let [lower-case-mapper (fn [[k v]] [(.toLowerCase k) (.toLowerCase v)])
        merge-artists (fn [a1 a2]
                        (let [name (or (:display-name a2) (:display-name a1))
                              albums (merge (:albums a1) (:albums a2))
                              url (or (:url a2) (:url a1))]
                          (->Artist name albums url)))
        artist-mappings (:artist-mappings collection-mapping)
        artist-mappings (map-into-table lower-case-mapper artist-mappings)]
    (reduce
     (fn [new-coll [artist-name artist-info]]
       (let [new-name (get artist-mappings artist-name artist-name)
             other-artist-info (get new-coll new-name)
             new-artist-info (merge-artists other-artist-info artist-info)]
         (assoc new-coll new-name new-artist-info)))
     {}
     collection)))
