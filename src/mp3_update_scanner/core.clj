(ns mp3-update-scanner.core
     (:gen-class)
     (:use mp3-update-scanner.libscan
           mp3-update-scanner.lastfm)
     (:require [clojure.tools.cli :refer [parse-opts]]
               [cheshire.core :refer :all]
               [clojure.java.io :refer [file]]
               [mp3-update-scanner.lastfm :as lastfm]))

(defn save-collection [collection path]
  (let [my-pretty-printer (create-pretty-printer
                           (assoc default-pretty-print-options
                                  :indent-arrays? true))]
   (spit path (generate-string
               (into (sorted-map)
                     (map (fn [[k v]] [k (sort (keys v))])
                          collection))
               {:pretty my-pretty-printer})))
  collection)

(defn save-diff [diff path]
  (let [my-pretty-printer (create-pretty-printer
                          (assoc default-pretty-print-options
                                 :indent-arrays? true))]
   (spit path
         (generate-string
          (into (sorted-map)
                (map (fn [[k v]]
                       [k (into (sorted-map)
                                (map (fn [[k v]] [k (sort v)])
                                     v))])
                     diff))
          {:pretty my-pretty-printer}))))

(defn read-collection [path]
  (parse-string (slurp path)))

(defn remove-trailing-0 [string]
  (apply str (remove #(= (int %) 0) string)))

(def cli-options
  [["-m" "--music-path PATH" "Path to your music library"
    :default nil]
   ["-c" "--cached-path PATH" "Path to collection if you have already scanned library"
    :default nil]
   ["-o" "--output PATH" "Path to output (results of music scan). Should default to path of cached-path or, if not given, to out.json"
    :default "diff.json"]
   ["-l" "--lastfm PATH" "Path to Last.fm version of your  library (not removing albums you already have"
    :default "lastfm.json"]
   ["-i" "--ignore-path PATH" "Path to ignore file"
    :default nil]])

(defn parse-prog-options [args]
  (let [{:keys [music-path cached-path output lastfm ignore-path]}
        (:options (parse-opts args cli-options))]
    [music-path cached-path output ignore-path lastfm]))

(defn validate-args [[mpath cachepath _ _ _ :as args]]
  (if (not (or mpath cachepath))
    (println (str "You must specify at least one of --music-path or --cached-path options"))
    true))

(defn build-user-collection
  [mpath cachepath ignored-stuff]
  (-> (if mpath (get-all-mp3-tags-in-dir mpath) [])
      (build-collection (if (and cachepath (.exists (file cachepath)))
                          (read-collection cachepath)
                          {}))
      (only-listened-authors)
      (remove-ignored ignored-stuff)
      (save-collection cachepath)))

(defn build-diff
  [user-collection ignored-stuff lastfmpath outputpath]
  (-> user-collection
           (get-authors-from-lastfm)
           (remove-ignored ignored-stuff)
           (update-song-counts)
           (remove-singles)
           (save-collection lastfmpath)
           (diff-collections user-collection)
           (save-diff outputpath)))

(defn -main [& args]
  (let [[mpath cachepath outputpath ignorepath lastfmpath :as parsed-args]
        (parse-prog-options args)
        ignored-stuff (when (and ignorepath (.exists (file ignorepath)))
                           (read-collection ignorepath))]
    (when (validate-args parsed-args)
      (let [user-collection (build-user-collection mpath cachepath ignored-stuff)]
       (build-diff user-collection ignored-stuff lastfmpath outputpath)))))
