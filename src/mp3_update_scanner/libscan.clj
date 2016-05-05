(ns mp3-update-scanner.libscan
  (:require claudio.id3
            [clojure.java.io :refer [file]]
            [mp3-update-scanner.lastfm :refer :all]
            [clojure.data :refer [diff]]))


"structure:
{
 author_name:
  {
     album_name: song_count
  }
}"

(defn add-author-info [mp3-tags collection]
  (let [artist-name (get mp3-tags :artist "Unknown Artist")
        album-name (get mp3-tags :album "Unknown Album")
        artist-info (get collection artist-name {})]
    (assoc collection artist-name
           (update-in artist-info [album-name] #(if % (inc %) 1)))))

(defn author-song-count [collection author-name]
  (reduce + (vals (get collection author-name {}))))

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
  (let [total-songs (reduce + (vals author-info))
        album-count (count author-info)]
    (or (> total-songs 5)
        (and (> album-count 1)
             (every? #(> % 1) (vals author-info))))))

(defn only-listened-authors [collection]
  (into {} (filter author-is-listened collection)))

(defn find-missing-albums [local-author-info lastfm-author-info]
  (let [[_ missing common] (diff (into #{} (keys local-author-info))
                                 (into #{} (keys lastfm-author-info)))]
   (reduce (fn [acc x] (assoc acc x 1)) {} missing)))

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

(defn find-all-missing [collection]
  (let [lastfm-looked-up (get-authors-from-lastfm collection)]
    (map (fn [[k1 v1]]
           (let [v2 (get lastfm-looked-up k1)]
            [k1 (find-missing-albums v1 v2)]))
         collection lastfm-looked-up)))
