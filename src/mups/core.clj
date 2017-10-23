(ns mups.core
     (:gen-class)
     (:use mups.libscan
           mups.lastfm)
     (:require [clojure.tools.cli :refer [parse-opts]]
               [cheshire.core :refer :all]
               [clojure.java.io :refer [file]]
               [hiccup.core :refer [html]]
               [swiss.arrows :refer [-<>]]
               [mups.lastfm :as lastfm]))

(def collection-writer :json)
(def collection-reader :json)
(def diff-writer :json)

(defmulti save-collection (fn [type collection path] type))
(defmulti read-collection (fn [type path] type))
(defmulti save-diff (fn [type collection path] type))

(defmethod save-collection :json [dispatcher collection path]
  (let [my-pretty-printer (create-pretty-printer
                           (assoc default-pretty-print-options
                                  :indent-arrays? true))]
    (spit path (generate-string
                (into (sorted-map)
                      (map (fn [[k v]] [k (sort (keys v))])
                           collection))
                {:pretty my-pretty-printer}))
    collection))

(defmethod save-collection :sqlite [dispatcher collection path]
  (println type " " collection " " path))

(defmethod read-collection :json [dispatcher path]
  (parse-string (slurp path)))


(defmethod save-diff :json [dispatcher diff path]
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

(defmethod save-diff :html [dispatcher diff path]
  (let [album-item
        (fn [album-info]
          [:li.album
           [:img {:src (get image-url-key album-info "")}]
           [:div.title (get album-title-key album-info)]])
        album-lst (fn [albums] [:ul (map album-item albums)])]
   (html (map (fn [artist-name diff]
                [:div.artist artist-name [:br]
                 "you have: " [:br]
                 (album-lst (get diff "you have"))
                 "you miss: " [:br]
                 (album-lst (get diff "you miss"))
                 "both have: " [:br]
                 (album-lst (get diff "both have"))])
              diff))))

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
                          (read-collection collection-reader cachepath)
                          {}))
      (only-listened-authors)
      (remove-ignored ignored-stuff)
      (save-collection collection-writer cachepath)))

(defn build-diff
  [user-collection ignored-stuff lastfmpath outputpath]
  (-<> user-collection
           (get-authors-from-lastfm)
           (remove-ignored ignored-stuff)
           (update-song-counts)
           (remove-singles)
           (save-collection collection-writer <> lastfmpath)
           (diff-collections user-collection)
           (save-diff diff-writer <> outputpath)))

(defn -main [& args]
  (let [[mpath cachepath outputpath ignorepath lastfmpath :as parsed-args]
        (parse-prog-options args)
        ignored-stuff (when (and ignorepath (.exists (file ignorepath)))
                           (read-collection collection-reader ignorepath))]
    (when (validate-args parsed-args)
      (let [user-collection (build-user-collection mpath cachepath ignored-stuff)]
       (build-diff user-collection ignored-stuff lastfmpath outputpath)))))
