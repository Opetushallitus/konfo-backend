(ns konfo-backend.external.api
  (:require
    [compojure.api.core :as c :refer [GET POST context]]
    [ring.util.http-response :refer :all]
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
    [konfo-backend.external.schema.sorakuvaus :as sorakuvaus]
    [konfo-backend.external.schema.response :as response]
    [konfo-backend.external.schema.liite :as liite]
    [konfo-backend.external.service :as service]
    [clj-log.access-log :refer [with-access-logging]]))

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
   |          name: path
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
   |          name: path
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
   |          name: path
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
   |          description: Not found")

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
       sorakuvaus/schemas "\n"
       response/schemas "\n"))

(def routes
  (context "/external" []
           :tags ["external"]

    (GET "/koulutus/:oid" [:as request]
         :path-params [oid :- String]
         :query-params [{toteutukset :- Boolean false}
                        {hakukohteet :- Boolean false}
                        {haut        :- Boolean false}]
         :return response/KoulutusResponse
      (with-access-logging request (if-let [result (service/koulutus oid toteutukset hakukohteet haut)]
                                     (ok result)
                                     (not-found "Not found"))))

    (GET "/toteutus/:oid" [:as request]
        :path-params [oid :- String]
        :query-params [{koulutus    :- Boolean false}
                       {hakukohteet :- Boolean false}
                       {haut        :- Boolean false}]
        :return response/ToteutusResponse
        (with-access-logging request (if-let [result (service/toteutus oid koulutus hakukohteet haut)]
                                       (ok result)
                                       (not-found "Not found"))))

    (GET "/hakukohde/:oid" [:as request]
         :path-params [oid :- String]
         :query-params [{koulutus             :- Boolean false}
                        {toteutus             :- Boolean false}
                        {valintaperustekuvaus :- Boolean false}
                        {haku                 :- Boolean false}]
         :return response/HakukohdeResponse
      (with-access-logging request (if-let [result (service/hakukohde oid koulutus toteutus valintaperustekuvaus haku)]
                                     (ok result)
                                     (not-found "Not found"))))

    (GET "/haku/:oid" [:as request]
         :path-params [oid :- String]
         :query-params [{koulutukset    :- Boolean false}
                        {toteutukset    :- Boolean false}
                        {hakukohteet    :- Boolean false}]
         :return response/HakuResponse
      (with-access-logging request (if-let [result (service/haku oid koulutukset toteutukset hakukohteet)]
                                     (ok result)
                                     (not-found "Not found"))))))