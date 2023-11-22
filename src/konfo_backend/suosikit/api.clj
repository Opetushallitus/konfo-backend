(ns konfo-backend.suosikit.api
  (:require
   [konfo-backend.suosikit.suosikit :as suosikit]
   [compojure.api.core :as c]
   [ring.util.http-response :refer [ok not-found]]
   [clj-log.access-log :refer [with-access-logging]]
   [konfo-backend.tools :refer [comma-separated-string->vec]]))

(def paths
  "|  /suosikit:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae suosikeille tietoja
   |      description: Hae annetuilla hakukohde-oideilla tietoja suosikit-listausta varten
   |        Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: query
   |          name: hakukohde-oids
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          required: true
   |          description: Pilkulla erotettu lista hakukohteiden oideja
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /suosikit-vertailu:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae hakukohteille vertailutietoja
   |      description: Hae annetuilla hakukohde-oideilla tietoja suosikkien vertailua varten
   |        Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: query
   |          name: hakukohde-oids
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          required: true
   |          description: Pilkulla erotettu lista hakukohteiden oideja
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found")

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
