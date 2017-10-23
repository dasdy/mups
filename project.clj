(defproject mp3-update-scanner "0.1.0-SNAPSHOT"
  :description "scan your media library; find if there are new albums by the artists you listen to"
  :url "https://github.com/dasdy/mp3-update-scanner"
  :license {:name "MIT License"
            :url "https://mit-license.org/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [zsau/id3 "0.1.1"]
                 [cheshire "5.7.0"]
                 [claudio "0.1.3"]
                 [http-kit "2.2.0"]
                 [clj-http "3.5.0"]
                 [compojure "1.5.2"]
                 [org.clojure/tools.cli "0.3.5"]
                 [selmer "1.10.7"]
                 [prone "1.1.4"]
                 [swiss-arrows "1.0.0"]
                 [hiccup "1.0.5"]]
  :main ^:skip-aot mp3-update-scanner.core
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {:uberjar {:aot :all
                       :uberjar-name "mups.jar"}
             :dev {:ring {:stacktrace-middleware prone.middleware/wrap-exceptions}}}
  :plugins [[lein-ring "0.11.0"]])
