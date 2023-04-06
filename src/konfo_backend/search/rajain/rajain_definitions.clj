(ns konfo-backend.search.rajain.rajain-definitions
  (:require
    [konfo-backend.tools :refer [current-time-as-kouta-format]]
    [konfo-backend.search.rajain.query-tools :refer :all]))

(def koulutustyyppi
  {:id :koulutustyyppi :make-query #(keyword-terms-query "koulutustyypit" %)})

(def sijainti
  {:id :sijainti :make-query #(keyword-terms-query "sijainti" %)})

(def opetuskieli
  {:id :opetuskieli :make-query #(keyword-terms-query "opetuskielet" %)})

(def koulutusala
  {:id :koulutusala :make-query #(keyword-terms-query "koulutusalat" %)})

(def opetustapa
  {:id :opetustapa :make-query #(keyword-terms-query "opetustavat" %)})

(def valintatapa
  {:id :valintatapa :make-query #(hakutieto-query "hakutiedot" "valintatavat" %)})

(def hakutapa
  {:id :hakutapa :make-query #(hakutieto-query "hakutiedot" "hakutapa" %)})

(def jotpa
  {:id :jotpa :make-query #(single-tyoelama-boolean-query "hasJotpaRahoitus")})

(def tyovoimakoulutus
  {:id :tyovoimakoulutus :make-query #(single-tyoelama-boolean-query "isTyovoimakoulutus")})

(def taydennyskoulutus
  {:id :taydennyskoulutus :make-query #(single-tyoelama-boolean-query "isTaydennyskoulutus")})

(def yhteishaku
  {:id :yhteishaku :make-query #(hakutieto-query "hakutiedot" "yhteishakuOid" %)})

(def pohjakoulutusvaatimus
  {:id :pohjakoulutusvaatimus :make-query #(hakutieto-query "hakutiedot" "pohjakoulutusvaatimukset" %)})

(def oppilaitos
  {:id :oppilaitos :make-query #(keyword-terms-query "oppilaitosOid" %)})

(def lukiopainotukset
  {:id :lukiopainotukset :make-query #(keyword-terms-query "lukiopainotukset" %)})

(def lukiolinjaterityinenkoulutustehtava
  {:id :lukiolinjaterityinenkoulutustehtava :make-query #(keyword-terms-query "lukiolinjaterityinenkoulutustehtava" %)})

(def osaamisala
  {:id :osaamisala :make-query #(keyword-terms-query "osaamisala" %)})

(def common-rajain-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus oppilaitos])

(def koulutus-or-oppilaitos-rajain-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus])

(def koulutus-jarjestaja-rajain-definitions
  [sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala oppilaitos])

(def oppilaitos-tarjonta-rajain-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa])

(def hakukaynnissa-rajain
  {:make-query (fn [constraints current-time] (when (true? (:hakukaynnissa constraints)) (hakuaika-filter-query current-time)))})

(def combined-tyoelama-rajain
  {:make-query #(make-combined-boolean-filter-query % [jotpa tyovoimakoulutus taydennyskoulutus])})

(def combined-jarjestaja-rajain
  {:make-query #(lukiolinjat-and-osaamisala-filters %)})

(def boolean-type-rajaimet
  (conj [:hakukaynnissa] (:id jotpa) (:id tyovoimakoulutus) (:id taydennyskoulutus)))

; Onko hakurajaimia valittuna?
(defn constraints?
  [constraints]
  (let [contains-non-boolean-rajaimet? (not-empty (filter #(constraint? constraints %) (map :id common-rajain-definitions)))
        contains-boolean-true-rajaimet? (not-empty (filter #(true? (% constraints)) boolean-type-rajaimet))]
    (or (contains-non-boolean-rajaimet?) (contains-boolean-true-rajaimet?))))

(defn common-filters
  [constraints current-time]
  (let [make-constraint-query
        (fn [rajain-def]
          (let [constraint-vals (get constraints (:id rajain-def))]
            (cond
              (and (boolean? constraint-vals) (true? constraint-vals)) (:make-query rajain-def)
              (and (vector? constraint-vals) (not-empty constraint-vals)) ((:make-query rajain-def) constraint-vals))))]
    (filterv
      some?
      (conj
        (mapv make-constraint-query common-rajain-definitions)
        ((:make-query combined-tyoelama-rajain) constraints)
        ((:make-query hakukaynnissa-rajain) constraints current-time)))))

(defn inner-hits-filters
  [tuleva? constraints]
  {:bool
   {:must
    [{:term {"search_terms.onkoTuleva" tuleva?}}
     {:bool ((:make-query combined-jarjestaja-rajain) constraints)}]
    :filter (common-filters constraints (current-time-as-kouta-format))}})

(defn- common-filters-without-rajainkeys
  [constraints current-time & rajain-keys]
  (let [constraints-wo-rajainkey (dissoc constraints (map keyword rajain-keys))]
    (when (constraints? constraints-wo-rajainkey)
      common-filters constraints-wo-rajainkey current-time)))

(def maakunta-agg
  {:id :maakunta :make-query (fn [constraints current-time]
                               (rajain-aggregation (->field-key "sijainti.keyword")
                                                   (common-filters-without-rajainkeys constraints current-time "maakunta")
                                                   {:include "maakunta.*"}))})

(def kunta-agg
  {:id :kunta :make-query (fn [constraints current-time]
                               (rajain-aggregation (->field-key "sijainti.keyword")
                                                   (common-filters-without-rajainkeys constraints current-time "kunta")
                                                   {:include "kunta.*"}))})

(def opetuskieli-agg
  {:id :opetuskieli :make-query (fn [constraints current-time]
                            (rajain-aggregation (->field-key "opetuskielet.keyword")
                                                (common-filters-without-rajainkeys constraints current-time "opetuskieli")))})

(def opetustapa-agg
  {:id :opetustapa :make-query (fn [constraints current-time]
                                  (rajain-aggregation (->field-key "opetustavat.keyword")
                                                      (common-filters-without-rajainkeys constraints current-time "opetustapa")))})

(def hakukaynnissa-agg
  {:id :hakukaynnissa :make-query (fn [constraints current-time]
                                 (hakukaynnissa-aggregation current-time (common-filters constraints current-time)))})

(def hakutapa-agg
  {:id :hakutapa :make-query (fn [constraints current-time]
                                 (nested-rajain-aggregation "hakutapa" "search_terms.hakutiedot.hakutapa"
                                                            (common-filters-without-rajainkeys constraints current-time "hakutapa")))})

(def pohjakoulutusvaatimus-agg
  {:id :pohjakoulutusvaatimus :make-query (fn [constraints current-time]
                               (nested-rajain-aggregation "pohjakoulutusvaatimus" "search_terms.hakutiedot.pohjakoulutusvaatimukset"
                                                          (common-filters-without-rajainkeys constraints current-time "pohjakoulutusvaatimus")))})

(def valintatapa-agg
  {:id :valintatapa :make-query (fn [constraints current-time]
                                  (nested-rajain-aggregation "valintatapa" "search_terms.hakutiedot.valintatavat"
                                                             (common-filters-without-rajainkeys constraints current-time "valintatapa")))})

(def yhteishaku-agg
  {:id :yhteishaku :make-query (fn [constraints current-time]
                                  (nested-rajain-aggregation "yhteishaku" "search_terms.hakutiedot.yhteishakuOid"
                                                             (common-filters-without-rajainkeys constraints current-time "yhteishaku")))})

(def koulutusala-agg
  {:id :koulutusala :make-query (fn [constraints current-time]
                                 (rajain-aggregation (->field-key "koulutusalat.keyword")
                                                     (common-filters-without-rajainkeys constraints current-time "koulutusala")))})

(def koulutustyyppi-agg
  {:id :koulutustyyppi :make-query (fn [constraints current-time]
                                  (rajain-aggregation (->field-key "koulutustyypit.keyword")
                                                      (common-filters-without-rajainkeys constraints current-time "koulutustyyppi") {:size   (count koulutustyypit)
                                                                                                                                    :include koulutustyypit}))})
(def jotpa-agg
  {:id :jotpa :make-query (fn [constraints current-time]
                            (bool-agg-filter (single-tyoelama-agg-boolean-filter "hasJotpaRahoitus")
                                             (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))})

(def tyovoimakoulutus-agg
  {:id :tyovoimakoulutus :make-query (fn [constraints current-time]
                            (bool-agg-filter (single-tyoelama-agg-boolean-filter "isTyovoimakoulutus")
                                             (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))})

(def taydennyskoulutus-agg
  {:id :taydennyskoulutus :make-query (fn [constraints current-time]
                                       (bool-agg-filter (single-tyoelama-agg-boolean-filter "isTaydennyskoulutus")
                                                        (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))})

(def lukiopainotukset-aggs
  {:id :lukiopainotukset-aggs :make-query #(rajain-aggregation (->field-key "lukiopainotukset.keyword") [])})

(def lukiolinjaterityinenkoulutustehtava-aggs
  {:id :lukiolinjaterityinenkoulutustehtava-aggs :make-query #(rajain-aggregation (->field-key "lukiolinjaterityinenkoulutustehtava.keyword") [])})

(def osaamisala-aggs
  {:id :osaamisala-aggs :make-query #(rajain-aggregation (->field-key "osaamisalat.keyword") [])})

(def default-aggregations
  [maakunta-agg kunta-agg opetuskieli-agg opetustapa-agg hakukaynnissa-agg hakutapa-agg pohjakoulutusvaatimus-agg valintatapa-agg yhteishaku-agg koulutusala-agg koulutustyyppi-agg])

(defn- generate-default-aggs
  [constraints current-time]
  (into {} (for [agg default-aggregations] {(:id agg) ((:make-query agg) constraints current-time)})))

(defn- generate-tyoelama-aggregations
  [constraints current-time]
  (into {} (for [agg [jotpa-agg tyovoimakoulutus-agg taydennyskoulutus-agg]] {(:id agg) ((:make-query agg) constraints current-time)})))

(defn generate-hakutulos-aggregations
  [constraints]
  (let [current-time (current-time-as-kouta-format)]
    (merge (generate-default-aggs constraints current-time)
           (generate-tyoelama-aggregations constraints current-time))))

(defn generate-jarjestajat-aggregations
  [tuleva? constraints oppilaitos-oids]
  (let [current-time (current-time-as-kouta-format)
        default-aggs (generate-default-aggs {} current-time)]
    (into
      {:inner_hits_agg
       {:filter (inner-hits-filters tuleva? constraints)
        :aggs
        (-> default-aggs
            (add-oppilaitos-aggs oppilaitos-oids)
            (dissoc :koulutusala :koulutustyyppi))}}
      (for [aggs [lukiopainotukset-aggs lukiolinjaterityinenkoulutustehtava-aggs osaamisala-aggs]]
        {(:id aggs) ((:make-query aggs))}))))

(defn generate-tarjoajat-aggregations
  [tuleva? constraints]
    {:inner_hits_agg
     {:filter (inner-hits-filters tuleva? constraints)
      :aggs (generate-default-aggs {} nil)}})
