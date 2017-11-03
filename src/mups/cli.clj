(ns mups.cli
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [mups.core :refer [build-user-collection build-diff
                               diff-writer collection-reader
                               collection-writer]]
            [mups.collection :refer [save-collection read-collection make-name]]
            [mups.diffgen :refer [save-diff]]
            [mups.utils :refer [file-exists]]))

(def cli-options
  [["-m" "--music-path PATH" "Path to your music library."
    :default nil]
   ["-c" "--cached-path PATH" "Path to collection if you have already scanned library."
    :default nil]
   ["-o" "--output PATH" "Path to output (results of music scan). Defaults to diff.<file-format>, at the path of your execution."
    :default (make-name diff-writer "diff")]
   ["-l" "--lastfm PATH" "Output your collection with info, fetched from last.fm."
    :default nil]
   ["-i" "--ignore-path PATH" "Path to ignore file"
    :default nil]])

(defn parse-prog-options [args]
  (let [{:keys [music-path cached-path output lastfm ignore-path]}
        (:options (parse-opts args cli-options))]
    [music-path cached-path output ignore-path lastfm]))

(defn validate-args [[mpath cachepath _ _ _]]
  (if-not (or mpath cachepath)
    (println "You must specify at least one of --music-path or --cached-path options")
    true))

(defn -main [& args]
  (let [[mpath cachepath outputpath ignorepath lastfmpath :as parsed-args]
        (parse-prog-options args)
        ignored-stuff (when (file-exists ignorepath)
                        (read-collection collection-reader ignorepath))]
    (when (validate-args parsed-args)
      (let [user-collection (build-user-collection mpath cachepath ignored-stuff)
            diff (build-diff user-collection ignored-stuff lastfmpath)]
       (save-diff diff-writer diff outputpath)))))
