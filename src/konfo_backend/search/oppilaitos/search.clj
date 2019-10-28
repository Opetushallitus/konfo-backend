(ns konfo-backend.search.oppilaitos.search
  (:require
    [konfo-backend.tools :refer [not-blank? log-pretty ammatillinen? koodi-uri-no-version]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.oppilaitos.query :refer [query aggregations]]
    [konfo-backend.search.oppilaitos.response :refer [parse-response]]
    [konfo-backend.elastic-tools :refer [search-with-pagination]]
    [clj-elasticsearch.elastic-connect :as e]
    [konfo-backend.index.eperuste :refer [get-kuvaukset-by-koulutuskoodit]]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial search-with-pagination index))

(comment defn do-search?
  [keyword constraints]
  (or (not-blank? keyword) (constraints? constraints)))

(defn search
  [keyword lng page size & {:as constraints}]
  ; (when (do-search? keyword constraints)
    (log-pretty (e/simple-search index "*" true))
    (let [query (query keyword lng constraints)
          aggs (aggregations)]
      (log-pretty query)
      (log-pretty aggs)
      (oppilaitos-kouta-search
        page
        size
        parse-response
        :_source ["oid", "nimi", "koulutusohjelmia", "kielivalinta", "kuvaus"]
        :sort [{:nimi.fi.keyword {:order "asc"}}]
        :query query
        :aggs aggs)))