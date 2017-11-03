(ns mups.data)

(defrecord Album [song-count title image-url album-url])
(defrecord Artist [display-name albums url])
(defrecord DiffItem [artist-name common-albums user-albums missing-albums])
(defrecord CollectionMapping [artist-mappings album-mappings])
