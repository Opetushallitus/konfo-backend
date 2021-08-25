(ns konfo-backend.external.api
  (:require
    [compojure.api.core :as c :refer [GET POST context]]
    [compojure.api.exception :as ex]
    [ring.util.http-response :refer :all]
    [compojure.api.sweet :refer [api]]
    [konfo-backend.external.schema.common :as common]
    [konfo-backend.external.schema.koodi :as koodi]
    [konfo-backend.external.schema.koulutus :as koulutus]
    [konfo-backend.external.schema.koulutus-metadata :as koulutus-metadata]
    [konfo-backend.external.schema.toteutus :as toteutus]
    [konfo-backend.external.schema.toteutus-metadata :as toteutus-metadata]
    [konfo-backend.external.schema.hakukohde :as hakukohde]
    [konfo-backend.external.schema.haku :as haku]
    [konfo-backend.external.schema.valintakoe :as valintakoe]
    [konfo-backend.external.schema.valintaperustekuvaus :as valintaperuste]
    [konfo-backend.external.schema.valintaperustekuvaus-metadata :as valintaperuste-metadata]
    [konfo-backend.external.schema.sorakuvaus :as sorakuvaus]
    [konfo-backend.external.schema.response :as response]
    [konfo-backend.external.schema.liite :as liite]
    [konfo-backend.external.schema.search :as search]
    [konfo-backend.external.service :as service]
    [konfo-backend.search.filters :as filters]
    [clj-log.access-log :refer [with-access-logging]]
    [konfo-backend.search.api :refer [->search-with-validated-params]]
    [konfo-backend.search.koulutus.search :refer [external-search]]))

(def paths
  "|  /external/koulutus/{oid}:
   |    get:
   |      summary: Hae koulutuksen tiedot annetulla oidilla
   |      operationId: Hae koulutus
   |      description: Hae koulutuksen ja tarvittaessa siihen liittyvien toteutusten ja hakujen tiedot
   |      tags:
   |        - External
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Koulutuksen oid
   |          example: 1.2.246.562.13.00000000000000000009
   |        - in: query
   |          name: toteutukset
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös koulutuksen toteutukset?
   |        - in: query
   |          name: hakukohteet
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös hakukohteet, joissa voi hakea koulutukseen?
   |        - in: query
   |          name: haut
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös haut, joissa voi hakea koulutukseen?
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/KoulutusResponse'
   |        '404':
   |          description: Not found
   |  /external/toteutus/{oid}:
   |    get:
   |      summary: Hae toteutuksen tiedot annetulla oidilla
   |      operationId: Hae toteutus
   |      description: Hae toteutuksen ja tarvittaessa siihen liittyvien hakukohteiden ja koulutusten tiedot
   |      tags:
   |        - External
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Toteutuksen oid
   |          example: 1.2.246.562.17.00000000000000000009
   |        - in: query
   |          name: koulutus
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös toteutuksen koulutus?
   |        - in: query
   |          name: hakukohteet
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös hakukohteet, joissa voi hakea toteutukseen?
   |        - in: query
   |          name: haut
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös haut, joissa voi hakea toteutukseen?
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/ToteutusResponse'
   |        '404':
   |          description: Not found
   |  /external/hakukohde/{oid}:
   |    get:
   |      summary: Hae hakukohteen tiedot annetulla oidilla
   |      operationId: Hae hakukohde
   |      description: Hae hakukohteen ja tarvittaessa siihen liittyvien hakujen ja koulutusten tiedot
   |      tags:
   |        - External
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Hakukohteen oid
   |          example: 1.2.246.562.20.00000000000000000009
   |        - in: query
   |          name: valintaperustekuvaus
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös hakukohteen valintaperusteiden kuvaus?
   |        - in: query
   |          name: koulutus
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös sen koulutuksen tiedot, johon hakukohteessa voi hakea?
   |        - in: query
   |          name: toteutus
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös sen toteutuksen tiedot, johon haussa voi hakea?
   |        - in: query
   |          name: haku
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös haku, johon hakukohde kuuluu?
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/HakukohdeResponse'
   |        '404':
   |          description: Not found
   |  /external/haku/{oid}:
   |    get:
   |      summary: Hae haun tiedot annetulla oidilla
   |      operationId: Hae haku
   |      description: Hae haun ja tarvittaessa siihen liittyvien hakukohteiden, koulutusten ja toteutusten tiedot
   |      tags:
   |        - External
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Haun oid
   |          example: 1.2.246.562.29.00000000000000000009
   |        - in: query
   |          name: koulutukset
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös niiden koulutusten tiedot, joihin haussa voi hakea?
   |        - in: query
   |          name: toteutukset
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko myös niiden toteutusten tiedot, joihin haussa voi hakea?
   |        - in: query
   |          name: hakukohteet
   |          schema:
   |            type: boolean
   |          required: false
   |          default: false
   |          description: Haetaanko haun hakukohteet?
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/HakuResponse'
   |        '404':
   |          description: Not found
   |  /external/search/toteutukset-koulutuksittain:
   |    get:
   |      tags:
   |        - External
   |      summary: Hae toteutuksia ja koulutuksia
   |      description: Hakee toteutuksia koulutuksittain jaoteltuna annetulla hakusanalla
   |      parameters:
   |        - in: query
   |          name: keyword
   |          schema:
   |            type: string
   |          required: false
   |          description: Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla tai halutaan hakea kaikki.
   |            Muussa tapauksessa vähimmäispituus on 3 merkkiä.
   |          default: nil
   |          example: Hevostalous
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivunumero
   |          default: 1
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivun koko
   |          default: 20
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |          default: fi
   |        - in: query
   |          name: sort
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestysperuste. 'name' tai 'score'
   |          default: score
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |          default: desc
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutustyypit. 'amm, 'yo' tai 'amk'
   |          default: nil
   |          example: amk
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |          default: nil
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |          default: nil
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |          default: nil
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |          default: nil
   |        - in: query
   |          name: valintatapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu valintatapojen koodeja
   |          example: valintatapajono_av, valintatapajono_tv
   |          default: nil
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |          required: false
   |          description: Haetaanko koulutuksia joilla on haku käynnissä
   |          default: false
   |        - in: query
   |          name: hakutapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu hakutapojen koodeja
   |          example: hakutapa_01, hakutapa_03
   |          default: nil
   |        - in: query
   |          name: yhteishaku
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista yhteishakujen oideja
   |          example: 1.2.246.562.29.00000000000000000800
   |          default: nil
   |        - in: query
   |          name: pohjakoulutusvaatimus
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu pohjakoulutusvaatimusten koodeja
   |          example: pohjakoulutusvaatimuskonfo_am, pohjakoulutusvaatimuskonfo_102
   |          default: nil
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/KoulutusToteutusSearchResponse'
   |        '404':
   |          description: Not found
   |        '400':
   |          description: Bad request
   |  /external/search/filters:
   |    get:
   |      tags:
   |        - External
   |      summary: Hae hakurajaimet
   |      description: Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /external/search/filters_as_array:
   |    get:
   |      tags:
   |        - External
   |      summary: Hae hakurajaimet taulukkomuodossa
   |      description: Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet taulukkomuodossa. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   ")

(def schemas
  (str common/schemas "\n"
       koodi/schemas "\n"
       koulutus/schemas "\n"
       koulutus-metadata/schemas "\n"
       toteutus/schemas "\n"
       toteutus-metadata/schemas "\n"
       hakukohde/schemas "\n"
       valintakoe/schemas "\n"
       liite/schemas "\n"
       haku/schemas "\n"
       valintaperuste/schemas "\n"
       valintaperuste-metadata/schemas "\n"
       sorakuvaus/schemas "\n"
       search/schemas "\n"
       response/schemas))

(def routes
  (api
    {:exceptions
     {:handlers
      {::ex/response-validation (ex/with-logging ex/response-validation-handler :error)}}}
    (context "/external" []
             :tags ["external"]

             (GET "/koulutus/:oid" [:as request]
                  :path-params [oid :- String]
                  :query-params [{toteutukset :- Boolean false}
                                 {hakukohteet :- Boolean false}
                                 {haut :- Boolean false}]
                  :return response/KoulutusResponse
                  (with-access-logging request (if-let [result (service/koulutus oid toteutukset hakukohteet haut)]
                                                 (ok result)
                                                 (not-found "Not found"))))

             (GET "/toteutus/:oid" [:as request]
                  :path-params [oid :- String]
                  :query-params [{koulutus :- Boolean false}
                                 {hakukohteet :- Boolean false}
                                 {haut :- Boolean false}]
                  :return response/ToteutusResponse
                  (with-access-logging request (if-let [result (service/toteutus oid koulutus hakukohteet haut)]
                                                 (ok result)
                                                 (not-found "Not found"))))

             (GET "/hakukohde/:oid" [:as request]
                  :path-params [oid :- String]
                  :query-params [{koulutus :- Boolean false}
                                 {toteutus :- Boolean false}
                                 {valintaperustekuvaus :- Boolean false}
                                 {haku :- Boolean false}]
                  :return response/HakukohdeResponse
                  (with-access-logging request (if-let [result (service/hakukohde oid koulutus toteutus valintaperustekuvaus haku)]
                                                 (ok result)
                                                 (not-found "Not found"))))

             (GET "/haku/:oid" [:as request]
                  :path-params [oid :- String]
                  :query-params [{koulutukset :- Boolean false}
                                 {toteutukset :- Boolean false}
                                 {hakukohteet :- Boolean false}]
                  :return response/HakuResponse
                  (with-access-logging request (if-let [result (service/haku oid koulutukset toteutukset hakukohteet)]
                                                 (ok result)
                                                 (not-found "Not found"))))


             (GET "/search/toteutukset-koulutuksittain" [:as request]
                  :query-params [{keyword               :- String nil}
                                 {page                  :- Long 1}
                                 {size                  :- Long 20}
                                 {lng                   :- String "fi"}
                                 {sort                  :- String "score"}
                                 {order                 :- String "desc"}
                                 {koulutustyyppi        :- String nil}
                                 {sijainti              :- String nil}
                                 {opetuskieli           :- String nil}
                                 {koulutusala           :- String nil}
                                 {opetustapa            :- String nil}
                                 {valintatapa           :- String nil}
                                 {hakukaynnissa         :- Boolean false}
                                 {hakutapa              :- String nil}
                                 {yhteishaku            :- String nil}
                                 {pohjakoulutusvaatimus :- String nil}]
                  :return response/KoulutusToteutusSearchResponse
                  (with-access-logging request (->search-with-validated-params external-search
                                                                               keyword
                                                                               lng
                                                                               page
                                                                               size
                                                                               sort
                                                                               order
                                                                               koulutustyyppi
                                                                               sijainti
                                                                               opetuskieli
                                                                               koulutusala
                                                                               opetustapa
                                                                               valintatapa
                                                                               hakukaynnissa
                                                                               hakutapa
                                                                               yhteishaku
                                                                               pohjakoulutusvaatimus)))
             (GET "/search/filters" [:as request]
                  (with-access-logging request (if-let [result (filters/generate-filter-counts)]
                                                 (ok result)
                                                 (not-found "Not found"))))

             (GET "/search/filters_as_array" [:as request]
                  (with-access-logging request (if-let [result (filters/flattened-filter-counts)]
                                                 (ok result)
                                                 (not-found "Not found")))))))
