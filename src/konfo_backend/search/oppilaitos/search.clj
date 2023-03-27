(ns konfo-backend.search.oppilaitos.search
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.search.query :refer [hakutulos-aggregations
                                                inner-hits-query
                                                inner-hits-query-osat match-all-query query sorts tarjoajat-aggregations]]
            [konfo-backend.search.response :refer [parse parse-inner-hits]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [log-pretty]]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [query (if (blank-search? keyword constraints)
                (match-all-query)
                (query keyword constraints lng ["words"]))
        aggs (hakutulos-aggregations constraints)]
    (log-pretty query)
    (log-pretty aggs)
    (oppilaitos-kouta-search
     page
     size
     parse
     :_source ["oid", "nimi", "koulutusohjelmia", "kielivalinta", "kuvaus", "paikkakunnat", "logo"]
     :sort (sorts sort order lng)
     :query query
     :aggs aggs)))

(defn search-oppilaitoksen-tarjonta
  [oid lng page size order tuleva? constraints]
  (let [query (inner-hits-query oid lng page size order tuleva? constraints)
        aggs (tarjoajat-aggregations tuleva? constraints)]
    (e/search index
              parse-inner-hits
              :_source ["oid"]
              :query query
              :aggs aggs)))

(defn search-oppilaitoksen-osan-tarjonta
  [oid lng page size order tuleva?]
  (e/search index
            parse-inner-hits
            :_source ["oid"]
            :query (inner-hits-query-osat oid lng page size order tuleva?)))
