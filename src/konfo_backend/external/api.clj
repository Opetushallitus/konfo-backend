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
    [konfo-backend.external.schema.valintakoe :as valintakoe]
    [konfo-backend.external.schema.liite :as liite]
    [konfo-backend.external.service :as service]
    [clj-log.access-log :refer [with-access-logging]]
    [cheshire.core :as cheshire]))

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
   |          name: path
   |          schema:
   |            type: string
   |          required: true
   |          description: Koulutuksen oid
   |          example: 1.2.246.562.13.00000000000000000009
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/Koulutus'
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
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/Toteutus'
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
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/Toteutus'
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
       liite/schemas "\n"))


(def routes
  (context "/external" []
           :tags ["external"]

    (GET "/koulutus/:oid" [:as request]
         :path-params [oid :- String]
         ;:query-params [{draft :- Boolean false}]
         :return koulutus/Koulutus
      (with-access-logging request (if-let [result (service/get-koulutus oid false false false)]
                                     (do
                                       (println (cheshire/generate-string result {:pretty true}))
                                       (ok result))
                                     (not-found "Not found"))))

    (GET "/toteutus/:oid" [:as request]
        :path-params [oid :- String]
        ;:query-params [{draft :- Boolean false}]
        :return toteutus/Toteutus
        (with-access-logging request (if-let [result (service/get-toteutus oid)]
                                       (do
                                         (println (cheshire/generate-string result {:pretty true}))
                                         (ok result))
                                       (not-found "Not found"))))

    (GET "/hakukohde/:oid" [:as request]
         :path-params [oid :- String]
         ;:query-params [{draft :- Boolean false}]
         :return hakukohde/Hakukohde
      (with-access-logging request (if-let [result (service/get-hakukohde oid)]
                                     (do
                                       (println (cheshire/generate-string result {:pretty true}))
                                       (ok result))
                                     (not-found "Not found"))))



           ))
