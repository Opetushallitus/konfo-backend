(ns konfo-backend.core
  (:require
    [konfo-backend.config :refer [config]]
    [clj-log.access-log :refer [with-access-logging]]
    [konfo-backend.contentful.contentful :as contentful]
    [konfo-backend.contentful.updater-api :refer [konfo-updater-api]]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.adapter.jetty :refer [run-jetty]]
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.http-response :refer :all]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]
    [konfo-backend.elastic-tools :as e]
    [compojure.api.exception :as ex]
    [cheshire.core :as cheshire]
    [konfo-backend.default-api :as default]
    [konfo-backend.index.api :as index]
    [konfo-backend.search.api :as search]
    [konfo-backend.palaute.api :as palaute]
    [konfo-backend.external.api :as external]
    [clojure.string]
    [ring.swagger.swagger-ui :as ui])
  (:gen-class))

(defn init []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "konfo-backend")
  (intern 'clj-log.error-log 'test false)
  (e/update-aliases-on-startup))

(defn- ->error-response-message
  [message type]
  {:message (str "Got exception when trying to fulfill request: " message) :type type})

(defn exeption-handler
  [^Exception e data request]
  (if-let [error (some-> e (ex-data) :body (cheshire/parse-string true) :error)]
    (do (log/error "Got exception from ElasticSearch:" (:reason error) e)
        (internal-server-error (->error-response-message (:reason error) (:type error))))
    (do (log/error "Got unknown exception for request" (:uri request) e)
        (internal-server-error (->error-response-message (ex-message e) (.getName (class e)))))))

(defn strip-margin
  [str]
  (clojure.string/join "\n" (for [s (clojure.string/split-lines str)]
                              (clojure.string/replace s #"\s*\|" ""))))

(def konfo-api
  (api
    {:exceptions {:handlers {::ex/default exeption-handler}}}

    (undocumented
      (ui/swagger-ui {:swagger-docs "/konfo-backend/swagger.yaml"
                      :path "/konfo-backend/swagger"}))

    (context "/konfo-backend"
      []

      (GET "/swagger.yaml" request
        :no-doc true
        (ok
          (strip-margin
            (str
              "
              |openapi: 3.0.0
              |info:
              |  title: konfo-backend
              |  description: \"Opintopolun oppijan puolen koulutustarjonta\"
              |  version: 0.1-SNAPSHOT
              |  termsOfService: https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/
              |  contact:
              |    name: \"\"
              |    email: verkkotoimitus_opintopolku@oph.fi
              |    url: \"\"
              |  license:
              |    name: \"EUPL 1.1 or latest approved by the European Commission\"
              |    url: \"http://www.osor.eu/eupl/\"
              |servers:
              |  - url: /konfo-backend/
              |  - url: http://localhost:3006/konfo-backend/
              |  - url: https://beta.opintopolku.fi/konfo-backend/
              |  - url: https://beta.untuvaopintopolku.fi/konfo-backend/
              |  - url: https://beta.hahtuvaopintopolku.fi/konfo-backend/
              |  - url: https://beta.testiopintopolku.fi/konfo-backend/
              |paths:
              "
              default/paths "\n"
              index/paths "\n"
              search/paths "\n"
              palaute/paths "\n"
              external/paths
              "
              |components:
              |  schemas:
              "
              default/schemas "\n"
              external/schemas
              ))))

      default/routes
      index/routes
      search/routes
      palaute/routes
      external/routes)))

(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error "Something really bad happened" e)
        {:status 500 :body (cheshire/generate-string (->error-response-message (ex-message e) (.getName (class e))))}))))

(def app
  (-> konfo-api
      (wrap-cors :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post])
      (wrap-exception-handling)))

(defn -main [& args]
      (log/info "Running app")
  (if (= (System/getProperty "mode") "updater")
      (do
        (log/info "Starting Konfo-backend-updater!")
        (let [clients (contentful/create-contentful-clients false)
            updater-app (wrap-cors (konfo-updater-api clients) :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post])]
           (log/info "Using clients for langs " (keys clients))
           (run-jetty (wrap-reload updater-app)
                      {:port (Integer/valueOf
                               (or (System/getenv "port")
                                   (System/getProperty "port")
                                   "8080"))})))
    (do
      (log/info "Starting Konfo-backend!")
      (init)
      (run-jetty (wrap-reload #'app)
        {:port (Integer/valueOf
                 (or (System/getenv "port")
                   (System/getProperty "port")
                   "8080"))}))))