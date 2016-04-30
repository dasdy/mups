(ns mp3-update-scanner.core
  (:gen-class)
  (:use clojure.java.io
        mp3-update-scanner.libscan)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.data.json :as json]
            [mp3-update-scanner.lastfm :as lastfm]))

(defn save-collection [collection path]
  (spit path (json/write-str collection)))

(defn read-collection [path]
  (json/read-str (slurp path)))

(defn remove-trailing-0 [string]
  (apply str (remove #(= (int %) 0) string)))

(def cli-options
  [["-m" "--music-path PATH" "Path to your music library"
    :default nil]
   ["-c" "--cached-path PATH" "Path to collection if you have already scanned library"
    :default nil]
   ["-i" "--ignore-path PATH" "Path to ignore file"
    :default nil]])

(defn parse-prog-options [args]
  (let [{mpath :music-path
         cachepath :cached-path
         ignorepath :ignore-path} (:options (parse-opts args cli-options))]
    [mpath cachepath ignorepath]))

(defn -main
  [& args]
  (let [[mpath cachepath ignorepath] (parse-prog-options args)]
    (if (not (or mpath cachepath))
      (println (str "You must specify at least one of --music-path or --cached-path options"))
      (-> (if mpath (get-all-mp3-tags-in-dir mpath) [])
          (build-collection (if (and cachepath (.exists (file cachepath)))
                              (read-collection cachepath)
                              {}))
          (save-collection (or cachepath "out.json"))))))
