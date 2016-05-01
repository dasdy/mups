(ns mp3-update-scanner.lastfm
  (:use clojure.java.io)
  (:require [clojure.string :as str]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]))



(def api-key (let [res (clojure.java.io/resource "api-key")]
               (if (and res (.exists res)) (slurp res)
                   (System/getenv "LASTFM_API"))))

;; (def private-key (let [res (clojure.java.io/resource "shared-secret")]
;;                    (if (and res (.exists res))
;;                      (slurp res)
;;                      (System/getenv "LASTFM_PRIVATEKEY"))))

(defn lastfm-getalbums-url [author-name]
  (str "http://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist="
       (java.net.URLEncoder/encode author-name)
       "&api_key=" (java.net.URLEncoder/encode api-key)
       "&format=json"))

(defn get-lastfm-author-info [author-name]
  "only for requesting info, returns decoded json from last.fm"
  (http/get (lastfm-getalbums-url author-name)
            (fn [{:keys [body]}]
              (let [decoded-body (json/read-str body)]
                (when (not (is-error-response decoded-body))
                   (albums-from-lastfm decoded-body))))))

(defn is-error-response [body]
  (not (nil? (get body "error"))))

(defn albums-from-lastfm [lastfm-response]
  "transforming last.fm response into appropriate form"
  (reduce (fn [acc x] (assoc acc (get x "name") 1))
          {}
          (get (get lastfm-response "topalbums") "album")))










