(ns konfo-backend.core
  (:require
    [konfo-backend.index.toteutus :as toteutus]
    [konfo-backend.index.koulutus :as koulutus]
    [konfo-backend.index.haku :as haku]
    [konfo-backend.index.hakukohde :as hakukohde]
    [konfo-backend.index.valintaperuste :as valintaperuste]
    [konfo-backend.eperuste.eperuste :as eperuste]
    [konfo-backend.search.koulutus.search :as koulutus-search]
    [konfo-backend.oppilaitos :as organisaatio]
    [konfo-backend.old-search.search :as old-search]
    [konfo-backend.palaute.palaute :as palaute]
    [konfo-backend.config :refer [config]]
    [konfo-backend.tools :refer [comma-separated-string->vec]]
    [clj-log.access-log :refer [with-access-logging]]
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.http-response :refer :all]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]))

(defn init []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "konfo-backend")
  (intern 'clj-log.error-log 'test false))

(def konfo-api
  (api
    {:swagger {:ui   "/konfo-backend"
               :spec "/konfo-backend/swagger.json"
               :data {:info {:title       "Konfo-backend"
                             :description "Backend for Konfo koulutusinformaatio UI."}}}}
    (context "/konfo-backend"
      []

      (GET "/healthcheck" [:as request]
        :summary "Healthcheck API"
        (with-access-logging request (ok "OK")))

      (context "/koulutus"
        []
        (GET "/:oid" [:as request]
          :summary "Hae koulutus oidilla"
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (koulutus/get oid)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/toteutus"
        []
        (GET "/:oid" [:as request]
          :summary "Hae toteutus oidilla"
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (toteutus/get oid)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/haku"
        []
        (GET "/:oid" [:as request]
          :summary "Hae haku oidilla"
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (haku/get oid)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/hakukohde"
        []
        (GET "/:oid" [:as request]
          :summary "Hae hakukohde oidilla"
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (hakukohde/get oid)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/valintaperuste"
        []
        (GET "/:id" [:as request]
          :summary "Hae valintaperuste id:llä"
          :path-params [id :- String]
          (with-access-logging request (if-let [result (valintaperuste/get id)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/kuvaus"
        []
        (GET "/:koulutuskoodi" [:as request]
          :summary "Hae koulutuksen kuvaus ePerusteista koulutuskoodin perusteella"
          :path-params  [koulutuskoodi :- String]
          :query-params [{osaamisalakuvaukset :- Boolean false}]
          (with-access-logging request (if-let [result (eperuste/get-kuvaus-by-koulutuskoodi-uri koulutuskoodi osaamisalakuvaukset)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/eperuste"
        []
        (GET "/:id" [:as request]
          :summary "Hae eperuste id:llä"
          :path-params [id :- String]
          (with-access-logging request (if-let [result (eperuste/get-eperuste-by-id id)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/oppilaitos"
        []
        (GET "/:oid" [:as request]
                 :summary "Oppilaitos API"
                 :path-params [oid :- String]
                 (with-access-logging request (if-let [res (organisaatio/get-oppilaitos oid)]
                                                (ok {:result res})
                                                (not-found "Not found")))))

      (context "/search" []
        (GET "/koulutukset" [:as request]
          :summary "Koulutus search API"
          :query-params [{keyword :- String nil}
                         {page :- Long 1}
                         {size :- Long 20}
                         {koulutustyyppi :- String nil}
                         {paikkakunta :- String nil}
                         {opetuskieli :- String nil}
                         {vainHakuKaynnissa :- Boolean false}
                         {lng :- String "fi"}]
          (with-access-logging request
            (let [koulutustyypit (comma-separated-string->vec koulutustyyppi)
                  opetuskielet   (comma-separated-string->vec opetuskieli)]
              (cond
                (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Invalid lng")
                (and (nil? keyword) (empty? koulutustyypit) (nil? paikkakunta) (empty? opetuskielet) (false? vainHakuKaynnissa)) (bad-request "Hakusana tai jokin rajain on pakollinen")
                (and (not (nil? keyword)) (> 3 (count keyword))) (bad-request "Hakusana on liian lyhyt")
                :else (ok (koulutus-search/search keyword
                                                  lng
                                                  page
                                                  size
                                                  :koulutustyyppi koulutustyypit
                                                  :paikkakunta paikkakunta
                                                  :opetuskieli opetuskielet
                                                  :vainHakuKaynnissa vainHakuKaynnissa))))))

        (GET "/oppilaitokset" [:as request]
          :summary "Oppilaitokset search API"
          :query-params [{keyword :- String nil}
                         {page :- Long 1}
                         {size :- Long 20}
                         {koulutustyyppi :- String nil}
                         {paikkakunta :- String nil}
                         {kieli :- String nil}
                         {lng :- String "fi"}]
          (with-access-logging request (if (some #{lng} ["fi" "sv" "en"])
                                         (ok (old-search/search-oppilaitos keyword lng page size
                                                                       (old-search/constraints :koulutustyyppi koulutustyyppi
                                                                                           :paikkakunta paikkakunta
                                                                                           :kieli kieli)))
                                         (bad-request "Invalid lng")))))

      (GET "/palaute" [:as request]
        :summary "GET palautteet"
        :query-params [{after :- Long 0}]
        (with-access-logging request (ok (palaute/get-palautteet after))))

      (POST "/palaute" [:as request]
        :summary "POST palaute"
        :form-params [{arvosana :- Long nil}
                      {palaute :- String ""}]
        (with-access-logging request (if (and arvosana (<= 1 arvosana 5))
                                       (ok {:result (palaute/post-palaute arvosana palaute)})
                                       (bad-request "Invalid arvosana")))))))

(def app
  (wrap-cors konfo-api :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post]))
