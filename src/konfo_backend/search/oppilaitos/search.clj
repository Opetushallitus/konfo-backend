(ns konfo-backend.search.oppilaitos.search
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.search.query :refer [constraints-post-filter-query
                                                hakutulos-aggregations
                                                inner-hits-query-osat search-term-query sorts tarjoajat-aggregations
                                                toteutukset-inner-hits toteutukset-query]]
            [konfo-backend.search.response :refer [parse parse-inner-hits-for-jarjestajat parse-inner-hits]]
            [konfo-backend.search.tools :refer :all]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [search-term-query (search-term-query keyword lng ["words"])
        post-filter-query (constraints-post-filter-query constraints false nil)
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
  (let [query (toteutukset-query oid)
        inner-hits (toteutukset-inner-hits lng page size order)
        post-filter-query (constraints-post-filter-query constraints inner-hits tuleva?)
        aggs (tarjoajat-aggregations constraints tuleva?)]
    (e/search index
              parse-inner-hits-for-jarjestajat
              :_source ["oid"]
              :query query
              :post_filter post-filter-query
              :aggs aggs)))

(defn search-oppilaitoksen-osan-tarjonta
  [oid lng page size order tuleva?]
  (e/search index
            parse-inner-hits
            :_source ["oid"]
            :query (inner-hits-query-osat oid lng page size order tuleva?)))
