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

(defn single-tyoelama-boolean-query
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

(defn lukiolinjat-and-osaamisala-filters
  [constraints]
  (let [filters
        (concat []
                (when (or (lukiolinjaterityinenkoulutustehtava? constraints) (lukiopainotukset? constraints))
                  (lukio-filters constraints))
                (when (osaamisala? constraints)
                  (osaamisala-filters constraints)))]
    (if (empty? filters)
      {}
      {:should filters})))

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

(defn- generate-sub-aggregation
  [sub-filters]
  {:constrained (with-real-hits (if (not-empty sub-filters)
                                    {:filter {:bool {:filter sub-filters}}}
                                    ; Käytetään "reverse_nested":iä dummy-aggregaationa kun ei ole rajaimia, jotta aggregaation tulosten rakenne pysyy samanlaisena
                                    {:reverse_nested {}}))})

(defn rajain-aggregation
  ([field-name sub-filters agg-details]
   (let [default-agg {:field field-name
                      :min_doc_count 0
                      :size 1000}]
     {:terms (merge default-agg agg-details)
      :aggs (generate-sub-aggregation sub-filters)}))
  ([field-name sub-filters] (rajain-aggregation field-name sub-filters nil)))

(defn single-tyoelama-agg-boolean-filter
  [key]
  {:term {(keyword (str "search_terms." key)) true}})

(defn bool-agg-filter [own-filter sub-filters]
  (with-real-hits {:filter {:bool
                            {:filter (distinct (conj sub-filters own-filter))}}}))

(defn hakukaynnissa-aggregation
  [current-time sub-filters]
  (bool-agg-filter (hakuaika-filter-query current-time) sub-filters))

(defn nested-rajain-aggregation
  ([rajain-key field-name sub-filters agg-details]
   {:nested {:path (-> field-name (replace-first ".keyword" "") (split #"\.") (drop-last) (#(join "." %)))}
    :aggs {(keyword rajain-key) (rajain-aggregation field-name sub-filters agg-details)}})
  ([rajain-key field-name sub-filters] (nested-rajain-aggregation rajain-key field-name sub-filters nil)))
