(ns mp3-update-scanner.core
  (:gen-class)
  (:use clojure.java.io)
  (:require claudio.id3
            [clojure.data.json :as json]
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

(defn save-collection [collection path]
  (println (str "woah: " collection))
  (println (str "saving to:" path))
  ;; (with-open [wrtr (writer path)]
  ;;   (.write wrtr (print (str collection))))


  (spit path (json/write-str collection))
  )

(defn read-collection [path]
  (json/read-str (slurp path)))

(defn remove-trailing-0 [string]
  (apply str (remove #(= (int %) 0) string)))

(defn -main
  [& args]
  (println lastfm/private-key)
  (println lastfm/api-key)
  (println (first args))
  (-> (or (first args) ".")
      (get-all-mp3-tags-in-dir)
      (build-collection {})
      (save-collection (or (second args) "out.json"))))
