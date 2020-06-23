(ns konfo-backend.core
  (:require
    [konfo-backend.config :refer [config]]
    [konfo-backend.index.toteutus :as toteutus]
    [konfo-backend.index.koulutus :as koulutus]
    [konfo-backend.contentful.contentful :as contentful]
    [konfo-backend.index.haku :as haku]
    [konfo-backend.index.hakukohde :as hakukohde]
    [konfo-backend.contentful.updater-api :refer [konfo-updater-api]]
    [konfo-backend.index.valintaperuste :as valintaperuste]
    [konfo-backend.index.oppilaitos :as oppilaitos]
    [konfo-backend.eperuste.eperuste :as eperuste]
    [konfo-backend.index.lokalisointi :as lokalisointi]
    [konfo-backend.search.koulutus.search :as koulutus-search]
    [konfo-backend.palaute.sqs :as sqs]
    [schema.core :as s]
    [konfo-backend.search.oppilaitos.search :as oppilaitos-search]
    [konfo-backend.search.filters :as filters]
    [konfo-backend.palaute.palaute :as palaute]
    [konfo-backend.suosittelu.service :as suosittelu]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.adapter.jetty :refer [run-jetty]]
    [konfo-backend.tools :refer [comma-separated-string->vec]]
    [clj-log.access-log :refer [with-access-logging]]
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.http-response :refer :all]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]
    [konfo-backend.elastic-tools :as e]
    [compojure.api.exception :as ex]
    [cheshire.core :as cheshire])
  (:gen-class))

(s/defschema ClientError {:error-message s/Str
                          :url s/Str
                          :line s/Int
                          :col s/Int
                          :user-agent s/Str
                          :stack s/Str})


(defn init []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "konfo-backend")
  (intern 'clj-log.error-log 'test false)
  (e/update-aliases-on-startup))

(defonce sqs-client (sqs/create-sqs-client))

(defn- ->search-with-validated-params
  [f keyword lng page size sort order koulutustyyppi sijainti opetuskieli koulutusala]
  (let [koulutustyypit      (comma-separated-string->vec koulutustyyppi)
        sijainnit           (comma-separated-string->vec sijainti)
        opetuskielet        (comma-separated-string->vec opetuskieli)
        koulutusalat        (comma-separated-string->vec koulutusala)]

    (cond
      (not (some #{lng} ["fi" "sv" "en"]))  (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
      (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
      (not (some #{order} ["asc" "desc"]))  (bad-request "Virheellinen järjestys ('asc'/'desc')")
      (and (not (nil? keyword))
           (> 3 (count keyword)))          (bad-request "Hakusana on liian lyhyt")
      :else                                (ok (f keyword
                                                  lng
                                                  page
                                                  size
                                                  sort
                                                  order
                                                  :koulutustyyppi koulutustyypit
                                                  :sijainti       sijainnit
                                                  :opetuskieli    opetuskielet
                                                  :koulutusala    koulutusalat)))))

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

(def konfo-api
  (api
    {:exceptions {:handlers {::ex/default exeption-handler}}
     :swagger {:ui   "/konfo-backend/swagger"
               :spec "/konfo-backend/swagger.json"
               :data {:info {:title       "Konfo-backend"
                             :description "Backend for Konfo koulutusinformaatio UI."}}}}
    (context "/konfo-backend"
      []

      (GET "/healthcheck" []
        :summary "Healthcheck API"
        (ok "OK"))

      (POST "/client-error" []
        :summary "Client error API"
        :body [error-details ClientError]
        (log/error (str "Error from client browser:\n"
                     (:error-message error-details) "\n"
                     (:url error-details) "\n"
                     "line: " (:line error-details) " column: " (:col error-details) "\n"
                     "user-agent: " (:user-agent error-details) "\n"
                     "stack trace: " (:stack error-details)))
        (ok "OK"))

      (GET "/translation/:lng" [:as request]
        :summary "Hae käännökset annetulla kielellä (fi/sv/en)"
        :path-params [lng :- String]
        (with-access-logging request (if (not (some #{lng} ["fi" "sv" "en"]))
                                       (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
                                       (if-let [result (lokalisointi/get lng)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/koulutus"
        []
        (GET "/:oid" [:as request]
          :summary "Hae koulutus oidilla"
          :query-params [{draft :- Boolean false}]
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (koulutus/get oid draft)]
                                         (ok result)
                                         (not-found "Not found")))))
      (context "/toteutus"
        []
        (GET "/:oid" [:as request]
          :summary "Hae toteutus oidilla"
          :query-params [{draft :- Boolean false}]
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (toteutus/get oid draft)]
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
          :query-params [{draft :- Boolean false}]
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (hakukohde/get oid draft)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/valintaperuste"
        []
        (GET "/:id" [:as request]
          :summary "Hae valintaperuste id:llä"
          :query-params [{draft :- Boolean false}]
          :path-params [id :- String]
          (with-access-logging request (if-let [result (valintaperuste/get id draft)]
                                         (ok result)
                                         (not-found "Not found")))))

      ;TODO: poista koulutuskoodi-uri, kun tuotannossa kaikilla koulutuksilla on eperuste-id
      (context "/kuvaus"
        []
        (GET "/:id" [:as request]
          :summary "Hae koulutuksen kuvaus (ja osaamisalakuvaukset) ePerusteista eperuste-id:n perusteella"
          :path-params [id :- String]
          :query-params [{osaamisalakuvaukset :- Boolean false}]
          (with-access-logging request (if-let [result (eperuste/get-kuvaus-by-eperuste-id id osaamisalakuvaukset)]
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
                 (with-access-logging request (if-let [result (oppilaitos/get oid)]
                                                (ok result)
                                                (not-found "Not found")))))

      (context "/search" []

        (GET "/filters" [:as request]
          :summary "Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet"
          (with-access-logging request (if-let [result (filters/hierarkia)]
                                         (ok result)
                                         (not-found "Not found"))))

        (GET "/filters_as_array" [:as request]
          :summary "Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet taulukkona"
          (with-access-logging request (if-let [result (filters/flattened-hierarkia)]
                                         (ok result)
                                         (not-found "Not found"))))

        (GET "/koulutukset" [:as request]
          :summary "Koulutus search API"
          :query-params [{keyword        :- (describe String "Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla tai halutaan hakea kaikki. Muussa tapauksessa vähimmäispituus on 3 merkkiä.") nil}
                         {page           :- Long 1}
                         {size           :- Long 20}
                         {lng            :- (describe String "Haun kieli. 'fi', 'sv' tai 'en'") "fi"}
                         {sort           :- (describe String "Järjestys. 'name' tai 'score'") "score"}
                         {order          :- (describe String "Järjestys. 'asc' tai 'desc'") "desc"}
                         {koulutustyyppi :- (describe String "Pilkulla eroteltu lista koulutustyyppejä, esim. 'amm,kk,lk'") nil}
                         {sijainti       :- (describe String "Pilkulla eroteltu kuntien ja maakuntien koodeja, esim. 'kunta_091,maakunta_01,maakunta_03'") nil}
                         {opetuskieli    :- (describe String "Pilkulla eroteltu opetuskielten koodeja, esim. 'oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2'") nil}
                         {koulutusala    :- (describe String "Pilkulla eroteltu koulutusalojen koodeja, esim. 'kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02'") nil}]
          (with-access-logging request (->search-with-validated-params koulutus-search/search
                                                                       keyword
                                                                       lng
                                                                       page
                                                                       size
                                                                       sort
                                                                       order
                                                                       koulutustyyppi
                                                                       sijainti
                                                                       opetuskieli
                                                                       koulutusala)))

        (GET "/koulutus/:oid/jarjestajat" [:as request]
          :summary "Hae koulutuksen tarjoajatiedot oidilla"
          :path-params [oid :- String]
          :query-params [{tuleva         :- Boolean false}
                         {page           :- Long 1}
                         {size           :- Long 20}
                         {lng            :- (describe String "Haun kieli. 'fi', 'sv' tai 'en'") "fi"}
                         {order          :- (describe String "Järjestys. 'asc' tai 'desc'") "asc"}]
          (with-access-logging request (cond
                                         (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                         (not (some #{order} ["asc" "desc"]))  (bad-request "Virheellinen järjestys")
                                         :else (if-let [result (koulutus-search/search-koulutuksen-jarjestajat oid lng page size order tuleva)]
                                                 (ok result)
                                                 (not-found "Not found")))))

        (GET "/oppilaitokset" [:as request]
          :summary "Oppilaitokset search API"
          :query-params [{keyword        :- (describe String "Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla tai halutaan hakea kaikki. Muussa tapauksessa vähimmäispituus on 3 merkkiä.") nil}
                         {page           :- Long 1}
                         {size           :- Long 20}
                         {lng            :- (describe String "Haun kieli. 'fi', 'sv' tai 'en'") "fi"}
                         {sort           :- (describe String "Järjestys. 'name' tai 'score'") "score"}
                         {order          :- (describe String "Järjestys. 'asc' tai 'desc'") "desc"}
                         {koulutustyyppi :- (describe String "Pilkulla eroteltu lista koulutustyyppejä, esim. 'amm,kk,lk'") nil}
                         {sijainti       :- (describe String "Pilkulla eroteltu kuntien ja maakuntien koodeja, esim. 'kunta_091,maakunta_01,maakunta_03'") nil}
                         {opetuskieli    :- (describe String "Pilkulla eroteltu opetuskielten koodeja, esim. 'oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2'") nil}
                         {koulutusala    :- (describe String "Pilkulla eroteltu koulutusalojen koodeja, esim. 'kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02'") nil}]
          (with-access-logging request (->search-with-validated-params oppilaitos-search/search
                                                                       keyword
                                                                       lng
                                                                       page
                                                                       size
                                                                       sort
                                                                       order
                                                                       koulutustyyppi
                                                                       sijainti
                                                                       opetuskieli
                                                                       koulutusala)))

        (GET "/oppilaitos/:oid/tarjonta" [:as request]
          :summary "Hae oppilaitoksen koulutustarjonta oidilla"
          :path-params [oid :- String]
          :query-params [{tuleva         :- Boolean false}
                         {page           :- Long 1}
                         {size           :- Long 20}
                         {lng            :- (describe String "Haun kieli. 'fi', 'sv' tai 'en'") "fi"}
                         {order           :- (describe String "Järjestys. 'asc' tai 'desc'") "asc"}]
          (with-access-logging request (cond
                                         (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                         (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys")
                                         :else (if-let [result (oppilaitos-search/search-oppilaitoksen-tarjonta oid lng page size order tuleva)]
                                                 (ok result)
                                                 (not-found "Not found"))))))

      (GET "/suosittelu" [:as request]
        :summary "Suosittelu API"
        :query-params [{koulutukset :- (describe String "Pilkulla eroteltu lista koulutusten oideja, joiden perusteella suosittelu annetaan") nil}]
        (with-access-logging request (let [oids (comma-separated-string->vec koulutukset)]
                                       (if (< 0 (count oids) 6)
                                         (ok (suosittelu/get-recommendations oids))
                                         (bad-request "Koulutusten oideja pitää olla 1-5 kpl suosittelua varten")))))

      (POST "/palaute" [:as request]
        :summary "POST palaute"
        :form-params [{arvosana :- Long nil}
                      {palaute :- String ""}
                      {path :- String ""}]
        (let [feedback {:stars      arvosana
                        :feedback   palaute
                        :path       path
                        :user-agent (get-in request [:headers "user-agent"])}]
          (palaute/send-feedback sqs-client feedback)
          (ok {}))))))

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
  (if (= (System/getProperty "mode") "updater")
    (do
      (def updater-app (wrap-cors (konfo-updater-api (contentful/create-contentful-client false)) :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post]))
      (log/info "Starting Konfo-backend-updater!")
      (run-jetty (wrap-reload #'updater-app)
                 {:port (Integer/valueOf
                          (or (System/getenv "port")
                              (System/getProperty "port")
                              "8080"))}))
    (do
      (log/info "Starting Konfo-backend!")
      (init)
      (run-jetty (wrap-reload #'app)
        {:port (Integer/valueOf
                 (or (System/getenv "port")
                   (System/getProperty "port")
                   "8080"))}))))
