(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject konfo-backend "0.2.0-SNAPSHOT"
  :description "Konfo-backend"
  :repositories [["releases" "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"]
                 ["snapshots" "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"]]
  :managed-dependencies [[org.flatland/ordered "1.5.7"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-time "0.15.0"]
                 [org.clojure/core.memoize "1.0.236"]
                 ; Rest + server
                 [metosin/compojure-api "2.0.0-alpha31"]
                 [metosin/ring-swagger-ui "3.25.3"]
                 [clj-http "3.10.0"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.913"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.913"]
                 [compojure "1.6.1"]
                 [lambdaisland/uri "1.1.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring/ring-devel "1.7.1"]
                 [ring-cors "0.1.13"]
                 ; Logging
                 [oph/clj-log "0.2.3-SNAPSHOT"]
                 [ring-basic-authentication "1.0.5"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.apache.logging.log4j/log4j-api "2.17.0"]
                 [org.apache.logging.log4j/log4j-core "2.17.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.17.0"]
                 [clj-log4j2 "0.3.0"]
                 ; Configuration
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [environ "1.1.0"]
                 [cprop "0.1.13"]
                 ; Contentful
                 [com.contentful.java/java-sdk "10.4.1"]
                 [commons-codec/commons-codec "1.13"]
                 ; Elasticsearch
                 [oph/clj-elasticsearch "0.5.0-SNAPSHOT"]
                 [mount "0.1.11"]
                 [org.clojure/data.xml "0.0.8"]]
  :env {:name "konfo-backend"}
  :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=dev-configuration/konfo-backend.edn"]
  :target-path "target/%s"
  :plugins [[lein-environ "1.1.0"]
            [lein-auto "0.1.3"]
            [lein-zprint "1.2.0"]
            [lein-cljfmt "0.8.0"]
            [com.jakemccrary/lein-test-refresh "0.24.1"]]
  :main konfo-backend.core
  :profiles {:dev {:plugins [[lein-cloverage "1.0.13" :exclusions [org.clojure/clojure]]]
                   :jvm-opts ["-Dport=3006"]}
             :updater {:jvm-opts ["-Dmode=updater" "-Dport=3006"]}
             :test {:dependencies [[ring/ring-mock "0.3.2"]
                                   [oph/clj-test-utils "0.5.0-SNAPSHOT"]
                                   [org.mockito/mockito-all "1.9.5"]
                                   [clj-http-fake "1.0.3"]
                                   [pjstadig/humane-test-output "0.11.0"]]
                    :injections [(require '[clj-test-utils.elasticsearch-docker-utils :as utils])
                                 (require '[clj-elasticsearch.elastic-utils :as eutils])
                                 (if-let [elasticPort (java.lang.System/getenv "elasticPort")]
                                   (do
                                     (prn "Using Elastic from port " elasticPort)
                                     (intern 'clj-elasticsearch.elastic-utils 'elastic-host (str "http://127.0.0.1:" elasticPort)))
                                   (utils/global-docker-elastic-fixture))
                                 (require 'pjstadig.humane-test-output)
                                 (pjstadig.humane-test-output/activate!)]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dconf=ci-configuration/konfo-backend.edn"]
                       :resource-paths ["oph-configuration" "resources"]}}
  :aliases {"run" ["with-profile" "+dev" "run"]
            "run-updater" ["with-profile" "+updater" "run"]
            "uberjar" ["do" "clean" ["uberjar"]]
            "test" ["with-profile" "+test" "test"]
            "auto-test" ["with-profile" "+test" "auto" "test"]
            "test-reload" ["with-profile" "+test" "test-refresh"]
            "cloverage" ["with-profile" "+test" "cloverage"]}
  :zprint {:width 100 :old? false :style :community :map {:comma? false}})
