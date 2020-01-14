(ns konfo-backend.search.oppilaitos.search
  (:require
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query aggregations inner-hits-query]]
    [konfo-backend.search.response :refer [parse parse-inner-hits]]
    [konfo-backend.elastic-tools :as e]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort & {:as constraints}]
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
        :sort [{(->lng-keyword "nimi.%s.keyword" lng) {:order sort}}]
        :query query
        :aggs aggs))))

(defn search-oppilaitoksen-tarjonta
  [oid lng page size sort tuleva?]
  (e/search index
            parse-inner-hits
            :_source ["oid"]
            :query (inner-hits-query oid lng page size sort tuleva?)))