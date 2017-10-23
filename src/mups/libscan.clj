(ns mups.libscan
  (:require claudio.id3
            [clojure.java.io :refer [file]]
            [mups.lastfm :refer :all]
            [clojure.data :refer [diff]]
            [cheshire.core :refer [generate-string]]))


"structure:
{
 author_name: {
     album_name: {
       song_count
       title?
       year?
   }
  }
}"

(defn add-author-info [mp3-tags collection]
  (let [artist-name (.toLowerCase (get mp3-tags :artist "Unknown Artist"))
        album-name (.toLowerCase (get mp3-tags :album "Unknown Album"))
        artist-info (get collection artist-name {})]
    (assoc collection artist-name
           (update-in artist-info [album-name]
                      #(if % (update-in % [song-count-key] inc)
                           {song-count-key 1})))))

(defn author-song-count
  ([author-info]
    (reduce + (map #(get % song-count-key 0) (vals author-info))))
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
        album-count (count author-info)]
    (or (> total-songs 5)
        (and (> album-count 1)
             (every? #(> % 1) (map #(get % song-count-key)
                                  (vals author-info)))))))

(defn only-listened-authors [collection]
  (into {} (filter author-is-listened collection)))

(def album-title-key "title")

(defn find-author-missing-albums [local-author-info lastfm-author-info]
  (let [[user-added missing common] (diff (into #{} (keys local-author-info))
                                          (into #{} (keys lastfm-author-info)))
        mapper (fn [map] (fn [album-title] (assoc (get map album-title)
                                                "title" album-title)))]
    (println "user-added: " user-added "missing: " missing "common: " common)
    {"you have" (if user-added
                  (map (mapper local-author-info) user-added)
                  {})
     "you miss" (if missing
                  (map (mapper lastfm-author-info) missing)
                  {})
     "both have" (if common
                   (map (mapper lastfm-author-info) common)
                   {})}))

(defn diff-collections [lastfm-collection user-collection]
  (into {} (map (fn [author]
                  (let [local-author-info (get user-collection author)
                        lastfm-author-info (get lastfm-collection author)]
                    [author (find-author-missing-albums
                             local-author-info
                             lastfm-author-info)]))
                (keys user-collection))))

(defn remove-ignored [collection ignore-collection]
  (if ignore-collection
    (let [authors (get ignore-collection "authors")
          albums (get ignore-collection "albums")
          author_albums (get ignore-collection "author_albums")]
      (->> authors
           (apply dissoc collection)
           (map (fn [[k v]]
                  (let [removed-globals (apply dissoc v albums)]
                    {k (apply dissoc removed-globals (get author_albums k))})))
           (into {})))
    collection))
