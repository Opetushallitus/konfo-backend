(ns konfo-backend.search.oppilaitos.search
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.search.query :refer [hakutulos-aggregations
                                                post-filter-query search-term-query sorts tarjoajat-aggregations toteutukset-inner-hits
                                                toteutukset-query]]
            [konfo-backend.search.rajain-tools :refer [onkoTuleva-query]]
            [konfo-backend.search.response :refer [parse parse-for-autocomplete parse-inner-hits-for-jarjestajat]]
            [konfo-backend.search.tools :refer :all]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [search-term-query (search-term-query keyword lng ["words"])
        post-filter-query (post-filter-query constraints)
        aggs (hakutulos-aggregations constraints)]
    (oppilaitos-kouta-search
     page
     size
     parse
     :_source ["oid", "nimi", "koulutusohjelmatLkm" "kielivalinta", "kuvaus", "paikkakunnat", "logo"]
     :sort (sorts sort order lng)
     :query {:bool {:must search-term-query
                    ; Otetaan vastaukseen mukaan pelkät oppilaitokset (=organisaatiotyyppi_02)
                    :filter {:match {:organisaatiotyypit "organisaatiotyyppi_02"}}}}
     :post_filter post-filter-query
     :aggs aggs)))

(defn search-oppilaitoksen-tarjonta
  [oid lng page size order tuleva? constraints]
  (let [query (toteutukset-query oid)
        inner-hits (toteutukset-inner-hits lng page size order)
        post-filter-query (post-filter-query constraints inner-hits (onkoTuleva-query tuleva?))
        aggs (tarjoajat-aggregations constraints tuleva?)]
    (e/search index
              parse-inner-hits-for-jarjestajat
              :_source ["oid"]
              :query query
              :post_filter post-filter-query
              :aggs aggs)))

(defn autocomplete-search
  [search-phrase lng size sort order constraints]
  (let [query (search-term-query search-phrase lng ["words"])
        post-filter-query (post-filter-query constraints)]
    (e/search index
              #(parse-for-autocomplete %)
              :_source ["oid", "nimi"]
              :size size
              :sort (sorts sort order lng)
              :post_filter post-filter-query
              :query query)))
