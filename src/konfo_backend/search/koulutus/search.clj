(ns konfo-backend.search.koulutus.search
  (:require
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query match-all-query hakutulos-aggregations jarjestajat-aggregations inner-hits-query sorts]]
    [konfo-backend.search.external-query :refer [external-query]]
    [konfo-backend.search.response :refer [parse parse-inner-hits-for-jarjestajat parse-external]]
    [konfo-backend.elastic-tools :as e]
    [konfo-backend.index.oppilaitos :as oppilaitos]
    [konfo-backend.search.koulutus.kuvaukset :refer [with-kuvaukset]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial e/search-with-pagination index))

(defn search
  [keyword lng page size sort order constraints]
  (let [query (if (match-all? keyword constraints)
                (match-all-query)
                (query keyword constraints lng ["words"]))
        aggs (hakutulos-aggregations constraints)]
    (log-pretty query)
    (log-pretty aggs)
    (koulutus-kouta-search
      page
      size
      #(-> % parse with-kuvaukset)
      :_source ["oid", "nimi", "koulutukset", "tutkintonimikkeet", "kielivalinta", "kuvaus", "teemakuva", "eperuste", "opintojenLaajuus", "opintojenLaajuusyksikko", "opintojenLaajuusNumero", "opintojenLaajuusNumeroMin", "opintojenLaajuusNumeroMax", "koulutustyyppi", "tutkinnonOsat", "osaamisala", "toteutustenTarjoajat"]
      :sort (sorts sort order lng)
      :query query
      :aggs aggs)))

(defn- add-oppilaitos-nimi-to-counts
  [oppilaitos indexed-oppilaitos]
  (assoc oppilaitos :nimi (:nimi indexed-oppilaitos)))

(defn- add-oppilaitos-nimet-to-counts
  [oppilaitokset]
  (let [oppilaitos-oids (map #(:key %) oppilaitokset)
        indexed-oppilaitokset (oppilaitos/get-many oppilaitos-oids false)]
    (map (fn [j] (add-oppilaitos-nimi-to-counts j (first (filter #(= (:oid %) (:key j)) indexed-oppilaitokset)))) oppilaitokset)))

(defn- oppilaitos-counts-mapper
  [response]
  (let [buckets (add-oppilaitos-nimet-to-counts (get-in response [:aggregations :hits_aggregation :inner_hits_agg :oppilaitos :oppilaitos :buckets
                                                        ]))
        oppilaitos-counts (zipmap (map #(keyword (get % :key)) buckets)
                                  (map #(hash-map :count (get % :doc_count)
                                                  :nimi (get % :nimi))
                                       buckets))]
    oppilaitos-counts))

(defn- assoc-oppilaitos-counts
  [koulutus-oid result]
  (let [query {:bool {:must {:term {:oid koulutus-oid}}}}
        aggs {:hits_aggregation {:nested {:path "search_terms"}
                                 :aggs   {:inner_hits_agg {:filter {:bool {:must {:term {:search_terms.onkoTuleva false}}}}
                                                           :aggs   {:oppilaitos {:filter {:bool {:must {:term {:search_terms.onkoTuleva false}}}},
                                                                                 :aggs   {:oppilaitos {:terms {:field "search_terms.oppilaitosOid.keyword",
                                                                                                               :size  10000}}}}}}}}}
        oppilaitos-counts (e/search index
                                    oppilaitos-counts-mapper
                                    :size 0
                                    :query query
                                    :aggs aggs)]
    (assoc-in result [:filters :oppilaitos] oppilaitos-counts)))

(defn search-koulutuksen-jarjestajat
  [oid lng page size order tuleva? constraints]
  (let [query (inner-hits-query oid lng page size order tuleva? constraints)
        aggs (jarjestajat-aggregations tuleva? constraints)]
    (assoc-oppilaitos-counts oid (e/search index
              parse-inner-hits-for-jarjestajat
              :_source ["oid", "koulutukset", "nimi"]
              :query query
              :aggs aggs))))

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
