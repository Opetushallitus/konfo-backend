(ns konfo-backend.search.query
  (:require [clojure.string :as str]
            [konfo-backend.elastic-tools :refer [->from ->size]]
            [konfo-backend.search.rajain-definitions :refer [common-filters
                                                                    constraints? generate-hakutulos-aggregations generate-jarjestajat-aggregations
                                                                    generate-tarjoajat-aggregations]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [assoc-if current-time-as-kouta-format]]))

(defn match-all-query
  []
  {:match_all {}})

(defn search-term-query [search-term user-lng suffixes]
  (if (not (str/blank? search-term))
    {:nested {:path "search_terms" :query {:bool {:must (make-search-term-query search-term user-lng suffixes)}}}}
    (match-all-query)))

(defn post-filter-query
  ([constraints inner-hits extra-filter]
   (when (or (constraints? constraints) inner-hits)
     (let [filters (vec (flatten
                          (cond-> []
                                  (constraints? constraints) (conj (common-filters constraints (current-time-as-kouta-format)))
                                   extra-filter (conj extra-filter))))]
       {:nested (assoc-if {:path "search_terms" :query {:bool {:filter filters}}}
                           :inner_hits inner-hits inner-hits)})))
  ([constraints]
   (post-filter-query constraints nil nil)))

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

; Käytetään sekä koulutuksen järjestäjille että oppilaitoksen tarjoajille, joissa listataan toteutuksia
(defn toteutukset-query [oid]
  {:bool {:must {:term {:oid oid}}}})

(defn toteutukset-inner-hits [lng page size order]
  (let [size (->size size)
        from (->from page size)]
    {:_source ["search_terms.koulutusOid"
               "search_terms.toteutusOid"
               "search_terms.toteutusNimi"
               "search_terms.opetuskielet"
               "search_terms.oppilaitosOid"
               "search_terms.kuva"
               "search_terms.nimi"
               "search_terms.metadata"
               "search_terms.hakutiedot"
               "search_terms.toteutusHakuaika"
               "search_terms.jarjestaaUrheilijanAmmKoulutusta"]
     :from    from
     :size    size
     :sort    {(str "search_terms.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}))

(defn- aggregations
  [aggs-generator]
  {:hits_aggregation {:nested {:path "search_terms"}, :aggs (aggs-generator)}})

(defn hakutulos-aggregations
  [constraints]
  (aggregations #(generate-hakutulos-aggregations constraints)))

(defn jarjestajat-aggregations
  [constraints tuleva?]
  (aggregations #(generate-jarjestajat-aggregations constraints tuleva?)))

(defn tarjoajat-aggregations
  [constraints tuleva?]
  (aggregations #(generate-tarjoajat-aggregations constraints tuleva?)))
