(ns mp3-update-scanner.lastfm
  (:use clojure.java.io)
  (:require [clojure.string :as str]))



(def api-key (let [res (clojure.java.io/resource "api-key")]
               (if (and res (.exists res)) (slurp res)
                   (System/getenv "LASTFM_API"))))
(def private-key (let [res (clojure.java.io/resource "shared-secret")]
                   (if (and res (.exists res))
                     (slurp res)
                     (System/getenv "LASTFM_PRIVATEKEY"))))
