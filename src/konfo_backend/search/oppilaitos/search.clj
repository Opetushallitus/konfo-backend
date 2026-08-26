(ns konfo-backend.search.oppilaitos.search
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.search.query :refer [autocomplete-query hakutulos-aggregations
                                                post-filter-query search-with-approximate-fallback sorts
                                                tarjoajat-aggregations toteutukset-inner-hits
                                                toteutukset-query]]
            [konfo-backend.search.rajain-tools :refer [onkoTuleva-query]]
            [konfo-backend.search.response :refer [parse parse-for-autocomplete]]
            [konfo-backend.index.toteutus :refer [parse-inner-hits-for-jarjestajat]]
            [konfo-backend.search.tools :refer :all]))

(defonce index "oppilaitos-kouta-search")

(def oppilaitos-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [post-filter (post-filter-query constraints)
        aggs (hakutulos-aggregations constraints)]
    (search-with-approximate-fallback
     keyword lng ["words"]
     (fn [search-term-query]
       (oppilaitos-kouta-search
        page
        size
        parse
        :_source ["oid", "nimi", "koulutusohjelmatLkm" "kielivalinta", "kuvaus", "paikkakunnat", "logo"]
        :sort (sorts sort order lng)
        :query {:bool {:must search-term-query
                       ; Otetaan vastaukseen mukaan pelkät oppilaitokset (=organisaatiotyyppi_02)
                       :filter {:match {:organisaatiotyypit "organisaatiotyyppi_02"}}}}
        :post_filter post-filter
        :aggs aggs)))))

(defn search-oppilaitoksen-tarjonta
  [oid lng page size order tuleva? constraints]
  (let [query (toteutukset-query oid)
        inner-hits (toteutukset-inner-hits lng page size order)
        post-filter (post-filter-query constraints inner-hits (onkoTuleva-query tuleva?))
        aggs (tarjoajat-aggregations constraints tuleva?)]
    (e/search index
              parse-inner-hits-for-jarjestajat
              :_source ["oid"]
              :query query
              :post_filter post-filter
              :aggs aggs)))

(defn autocomplete-search
  [search-phrase lng size sort order constraints]
  (let [query (autocomplete-query search-phrase lng ["words"])
        post-filter (post-filter-query constraints)]
    (e/search index
              #(parse-for-autocomplete %)
              :_source ["oid", "nimi"]
              :size size
              :sort (sorts sort order lng)
              :post_filter post-filter
              :query query)))
