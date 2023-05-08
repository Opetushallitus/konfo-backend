(ns konfo-backend.search.rajain.rajain-definitions
  (:require
    [konfo-backend.tools :refer [current-time-as-kouta-format]]
    [konfo-backend.search.rajain.query-tools :refer :all]))

(def common-rajain-definitions (atom []))
(def combined-tyoelama-rajain (atom {}))
(def hakukaynnissa-rajain (atom {}))
(def boolean-type-rajaimet (atom []))

(def combined-jarjestaja-rajain
  {:make-query #(lukiolinjat-and-osaamisala-filters %)})

(defn constraints?
  [constraints]
  (let [contains-non-boolean-rajaimet? (not-empty (filter #(constraint? constraints %) (map :id @common-rajain-definitions)))
        contains-boolean-true-rajaimet? (not-empty (filter #(true? (% constraints)) @boolean-type-rajaimet))]
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
        (mapv make-constraint-query @common-rajain-definitions)
        ((:make-query @combined-tyoelama-rajain) constraints)
        ((:make-query @hakukaynnissa-rajain) constraints current-time)))))

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


(def koulutustyyppi
  {:id :koulutustyyppi :make-query #(keyword-terms-query "koulutustyypit" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "koulutustyypit.keyword")
                               (common-filters-without-rajainkeys constraints current-time "koulutustyyppi") {:size   (count koulutustyypit)
                                                                                                              :include koulutustyypit}))
   :desc (str
   "   |        - in: query\n"
   "   |          name: koulutustyyppi\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu lista koulutustyyppejä\n"
   "   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala")})

(def sijainti
  {:id :sijainti :make-query #(keyword-terms-query "sijainti" %)
   :desc (str
   "   |        - in: query\n"
   "   |          name: sijainti\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja\n"
   "   |          example: kunta_091,maakunta_01,maakunta_03")})

(def maakunta
  {:id :maakunta
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "sijainti.keyword")
                               (common-filters-without-rajainkeys constraints current-time "maakunta")
                               {:include "maakunta.*"}))})
(def kunta
  {:id :kunta
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "sijainti.keyword")
                               (common-filters-without-rajainkeys constraints current-time "kunta")
                               {:include "kunta.*"}))})

(def opetuskieli
  {:id :opetuskieli :make-query #(keyword-terms-query "opetuskielet" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "opetuskielet.keyword")
                               (common-filters-without-rajainkeys constraints current-time "opetuskieli")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: opetuskieli\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu opetuskielten koodeja\n"
   "   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2")})

(def koulutusala
  {:id :koulutusala :make-query #(keyword-terms-query "koulutusalat" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "koulutusalat.keyword")
                               (common-filters-without-rajainkeys constraints current-time "koulutusala")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: koulutusala\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu koulutusalojen koodeja\n"
   "   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02")})

(def opetustapa
  {:id :opetustapa :make-query #(keyword-terms-query "opetustavat" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "opetustavat.keyword")
                               (common-filters-without-rajainkeys constraints current-time "opetustapa")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: opetustapa\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu opetustapojen koodeja\n"
   "   |          example: opetuspaikkakk_1, opetuspaikkakk_2")})

(def valintatapa
  {:id :valintatapa :make-query #(hakutieto-query "hakutiedot" "valintatavat" %)
   :aggs (fn [constraints current-time]
           (nested-rajain-aggregation "valintatapa" "search_terms.hakutiedot.valintatavat"
                                      (common-filters-without-rajainkeys constraints current-time "valintatapa")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: valintatapa\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu valintatapojen koodeja\n"
   "   |          example: valintatapajono_av, valintatapajono_tv")})

(def hakutapa
  {:id :hakutapa :make-query #(hakutieto-query "hakutiedot" "hakutapa" %)
   :aggs (fn [constraints current-time]
           (nested-rajain-aggregation "hakutapa" "search_terms.hakutiedot.hakutapa"
                                      (common-filters-without-rajainkeys constraints current-time "hakutapa")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: hakutapa\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu hakutapojen koodeja\n"
   "   |          example: hakutapa_01, hakutapa_03")})

(def jotpa
  {:id :jotpa :make-query #(single-tyoelama-boolean-query "hasJotpaRahoitus")
   :aggs (fn [constraints current-time]
           (bool-agg-filter (single-tyoelama-agg-boolean-filter "hasJotpaRahoitus")
                            (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: jotpa\n"
   "   |          schema:\n"
   "   |            type: boolean\n"
   "   |            default: false\n"
   "   |          required: false\n"
   "   |          description: Haetaanko koulutuksia joilla on JOTPA-rahoitus")})

(def tyovoimakoulutus
  {:id :tyovoimakoulutus :make-query #(single-tyoelama-boolean-query "isTyovoimakoulutus")
   :aggs (fn [constraints current-time]
           (bool-agg-filter (single-tyoelama-agg-boolean-filter "isTyovoimakoulutus")
                            (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: tyovoimakoulutus\n"
   "   |          schema:\n"
   "   |            type: boolean\n"
   "   |            default: false\n"
   "   |          required: false\n"
   "   |          description: Haetaanko koulutuksia jotka ovat työvoimakoulutusta")})

(def taydennyskoulutus
  {:id :taydennyskoulutus :make-query #(single-tyoelama-boolean-query "isTaydennyskoulutus")
   :aggs (fn [constraints current-time]
           (bool-agg-filter (single-tyoelama-agg-boolean-filter "isTaydennyskoulutus")
                            (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: taydennyskoulutus\n"
   "   |          schema:\n"
   "   |            type: boolean\n"
   "   |            default: false\n"
   "   |          required: false\n"
   "   |          description: Haetaanko koulutuksia jotka ovat täydennyskoulutusta")})

(def yhteishaku
  {:id :yhteishaku :make-query #(hakutieto-query "hakutiedot" "yhteishakuOid" %)
   :aggs (fn [constraints current-time]
           (nested-rajain-aggregation "yhteishaku" "search_terms.hakutiedot.yhteishakuOid"
                                      (common-filters-without-rajainkeys constraints current-time "yhteishaku")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: yhteishaku\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu lista yhteishakujen oideja\n"
   "   |          example: 1.2.246.562.29.00000000000000000800")})

(def pohjakoulutusvaatimus
  {:id :pohjakoulutusvaatimus :make-query #(hakutieto-query "hakutiedot" "pohjakoulutusvaatimukset" %)
   :aggs (fn [constraints current-time]
            (nested-rajain-aggregation "pohjakoulutusvaatimus" "search_terms.hakutiedot.pohjakoulutusvaatimukset"
                                       (common-filters-without-rajainkeys constraints current-time "pohjakoulutusvaatimus")))
   :desc (str
   "   |        - in: query\n"
   "   |          name: pohjakoulutusvaatimus\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltu pohjakoulutusvaatimusten koodeja\n"
   "   |          example: pohjakoulutusvaatimuskonfo_am, pohjakoulutusvaatimuskonfo_102")})

(def oppilaitos
  {:id :oppilaitos :make-query #(keyword-terms-query "oppilaitosOid" %)
   :desc (str
   "   |        - in: query\n"
   "   |          name: oppilaitos\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltuna lista toteutusten oppilaitoksista\n"
   "   |          example: 1.2.246.562.10.93483820481, 1.2.246.562.10.29176843356")})

(def lukiopainotukset
  {:id :lukiopainotukset :make-query #(keyword-terms-query "lukiopainotukset" %)
   :desc (str
   "   |        - in: query\n"
   "   |          name: lukiopainotukset\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltuna lukiopainotusten koodeja\n"
   "   |          example: lukiopainotukset_0111, lukiopainotukset_001")})

(def lukiolinjaterityinenkoulutustehtava
  {:id :lukiolinjaterityinenkoulutustehtava :make-query #(keyword-terms-query "lukiolinjaterityinenkoulutustehtava" %)
   :desc (str
   "   |        - in: query\n"
   "   |          name: lukiolinjaterityinenkoulutustehtava\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltuna lukiolinjaterityinenkoulutustehtava-koodeja\n"
   "   |          example: lukiolinjaterityinenkoulutustehtava_0100, lukiolinjaterityinenkoulutustehtava_0126")})

(def osaamisala
  {:id :osaamisala :make-query #(keyword-terms-query "osaamisala" %)
   :desc (str
   "   |        - in: query\n"
   "   |          name: osaamisala\n"
   "   |          schema:\n"
   "   |            type: string\n"
   "   |          required: false\n"
   "   |          description: Pilkulla eroteltuna ammatillisten osaamisalojen koodeja\n"
   "   |          example: osaamisala_1756, osaamisala_3076")})

(def hakukaynnissa
  {:id :hakukaynnissa :make-query (fn [constraints current-time] (when (true? (:hakukaynnissa constraints)) (hakuaika-filter-query current-time)))
   :aggs (fn [constraints current-time]
           (hakukaynnissa-aggregation current-time (common-filters constraints current-time)))
   :desc (str
           "   |        - in: query\n"
           "   |          name: hakukaynnissa\n"
           "   |          schema:\n"
           "   |            type: boolean\n"
           "   |            default: false\n"
           "   |          required: false\n"
           "   |          description: Haetaanko koulutuksia joilla on haku käynnissä")})

(swap! common-rajain-definitions conj koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus)
(swap! boolean-type-rajaimet conj (:id hakukaynnissa) (:id jotpa) (:id tyovoimakoulutus) (:id taydennyskoulutus))

(reset! combined-tyoelama-rajain {:make-query #(make-combined-boolean-filter-query % [jotpa tyovoimakoulutus taydennyskoulutus])})
(reset! hakukaynnissa-rajain hakukaynnissa)

(def koulutus-or-oppilaitos-rajain-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus])

(def koulutus-jarjestaja-rajain-definitions
  [sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala oppilaitos])

(def oppilaitos-tarjonta-rajain-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa])

(def lukiopainotukset-aggs
  {:id :lukiopainotukset-aggs :make-query #(rajain-aggregation (->field-key "lukiopainotukset.keyword") [])})

(def lukiolinjaterityinenkoulutustehtava-aggs
  {:id :lukiolinjaterityinenkoulutustehtava-aggs :make-query #(rajain-aggregation (->field-key "lukiolinjaterityinenkoulutustehtava.keyword") [])})

(def osaamisala-aggs
  {:id :osaamisala-aggs :make-query #(rajain-aggregation (->field-key "osaamisalat.keyword") [])})

(def default-aggregation-defs
  [maakunta kunta opetuskieli opetustapa hakukaynnissa hakutapa pohjakoulutusvaatimus valintatapa yhteishaku koulutusala koulutustyyppi])

(defn- generate-default-aggs
  [constraints current-time]
  (into {} (for [agg default-aggregation-defs] {(:id agg) ((:aggs agg) constraints current-time)})))

(defn- generate-tyoelama-aggregations
  [constraints current-time]
  (into {} (for [agg [jotpa tyovoimakoulutus taydennyskoulutus]] {(:id agg) ((:aggs agg) constraints current-time)})))

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