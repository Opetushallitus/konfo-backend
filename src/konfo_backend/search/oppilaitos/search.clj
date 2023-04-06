(ns konfo-backend.search.oppilaitos.search
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.search.query :refer [constraints-post-filter-query
                                                hakutulos-aggregations
                                                inner-hits-query inner-hits-query-osat search-term-query sorts
                                                tarjoajat-aggregations]]
            [konfo-backend.search.response :refer [parse parse-inner-hits]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [log-pretty]]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [search-term-query (search-term-query keyword lng ["words"])
        post-filter-query (constraints-post-filter-query constraints)
        aggs (hakutulos-aggregations constraints)]
    (oppilaitos-kouta-search
     page
     size
     parse
     :_source ["oid", "nimi", "koulutusohjelmia", "kielivalinta", "kuvaus", "paikkakunnat", "logo"]
     :sort (sorts sort order lng)
     :query search-term-query
     :post_filter post-filter-query
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
