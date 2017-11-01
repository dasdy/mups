(ns mups.lastfm
  (:require [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [mups.libscan :refer [->Artist ->Album]]))

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

(defn album-response->album-info
  "transform body of last.fm album.getInfo http response into detailed album representation, for example
    {\"song-count\" 15
     \"album-url\" \"http://some-album-url\"
     \"image-url\" \"http://some-image-url\"}"
  [body]
  (let [decoded-body (json/parse-string body)]
    (when-not (is-error-response decoded-body)
      (let [song-count (-> decoded-body
                            (get "album")
                            (get "tracks")
                            (get "track")
                            (count))
            album-name (-> decoded-body
                           (get "album")
                           (get "name"))
            url (-> decoded-body
                    (get "album")
                    (get "url"))
            images  (-> decoded-body
                          (get "album")
                          (get "image"))
            image-url (and images
                           (get (last images) "#text"))]
        (->Album song-count
                 album-name
                 image-url
                 url)))))

(defn fetch-album-details
  "load album details for all authors in collections from last.fm"
  [collection]
  (into {}
        (map (fn [[author author-info]]
               (let [albums (keys (:albums author-info))
                     urls (map #(lastfm-get-detailed-album author %)
                               albums)
                     bodies (concur-get urls)]
                 (println "fetching detailed info for author: " author)
                 [author (assoc author-info
                                :albums
                                (into {}
                                      (map (fn [album body] [album (album-response->album-info body)])
                                           albums bodies)))]))
             collection)))

(defn albums-from-lastfm
  "transforming last.fm response into appropriate form"
  [lastfm-response]
  (let [author-name (get (get (get lastfm-response "topalbums") "@attr") "artist")
        albums (get (get lastfm-response "topalbums") "album")
        album-items (into {}
                          (map
                           (fn [x]
                             (let [album-name (get x "name")]
                               [(.toLowerCase album-name)
                                (->Album 1 ;; this will be updated in the next step,
                                         ;; for now this is 1 to make less requests
                                         album-name
                                         nil
                                         nil)]))
                           albums))]
    (->Artist author-name album-items nil)))

(defn remove-singles [collection]
  (let [album-is-single (fn [[album-name album-info]]
                          (let [song-count (:song-count album-info)]
                            (and (and song-count (> song-count 1))
                                 (not (re-find #"^.*?(single|\[single\]|\(single\))$" album-name)))))
        new-coll (map (fn [[author author-info]]
                        (let [albums (:albums author-info)
                              new-albums (filter album-is-single albums)
                              new-albums-map (into {} new-albums)]
                          [author (assoc author-info :albums new-albums-map)]))
                      collection)]
    (into {} new-coll)))

(defn author-response->author-info
  "transform body of last.fm artist.getInfo http response into basic artist representation, for example
  {\"album1\" {\"song-count\" 1}
   \"album2\" {\"song-count\" 1}}
  since the artist.getInfo does not contain track amount of album, result will only contain names of albums
  with \"song-count\" attribute equal to 1"
  [body]
  (let [decoded-body (json/parse-string body)]
    (when-not (is-error-response decoded-body)
      (albums-from-lastfm decoded-body))))

(defn get-authors-from-lastfm
  "fetch basic authors info (which albums they have) from last.fm"
  [collection]
  (let [authors (keys collection)
        urls (map lastfm-getalbums-url authors)
        bodies (concur-get urls)]
    (let [vals (map (fn [author body]
                    (let [author-info (author-response->author-info body)]
                      [author author-info]))
                    authors bodies)]
      (into {} vals))))
