(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject konfo-backend "0.2.1-SNAPSHOT"
  :description "Konfo-backend"
  :repositories [["releases" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"
                              :username :env/artifactory_username
                              :password :env/artifactory_password
                              :sign-releases false
                              :snapshots false}]
                 ["snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"
                               :username :env/artifactory_username
                               :password :env/artifactory_password
                               :sign-releases false
                               :snapshots true}]]
  :managed-dependencies [[org.clojure/clojure "1.11.4"]
                         [org.clojure/core.memoize "1.1.266"]
                         [clj-time "0.15.2"]
                         [joda-time "2.13.1"]
                         [commons-io "2.18.0"]
                         [cheshire "5.13.0"]
                         [metosin/compojure-api "2.0.0-alpha31"]
                         [metosin/ring-swagger-ui "5.20.0"]
                         [metosin/schema-tools "0.13.1"]
                         [clj-commons/clj-yaml "1.0.29"]
                         [clj-http "3.13.0"]
                         [com.amazonaws/aws-java-sdk-s3 "1.12.782"]
                         [com.amazonaws/aws-java-sdk-sqs "1.12.782"]
                         [compojure "1.7.1"]
                         [lambdaisland/uri "1.19.155"]
                         [ring/ring-core "1.13.0"]
                         [ring/ring-jetty-adapter "1.13.0"]
                         [ring/ring-devel "1.13.0"]
                         [ring-cors "0.1.13"]
                         [oph/clj-log "0.3.2-SNAPSHOT" :exclusions [io.findify/s3mock_2.12
                                                                    pl.allegro.tech/embedded-elasticsearch]]
                         [ring-basic-authentication "1.2.0"]
                         [org.clojure/tools.logging "1.3.0"]
                         [org.apache.logging.log4j/log4j-api "2.24.3"]
                         [org.apache.logging.log4j/log4j-core "2.24.3"]
                         [org.apache.logging.log4j/log4j-slf4j-impl "2.24.3"]
                         [clj-log4j2 "0.4.0"]
                         [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                         [environ "1.2.0"]
                         [cprop "0.1.20"]
                         [com.contentful.java/java-sdk "10.5.21"]
                         [commons-codec/commons-codec "1.18.0"]
                         [oph/clj-elasticsearch "0.5.4-SNAPSHOT" :exclusions [io.findify/s3mock_2.12
                                                                              pl.allegro.tech/embedded-elasticsearch]]
                         [mount "0.1.21"]
                         [org.clojure/data.xml "0.0.8"]

                         ; oph:clj-test-utils transitive dependency
                         [org.apache.commons/commons-compress "1.21"]

                         ; compojure transitive dependency
                         [instaparse "1.5.0"]

                         [com.squareup.okhttp3/okhttp "3.14.9"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.18.3"]
                         [com.fasterxml.jackson.core/jackson-core "2.18.3"]
                         [com.fasterxml.jackson.core/jackson-databind "2.18.3"]]
  :dependencies [[org.clojure/clojure]
                 [clj-time]
                 [org.clojure/core.memoize]
                 [cheshire]
                 ; Rest + server
                 [metosin/compojure-api]
                 [metosin/ring-swagger-ui]
                 [metosin/schema-tools]
                 [clj-commons/clj-yaml]
                 [clj-http]
                 [com.amazonaws/aws-java-sdk-s3]
                 [com.amazonaws/aws-java-sdk-sqs]
                 [compojure]
                 [lambdaisland/uri]
                 [ring/ring-core]
                 [ring/ring-jetty-adapter]
                 [ring/ring-devel]
                 [ring-cors]
                 ; Logging
                 [oph/clj-log]
                 [ring-basic-authentication]
                 [org.clojure/tools.logging]
                 [org.apache.logging.log4j/log4j-api]
                 [org.apache.logging.log4j/log4j-core]
                 [org.apache.logging.log4j/log4j-slf4j-impl]
                 [clj-log4j2]
                 ; Configuration
                 [fi.vm.sade.java-utils/java-properties]
                 [environ]
                 [cprop]
                 ; Contentful
                 [com.contentful.java/java-sdk]
                 [commons-codec/commons-codec]
                 ; Elasticsearch
                 [oph/clj-elasticsearch]
                 [mount]
                 [org.clojure/data.xml]]
  :env {:name "konfo-backend"}
  :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=dev-configuration/konfo-backend.edn"]
  :target-path "target/%s"
  :plugins [[lein-environ "1.2.0"]
            [lein-auto "0.1.3"]
            [lein-zprint "1.2.7"]
            [lein-cljfmt "0.8.0"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.7.0"]
            ]
  :main konfo-backend.core
  :profiles {:dev {:plugins [[lein-cloverage "1.0.13" :exclusions [org.clojure/clojure]]]
                   :jvm-opts ["-Dport=3006"]}
             :updater {:jvm-opts ["-Dmode=updater" "-Dport=3006"]}
             :test {:dependencies [[ring/ring-mock "0.4.0"]
                                   [oph/clj-test-utils "0.5.6-SNAPSHOT"]
                                   [org.mockito/mockito-core "5.16.1"]
                                   [clj-http-fake "1.0.4"]
                                   [net.java.dev.jna/jna "5.17.0"]
                                   [io.swagger.parser.v3/swagger-parser "2.1.25"]
                                   [com.fasterxml.jackson.dataformat/jackson-dataformat-yaml "2.18.3"]
                                   [com.fasterxml.jackson.core/jackson-annotations "2.18.3"]
                                   [nubank/matcher-combinators "3.9.1"]
                                   [lambdaisland/kaocha "1.91.1392"]]
                    :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=ci-configuration/konfo-backend.edn"]
                    :injections [(require '[clojure.test]
                                          '[clj-test-utils.elasticsearch-docker-utils :as utils])
                                 (utils/global-docker-elastic-fixture)]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dconf=ci-configuration/konfo-backend.edn"]
                       :resource-paths ["oph-configuration" "resources"]}}
  :aliases {"run" ["with-profile" "+dev" "run"]
            "run-updater" ["with-profile" "+updater" "run"]
            "uberjar" ["do" "clean" ["uberjar"]]
            "test" ["with-profile" "+test" ["run" "-m" "konfo-backend.kaocha/run"]]
            "cloverage" ["with-profile" "+test" "cloverage"]}
  :zprint {:width 100 :old? false :style :community :map {:comma? false}})
