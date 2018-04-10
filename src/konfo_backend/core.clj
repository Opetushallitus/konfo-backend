(ns konfo-backend.core
  (:require
    [konfo-backend.koulutus :as koulutus]
    [clj-log.access-log :refer [with-access-logging]]
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.http-response :refer :all]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]))

(defn init []
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host "http://127.0.0.1:9200")
  (intern 'clj-log.access-log 'service "konfo-backend")
  (intern 'clj-log.error-log 'test false))

(def konfo-api
  (api
    {:swagger {:ui   "/konfo-backend"
               :spec "/konfo-backend/swagger.json"
               :data {:info {:title       "Konfo-backend"
                             :description "Backend for Konfo koulutusinformaatio UI."}}}
     ;;:exceptions {:handlers {:compojure.api.exception/default logging/error-handler*}}
     }
    (context "/konfo-backend" []
      (GET "/healthcheck" [:as request]
        :summary "Healthcheck API"
        (with-access-logging request (ok "OK")))

      (GET "/search" [:as request]
        :summary "Search API"
        :query-params [query :- String]
        (with-access-logging request (ok {:result (koulutus/text-search query)})))

      (GET "/koulutus/:oid" [:as request]
        :summary "Koulutus API"
        :path-params [oid :- String]
        (with-access-logging
          request
          (ok {:result (koulutus/get-koulutus-tulos oid)}))))))

(def app
  (wrap-cors konfo-api :access-control-allow-origin [#".*"] :access-control-allow-methods [:get]))
