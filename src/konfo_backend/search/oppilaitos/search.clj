(ns konfo-backend.search.oppilaitos.search
  (:require
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query aggregations]]
    [konfo-backend.search.response :refer [parse]]
    [konfo-backend.elastic-tools :refer [search-with-pagination]]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial search-with-pagination index))

(defn search
  [keyword lng page size & {:as constraints}]
  (when (do-search? keyword constraints)
    (let [query (query keyword lng constraints)
          aggs (aggregations)]
      (log-pretty query)
      (log-pretty aggs)
      (oppilaitos-kouta-search
        page
        size
        parse
        :_source ["oid", "nimi", "koulutusohjelmia", "kielivalinta", "kuvaus", "paikkakunnat"]
        :sort [{(->lng-keyword "nimi.%s.keyword" lng) {:order "asc"}}]
        :query query
        :aggs aggs))))