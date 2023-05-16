(ns konfo-backend.search.rajain-tools
  (:require [clojure.string :refer [lower-case replace-first split join]]
            [konfo-backend.tools :refer [->lower-case-vec]]))

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
                     "kk-muu"
                     "muut-ammatilliset"
                     ; ammatilliset koulutus-koodit
                     "koulutustyyppi_26"
                     "koulutustyyppi_4"
                     "koulutustyyppi_11"
                     "koulutustyyppi_12"])

(defn ->terms-query [key value]
  (let [term-key (keyword (str "search_terms." key))
        ->lower-case (fn [val] (if (string? val) (lower-case val) val))]
    (cond
      (and (coll? value) (= (count value) 1)) {:term {term-key (->lower-case (first value))}}
      (coll? value) {:terms {term-key (->lower-case-vec value)}}
      :else {:term {term-key (->lower-case value)}})))

(defn nested-query
  [nested-field-name field-name constraint]
  {:nested
   {:path (str "search_terms." nested-field-name)
    :query
    {:bool
     {:filter (->terms-query (str nested-field-name "." field-name) constraint)}}}})

(defn ->boolean-term-query
  [key]
  (->terms-query key true))

(defn onkoTuleva-query [tuleva?]
  {:term {:search_terms.onkoTuleva tuleva?}})

(defn make-combined-boolean-filter-query
  [constraints sub-filters]
  (let [selected-sub-filters (filter #(true? (get constraints (:id %))) sub-filters)]
    (when (not-empty selected-sub-filters)
      (mapv #((:make-query %)) selected-sub-filters))))

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

(defn- with-real-hits
  ([agg rajain-context]
   (let [reverse-nested-path (get-in rajain-context [:reverse-nested-path])]
     (assoc agg :aggs {:real_hits {:reverse_nested (if reverse-nested-path {:path reverse-nested-path} {})}})))
  ([agg]
   (with-real-hits agg nil)))

(defn- rajain-terms-agg
  ([field-name rajain-context]
   (let [default-terms {:field field-name
                        :min_doc_count 0
                        :size 1000}]
     (with-real-hits {:terms (merge default-terms (get-in rajain-context [:term-params]))} rajain-context))))

(defn- constrained-agg [constraints filtered-aggs plain-aggs]
  (if (not-empty constraints)
    {:filter {:bool {:filter constraints}}
     :aggs filtered-aggs}
    plain-aggs))

(defn rajain-aggregation
  [field-name contraints rajain-context]
  (constrained-agg
   contraints
   {:rajain (rajain-terms-agg field-name rajain-context)}
   (rajain-terms-agg field-name rajain-context)))

(defn bool-agg-filter [own-filter constraints rajain-context]
  (with-real-hits
    {:filter {:bool
              {:filter (vec (distinct (conj constraints own-filter)))}}}
    rajain-context))

(defn nested-rajain-aggregation
  [rajain-key field-name constraints rajain-context]
  (let [nested-agg {:nested  {:path (-> field-name
                                        (replace-first ".keyword" "")
                                        (split #"\.")
                                        (drop-last) (#(join "." %)))}
                    :aggs {:rajain (rajain-terms-agg field-name rajain-context)}}]
    (constrained-agg
     constraints
     {(keyword rajain-key) nested-agg}
     nested-agg)))
