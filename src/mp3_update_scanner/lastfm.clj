(ns mp3-update-scanner.lastfm
  (:require [clojure.string :as str]
            [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))



(def api-key (let [res (io/resource "api-key")]
               (if (and res (.exists res)) (slurp res)
                   (System/getenv "LASTFM_API"))))

;; (def private-key (let [res (io/resource "shared-secret")]
;;                    (if (and res (.exists res))
;;                      (slurp res)
;;                      (System/getenv "LASTFM_PRIVATEKEY"))))

(defn lastfm-getalbums-url
  "url to get all albums of artist"
  [author-name]
  (str "http://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist="
       (java.net.URLEncoder/encode author-name "UTF-8")
       "&api_key=" (java.net.URLEncoder/encode api-key "UTF-8")
       "&format=json"))

(defn lastfm-get-detailed-album
  "url to get detail of certain album"
  [author-name album-name]
  (str "http://ws.audioscrobbler.com/2.0/?method=album.getinfo&api_key="
       (java.net.URLEncoder/encode api-key "UTF-8")
       "&artist=" (java.net.URLEncoder/encode author-name "UTF-8")
       "&album=" (java.net.URLEncoder/encode album-name "UTF-8")
       "&format=json"))

(defn is-error-response [body]
  (not (nil? (get body "error"))))

(defn album-song-count [author-name album-name]
  (http/get (lastfm-get-detailed-album author-name album-name)
            (fn [{:keys [body]}]
              (println (str "got response for: " author-name ": " album-name))
              (let [decoded-body (json/read-str body)]
                (when (not (is-error-response decoded-body))
                  (-> decoded-body
                      (get "album")
                      (get "tracks")
                      (get "track")
                      (count)))))))

(defn albums-from-lastfm
  "transforming last.fm response into appropriate form"
  [lastfm-response]
  (->> (get (get lastfm-response "topalbums") "album")
       (reduce
        (fn [acc x]
          (let [album-name (get x "name")
                author-name (-> x  ;; sometimes author name != the one that was requested,
                                   ;;lastfm corrects it and some shit
                                (get "artist")
                                (get "name"))
                song-count @(album-song-count author-name album-name)]
            (assoc acc album-name song-count)))
        {})
       (filter (fn [[_ v]] (> v 0)))
       (into {})))

(defn get-lastfm-author-info
  "only for requesting info, returns decoded json from last.fm"
  [author-name]
  (http/get (lastfm-getalbums-url author-name)
            (fn [{:keys [body]}]
              (println (str "got response for: " author-name))
              (let [decoded-body (json/read-str body)]
                (when (not (is-error-response decoded-body))
                  (println (str "response for " author-name " is actually ok!"))
                  (albums-from-lastfm decoded-body))))))

(defn get-authors-from-lastfm [collection]
  (into {} (map (fn [[k _]]
                  (println (str "looking up: " k))
                  [k @(get-lastfm-author-info k)]) collection)))
