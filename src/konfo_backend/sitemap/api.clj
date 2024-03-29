(ns konfo-backend.sitemap.api
  (:require
    [konfo-backend.sitemap.sitemap :as sitemap]
    [compojure.api.core :refer [GET context]]
    [clojure.data.xml :as xml]
    [ring.util.response :as resp]
    [clj-log.access-log :refer [with-access-logging]]
    [ring.util.http-response :refer :all]))

(def paths
  "|  /sitemap/sitemap.xml:
   |    get:
   |      tags:
   |        - sitemap
   |      summary: Opintopolun sitemap.xml
   |      description: Opintopolun sitemap.xml hakukoneita varten
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/xml:
   |              schema:
   |                type: object
   |  /sitemap/sivut-sitemap.xml:
   |    get:
   |      tags:
   |        - sitemap
   |      summary: Opintopolun Contentful-sisällön sitemap.xml
   |      description: Opintopolun Contentful-sisällön sitemap.xml hakukoneita varten
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/xml:
   |              schema:
   |                type: object
   |  /sitemap/koulutus-sitemap.xml:
   |    get:
   |      tags:
   |        - sitemap
   |      summary: Opintopolun koulutusten sitemap.xml
   |      description: Opintopolun koulutusten sitemap.xml hakukoneita varten
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/xml:
   |              schema:
   |                type: object
   |  /sitemap/toteutus-sitemap.xml:
   |    get:
   |      tags:
   |        - sitemap
   |      summary: Opintopolun toteutusten sitemap.xml
   |      description: Opintopolun toteutusten sitemap.xml hakukoneita varten
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/xml:
   |              schema:
   |                type: object
   |  /sitemap/hakukohde-sitemap.xml:
   |    get:
   |      tags:
   |        - sitemap
   |      summary: Opintopolun hakukohteiden sitemap.xml
   |      description: Opintopolun hakukohteiden joilla on valintaperusteet sitemap.xml hakukoneita varten
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/xml:
   |              schema:
   |                type: object")

(defn- handle-request
  [request f & f-args]
  (with-access-logging request
                       (let [resp (-> (resp/response (xml/indent-str (apply f f-args)))
                                      (resp/status 200)
                                      (resp/header "Content-type" "application/xml"))]
                         resp)))

(def routes
  (context "/sitemap" []
           (GET "/sitemap.xml" [:as request]
                (handle-request request sitemap/get-sitemap-with-cache))

           (GET "/sivut-sitemap.xml" [:as request]
                (handle-request request sitemap/get-sivut-urlset-with-cache))

           (GET "/koulutus-sitemap.xml" [:as request]
                (handle-request request sitemap/get-koulutus-urlset-with-cache))

           (GET "/toteutus-sitemap.xml" [:as request]
                (handle-request request sitemap/get-toteutus-urlset-with-cache))

           (GET "/hakukohde-sitemap.xml" [:as request]
                (handle-request request sitemap/get-hakukohde-urlset-with-cache))))
