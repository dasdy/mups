(ns mp3-update-scanner.lastfm
  (:require [clojure.string :as str]
            [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(def http-client-parameters
  {:user-agent "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:46.0) Gecko/20100101 Firefox/46.0"})

(defn concur-get [urls]
  (let [qs (doall (map #(http/get % http-client-parameters)
                       urls))
        bodies (map (fn [resp] (:body @resp)) qs)]
    bodies))

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
              (print (str "got response for: " author-name ": " album-name))
              (let [decoded-body (json/parse-string body)]
                (when (not (is-error-response decoded-body))
                  (-> decoded-body
                      (get "album")
                      (get "tracks")
                      (get "track")
                      (count)))))))


(defn album-response->song-count
  [body]
  (let [decoded-body (json/parse-string body)]
    (when (not (is-error-response decoded-body))
      (-> decoded-body
          (get "album")
          (get "tracks")
          (get "track")
                      (count)))))

(defn update-song-counts [collection]
  (into {}
        (map (fn [[author albums]]
               (let [albums (keys albums)
                     urls (map #(lastfm-get-detailed-album author %)
                               albums)
                     bodies (concur-get urls)]
                 (println "updating counts for author: " author)
                 [author (into {}
                               (map (fn [album body] [album (album-response->song-count body)])
                                    albums bodies))]))
             collection)))

(defn albums-from-lastfm
  "transforming last.fm response into appropriate form"
  [lastfm-response]
  (->> (get (get lastfm-response "topalbums") "album")
       (reduce
        (fn [acc x]
          (let [album-name (get x "name")
                author-name ;; sometimes author name != the one that was requested,
                            ;; lastfm corrects it and some shit
                (-> x
                    (get "artist")
                    (get "name"))
                song-count 1] ;; this will be updated in the next step,
            ;; for now this is 1 to make less requests
            (assoc acc album-name song-count)))
        {})))

(defn remove-singles [collection]
  (into {}
        (map (fn [[author albums]]
               [author (into {} (filter (fn [[_ v]] (> v 1))
                                        albums))])
             collection)))

(defn get-lastfm-author-info
  "only for requesting info, returns decoded json from last.fm"
  [author-name]
  (http/get (lastfm-getalbums-url author-name)
            (fn [{:keys [body]}]
              (println (str "got response for: " author-name))
              (let [decoded-body (json/parse-string body)]
                (when (not (is-error-response decoded-body))
                  (albums-from-lastfm decoded-body))))))

(defn author-response->author-info
  [body]
  (let [decoded-body (json/parse-string body)]
    (when (not (is-error-response decoded-body))
      (albums-from-lastfm decoded-body))))

(defn get-authors-from-lastfm [collection]
  (let [authors (keys collection)
        urls (map lastfm-getalbums-url authors)
        bodies (concur-get urls)]
    (into {} (map (fn [author body]
                    [author (author-response->author-info body)])
                  authors bodies))))










