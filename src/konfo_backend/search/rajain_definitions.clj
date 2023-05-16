(ns konfo-backend.search.rajain-definitions
  (:require
   [konfo-backend.tools :refer [current-time-as-kouta-format]]
   [konfo-backend.search.rajain-tools :refer :all]))

;; Seuraavat toteutettu atomeina ristikkäisten riippuvuuksien vuoksi: Näitä käytetään heti alussa esim. common-filtersissä,
;; vaikka varsinaiset sisällöt (rajain-määritykset) asetetaan vasta myöhempänä. Ja common-filtersiä taas käytetään
;; rajain-määritysten sisällä, eli se täytyy olla määriteltynä ennen rajain-määrityksiä.
(def common-rajain-definitions (atom []))
(def combined-tyoelama-rajain (atom {}))
(def hakukaynnissa-rajain (atom {}))
(def boolean-type-rajaimet (atom []))
(def jarjestaja-rajain-definitions (atom []))

(defn constraint?
  [constraints key]
  (not-empty (key constraints)))

(defn constraints?
  [constraints]
  (let [contains-non-boolean-rajaimet (not-empty (filter #(constraint? constraints %) (map :id @common-rajain-definitions)))
        contains-boolean-true-rajaimet (not-empty (filter #(true? (% constraints)) @boolean-type-rajaimet))
        contains-jarjestaja-rajaimet (not-empty (filter #(constraint? constraints %) (map :id @jarjestaja-rajain-definitions)))]
    (or contains-non-boolean-rajaimet contains-boolean-true-rajaimet contains-jarjestaja-rajaimet)))

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
       ((:make-query @hakukaynnissa-rajain) constraints current-time)
       (mapv make-constraint-query @jarjestaja-rajain-definitions))))))


(defn aggregation-filters-without-rajainkeys
  [constraints excluded-rajain-keys rajain-context]
  (let [extra-constraints (:extra-filter rajain-context)
        current-time (:current-time rajain-context)
        constraints-wo-rajainkey (apply dissoc constraints (map keyword excluded-rajain-keys))]
    (when (or (constraints? constraints-wo-rajainkey) (not (nil? extra-constraints)))
      (vec (flatten
            (cond-> []
              (constraints? constraints-wo-rajainkey) (conj (common-filters constraints-wo-rajainkey current-time))
              extra-constraints (concat (vector extra-constraints))))))))

(def koulutustyyppi
  {:id :koulutustyyppi
   :make-query #(->terms-query "koulutustyypit.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "koulutustyypit.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["koulutustyyppi"] rajain-context)
                                   (merge rajain-context
                                          {:term-params {:size (count koulutustyypit)
                                                         :include koulutustyypit}})))
   :desc "
   |        - in: query
   |          name: koulutustyyppi
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala"})

(def sijainti
  {:id :sijainti
   :make-query #(->terms-query "sijainti.keyword" %)
   :desc "
   |        - in: query
   |          name: sijainti
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna kuntien ja maakuntien koodeja (koodistot \"kunta\" ja \"maakunta\")
   |          example: kunta_091,maakunta_01"})

(def maakunta
  {:id :maakunta
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "sijainti.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["sijainti"] rajain-context)
                                   (merge rajain-context {:term-params {:include "maakunta.*"}})))})
(def kunta
  {:id :kunta
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "sijainti.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["sijainti"] rajain-context)
                                   (merge rajain-context {:term-params {:include "kunta.*"}})))})

(def opetuskieli
  {:id :opetuskieli
   :make-query #(->terms-query "opetuskielet.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "opetuskielet.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["opetuskieli"] rajain-context)
                                   rajain-context))
   :desc "
   |        - in: query
   |          name: opetuskieli
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna \"oppilaitoksenopetuskieli\"-koodiston koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2"})

(def koulutusala
  {:id :koulutusala
   :make-query #(->terms-query "koulutusalat.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "koulutusalat.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["koulutusala"] rajain-context)
                                   rajain-context))
   :desc "
   |        - in: query
   |          name: koulutusala
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna koulutusalojen koodeja (koodistot \"kansallinenkoulutusluokitus2016koulutusalataso1\" ja \"kansallinenkoulutusluokitus2016koulutusalataso2\")
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02"})

(def opetustapa
  {:id :opetustapa
   :make-query #(->terms-query "opetustavat.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "opetustavat.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["opetustapa"] rajain-context)
                                   rajain-context))
   :desc "
   |        - in: query
   |          name: opetustapa
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna \"opetuspaikkakk\"-koodiston koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2"})

(def opetusaika
  {:id :opetusaika
   :make-query #(->terms-query "metadata.opetusajat.koodiUri" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "metadata.opetusajat.koodiUri.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["opetusaika"] rajain-context)
                                   rajain-context))
   :desc "
   |        - in: query
   |          name: opetusaika
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          required: false
   |          description: Pilkulla eroteltuna \"opetusaikakk\"-koodiston koodeja
   |          example: opetusaikakk_1,opetusaikakk_2"})

(def valintatapa
  {:id :valintatapa
   :make-query #(nested-query "hakutiedot" "valintatavat" %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-aggregation "valintatapa" "search_terms.hakutiedot.valintatavat"
                                          (aggregation-filters-without-rajainkeys constraints ["valintatapa"] rajain-context)
                                          rajain-context))
   :desc "
   |        - in: query
   |          name: valintatapa
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna \"valintatapajono\"-koodiston koodeja
   |          example: valintatapajono_av, valintatapajono_tv"})

(def hakutapa
  {:id :hakutapa
   :make-query #(nested-query "hakutiedot" "hakutapa" %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-aggregation "hakutapa" "search_terms.hakutiedot.hakutapa"
                                          (aggregation-filters-without-rajainkeys constraints ["hakutapa"] rajain-context)
                                          rajain-context))
   :desc "
   |        - in: query
   |          name: hakutapa
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna \"hakutapa\"-koodiston koodeja
   |          example: hakutapa_01, hakutapa_03"})

(def jotpa
  {:id :jotpa
   :make-query #(->boolean-term-query "hasJotpaRahoitus")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "hasJotpaRahoitus")
                                (aggregation-filters-without-rajainkeys constraints ["jotpa" "tyovoimakoulutus" "taydennyskoulutus"] rajain-context)
                                rajain-context))
   :desc "
   |        - in: query
   |          name: jotpa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia, joilla on JOTPA-rahoitus?"})

(def tyovoimakoulutus
  {:id :tyovoimakoulutus
   :make-query #(->boolean-term-query "isTyovoimakoulutus")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "isTyovoimakoulutus")
                                (aggregation-filters-without-rajainkeys constraints ["jotpa" "tyovoimakoulutus" "taydennyskoulutus"] rajain-context)
                                rajain-context))
   :desc "
   |        - in: query
   |          name: tyovoimakoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia, jotka ovat työvoimakoulutusta?"})

(def taydennyskoulutus
  {:id :taydennyskoulutus
   :make-query #(->boolean-term-query "isTaydennyskoulutus")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "isTaydennyskoulutus")
                                (aggregation-filters-without-rajainkeys constraints ["jotpa" "tyovoimakoulutus" "taydennyskoulutus"] rajain-context)
                                rajain-context))
   :desc "
   |        - in: query
   |          name: taydennyskoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia, jotka ovat täydennyskoulutusta?"})

(def yhteishaku
  {:id :yhteishaku
   :make-query #(nested-query "hakutiedot" "yhteishakuOid" %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-aggregation "yhteishaku" "search_terms.hakutiedot.yhteishakuOid"
                                          (aggregation-filters-without-rajainkeys constraints ["yhteishaku"] rajain-context)
                                          rajain-context))
   :desc "
   |        - in: query
   |          name: yhteishaku
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna yhteishakujen oideja
   |          example: 1.2.246.562.29.00000000000000000800"})

(def pohjakoulutusvaatimus
  {:id :pohjakoulutusvaatimus
   :make-query #(nested-query "hakutiedot" "pohjakoulutusvaatimukset" %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-aggregation "pohjakoulutusvaatimus" "search_terms.hakutiedot.pohjakoulutusvaatimukset"
                                          (aggregation-filters-without-rajainkeys constraints ["pohjakoulutusvaatimus"] rajain-context)
                                          rajain-context))
   :desc "
   |        - in: query
   |          name: pohjakoulutusvaatimus
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna \"pohjakoulutusvaatimuskonfo\"-koodiston koodeja
   |          example: pohjakoulutusvaatimuskonfo_am, pohjakoulutusvaatimuskonfo_102"})

(def oppilaitos
  {:id :oppilaitos
   :make-query #(->terms-query "oppilaitosOid.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "oppilaitosOid.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["oppilaitos"] rajain-context)
                                   (merge rajain-context {:term-params {:size 10000
                                                                        :min_doc_count 1}})))
   :desc "
   |        - in: query
   |          name: oppilaitos
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna toteutusten oppilaitosten oideja
   |          example: 1.2.246.562.10.93483820481, 1.2.246.562.10.29176843356"})

(def lukiopainotukset
  {:id :lukiopainotukset
   :make-query #(->terms-query "lukiopainotukset.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "lukiopainotukset.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["lukiopainotukset"] rajain-context)
                                   rajain-context))
   :desc "
   |        - in: query
   |          name: lukiopainotukset
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna \"lukiopainotukset\"-koodiston koodeja
   |          example: lukiopainotukset_0111, lukiopainotukset_001"})

(def lukiolinjaterityinenkoulutustehtava
  {:id :lukiolinjaterityinenkoulutustehtava
   :make-query #(->terms-query "lukiolinjaterityinenkoulutustehtava.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "lukiolinjaterityinenkoulutustehtava.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["lukiolinjaterityinenkoulutustehtava"] rajain-context)
                                   rajain-context))
   :desc "
   |        - in: query
   |          name: lukiolinjaterityinenkoulutustehtava
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna \"lukiolinjaterityinenkoulutustehtava\"-koodiston koodeja
   |          example: lukiolinjaterityinenkoulutustehtava_0100, lukiolinjaterityinenkoulutustehtava_0126"})

(def osaamisala
  {:id :osaamisala
   :make-query #(->terms-query "osaamisala.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-aggregation (->field-key "osaamisalat.keyword")
                                   (aggregation-filters-without-rajainkeys constraints ["osaamisala"] rajain-context)
                                   rajain-context))
   :desc "
   |        - in: query
   |          name: osaamisala
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          description: Pilkulla eroteltuna ammatillisten koulutusten \"osaamisala\"-koodiston koodeja
   |          example: osaamisala_1756, osaamisala_3076"})

(def hakukaynnissa
  {:id :hakukaynnissa
   :make-query (fn [constraints current-time] (when (true? (:hakukaynnissa constraints)) (hakuaika-filter-query current-time)))
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (hakuaika-filter-query (:current-time rajain-context))
                                (aggregation-filters-without-rajainkeys constraints ["hakukaynnissa"] rajain-context)
                                rajain-context))
   :desc "
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutukset, joilla on haku käynnissä?"})

(swap! common-rajain-definitions conj koulutustyyppi sijainti opetuskieli koulutusala opetustapa opetusaika valintatapa hakutapa yhteishaku pohjakoulutusvaatimus)
(swap! boolean-type-rajaimet conj (:id hakukaynnissa) (:id jotpa) (:id tyovoimakoulutus) (:id taydennyskoulutus))
(swap! jarjestaja-rajain-definitions conj lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala oppilaitos)

(reset! combined-tyoelama-rajain {:make-query #(make-combined-boolean-filter-query % [jotpa tyovoimakoulutus taydennyskoulutus])})
(reset! hakukaynnissa-rajain hakukaynnissa)

(def default-aggregation-defs
  [maakunta kunta opetuskieli opetustapa opetusaika hakukaynnissa hakutapa pohjakoulutusvaatimus valintatapa yhteishaku koulutusala koulutustyyppi])

(def all-aggregation-defs (concat default-aggregation-defs [jotpa tyovoimakoulutus taydennyskoulutus oppilaitos osaamisala lukiopainotukset lukiolinjaterityinenkoulutustehtava]))

(defn- generate-default-aggs
  [constraints rajain-context]
  (into {} (for [agg default-aggregation-defs] {(:id agg) ((:make-agg agg) constraints rajain-context)})))

(defn- generate-tyoelama-aggregations
  [constraints rajain-context]
  (let [constraints-wo-tyoelama (dissoc constraints conj (:id jotpa) (:id tyovoimakoulutus) (:id taydennyskoulutus))]
    (into {} (for [agg [jotpa tyovoimakoulutus taydennyskoulutus]]
               {(:id agg) ((:make-agg agg) constraints-wo-tyoelama rajain-context)}))))

(defn generate-hakutulos-aggregations
  [constraints]
  (let [rajain-context {:current-time (current-time-as-kouta-format)}]
    (merge (generate-default-aggs constraints rajain-context)
           (generate-tyoelama-aggregations constraints rajain-context))))

(defn generate-jarjestajat-aggregations
  [constraints tuleva?]
  (let [rajain-context {:current-time (current-time-as-kouta-format)
                        :extra-filter (onkoTuleva-query tuleva?)
                        :reverse-nested-path "search_terms"}
        default-aggs (generate-default-aggs constraints rajain-context)]
    (-> default-aggs
        (into (for [agg @jarjestaja-rajain-definitions] {(:id agg) ((:make-agg agg) constraints rajain-context)}))
        (dissoc :koulutusala :koulutustyyppi))))

(defn generate-tarjoajat-aggregations
  [constraints tuleva?]
  (generate-default-aggs constraints {:current-time (current-time-as-kouta-format)
                                      :extra-filter (onkoTuleva-query tuleva?)
                                      :reverse-nested-path "search_terms"}))
