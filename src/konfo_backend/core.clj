(ns konfo-backend.core
  (:require
    [konfo-backend.config :refer [config]]
    [clj-log.access-log :refer [with-access-logging]]
    [clj-log.error-log] 
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
    [konfo-backend.external.api :as external]
    [konfo-backend.sitemap.api :as sitemap]
    [konfo-backend.suosikit.api :as suosikit]
    [clojure.string]
    [ring.util.response :as resp]
    [ring.swagger.swagger-ui :as ui])
  (:gen-class))

(defn init []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "konfo-backend")
  (intern 'clj-log.error-log 'test false))

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
      (ui/swagger-ui (merge {:swagger-docs "/konfo-backend/swagger.yaml"
                             :path "/konfo-backend/swagger"}
                            (:swagger-ui config))))

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
              |  description: 
               \"
               <p>Tämä sivu sisältää Opintopolun konfo-backend-rajapintojen Swagger-dokumentaation. </p>

               <p>Rajapintojen avulla voi hakea Opintopolussa julkaistua koulutus-, toteutus-, haku-, hakukohde- ja oppilaitostietoja. 
               Dokumentaatiosta löydät käytettävissä olevat rajapinnat, 
               niiden parametrikuvaukset sekä esimerkit kutsujen tekemiseen.</p>

               <p>Ohjeita rajapintojen kutsujalle: <a target='_blank' href='https://opintopolku.fi/konfo/fi/sivu/opintopolku-fi-rajapinnat'>https://opintopolku.fi/konfo/fi/sivu/opintopolku-fi-rajapinnat</a></p>

               <p>Jos sinulla on kysyttävää rajapinnan käytöstä, lähetä sähköpostia osoitteeseen <a href='mailto:palaute@opintopolku.fi'>palaute@opintopolku.fi</a></p>

               <p>Copyright (c) 2026 Finnish National Agency for Education</p>

               <p>Licensed under the EUPL, Version 1.2 or - as soon as they will be approved by the 
               European Commission - subsequent versions of the EUPL.</p>

               <p>You may obtain a copy of the EUPL at: <a target='_blank' href='https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12'>https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12</a> (edited) </p>

               \"
              |  version: 0.1-SNAPSHOT
              |servers:
              |  - url: /konfo-backend/
              |paths:
              "
              default/paths "\n"
              index/paths "\n"
              search/paths "\n"
              external/paths "\n"
              suosikit/paths "\n"
              sitemap/paths
              "
              |components:
              |  schemas:
              "
              default/schemas "\n"
              external/schemas "\n"
              search/schemas "\n"
              suosikit/schemas
              ))))

      (GET "/redoc/index.html" request
        :no-doc true
        (let [html (strip-margin
                     (str
                       "
                       | <html>
                       |   <head>
                       |     <title>Konfo-backend: API Documentation</title>
                       |   </head>
                       |   <body>
                       |     <redoc spec-url=\"/konfo-backend/swagger.yaml\"></redoc>
                       |     <script src=\"https://cdn.jsdelivr.net/npm/redoc/bundles/redoc.standalone.js\"></script>
                       |   </body>
                       | </html>
                       "))
              res (-> (resp/response html)
                       (resp/status 200)
                       (resp/header "Content-Type" "text/html"))]
          res))

      default/routes
      index/routes
      search/routes
      external/routes
      sitemap/routes
      suosikit/routes)))

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
