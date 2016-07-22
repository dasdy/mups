(defproject mp3-update-scanner "0.1.0-SNAPSHOT"
  :description "scan your media library; find if there are new albums by the artists you listen to"
  :url "https://github.com/dasdy/mp3-update-scanner"
  :license {:name "MIT License"
            :url "https://mit-license.org/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [zsau/id3 "0.1.1"]
                 [cheshire "5.6.3"]
                 [claudio "0.1.3"]
                 [http-kit "2.2.0"]
                 [clj-http "3.1.0"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main ^:skip-aot mp3-update-scanner.core
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {:uberjar {:aot :all}})
