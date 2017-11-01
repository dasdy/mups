(ns mups.diffgen
  (:require [cheshire.core :refer [generate-string create-pretty-printer
                                   default-pretty-print-options]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css]]))

(defmulti save-diff (fn [type _ _] type))

(defn make-json-diff-string [diff]
  (let [my-pretty-printer (create-pretty-printer
                           (assoc default-pretty-print-options
                                  :indent-arrays? true))]
    (generate-string
     (into (sorted-map)
           (map (fn [[artist-name artist-diff]]
                  [artist-name
                   (into (sorted-map)
                         (map (fn [[diff-mode albums]]
                                [diff-mode (sort-by (fn [album-info] (get album-info "title")) albums)])
                              artist-diff))])
                diff))
     {:pretty my-pretty-printer})))

(defmethod save-diff :json [_ diff path]
  (spit path (make-json-diff-string diff)))

(defn remove-nil [lst]
  (filter (complement nil?) lst))

(defn album-info-html [album-info]
  (let [song-count (:song-count album-info nil)
        album-title (:title album-info)
        album-title-text (if song-count
                           (str "(" song-count ")" album-title)
                           (str "(?)" album-title))
        image-url (:image-url album-info)
        image-item (if image-url
                     [:img {:src image-url :height 120}]
                     nil)
        album-url (:album-url album-info)
        album-item (if album-url
                     [:a {:href album-url} album-title-text]
                     album-title-text)]
    (remove-nil [:div.album-info image-item album-item])))

(defn albums-list-html [albums]
  (let [album-htmls (map (fn [album] [:li (album-info-html album)]) albums)]
    [:ul.albums-list album-htmls]))

(defn diff-item-html [message diff-item]
  [:div.diff-item
   [:details [:summary (str message "(" (count diff-item) ")")]
    (albums-list-html diff-item)]])

(defn artist-name-elem [artist-name diff]
  (let [actual-artist-name (:display-name diff artist-name)]
   (if-let [artist-url (:artist-url diff)]
     [:a {:href artist-url} actual-artist-name]
     actual-artist-name)))

(defn artist-list-html [artist-list]
  (map (fn [[artist-name diff]]
         (let [artist-elem (artist-name-elem artist-name diff)]
          [:div.artist artist-elem
           (diff-item-html "you have" (get diff "you have"))
           (diff-item-html "you miss" (get diff "you miss"))
           (diff-item-html "both have" (get diff "both have"))]))
       (sort-by (fn [[artist-name _]] artist-name)
                  artist-list)))

(defn grouped-artists-list-html [artist-list]
  (let [groups (group-by (fn [[artist-name _]]
                           (str (first artist-name)))
                         artist-list)
        sorted-groups (sort-by (fn [[first-letter _]] first-letter) groups)]
    (map (fn [[first-letter artist-list]]
           [:div.artist-list
            [:details
             [:summary first-letter]
             (artist-list-html artist-list)]])
         sorted-groups)))

(defn make-html-diff-string [diff]
  (html [:head (include-css "resources/public/css/albums-list.css")
         [:body (grouped-artists-list-html diff)]]))

(defmethod save-diff :html [_ diff path]
  (spit path (make-html-diff-string diff)))
