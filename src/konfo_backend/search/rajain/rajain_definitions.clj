(ns konfo-backend.search.rajain.rajain-definitions
  (:require
    [konfo-backend.tools :refer [current-time-as-kouta-format]]
    [konfo-backend.search.rajain.query-tools :refer :all]))

;; Seuraavat toteutettu atomeina ristikkäisten riippuvuuksien vuoksi: Näitä käytetään heti alussa esim. common-filtersissä,
;; vaikka varsinaiset sisällöt (rajain-määritykset) asetetaan vasta myöhempänä. Ja common-filtersiä taas käytetään
;; rajain-määritysten sisällä, eli se täytyy olla määriteltynä ennen rajain-määrityksiä.
(def common-rajain-definitions (atom []))
(def combined-tyoelama-rajain (atom {}))
(def hakukaynnissa-rajain (atom {}))
(def boolean-type-rajaimet (atom []))

(def combined-jarjestaja-rajain
  {:make-query #(lukiolinjat-and-osaamisala-filters %)})

(defn constraints?
  [constraints]
  (let [contains-non-boolean-rajaimet? (not (empty? (filter #(constraint? constraints %) (map :id @common-rajain-definitions))))
        contains-boolean-true-rajaimet? (not (empty? (filter #(true? (% constraints)) @boolean-type-rajaimet)))]
    (or contains-non-boolean-rajaimet? contains-boolean-true-rajaimet?)))

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
      (flatten
        (conj
          (mapv make-constraint-query @common-rajain-definitions)
          ((:make-query @combined-tyoelama-rajain) constraints)
          ((:make-query @hakukaynnissa-rajain) constraints current-time))))))

(defn inner-hits-filters
  [tuleva? constraints]
  {:bool
   {:must
    [{:term {"search_terms.onkoTuleva" tuleva?}}
     {:bool ((:make-query combined-jarjestaja-rajain) constraints)}]
    :filter (common-filters constraints (current-time-as-kouta-format))}})

(defn common-filters-without-rajainkeys
  [constraints current-time & rajain-keys]
  (let [constraints-wo-rajainkey (apply dissoc constraints (map keyword rajain-keys))]
    (when (constraints? constraints-wo-rajainkey)
      (common-filters constraints-wo-rajainkey current-time))))

(def koulutustyyppi
  {:id :koulutustyyppi :make-query #(keyword-terms-query "koulutustyypit" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "koulutustyypit.keyword")
                               (common-filters-without-rajainkeys constraints current-time "koulutustyyppi") {:size   (count koulutustyypit)
                                                                                                              :include koulutustyypit}))
   :desc "
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala"})

(def sijainti
  {:id :sijainti :make-query #(keyword-terms-query "sijainti" %)
   :desc "
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03"})

(def maakunta
  {:id :maakunta
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "sijainti.keyword")
                               (common-filters-without-rajainkeys constraints current-time "sijainti")
                               {:include "maakunta.*"}))})
(def kunta
  {:id :kunta
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "sijainti.keyword")
                               (common-filters-without-rajainkeys constraints current-time "sijainti")
                               {:include "kunta.*"}))})

(def opetuskieli
  {:id :opetuskieli :make-query #(keyword-terms-query "opetuskielet" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "opetuskielet.keyword")
                               (common-filters-without-rajainkeys constraints current-time "opetuskieli")))
   :desc "
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2"})

(def koulutusala
  {:id :koulutusala :make-query #(keyword-terms-query "koulutusalat" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "koulutusalat.keyword")
                               (common-filters-without-rajainkeys constraints current-time "koulutusala")))
   :desc "
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02"})

(def opetustapa
  {:id :opetustapa :make-query #(keyword-terms-query "opetustavat" %)
   :aggs (fn [constraints current-time]
           (rajain-aggregation (->field-key "opetustavat.keyword")
                               (common-filters-without-rajainkeys constraints current-time "opetustapa")))
   :desc "
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2"})

(def valintatapa
  {:id :valintatapa :make-query #(hakutieto-query "hakutiedot" "valintatavat" %)
   :aggs (fn [constraints current-time]
           (nested-rajain-aggregation "valintatapa" "search_terms.hakutiedot.valintatavat"
                                      (common-filters-without-rajainkeys constraints current-time "valintatapa")))
   :desc "
   |        - in: query
   |          name: valintatapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu valintatapojen koodeja
   |          example: valintatapajono_av, valintatapajono_tv"})

(def hakutapa
  {:id :hakutapa :make-query #(hakutieto-query "hakutiedot" "hakutapa" %)
   :aggs (fn [constraints current-time]
           (nested-rajain-aggregation "hakutapa" "search_terms.hakutiedot.hakutapa"
                                      (common-filters-without-rajainkeys constraints current-time "hakutapa")))
   :desc "
   |        - in: query
   |          name: hakutapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu hakutapojen koodeja
   |          example: hakutapa_01, hakutapa_03"})

(def jotpa
  {:id :jotpa :make-query #(single-tyoelama-boolean-term "hasJotpaRahoitus")
   :aggs (fn [constraints current-time]
           (bool-agg-filter (single-tyoelama-boolean-term "hasJotpaRahoitus")
                            (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))
   :desc "
   |        - in: query
   |          name: jotpa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on JOTPA-rahoitus"})

(def tyovoimakoulutus
  {:id :tyovoimakoulutus :make-query #(single-tyoelama-boolean-term "isTyovoimakoulutus")
   :aggs (fn [constraints current-time]
           (bool-agg-filter (single-tyoelama-boolean-term "isTyovoimakoulutus")
                            (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))
   :desc "
   |        - in: query
   |          name: tyovoimakoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat työvoimakoulutusta"})

(def taydennyskoulutus
  {:id :taydennyskoulutus :make-query #(single-tyoelama-boolean-term "isTaydennyskoulutus")
   :aggs (fn [constraints current-time]
           (bool-agg-filter (single-tyoelama-boolean-term "isTaydennyskoulutus")
                            (common-filters-without-rajainkeys constraints current-time "jotpa" "tyovoimakoulutus" "taydennyskoulutus")))
   :desc "
   |        - in: query
   |          name: taydennyskoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat täydennyskoulutusta"})

(def yhteishaku
  {:id :yhteishaku :make-query #(hakutieto-query "hakutiedot" "yhteishakuOid" %)
   :aggs (fn [constraints current-time]
           (nested-rajain-aggregation "yhteishaku" "search_terms.hakutiedot.yhteishakuOid"
                                      (common-filters-without-rajainkeys constraints current-time "yhteishaku")))
   :desc "
   |        - in: query
   |          name: yhteishaku
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista yhteishakujen oideja
   |          example: 1.2.246.562.29.00000000000000000800"})

(def pohjakoulutusvaatimus
  {:id :pohjakoulutusvaatimus :make-query #(hakutieto-query "hakutiedot" "pohjakoulutusvaatimukset" %)
   :aggs (fn [constraints current-time]
            (nested-rajain-aggregation "pohjakoulutusvaatimus" "search_terms.hakutiedot.pohjakoulutusvaatimukset"
                                       (common-filters-without-rajainkeys constraints current-time "pohjakoulutusvaatimus")))
   :desc "
   |        - in: query
   |          name: pohjakoulutusvaatimus
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu pohjakoulutusvaatimusten koodeja
   |          example: pohjakoulutusvaatimuskonfo_am, pohjakoulutusvaatimuskonfo_102"})

(def oppilaitos
  {:id :oppilaitos :make-query #(keyword-terms-query "oppilaitosOid" %)
   :desc "
   |        - in: query
   |          name: oppilaitos
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna lista toteutusten oppilaitoksista
   |          example: 1.2.246.562.10.93483820481, 1.2.246.562.10.29176843356"})

(def lukiopainotukset
  {:id :lukiopainotukset :make-query #(keyword-terms-query "lukiopainotukset" %)
   :desc "
   |        - in: query
   |          name: lukiopainotukset
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna lukiopainotusten koodeja
   |          example: lukiopainotukset_0111, lukiopainotukset_001"})

(def lukiolinjaterityinenkoulutustehtava
  {:id :lukiolinjaterityinenkoulutustehtava :make-query #(keyword-terms-query "lukiolinjaterityinenkoulutustehtava" %)
   :desc "
   |        - in: query
   |          name: lukiolinjaterityinenkoulutustehtava
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna lukiolinjaterityinenkoulutustehtava-koodeja
   |          example: lukiolinjaterityinenkoulutustehtava_0100, lukiolinjaterityinenkoulutustehtava_0126"})

(def osaamisala
  {:id :osaamisala :make-query #(keyword-terms-query "osaamisala" %)
   :desc "
   |        - in: query
   |          name: osaamisala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna ammatillisten osaamisalojen koodeja
   |          example: osaamisala_1756, osaamisala_3076"})

(def hakukaynnissa
  {:id :hakukaynnissa :make-query (fn [constraints current-time] (when (true? (:hakukaynnissa constraints)) (hakuaika-filter-query current-time)))
   :aggs (fn [constraints current-time]
           (hakukaynnissa-aggregation current-time (common-filters-without-rajainkeys constraints current-time "hakukaynnissa")))
   :desc "
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on haku käynnissä"})

(swap! common-rajain-definitions conj koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus)
(swap! boolean-type-rajaimet conj (:id hakukaynnissa) (:id jotpa) (:id tyovoimakoulutus) (:id taydennyskoulutus))

(reset! combined-tyoelama-rajain {:make-query #(make-combined-boolean-filter-query % [jotpa tyovoimakoulutus taydennyskoulutus])})
(reset! hakukaynnissa-rajain hakukaynnissa)

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
  (let [constraints-wo-tyoelama (dissoc constraints conj (:id jotpa) (:id tyovoimakoulutus) (:id taydennyskoulutus))]
  (into {} (for [agg [jotpa tyovoimakoulutus taydennyskoulutus]]
             {(:id agg) ((:aggs agg) constraints-wo-tyoelama current-time)}))))

(defn generate-hakutulos-aggregations
  [constraints]
  (let [current-time (current-time-as-kouta-format)]
    (merge (generate-default-aggs constraints current-time)
           (generate-tyoelama-aggregations constraints current-time))))

(defn generate-jarjestajat-aggregations
  [tuleva? constraints]
  (let [default-aggs (generate-default-aggs {} (current-time-as-kouta-format))]
    (into
      {:inner_hits_agg
       {:filter (inner-hits-filters tuleva? constraints)
        :aggs
        (-> default-aggs
            (assoc :oppilaitos (rajain-aggregation "search_terms.oppilaitosOid.keyword" {} {:size 10000
                                                                                            :min_doc_count 1}))
            (dissoc :koulutusala :koulutustyyppi))}}
      (for [aggs [lukiopainotukset-aggs lukiolinjaterityinenkoulutustehtava-aggs osaamisala-aggs]]
        {(:id aggs) ((:make-query aggs))}))))

(defn generate-tarjoajat-aggregations
  [tuleva? constraints]
    {:inner_hits_agg
     {:filter (inner-hits-filters tuleva? constraints)
      :aggs (generate-default-aggs {} (current-time-as-kouta-format))}})
