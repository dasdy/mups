(ns mups.diffgen
  (:require [cheshire.core :refer [generate-string create-pretty-printer
                                   default-pretty-print-options]]
            [mups.libscan :refer [album-title-key]]
            [mups.lastfm :refer [album-url-key image-url-key song-count-key]]
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

(defn album-info-html [album-info]
  (let [song-count (get album-info song-count-key nil)
        album-title (get album-info album-title-key)
        album-title-text (if song-count
                           (str "(" song-count ")" album-title)
                           (str "(?)" album-title))]
   [:div.album-info
    [:img {:src (get album-info image-url-key "")
           :height 120}]
    (if-let [album-url (get album-info album-url-key)]
      [:a {:href album-url} album-title-text]
      album-title-text)]))

(defn albums-list-html [albums]
  (let [album-htmls (map (fn [album] [:li (album-info-html album)]) albums)]
    [:ul.albums-list album-htmls]))

(defn diff-item-html [message diff-item]
  [:div.diff-item
   [:details [:summary (str message "(" (count diff-item) ")")]
    (albums-list-html diff-item)]])

(defn artist-name-elem [artist-name diff]
  (let [actual-artist-name (get diff :artist-name artist-name)]
   (if-let [artist-url (get diff :artist-url nil)]
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
