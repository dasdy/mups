(ns mp3-update-scanner.core
  (:gen-class)
  (:use clojure.java.io)
  (:require id3
            [mp3-update-scanner.lastfm :as lastfm]))

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
  (walk "." #".*\.mp3"))

(defn get-all-mp3-tags-in-dir [dir-path]
  (map #(id3/with-mp3 [mp3 (.getPath %)]
          (:tag mp3))
       (get-all-mp3-in-dir dir-path)))

(defn build-collection [mp3-info coll]
  (reduce (fn [acc x] (add-author-info x acc)) coll mp3-info))

(defn save-collection [collection path]
  (spit path (str collection)))

(defn read-collection [path]
  (read-string (slurp path)))

(defn -main
  [& args]
  (println lastfm/private-key)
  (println lastfm/api-key))
