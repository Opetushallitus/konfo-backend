(ns konfo-backend.search.query
  (:require [clojure.string :as str]
            [konfo-backend.elastic-tools :refer [->from ->size]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.search.rajain.rajain-definitions :refer [constraints? common-filters inner-hits-filters
                                                                    generate-hakutulos-aggregations
                                                                    generate-jarjestajat-aggregations
                                                                    generate-tarjoajat-aggregations]]
            [konfo-backend.tools :refer [current-time-as-kouta-format]]))

(defn match-all-query
  []
  {:match_all {}})

(defn koulutus-wildcard-query
  [search-phrase user-lng constraints]
  {:nested {:path "search_terms", :query {:bool (wildcard-query-fields search-phrase constraints user-lng) }}})

(defn search-term-query [search-term user-lng suffixes]
  (if (not (str/blank? search-term))
    {:nested {:path "search_terms", :query {:bool {:must (make-search-term-query search-term user-lng suffixes)}}}}
    (match-all-query)))

(defn constraints-post-filter-query [constraints]
  (when (constraints? constraints)
    {:nested {:path "search_terms", :query {:bool {:filter (common-filters constraints (current-time-as-kouta-format))}}}}))

;OY-3870 Kenttä nimi_sort lisätty indekseihin oppilaitos-kouta-search ja koulutus-kouta-search.
(defn- ->name-sort
  [order lng]
  [{(->lng-keyword "nimi.%s.keyword" lng) {:order order :unmapped_type "string"}}
   {(->lng-keyword "nimi_sort.%s.keyword" lng) {:order order :unmapped_type "string"}}])

(defn sorts
  [sort order lng]
  (if (= "name" sort)
    (->name-sort order lng)
    (vec (concat [{:_score {:order order}}] (->name-sort "asc" lng)))))

(defn inner-hits-query
  [oid lng page size order tuleva? constraints]
  (let [size (->size size)
        from (->from page size)]
    {:bool {:must [{:term {:oid oid}}
                   {:nested {:inner_hits {:_source ["search_terms.koulutusOid", "search_terms.toteutusOid", "search_terms.toteutusNimi", "search_terms.opetuskielet", "search_terms.oppilaitosOid", "search_terms.kuva", "search_terms.nimi", "search_terms.metadata" "search_terms.hakutiedot" "search_terms.toteutusHakuaika" "search_terms.jarjestaaUrheilijanAmmKoulutusta"]
                                          :from    from
                                          :size    size
                                          :sort    {(str "search_terms.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}
                             :path       "search_terms"
                             :query      (inner-hits-filters tuleva? constraints)}}]}}))

(defn inner-hits-query-osat
  [oid lng page size order tuleva?]
  (let [size (->size size)
        from (->from page size)]
    {:nested {:inner_hits {:_source ["search_terms.koulutusOid", "search_terms.toteutusOid", "search_terms.oppilaitosOid", "search_terms.kuva", "search_terms.nimi", "search_terms.metadata"]
                           :from    from
                           :size    size
                           :sort    {(str "search_terms.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}
              :path       "search_terms"
              :query      {:bool {:must [{:term {"search_terms.onkoTuleva" tuleva?}}
                                         {:term {"search_terms.tarjoajat" oid}}]}}}}))

(defn- aggregations
  [aggs-generator]
  {:hits_aggregation {:nested {:path "search_terms"}, :aggs (aggs-generator)}})

(defn hakutulos-aggregations
  [constraints]
  (aggregations #(generate-hakutulos-aggregations constraints)))

(defn jarjestajat-aggregations
  [tuleva? constraints]
  (aggregations #(generate-jarjestajat-aggregations tuleva? constraints)))

(defn tarjoajat-aggregations
  [tuleva? constraints]
  (aggregations #(generate-tarjoajat-aggregations tuleva? constraints)))
