(ns konfo-backend.search.rajain.query-tools
  (:require [clojure.string :refer [lower-case replace-first split join]]
            [konfo-backend.tools :refer [->lower-case-vec remove-nils]]))

(def koulutustyypit ["amm"
                     "amm-muu"
                     "amm-tutkinnon-osa"
                     "amm-osaamisala"
                     "lk"
                     "amk"
                     "amk-muu"
                     "amm-ope-erityisope-ja-opo"
                     "ope-pedag-opinnot"
                     "yo"
                     "kk-opintojakso-normal"
                     "kk-opintojakso-avoin"
                     "kk-opintokokonaisuus-avoin"
                     "kk-opintokokonaisuus-normal"
                     "erikoislaakari"
                     "erikoistumiskoulutus"
                     "amk-alempi"
                     "amk-ylempi"
                     "kandi"
                     "kandi-ja-maisteri"
                     "maisteri"
                     "tohtori"
                     "tuva"
                     "tuva-normal"
                     "tuva-erityisopetus"
                     "telma"
                     "vapaa-sivistystyo"
                     "vapaa-sivistystyo-opistovuosi"
                     "vapaa-sivistystyo-muu"
                     "aikuisten-perusopetus"
                     "taiteen-perusopetus"
                     "muu"
                     "koulutustyyppi_26"
                     "koulutustyyppi_4"
                     "koulutustyyppi_11"
                     "koulutustyyppi_12"])

(defn constraint?
  [constraints key]
  (not-empty (key constraints)))

(defn lukiopainotukset?
  [constraints]
  (constraint? constraints :lukiopainotukset))

(defn lukiolinjaterityinenkoulutustehtava?
  [constraints]
  (constraint? constraints :lukiolinjaterityinenkoulutustehtava))

(defn osaamisala?
  [constraints]
  (constraint? constraints :osaamisala))

(defn ->terms-query
  [key coll]
  (let [search-field (keyword (str "search_terms." key))]
    (if (= 1 (count coll))
      {:term {search-field (lower-case (first coll))}}
      {:terms {search-field (->lower-case-vec coll)}})))

(defn keyword-terms-query
  [field coll]
  (->terms-query (str field ".keyword") coll))

(defn hakutieto-query
  [nested-field-name field-name constraint]
  {:nested
   {:path "search_terms.hakutiedot"
    :query
    {:bool
     {:filter (->terms-query (str nested-field-name "." field-name) constraint)}}}})

(defn single-tyoelama-boolean-term
  [key]
  {:term {(keyword (str "search_terms." key)) true}})

(defn make-combined-boolean-filter-query
  [constraints sub-filters]
  (let [selected-sub-filters (filter #(true? (get constraints (:id %))) sub-filters)]
    (when (not-empty selected-sub-filters)
      (mapv #((:make-query %)) selected-sub-filters))))

(defn- lukio-filters [constraints]
  (cond-> []
    (lukiopainotukset? constraints) (conj (->terms-query "lukiopainotukset" (:lukiopainotukset constraints)))
    (lukiolinjaterityinenkoulutustehtava? constraints) (conj (->terms-query "lukiolinjaterityinenkoulutustehtava" (:lukiolinjaterityinenkoulutustehtava constraints)))))

(defn- osaamisala-filters [constraints]
  [(->terms-query "osaamisalat" (:osaamisala constraints))])

(defn make-combined-jarjestaja-filter-query
  [constraints sub-filters]
  (let [selected-sub-filters (filter #(constraint? constraints %) sub-filters)]
    (when (not-empty selected-sub-filters)
      {:should (mapv #((:make-query %)) selected-sub-filters)})))

(defn hakuaika-filter-query
  [current-time]
  {:bool {:should [{:bool {:filter [{:range {:search_terms.toteutusHakuaika.alkaa {:lte current-time}}}
                                    {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}},
                                                     {:range {:search_terms.toteutusHakuaika.paattyy {:gt current-time}}}]}}]}}
                   {:nested {:path  "search_terms.hakutiedot.hakuajat"
                             :query {:bool {:filter [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte current-time}}}
                                                     {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                                                      {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt current-time}}}]}}]}}}}]}})

(defn ->field-key [field-name]
  (str "search_terms." (name field-name)))

(defn- with-real-hits [agg]
  (assoc agg :aggs {:real_hits {:reverse_nested {}}}))

(defn- rajain-terms-agg [field-name term-details]
  (let [default-terms {:field field-name
                       :min_doc_count 0
                       :size 1000}]
    (with-real-hits {:terms (merge default-terms term-details)})))

(defn- constrained-agg [with-constraints filtered-aggs plain-aggs]
  (if (not-empty with-constraints)
    {:filter {:bool {:filter with-constraints}}
     :aggs filtered-aggs}
    plain-aggs))

(defn rajain-aggregation
  ([field-name with-constraints term-details]
   (constrained-agg
    with-constraints
    {:rajain (rajain-terms-agg field-name term-details)}
    (rajain-terms-agg field-name term-details)))
  ([field-name with-constraints] (rajain-aggregation field-name with-constraints nil)))

(defn bool-agg-filter [own-filter with-constraints]
  (with-real-hits {:filter {:bool
                            {:filter (vec (distinct (conj with-constraints own-filter)))}}}))

(defn hakukaynnissa-aggregation
  [current-time with-constraints]
  (bool-agg-filter (hakuaika-filter-query current-time) with-constraints))

(defn nested-rajain-aggregation
  ([rajain-key field-name with-constraints term-details]
   (let [nested-agg {:nested  {:path (-> field-name
                                         (replace-first ".keyword" "")
                                         (split #"\.")
                                         (drop-last) (#(join "." %)))}
                     :aggs {:rajain (rajain-terms-agg field-name term-details)}}]
     (constrained-agg
      with-constraints
      {(keyword rajain-key) nested-agg}
      nested-agg)))
  ([rajain-key field-name with-constraints] (nested-rajain-aggregation rajain-key field-name with-constraints nil)))
