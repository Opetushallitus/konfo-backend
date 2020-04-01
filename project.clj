(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject konfo-backend "0.1.1-SNAPSHOT"
  :description "Konfo-backend"
  :repositories [["releases" "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"]
                 ["snapshots" "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"]]
  :managed-dependencies [[org.flatland/ordered "1.5.7"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-time "0.15.0"]
                 ; Rest + server
                 [metosin/compojure-api "1.1.11"]
                 [clj-http "3.10.0"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.641"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.642"]
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
                 [org.apache.logging.log4j/log4j-api "2.11.1"]
                 [org.apache.logging.log4j/log4j-core "2.11.1"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.11.1"]
                 [clj-log4j2 "0.2.0"]
                 ; Configuration
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [environ "1.1.0"]
                 [cprop "0.1.13"]
                 ; Contentful
                 [com.contentful.java/java-sdk "10.4.1"]
                 [commons-codec/commons-codec "1.13"]
                 ; Elasticsearch
                 [oph/clj-elasticsearch "0.2.2-SNAPSHOT"]]
  :env {:name "konfo-backend"}
  :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=dev-configuration/konfo-backend.edn"]
  :target-path "target/%s"
  :plugins [[lein-environ "1.1.0"]]
  :main konfo-backend.core
  :profiles {:dev {:plugins [[lein-cloverage "1.0.13" :exclusions [org.clojure/clojure]]]
                   :jvm-opts ["-Dport=3006"]}
             :updater {:jvm-opts ["-Dmode=updater" "-Dport=3006"]}
             :test {:dependencies [[ring/ring-mock "0.3.2"]
                                   [kouta-indeksoija-service "0.3.1-SNAPSHOT"]
                                   [fi.oph.kouta/kouta-backend "0.21-SNAPSHOT"]
                                   [fi.oph.kouta/kouta-backend "0.21-SNAPSHOT" :classifier "tests"]
                                   [org.mockito/mockito-core "2.28.2"]
                                   [oph/clj-test-utils "0.2.6-SNAPSHOT"]]
                    :injections [(require '[clj-test-utils.elasticsearch-mock-utils :as utils])
                                 (utils/global-elasticsearch-fixture)]}
             :ci-test {:dependencies [[ring/ring-mock "0.3.2"]
                                      [kouta-indeksoija-service "0.3.1-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "0.21-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "0.21-SNAPSHOT" :classifier "tests"]
                                      [org.mockito/mockito-core "2.28.2"]
                                      [oph/clj-test-utils "0.2.6-SNAPSHOT"]]
                       :injections [(require '[clj-test-utils.elasticsearch-mock-utils :as utils])
                                    (utils/global-elasticsearch-fixture)]
                       :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=ci-configuration/konfo-backend.edn"]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dconf=ci-configuration/konfo-backend.edn"]
                       :resource-paths ["oph-configuration" "resources"]}}
  :aliases {"run" ["with-profile" "+dev" "run"]
            "run-updater" ["with-profile" "+updater" "run"]
            "uberjar" ["do" "clean" ["uberjar"]]
            "test" ["with-profile" "+test" "test"]
            "ci-test" ["with-profile" "+ci-test" "test"]
            "cloverage" ["with-profile" "+test" "cloverage"]})
