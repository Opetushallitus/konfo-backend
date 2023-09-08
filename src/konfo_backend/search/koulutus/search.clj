(ns konfo-backend.search.koulutus.search
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.search.external-query :refer [external-query]]
            [konfo-backend.search.koulutus.kuvaukset :refer [with-kuvaukset]]
            [konfo-backend.search.query :refer [post-filter-query
                                                hakutulos-aggregations
                                                jarjestajat-aggregations search-term-query sorts
                                                toteutukset-inner-hits toteutukset-query]]
            [konfo-backend.search.rajain-tools :refer [onkoTuleva-query]]
            [konfo-backend.search.response :refer [parse parse-external
                                                   parse-for-autocomplete
                                                   parse-inner-hits-for-jarjestajat]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [log-pretty]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [search-term-query (search-term-query keyword lng ["words"])
        post-filter-query (post-filter-query constraints)
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
  (let [query (toteutukset-query oid)
        inner-hits (toteutukset-inner-hits lng page size order)
        aggs (jarjestajat-aggregations constraints tuleva?)
        post-filter-query (post-filter-query constraints inner-hits (onkoTuleva-query tuleva?))]
    (e/search index
              parse-inner-hits-for-jarjestajat
              :_source ["oid", "koulutukset", "nimi"]
              :query query
              :post_filter post-filter-query
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
  [search-phrase lng size sort order constraints]
  (let [query (search-term-query search-phrase lng ["words"])
        post-filter-query (post-filter-query constraints)]
    (e/search index
              #(parse-for-autocomplete %)
              :_source ["oid", "nimi", "toteutustenTarjoajat"]
              :size size
              :sort (sorts sort order lng)
              :post_filter post-filter-query
              :query query)))