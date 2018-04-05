(ns konfo-backend.core
  (:require
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.http-response :refer :all]
    [clojure.tools.logging :as log]))

(def konfo-api
  (api
    {:swagger {:ui   "/konfo-backend"
               :spec "/konfo-backend/swagger.json"
               :data {:info {:title       "Konfo-backend"
                             :description "Backend for Konfo koulutusinformaatio UI."}}}
     ;;:exceptions {:handlers {:compojure.api.exception/default logging/error-handler*}}
     }
    (context "/konfo-backend" []
      (GET "/healthcheck" []
        :summary "Healthcheck API"
        (ok "OK"))

      (GET "/search" []
        :summary "Search API"
        :query-params [query :- String]
        (ok "OK"))

      (GET "/koulutus/:oid" []
        :summary "Koulutus API"
        :path-params [oid :- String]
        (log/info (str "koulutus " oid " fetch"))
        (ok "OK")))))

(def app
  (wrap-cors konfo-api :access-control-allow-origin [#".*"] :access-control-allow-methods [:get]))
