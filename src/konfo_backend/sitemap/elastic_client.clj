(ns konfo-backend.sitemap.elastic-client
  (:require
    [konfo-backend.elastic-tools :as e]))

(defonce koulutus-index "koulutus-kouta")
(defonce toteutus-index "toteutus-kouta")
(defonce hakukohde-index "hakukohde-kouta")

(defonce entities-query {:term {:tila "julkaistu"}})
(defonce hakukohde-entities-query {:bool {:must [{:term {:tila "julkaistu"}}
                                                 {:term {:valintaperuste.tila "julkaistu"}}]}})

(defn- parse-entities
  [result]
  (let [hits (get-in result [:hits :hits])]
    (map #(hash-map :oid (get-in % [:_source :oid]) :langs (keys (get-in % [:_source :nimi]))) hits)))

(defn- count-docs
  [index query]
  (e/count index :query query)
  )

(defn- get-entities
  [index query]
  (let [total (count-docs index query)]
    (if (< e/limit-to-use-search-after total)
      (e/search-all-with-do-after
        index
        parse-entities
        total
        :_source ["oid", "nimi"]
        :sort [{:oid.keyword "asc"}]
        :query query)
      (e/search
        index
        parse-entities
        :_source ["oid", "nimi"]
        :size total
        :query query))))

(defn get-koulutus-entities
  []
  (get-entities koulutus-index entities-query))

(defn get-toteutus-entities
  []
  (get-entities toteutus-index entities-query))

(defn get-hakukohde-with-valintaperusteet-entities
  []
  (get-entities hakukohde-index hakukohde-entities-query))
