(defproject mups "0.2.0"
  :description "scan your media library; find if there are new albums by the artists you listen to"
  :url "https://github.com/dasdy/mups"
  :license {:name "MIT License"
            :url "https://mit-license.org/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.12.0"]
                 [claudio "0.1.3"]
                 [http-kit "2.7.0"]
                 [org.clojure/tools.cli "1.0.219"]
                 [mock-clj "0.2.1"]
                 [hiccup "1.0.5"]]
  :main mups.cli
  :aot [mups.cli]
  :target-path "target/%s"
  :resource-paths ["resources"]
  :jvm-opts ["-Xmx1g"]
  :profiles {:uberjar {:aot :all
                       :uberjar-name "mups.jar"}}
  :plugins [[lein-ring "0.12.1"] [lein-ancient "1.0.0-RC3"]])
