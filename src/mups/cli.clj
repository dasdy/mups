(ns mups.cli
  (:gen-class)
  (:use mups.core
        mups.collection
        mups.diffgen)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :refer [file]]))

(def cli-options
  [["-m" "--music-path PATH" "Path to your music library"
    :default nil]
   ["-c" "--cached-path PATH" "Path to collection if you have already scanned library"
    :default "cache.json"]
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
  (if-not (or mpath cachepath)
    (println (str "You must specify at least one of --music-path or --cached-path options"))
    true))

(defn -main [& args]
  (let [[mpath cachepath outputpath ignorepath lastfmpath :as parsed-args]
        (parse-prog-options args)
        ignored-stuff (when (and ignorepath (.exists (file ignorepath)))
                           (read-collection collection-reader ignorepath))]
    (when (validate-args parsed-args)
      (let [user-collection (build-user-collection mpath cachepath ignored-stuff)
            diff (build-diff user-collection ignored-stuff lastfmpath outputpath)]
       (save-diff diff-writer diff outputpath)))))
