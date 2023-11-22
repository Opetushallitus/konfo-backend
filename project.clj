(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject konfo-backend "0.2.1-SNAPSHOT"
  :description "Konfo-backend"
  :repositories [["releases" "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"]
                 ["snapshots" "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"]]
  :managed-dependencies [[joda-time "2.10.5"]
                         [commons-io "2.11.0"]
                         [com.squareup.okhttp3/okhttp "3.14.9"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.12.7"]
                         [com.fasterxml.jackson.core/jackson-core "2.12.7"]
                         [com.fasterxml.jackson.core/jackson-databind "2.12.7"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-time "0.15.2"]
                 [org.clojure/core.memoize "1.0.257"]
                 ; Rest + server
                 [metosin/compojure-api "2.0.0-alpha31"]
                 [metosin/ring-swagger-ui "4.18.1"]
                 [metosin/schema-tools "0.13.1"]
                 [clj-commons/clj-yaml "1.0.27"]
                 [clj-http "3.12.3"]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.550"]
                 [com.amazonaws/aws-java-sdk-sqs "1.12.550"]
                 [compojure "1.7.0"]
                 [lambdaisland/uri "1.15.125"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-devel "1.10.0"]
                 [ring-cors "0.1.13"]
                 ; Logging
                 [oph/clj-log "0.3.2-SNAPSHOT" :exclusions [org.scala-lang/scala-library]]
                 [ring-basic-authentication "1.2.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.apache.logging.log4j/log4j-api "2.20.0"]
                 [org.apache.logging.log4j/log4j-core "2.20.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.20.0"]
                 [clj-log4j2 "0.4.0"]
                 ; Configuration
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [environ "1.2.0"]
                 [cprop "0.1.19"]
                 ; Contentful
                 [com.contentful.java/java-sdk "10.5.15"]
                 [commons-codec/commons-codec "1.16.0"]
                 ; Elasticsearch
                 [oph/clj-elasticsearch "0.5.4-SNAPSHOT" :exclusions [org.scala-lang/scala-library]]
                 [mount "0.1.17"]
                 [org.clojure/data.xml "0.0.8"]]
  :env {:name "konfo-backend"}
  :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=dev-configuration/konfo-backend.edn"]
  :target-path "target/%s"
  :plugins [[lein-environ "1.2.0"]
            [lein-auto "0.1.3"]
            [lein-zprint "1.2.7"]
            [lein-cljfmt "0.8.0"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.7.0"]
            [com.jakemccrary/lein-test-refresh "0.24.1"]]
  :main konfo-backend.core
  :profiles {:dev {:plugins [[lein-cloverage "1.0.13" :exclusions [org.clojure/clojure]]]
                   :jvm-opts ["-Dport=3006"]}
             :updater {:jvm-opts ["-Dmode=updater" "-Dport=3006"]}
             :test {:dependencies [[ring/ring-mock "0.4.0"]
                                   [oph/clj-test-utils "0.5.6-SNAPSHOT"]
                                   [org.mockito/mockito-all "1.10.19"]
                                   [clj-http-fake "1.0.4"]
                                   [net.java.dev.jna/jna "5.13.0"]
                                   [io.swagger.parser.v3/swagger-parser "2.1.16"]
                                   [com.fasterxml.jackson.dataformat/jackson-dataformat-yaml "2.15.2"]
                                   [com.fasterxml.jackson.core/jackson-annotations "2.15.2"]
                                   [nubank/matcher-combinators "3.8.8"]]
                    :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=ci-configuration/konfo-backend.edn"]
                    :injections [(require '[clj-test-utils.elasticsearch-docker-utils :as utils])
                                 (require '[clj-elasticsearch.elastic-utils :as eutils])
                                 (when (not (System/getenv "WITHOUT_ELASTIC"))
                                   (if-let [elasticPort (java.lang.System/getenv "elasticPort")]
                                     (do
                                       (prn "Using Elastic from port " elasticPort)
                                       (intern 'clj-elasticsearch.elastic-utils 'elastic-host (str "http://127.0.0.1:" elasticPort)))
                                     (utils/global-docker-elastic-fixture)))]}
             :only-unit-tests {:test-selectors {:default (fn [m]
                                                           (or (clojure.string/includes? (str (:ns m))
                                                                                         "unit-test")
                                                               (clojure.string/includes? (str (:name m))
                                                                                         "unit-test")))}}
             :only-integration-tests {:test-selectors {:default (fn [m]
                                                                  (not (or (clojure.string/includes? (str (:ns m))
                                                                                                     "unit-test")
                                                                           (clojure.string/includes? (str (:name m))
                                                                                                     "unit-test"))))}}
             :uberjar {:aot :all
                       :jvm-opts ["-Dconf=ci-configuration/konfo-backend.edn"]
                       :resource-paths ["oph-configuration" "resources"]}}
  :aliases {"run" ["with-profile" "+dev" "run"]
            "run-updater" ["with-profile" "+updater" "run"]
            "uberjar" ["do" "clean" ["uberjar"]]
            "test" ["with-profile" "+test" "test"]
            "test-unit" ["shell" "without-elastic" "with-profile" "+test,+only-unit-tests" "test"]
            "test-integration" ["with-profile" "+test,+only-integration-tests", "test"]
            "auto-test" ["with-profile" "+test" "auto" "test"]
            "test-reload" ["with-profile" "+test" "test-refresh"]
            "cloverage" ["with-profile" "+test" "cloverage"]}
  :shell {:commands {"without-elastic" {:default-command "lein" :env {"WITHOUT_ELASTIC" "true"}}}}
  :zprint {:width 100 :old? false :style :community :map {:comma? false}})
