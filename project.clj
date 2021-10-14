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
                 [oph/clj-elasticsearch "0.3.3-SNAPSHOT"]]
  :env {:name "konfo-backend"}
  :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=dev-configuration/konfo-backend.edn"]
  :target-path "target/%s"
  :plugins [[lein-environ "1.1.0"]
            [lein-auto "0.1.3"]
            [com.jakemccrary/lein-test-refresh "0.24.1"]]
  :main konfo-backend.core
  :profiles {:dev {:plugins [[lein-cloverage "1.0.13" :exclusions [org.clojure/clojure]]]
                   :jvm-opts ["-Dport=3006"]}
             :updater {:jvm-opts ["-Dmode=updater" "-Dport=3006"]}
             :test {:dependencies [[ring/ring-mock "0.3.2"]
                                   [kouta-indeksoija-service "8.3.0-SNAPSHOT"]
                                   [fi.oph.kouta/kouta-backend "6.16.0-SNAPSHOT"]
                                   [fi.oph.kouta/kouta-backend "6.16.0-SNAPSHOT" :classifier "tests"]
                                   [fi.oph.kouta/kouta-common "2.5.0-SNAPSHOT" :classifier "tests"]
                                   [org.mockito/mockito-core "2.28.2"]
                                   [oph/clj-test-utils "0.3.0-SNAPSHOT"]]
                    :injections [(require '[clj-test-utils.elasticsearch-docker-utils :as utils])
                                 (require '[clj-elasticsearch.elastic-utils :as eutils])
                                 (if-let [elasticPort (java.lang.System/getenv "elasticPort")]
                                   (do
                                     (prn "Using Elastic from port " elasticPort)
                                     (intern 'clj-elasticsearch.elastic-utils 'elastic-host (str "http://127.0.0.1:" elasticPort)))
                                   (utils/global-docker-elastic-fixture))]}
             :ci-test {:dependencies [[ring/ring-mock "0.3.2"]
                                      [kouta-indeksoija-service "8.3.0-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "6.16.0-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "6.16.0-SNAPSHOT" :classifier "tests"]
                                      [fi.oph.kouta/kouta-common "2.5.0-SNAPSHOT" :classifier "tests"]
                                      [org.mockito/mockito-core "2.28.2"]
                                      [oph/clj-test-utils "0.3.0-SNAPSHOT"]]
                       :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=ci-configuration/konfo-backend.edn"]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dconf=ci-configuration/konfo-backend.edn"]
                       :resource-paths ["oph-configuration" "resources"]}}
  :aliases {"run" ["with-profile" "+dev" "run"]
            "run-updater" ["with-profile" "+updater" "run"]
            "uberjar" ["do" "clean" ["uberjar"]]
            "test" ["with-profile" "+test" "test"]
            "ci-test" ["with-profile" "+ci-test" "test"]
            "auto-test" ["with-profile" "+ci-test" "auto" "test"]
            "test-reload" ["with-profile" "+ci-test" "test-refresh"]
            "cloverage" ["with-profile" "+test" "cloverage"]})
