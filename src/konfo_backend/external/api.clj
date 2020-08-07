(ns konfo-backend.external.api
  (:require
    [compojure.api.core :as c :refer [GET POST context]]
    [ring.util.http-response :refer :all]
    [konfo-backend.external.schema.common :as common]
    [konfo-backend.external.schema.koodi :as koodi]
    [konfo-backend.external.schema.koulutus :as koulutus]
    [konfo-backend.external.schema.koulutus-metadata :as koulutus-metadata]
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
   |          description: Koulutus-oid
   |          example: 1.2.246.562.13.00000000000000000009
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                $ref: '#/components/schemas/Koulutus'
   |        '404':
   |          description: Not found")

(def schemas
  (str common/schemas "\n"
       koodi/schemas "\n"
       koulutus/schemas "\n"
       koulutus-metadata/schemas "\n"))


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
                                     (not-found "Not found"))))))
