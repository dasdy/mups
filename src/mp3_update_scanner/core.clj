(ns mp3-update-scanner.core
  (:gen-class)
  (:use clojure.java.io)
  (:require id3))

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
  (map #(id3/with-mp3 [mp3 (.getPath %)]
          (:tag mp3))
       (walk "." #".*\.mp3")))

(defn build-collection [mp3-info]
  (reduce (fn [acc x] (add-author-info x acc)) {} mp3-info))

(defn -main
  [& args])
