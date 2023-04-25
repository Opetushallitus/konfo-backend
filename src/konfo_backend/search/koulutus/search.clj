(ns konfo-backend.search.koulutus.search
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.search.external-query :refer [external-query]]
            [konfo-backend.search.koulutus.kuvaukset :refer [with-kuvaukset]]
            [konfo-backend.search.query :refer [constraints-post-filter-query
                                                hakutulos-aggregations
                                                inner-hits-query jarjestajat-aggregations search-term-query
                                                koulutus-wildcard-query sorts]]
            [konfo-backend.search.response :refer [parse parse-external
                                                   parse-inner-hits-for-jarjestajat parse-for-autocomplete]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [log-pretty]]
            [cheshire.core :refer [parse-string, generate-string]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [search-term-query (search-term-query keyword lng ["words"])
        post-filter-query (constraints-post-filter-query constraints)
        aggs (hakutulos-aggregations constraints)]
    (koulutus-kouta-search
     page
     size
     #(-> % parse with-kuvaukset)
     :_source ["oid", "nimi", "koulutukset", "tutkintonimikkeet", "kielivalinta", "kuvaus", "teemakuva", "eperuste", "opintojenLaajuus", "opintojenLaajuusyksikko", "opintojenLaajuusNumero", "opintojenLaajuusNumeroMin", "opintojenLaajuusNumeroMax", "koulutustyyppi", "tutkinnonOsat", "osaamisala", "toteutustenTarjoajat" "isAvoinKorkeakoulutus"]
     :sort (sorts sort order lng)
     :query search-term-query
     :post_filter post-filter-query
     :aggs aggs)))

(defn search-koulutuksen-jarjestajat
  [oid lng page size order tuleva? constraints]
  (let [query (inner-hits-query oid lng page size order tuleva? constraints)
        aggs (jarjestajat-aggregations tuleva? constraints)]
    (e/search index
              parse-inner-hits-for-jarjestajat
              :_source ["oid", "koulutukset", "nimi"]
              :query query
              :aggs aggs)))

(defn external-search
  [keyword lng page size sort order constraints]
  (let [query (external-query keyword constraints lng ["words"])]
    (log-pretty query)
    (koulutus-kouta-search
      page
      size
      #(-> % parse-external with-kuvaukset)
      :_source ["oid", "nimi", "koulutukset", "tutkintonimikkeet", "kielivalinta", "kuvaus", "teemakuva", "eperuste", "opintojenLaajuus", "opintojenLaajuusyksikko", "opintojenLaajuusNumero", "opintojenLaajuusNumeroMin", "opintojenLaajuusNumeroMax" "koulutustyyppi"]
      :sort (sorts sort order lng)
      :query query)))

(defn autocomplete-search
  [search-phrase lng sort order constraints]
  (let [query (koulutus-wildcard-query search-phrase lng constraints)]
    (e/search index
              #(parse-for-autocomplete lng %)
              :_source ["oid", "nimi"]
              :sort (sorts sort order lng)
              :query query)))