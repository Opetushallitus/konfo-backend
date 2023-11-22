(ns konfo-backend.suosikit.api
  (:require [clj-log.access-log :refer [with-access-logging]]
            [clojure.string :as string]
            [compojure.api.core :as c]
            [konfo-backend.external.schema.common :refer [->Koodi
                                                          create-kielistetty-schema
                                                          Kielistetty Kielivalikoima KoutaKoulutustyyppi Kuvaus Nimi Organisaatio
                                                          schema-to-swagger-yaml spec-paths-to-swagger-yaml]]
            [konfo-backend.external.schema.toteutus-metadata :refer [AmmOsaamisala
                                                                     LukiodiplomiTieto]]
            [konfo-backend.external.schema.valintakoe :refer [Valintakoe]]
            [konfo-backend.suosikit.suosikit :as suosikit]
            [konfo-backend.tools :refer [comma-separated-string->vec]]
            [ring.util.http-response :refer [not-found ok]]
            [schema-tools.core :as st]
            [schema-tools.openapi.core :as openapi]
            [schema.core :as s]))

(s/defschema HakutietoHakuaika
  {:alkaa s/Str
   (s/optional-key :paattyy) s/Str
   :formatoituAlkaa Kielistetty
   (s/optional-key :formatoituPaattyy) Kielistetty
   :hakuAuki Boolean
   :hakuMennyt Boolean})

(s/defschema SuosikitItem
  {:nimi (st/schema Nimi {:description "Hakukohteen nimi eri kielillä"})
   :hakukohdeOid (st/schema s/Str {:description "Hakukohteen yksilöivä tunniste"})
   :toteutusOid (st/schema s/Str {:description "Hakukohteeseen liitetyn toteutuksen yksilöivä tunniste"
                                  :example "1.2.246.562.17.00000000000000000009"})
   :logo (st/schema s/Str {:description "Hakukohteen järjestyspaikan oppilaitoksen logon URL"})
   :esittely (st/schema Kuvaus {:description "Hakukohteen järjestyspaikan oppilaitoksen esittely eri kielillä"})
   (s/optional-key :tutkintonimikkeet) (st/schema [(->Koodi s/Str)] {:description "Lista tutkintonimikkeitä käännöksineen (tutkintonimikkeet-koodisto)"})
   :jarjestyspaikka (st/schema Organisaatio {:description "Hakukohteen järjestyspaikan tiedot"})
   :jarjestaaUrheilijanAmmKoulutusta s/Bool
   :hakuajat [HakutietoHakuaika]})

(s/defschema Pistetieto
  {:tarjoaja s/Str
   :hakukohdekoodi s/Str
   :pisteet s/Num
   :vuosi s/Str
   :valintatapajonoOid s/Str
   :hakukohdeOid s/Str
   :hakuOid s/Str
   :valintatapajonoTyyppi s/Str})

(s/defschema KayntiOsoite (st/schema (create-kielistetty-schema "osoite")))
(s/defschema SuosikitVertailuItem
  (st/schema {:koulutustyyppi KoutaKoulutustyyppi
              :nimi (st/schema Nimi {:description "Hakukohteen nimi eri kielillä"})
              :hakukohdeOid (st/schema s/Str {:description "Hakukohteen yksilöivä tunniste"})
              :toteutusOid (st/schema s/Str {:description "Hakukohteeseen liitetyn toteutuksen yksilöivä tunniste"
                                             :example "1.2.246.562.17.00000000000000000009"})
              :logo (st/schema s/Str {:description "Hakukohteen järjestyspaikan oppilaitoksen logon URL"})
              :esittely (st/schema Kuvaus {:description "Hakukohteen järjestyspaikan oppilaitoksen esittely eri kielillä"})
              :osoite (st/schema KayntiOsoite {:description "Hakukohteen järjestyspaikan käyntiosoite"})
              (s/optional-key :opiskelijoita) (st/schema s/Int {:description "Hakukohteen järjestyspaikan oppilaitoksen opiskelijoiden määrä"})
              (s/optional-key :osaamisalat) (st/schema [AmmOsaamisala] {:description "Lista ammatillisen koulutuksen osaamisalojen kuvauksia"})
              :edellinenHaku (st/schema Pistetieto {:description "Edellisen haun tiedot"})
              :valintakokeet (st/schema [Valintakoe] {:description "Hakukohteeseen liittyvät valintakokeet"})
              :toinenAsteOnkoKaksoistutkinto (st/schema s/Bool {:description "Onko hakukohteen toisen asteen koulutuksessa mahdollista suorittaa kaksoistutkinto?"})
              (s/->OptionalKey :lukiodiplomit) (st/schema [LukiodiplomiTieto])
              :kielivalikoima (st/schema Kielivalikoima)
              :jarjestaaUrheilijanAmmKoulutusta s/Bool}))

(def schemas (string/join "\n" (map schema-to-swagger-yaml [SuosikitItem SuosikitVertailuItem])))

(def suosikit-paths-spec
  (openapi/openapi-spec
   {:paths
    {"/suosikit"
     {:get {:tags ["internal"]
            :summary "Hae suosikeille tietoja"
            :description "Hae annetuilla hakukohde-oideilla tietoja suosikit-listausta varten. Huom.! Vain Opintopolun sisäiseen käyttöön"
            :parameters [{:in "query"
                          :name "hakukohde-oids"
                          :style "form"
                          :explode false
                          :description "Pilkulla erotettu lista hakukohteiden oideja"
                          :schema {:type "array"
                                   :items {:type "string"}}}]
            :responses {200 {:description "Ok"
                             ::openapi/content {"application/json" (st/schema [SuosikitItem])}}
                        404 {:description "Not found"}}}}
     "/suosikit-vertailu"
     {:get {:tags ["internal"]
            :summary "Hae hakukohteille vertailutietoja"
            :description "Hae annetuilla hakukohde-oideilla tietoja suosikkien vertailua varten. Huom.! Vain Opintopolun sisäiseen käyttöön"
            :parameters [{:in "query"
                          :name "hakukohde-oids"
                          :style "form"
                          :explode false
                          :description "Pilkulla erotettu lista hakukohteiden oideja"
                          :schema {:type "array"
                                   :items {:type "string"}}}]
            :responses {200 {:description "Ok"
                             ::openapi/content {"application/json" (st/schema [SuosikitVertailuItem])}}
                        404 {:description "Not found"}}}}}}))

(def paths (spec-paths-to-swagger-yaml suosikit-paths-spec))

(def routes
  (c/routes
   (c/GET "/suosikit" [:as request]
     :query-params [{hakukohde-oids :- String nil}]
     (with-access-logging request
       (if-let [result (suosikit/get-by-hakukohde-oids (comma-separated-string->vec hakukohde-oids))]
         (ok result)
         (not-found "Not found"))))
   (c/GET "/suosikit-vertailu" [:as request]
     :query-params [{hakukohde-oids :- String nil}]
     (with-access-logging request
       (if-let [result (suosikit/get-vertailu-by-hakukohde-oids (comma-separated-string->vec hakukohde-oids))]
         (ok result)
         (not-found "Not found"))))))
