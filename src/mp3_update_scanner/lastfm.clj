(ns mp3-update-scanner.lastfm
  (:use clojure.java.io)
  (:require [clojure.string :as str]))



(def api-key (slurp (clojure.java.io/resource "api-key")))
(def private-key (slurp (clojure.java.io/resource "shared-secret")))
