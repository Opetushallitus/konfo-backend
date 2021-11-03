(ns konfo-backend.sitemap.sitemap
  (:require
    [clojure.data.xml :as xml]
    [konfo-backend.util.rest :refer [get->json-body]]
    [konfo-backend.util.urls :refer [resolve-url]]
    [konfo-backend.sitemap.elastic-client :as e]
    [clj-time.core :as time]
    [clj-time.format :as f]))

(defonce sitemap-xml-ins "http://www.sitemaps.org/schemas/sitemap/0.9")

(defonce koulutus-url-template "https://opintopolku.fi/konfo/%s/koulutus/%s")
(defonce toteutus-url-template "https://opintopolku.fi/konfo/%s/toteutus/%s")
(defonce hakukohde-url-template "https://opintopolku.fi/konfo/%s/hakukohde/%s/valintaperuste")
(defonce contentful-page-url-template "https://opintopolku.fi/konfo/%s/sivu/")

(defonce sitemap-urls ["https://opintopolku.fi/sitemap-learningopportunity.xml"
                       "https://opintopolku.fi/wp/sitemap.xml"
                       "https://opintopolku.fi/konfo-backend/sitemap/sivut-sitemap.xml"
                       "https://opintopolku.fi/konfo-backend/sitemap/koulutus-sitemap.xml"
                       "https://opintopolku.fi/konfo-backend/sitemap/toteutus-sitemap.xml"
                       "https://opintopolku.fi/konfo-backend/sitemap/hakukohde-sitemap.xml"])

(defn- current-date-formatted
  []
  (f/unparse (f/formatter "yyyy-MM-dd") (time/now)))

(defn- ->sitemap-index
  [sitemaps]
  (xml/element :sitemapindex {:xmlns sitemap-xml-ins} sitemaps))

(defn- ->sitemap
  [loc]
  (xml/element :sitemap {} [(xml/element :loc {} loc)
                            (xml/element :lastmod {} (current-date-formatted))]))

(defn- ->urlset
  [sitemaps]
  (xml/element :urlset {:xmlns sitemap-xml-ins} sitemaps))

(defn- ->url
  [loc]
  (xml/element :url {} [(xml/element :loc {} loc)
                            (xml/element :lastmod {} (current-date-formatted))]))

(defn ->contentful-sivu-link
  [sivu-nimi sivut-linkki]
  (str sivut-linkki sivu-nimi))

(defn- get-sivut-urls
  [contentful-data-url-key sivut-linkki]
  (let [url (resolve-url contentful-data-url-key)
        json (get->json-body url)]
    (map #(->url (->contentful-sivu-link (get % :slug) sivut-linkki)) json)))

(defn get-sivut-urlset
  []
  (let [urls (flatten (map #(get-sivut-urls
                              (str "konfo-content.prod.sivut." %)
                              (format contentful-page-url-template %))
                           ["fi" "sv" "en"]))]
    (->urlset urls)))

(defn- ->urls
  [oid langs url-template]
  (map #(->url (format url-template (name %) oid)) langs))

(defn entities->urlset
  [oids url-template]
  (let [urls (map #(->urls (get % :oid) (get % :langs) url-template) oids)]
    (->urlset (flatten urls))))

(defn get-koulutus-urlset
  []
  (entities->urlset (e/get-koulutus-entities) koulutus-url-template))

(defn get-toteutus-urlset
  []
  (entities->urlset (e/get-toteutus-entities) toteutus-url-template))

(defn get-hakukohde-urlset
  []
  (entities->urlset (e/get-hakukohde-with-valintaperusteet-entities) hakukohde-url-template))

(defn get-sitemap
  []
  (->sitemap-index (map #(->sitemap %) sitemap-urls)))

