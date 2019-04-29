(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject konfo-backend "0.1.0-SNAPSHOT"
  :description "Konfo-backend"
  :repositories [["releases" "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"]
                 ["snapshots" "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-time "0.15.0"]
                 ; Rest + server
                 [metosin/compojure-api "1.1.11"]
                 [compojure "1.6.1"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring-cors "0.1.13"]
                 ; Logging
                 [oph/clj-log "0.2.0-SNAPSHOT"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.apache.logging.log4j/log4j-api "2.11.1"]
                 [org.apache.logging.log4j/log4j-core "2.11.1"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.11.1"]
                 [clj-log4j2 "0.2.0"]
                 ; Configuration
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [environ "1.1.0"]
                 [cprop "0.1.13"]
                 ; Elasticsearch
                 [oph/clj-elasticsearch "0.2.0-SNAPSHOT"]]
  :ring {:handler konfo-backend.core/app
         :init konfo-backend.core/init
         ;:destroy konfo-backend.core/destroy
         :browser-uri "konfo-backend"}
  :env {:name "konfo-backend"}
  :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=dev-configuration/konfo-backend.edn"]
  :target-path "target/%s"
  :plugins [[lein-ring "0.12.4"]
            [lein-environ "1.1.0"]]
  :profiles {:dev {:plugins [[lein-cloverage "1.0.13" :exclusions [org.clojure/clojure]]]}
             :test {:dependencies [[robert/hooke "1.3.0"]
                                   [ring/ring-mock "0.3.2"]
                                   [konfo-indeksoija-service "0.1.3-SNAPSHOT"]
                                   [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT"]
                                   [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT" :classifier "tests"]
                                   [oph/clj-test-utils "0.2.0-SNAPSHOT"]]
                    :injections [(require '[robert.hooke :as hooke] '[clj-test-utils.elasticsearch-mock-utils :as utils])
                                 (defn run-all-test-hook [f & nss]
                                   (utils/init-elastic-test)
                                   (let [result (apply f nss)]
                                     (utils/stop-elastic-test)
                                     result))
                                 (hooke/add-hook #'clojure.test/run-tests #'run-all-test-hook)]}
             :ci-test {:dependencies [[robert/hooke "1.3.0"]
                                      [ring/ring-mock "0.3.2"]
                                      [konfo-indeksoija-service "0.1.3-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT" :classifier "tests"]
                                      [oph/clj-test-utils "0.2.0-SNAPSHOT"]]
                       :injections [(require '[robert.hooke :as hooke] '[clj-test-utils.elasticsearch-mock-utils :as utils])
                                    (defn run-all-test-hook [f & nss]
                                      (utils/init-elastic-test)
                                      (let [result (apply f nss)]
                                        (utils/stop-elastic-test)
                                        result))
                                    (hooke/add-hook #'clojure.test/run-tests #'run-all-test-hook)]
                       :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=ci-configuration/konfo-backend.edn"]}
             :uberjar {:ring {:port 8080}}}
  :aliases {"run" ["ring" "server" "3006"]
            "uberjar" ["do" "clean" ["ring" "uberjar"]]
            "test" ["with-profile" "+test" "test"]
            "ci-test" ["with-profile" "+ci-test" "test"]
            "cloverage" ["with-profile" "+test" "cloverage"]})


