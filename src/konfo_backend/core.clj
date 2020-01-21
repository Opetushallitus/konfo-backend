(ns konfo-backend.core
  (:require
    [konfo-backend.config :refer [config]]
    [konfo-backend.index.toteutus :as toteutus]
    [konfo-backend.index.koulutus :as koulutus]
    [konfo-backend.contentful.contentful :as contentful]
    [konfo-backend.index.haku :as haku]
    [konfo-backend.index.hakukohde :as hakukohde]
    [konfo-backend.contentful.updater-api :refer [updater-app]]
    [konfo-backend.index.valintaperuste :as valintaperuste]
    [konfo-backend.index.oppilaitos :as oppilaitos]
    [konfo-backend.eperuste.eperuste :as eperuste]
    [konfo-backend.search.koulutus.search :as koulutus-search]
    [konfo-backend.old-search.search :as old-search]
    [konfo-backend.palaute.sqs :as sqs]
    [konfo-backend.search.oppilaitos.search :as oppilaitos-search]
    [konfo-backend.search.filters :as filters]
    [konfo-backend.palaute.palaute :as palaute]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.adapter.jetty :refer [run-jetty]]
    [konfo-backend.tools :refer [comma-separated-string->vec]]
    [clj-log.access-log :refer [with-access-logging]]
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.http-response :refer :all]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log])
  (:gen-class))

(defn init []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "konfo-backend")
  (intern 'clj-log.error-log 'test false))

(defonce sqs-client (sqs/create-sqs-client))

(defn- ->search-with-validated-params
  [f keyword lng page size sort koulutustyyppi sijainti opetuskieli koulutusala]
  (let [koulutustyypit      (comma-separated-string->vec koulutustyyppi)
        sijainnit           (comma-separated-string->vec sijainti)
        opetuskielet        (comma-separated-string->vec opetuskieli)
        koulutusalat        (comma-separated-string->vec koulutusala)]

    (cond
      (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
      (not (some #{sort} ["asc" "desc"]))  (bad-request "Virheellinen järjestys")
      (and (nil? keyword)
           (empty? koulutustyypit)
           (empty? sijainti)
           (empty? opetuskielet)
           (empty? koulutusalat))          (bad-request "Hakusana tai jokin rajain on pakollinen")
      (and (not (nil? keyword))
           (> 3 (count keyword)))          (bad-request "Hakusana on liian lyhyt")
      :else                                (ok (f keyword
                                                  lng
                                                  page
                                                  size
                                                  sort
                                                  :koulutustyyppi koulutustyypit
                                                  :sijainti       sijainnit
                                                  :opetuskieli    opetuskielet
                                                  :koulutusala    koulutusalat)))))

(def konfo-api
  (api
    {:swagger {:ui   "/konfo-backend/swagger"
               :spec "/konfo-backend/swagger.json"
               :data {:info {:title       "Konfo-backend"
                             :description "Backend for Konfo koulutusinformaatio UI."}}}}
    (context "/konfo-backend"
      []

      (GET "/healthcheck" [:as request]
        :summary "Healthcheck API"
           (-> (ok "OK")))

      (context "/koulutus"
        []
        (GET "/:oid" [:as request]
          :summary "Hae koulutus oidilla"
          :path-params [oid :- String]
          (with-access-logging request (if-let [result (koulutus/get oid)]
                                         (ok result)
                                         (not-found "Not found")))))

      (context "/content"
               []
         (GET "/:locale" [:as request]
              :summary     "Contentful-versio tiedosto"
              :path-params [locale :- String]
              (let [url (fn [content-type]
                          (str (-> config :konfo-host) "/konfo-backend/content/" locale "/" content-type))]
                (ok (->> ["sivu"
                    "valikot"
                    "info"
                    "valikko"
                    "uutiset"
                    "kortit"
                    "kortti"
                    "footer"
                    "ohjeetJaTuki"
                    "uutinen"
                    "palvelut"
                    "palvelu"]
                   (map (fn [asset] [asset (url asset)]))
                   (into {})))))
        (GET "/:locale/:content" [:as request]
          :summary "Contentful-elementit"
          :path-params [locale :- String
                        content :- String]
          :query-params [{preview :- Boolean false}]
          (with-access-logging request (if-let [result (contentful/get-entries content locale preview)]
                                         (-> (ok result)
                                             (content-type "application/json"))
                                         (not-found "Not found"))))
        (GET "/:locale/:content/:id" [:as request]
          :summary "Contentful-elementti"
          :path-params [locale :- String
                        content :- String
                        id :- String]
          :query-params [{preview :- Boolean false}]
          (with-access-logging request (if-let [result (contentful/get-entry content id locale preview)]
                                         (-> (ok result)
                                             (content-type "application/json"))
                                         (not-found "Not found")))))

      (context "/assets"
               []
        (GET "/:locale" [:as request]
          :summary "Contentful-elementit"
          :path-params [locale :- String]
          :query-params [{preview :- Boolean false}]
          (with-access-logging request (if-let [result (contentful/get-assets locale preview)]
                                         (-> (ok result)
                                             (content-type "application/json"))
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
                 (with-access-logging request (if-let [result (oppilaitos/get oid)]
                                                (ok result)
                                                (not-found "Not found")))))

      (context "/search" []

        (GET "/filters" [:as request]
          :summary "Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet"
          (with-access-logging request (if-let [result (filters/hierarkia)]
                                         (ok result)
                                         (not-found "Not found"))))

        (GET "/koulutukset" [:as request]
          :summary "Koulutus search API"
          :query-params [{keyword        :- (describe String "Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla. Muussa tapauksessa vähimmäispituus on 3 merkkiä.") nil}
                         {page           :- Long 1}
                         {size           :- Long 20}
                         {lng            :- (describe String "Haun kieli. 'fi', 'sv' tai 'en'") "fi"}
                         {sort           :- (describe String "Järjestys. 'asc' tai 'desc'") "asc"}
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
                         {sort           :- (describe String "Järjestys. 'asc' tai 'desc'") "asc"}]
          (with-access-logging request (cond
                                         (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                         (not (some #{sort} ["asc" "desc"]))  (bad-request "Virheellinen järjestys")
                                         :else (if-let [result (koulutus-search/search-koulutuksen-jarjestajat oid lng page size sort tuleva)]
                                                 (ok result)
                                                 (not-found "Not found")))))

        (GET "/oppilaitokset" [:as request]
          :summary "Oppilaitokset search API"
          :query-params [{keyword        :- (describe String "Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla. Muussa tapauksessa vähimmäispituus on 3 merkkiä.") nil}
                         {page           :- Long 1}
                         {size           :- Long 20}
                         {lng            :- (describe String "Haun kieli. 'fi', 'sv' tai 'en'") "fi"}
                         {sort           :- (describe String "Järjestys. 'asc' tai 'desc'") "asc"}
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
                         {sort           :- (describe String "Järjestys. 'asc' tai 'desc'") "asc"}]
          (with-access-logging request (cond
                                         (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                         (not (some #{sort} ["asc" "desc"]))  (bad-request "Virheellinen järjestys")
                                         :else (if-let [result (oppilaitos-search/search-oppilaitoksen-tarjonta oid lng page size sort tuleva)]
                                                 (ok result)
                                                 (not-found "Not found"))))))

      (POST "/palaute" [:as request]
        :summary "POST palaute"
        :form-params [{arvosana :- Long nil}
                      {palaute :- String ""}]
        (let [feedback {:stars      arvosana
                        :feedback   palaute
                        :user-agent (get-in request [:headers "user-agent"])}]
          (palaute/send-feedback sqs-client feedback)
          (ok {}))))))

(def app
  (wrap-cors konfo-api :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post]))

(defn -main [& args]
  (if (= (System/getProperty "mode") "updater")
    (do
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
