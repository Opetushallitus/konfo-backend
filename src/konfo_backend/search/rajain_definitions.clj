(ns konfo-backend.search.rajain-definitions
  (:require [clj-time.core :as time]
            [clojure.string :as str]
            [konfo-backend.search.rajain-tools :refer :all]
            [konfo-backend.tools :refer [current-time-as-kouta-format]]))

;; Esitellään myöhemmin alustettu muuttuja ristikkäisten riippuvuuksien vuoksi. Tätä käytetään heti
;; alla olevissa funktioissa, vaikka varsinaiset sisällöt (rajain-määritykset) asetetaan vasta 
;; myöhempänä. Ja common-filtersiä taas käytetään rajain-määritysten sisällä, eli se täytyy olla 
;; määriteltynä ennen rajain-määrityksiä.
(declare all-rajain-definitions)

(defn constraints?
  [constraints]
  (not-empty (filter #(constraint? ((keyword %) constraints)) (map :id all-rajain-definitions))))

(defn- create-rajain-query-groups [constraints current-time]
  (reduce (fn [result rajain]
            (if-let [query (make-query-for-rajain constraints rajain current-time)]
              (let [nested-path (get-in query [:nested :path])
                    nest-id (get-top-level-nested-path nested-path)
                    group-id (:rajainGroupId rajain)]
                (update-in result [nest-id group-id] #(conj (or % []) query)))
              result))
          {}
          all-rajain-definitions))

(defn- single-nested-query? [queries] (and (= 1 (count queries)) (get-in (first queries) [:nested])))

(defn common-filters
  [constraints current-time]
  ; Muodostetaan ensin kolmitasoinen mappi (nested-id -> group-id -> query), jotta voidaan helposti yhdistellä queryt tasojen mukaan
  (let [nested-grouped-queries (create-rajain-query-groups constraints current-time)]
    ; Yhdistetään nested- ja group- queryt
    (reduce-kv (fn [result nest-id nest-groups]
                 (let [queries (reduce-kv (fn [group-result group-id group-queries]
                                            ; Karsitaan pois queryista yhteinen nested-taso, jos sellainen on
                                            (let [stripped-queries (map #(strip-excess-nested % nest-id) group-queries)]
                                              (concat group-result
                                                      ; Bool-should tarvitsee lisätä vain jos rajain-ryhmä on olemassa ja siinä on enemmän kuin yksi query
                                                      (if (and group-id stripped-queries (< 1 (count stripped-queries)))
                                                        [{:bool {:should stripped-queries}}] ; Rajain-ryhmät pitää yhdistää "or":lla, joten lisätään bool-should
                                                        stripped-queries))))
                                          []
                                          nest-groups)]
                   (concat result
                           ; Yhdistetään nested-polun (käytännössä search_terms.hakutiedot) queryt yhdeksi queryksi, jotta ne kohdistuvat samaan nested-elementtiin.
                           ; Jos ryhmässä on ainoastaan yksi nested-query karsimisen jälkeen, sitä ei tarvitse kääriä enää uudelleen nested-queryyn
                           (if (and nest-id (not (single-nested-query? queries)))
                             [{:nested {:path nest-id
                                        :query {:bool {:filter queries}}}}]
                             queries))))
               []
               nested-grouped-queries)))

(defn- get-rajain-keys-by-group [rajain-group-id]
  (if rajain-group-id
    (map :id (filter #(= (:rajainGroupId %) rajain-group-id) all-rajain-definitions))
    []))

(defn aggregation-filters-for-rajain
  [own-rajain-key constraints rajain-context]
  (let [own-rajain (first (filter #(= own-rajain-key (:id %)) all-rajain-definitions))
        excluded-rajain-keys (concat [own-rajain-key] (get-rajain-keys-by-group (:rajainGroupId own-rajain)))
        extra-constraints (:extra-filter rajain-context)
        current-time (:current-time rajain-context)
        constraints-without-own (apply dissoc constraints excluded-rajain-keys)]
    (not-empty
     (vec (flatten
           (cond-> []
             (constraints? constraints-without-own) (conj (common-filters constraints-without-own current-time))
             extra-constraints (concat (vector extra-constraints))))))))

(def koulutustyyppi
  {:id :koulutustyyppi
   :make-query #(->terms-query "koulutustyypit.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "koulutustyypit.keyword")
                                         (aggregation-filters-for-rajain :koulutustyyppi constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: koulutustyyppi
          style: form
          explode: false
          schema:
            type: array
            items:
              $ref: '#/components/schemas/KonfoKoulutustyyppi'
          description: Pilkulla eroteltu lista koulutustyyppejä
          example: [amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala]"})

(def sijainti
  {:id :sijainti
   :make-query #(->terms-query "sijainti.keyword" %)
   :desc "
        - in: query
          name: sijainti
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna kuntien ja maakuntien koodeja (koodistot \"kunta\" ja \"maakunta\")
          example: [kunta_091,maakunta_01]"})

(def maakunta
  {:id :maakunta
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "sijainti.keyword")
                                         (aggregation-filters-for-rajain :sijainti constraints rajain-context)
                                         (merge rajain-context {:term-params {:include "maakunta.*"}})))})
(def kunta
  {:id :kunta
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "sijainti.keyword")
                                         (aggregation-filters-for-rajain :sijainti constraints rajain-context)
                                         (merge rajain-context {:term-params {:include "kunta.*"}})))})

(def opetuskieli
  {:id :opetuskieli
   :make-query #(->terms-query "opetuskielet.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "opetuskielet.keyword")
                                         (aggregation-filters-for-rajain :opetuskieli constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: opetuskieli
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna \"oppilaitoksenopetuskieli\"-koodiston koodeja
          example: [oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2]"})

(def koulutusala
  {:id :koulutusala
   :make-query #(->terms-query "koulutusalat.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "koulutusalat.keyword")
                                         (aggregation-filters-for-rajain :koulutusala constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: koulutusala
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna koulutusalojen koodeja (koodistot \"kansallinenkoulutusluokitus2016koulutusalataso1\" ja \"kansallinenkoulutusluokitus2016koulutusalataso2\")
          example: [kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02]"})

(def opetustapa
  {:id :opetustapa
   :make-query #(->terms-query "opetustavat.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "opetustavat.keyword")
                                         (aggregation-filters-for-rajain :opetustapa constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: opetustapa
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna \"opetuspaikkakk\"-koodiston koodeja
          example: [opetuspaikkakk_1, opetuspaikkakk_2]"})

(def opetusaika
  {:id :opetusaika
   :make-query #(->terms-query "metadata.opetusajat.koodiUri" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "metadata.opetusajat.koodiUri.keyword")
                                         (aggregation-filters-for-rajain :opetusaika constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: opetusaika
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          required: false
          description: Pilkulla eroteltuna \"opetusaikakk\"-koodiston koodeja
          example: [opetusaikakk_1,opetusaikakk_2]"})

(def koulutuksenkestokuukausina
  {:id :koulutuksenkestokuukausina
   :make-query #(number-range-query "metadata.suunniteltuKestoKuukausina" %)
   :make-agg (fn [constraints rajain-context]
               (let [kesto (if (not-empty (:koulutuksenkestokuukausina constraints))
                             (:koulutuksenkestokuukausina constraints) [0])]
                 (bool-agg-filter (number-range-query "metadata.suunniteltuKestoKuukausina" kesto)
                                  (aggregation-filters-for-rajain :koulutuksenkestokuukausina constraints
                                                                  rajain-context)
                                  rajain-context)))
   :make-max-agg (fn [_] (max-agg-filter "search_terms.metadata.suunniteltuKestoKuukausina"))
   :desc "
        - in: query
          name: koulutuksenkestokuukausina_min
          style: form
          schema:
            type: number
          description: Koulutuksen suunnitellun keston alaraja.
          example: 10
        - in: query
          name: koulutuksenkestokuukausina_max
          style: form
          schema:
            type: number
          description: Koulutuksen suunnitellun keston yläraja.
          example: 100"})

(def valintatapa
  {:id :valintatapa
   :make-query #(nested-terms-query "hakutiedot" "valintatavat" %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-terms-aggregation "search_terms.hakutiedot.valintatavat"
                                                (aggregation-filters-for-rajain :valintatapa constraints rajain-context)
                                                rajain-context))
   :desc "
        - in: query
          name: valintatapa
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna \"valintatapajono\"-koodiston koodeja
          example: [valintatapajono_av, valintatapajono_tv]"})

(def hakutapa
  {:id :hakutapa
   :rajainGroupId :hakutapa
   :make-query #(nested-terms-query "hakutiedot" "hakutapa" %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-terms-aggregation "search_terms.hakutiedot.hakutapa"
                                                (aggregation-filters-for-rajain :hakutapa constraints rajain-context)
                                                rajain-context))
   :desc "
        - in: query
          name: hakutapa
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna \"hakutapa\"-koodiston koodeja
          example: [hakutapa_01, hakutapa_03]"})

(def jotpa
  {:id :jotpa
   :rajainGroupId :tyoelama
   :make-query #(->boolean-term-query "hasJotpaRahoitus")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "hasJotpaRahoitus")
                                (aggregation-filters-for-rajain :jotpa constraints rajain-context)
                                rajain-context))
   :desc "
        - in: query
          name: jotpa
          schema:
            type: boolean
            default: false
          required: false
          description: Haetaanko koulutuksia, joilla on JOTPA-rahoitus?"})

(def tyovoimakoulutus
  {:id :tyovoimakoulutus
   :rajainGroupId :tyoelama
   :make-query #(->boolean-term-query "isTyovoimakoulutus")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "isTyovoimakoulutus")
                                (aggregation-filters-for-rajain :tyovoimakoulutus constraints rajain-context)
                                rajain-context))
   :desc "
        - in: query
          name: tyovoimakoulutus
          schema:
            type: boolean
            default: false
          required: false
          description: Haetaanko koulutuksia, jotka ovat työvoimakoulutusta?"})

(def taydennyskoulutus
  {:id :taydennyskoulutus
   :rajainGroupId :tyoelama
   :make-query #(->boolean-term-query "isTaydennyskoulutus")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "isTaydennyskoulutus")
                                (aggregation-filters-for-rajain :taydennyskoulutus constraints rajain-context)
                                rajain-context))
   :desc "
        - in: query
          name: taydennyskoulutus
          schema:
            type: boolean
            default: false
          required: false
          description: Haetaanko koulutuksia, jotka ovat täydennyskoulutusta?"})

;; Päätellään koulutustyypin perusteella (koulutustyyppi_4)
(def amm_erityisopetus
  {:id :amm_erityisopetus
   :rajainGroupId :erityisopetus
   :make-query #(->boolean-term-query "metadata.ammatillinenPerustutkintoErityisopetuksena")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "metadata.ammPerustutkintoErityisopetuksena")
                                (aggregation-filters-for-rajain :amm_erityisopetus constraints rajain-context)
                                rajain-context))})

;; Päätellään koulutustyypin perusteella (tuva-erityisopetus)
(def tuva_erityisopetus
  {:id :tuva_erityisopetus
   :rajainGroupId :erityisopetus
   :make-query #(->boolean-term-query "metadata.jarjestetaanErityisopetuksena")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->boolean-term-query "metadata.jarjestetaanErityisopetuksena")
                                (aggregation-filters-for-rajain :tuva_erityisopetus constraints rajain-context)
                                rajain-context))})

(def maksullisuus
  {:desc "
        - in: query
          name: maksullisuustyyppi
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltu lista koulutuksen maksullisuustyyppejä
          example: [maksuton,maksullinen,lukuvuosimaksu]
        - in: query
          name: maksunmaara_min
          style: form
          schema:
            type: number
          description: Koulutuksen maksun minimimäärä. Käytetään vain jos maksullisuustyypiksi valittu \"maksullinen\"
          example: 100
        - in: query
          name: maksunmaara_max
          style: form
          schema:
            type: number
          description: Koulutuksen maksun maksimimäärä. Käytetään vain jos maksullisuustyypiksi valittu \"maksullinen\"
          example: 100
        - in: query
          name: lukuvuosimaksunmaara_min
          style: form
          schema:
            type: number
          description: Koulutuksen lukuvuosimaksun minimimäärä. Käytetään vain jos maksullisuustyypiksi valittu \"lukuvuosimaksu\"
          example: 100
        - in: query
          name: lukuvuosimaksunmaara_max
          style: form
          schema:
            type: number
          description: Koulutuksen lukuvuosimaksun maksimimäärä. Käytetään vain jos maksullisuustyypiksi valittu \"lukuvuosimaksu\"
          example: 100
        - in: query
          name: apuraha
          schema:
            type: boolean
            default: false
          required: false
          description: Haetaanko koulutuksia, joilla on käytössä apuraha? Käytetään vain jos maksullisuustyypiksi valittu \"lukuvuosimaksu\""})

(def maksuton
  {:id :maksuton
   :rajainGroupId :maksullisuus
   :make-query #(->terms-query "metadata.maksullisuustyyppi.keyword" "maksuton")
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (->terms-query "metadata.maksullisuustyyppi.keyword" "maksuton")
                                (aggregation-filters-for-rajain :maksuton constraints rajain-context)
                                rajain-context))})

(def maksullinen
  {:id :maksullinen
   :rajainGroupId :maksullisuus
   :make-query #(all-must [(->terms-query "metadata.maksullisuustyyppi.keyword" "maksullinen")
                           (number-range-query "metadata.maksunMaara" (:maksunmaara %))])
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (all-must [(->terms-query "metadata.maksullisuustyyppi.keyword" "maksullinen")
                                           (number-range-query "metadata.maksunMaara"
                                                               (get-in constraints [:maksullinen :maksunmaara]))])
                                (aggregation-filters-for-rajain :maksullinen constraints rajain-context)
                                rajain-context))
   :make-max-agg (fn [_] (max-agg-filter "search_terms.metadata.maksunMaara"
                                         (->terms-query "metadata.maksullisuustyyppi.keyword" "maksullinen")))})

(def lukuvuosimaksu
  {:id :lukuvuosimaksu
   :rajainGroupId :maksullisuus
   :make-query #(all-must [(->terms-query "metadata.maksullisuustyyppi.keyword" "lukuvuosimaksu")
                           (number-range-query "metadata.maksunMaara" (:maksunmaara %))
                           (->conditional-boolean-term-query "metadata.onkoApuraha" true (:apuraha %))])
   :make-agg (fn [constraints rajain-context]
               (bool-agg-filter (all-must [(->terms-query "metadata.maksullisuustyyppi.keyword" "lukuvuosimaksu")
                                           (number-range-query "metadata.maksunMaara" (get-in constraints [:lukuvuosimaksu :maksunmaara]))
                                           (->conditional-boolean-term-query "metadata.onkoApuraha" true (get-in constraints [:lukuvuosimaksu :apuraha]))])
                                (aggregation-filters-for-rajain :lukuvuosimaksu constraints rajain-context)
                                rajain-context))
   :make-max-agg (fn [constraints] (max-agg-filter "search_terms.metadata.maksunMaara"
                                                   (all-must [(->terms-query "metadata.maksullisuustyyppi.keyword" "lukuvuosimaksu")
                                                              (->conditional-boolean-term-query "metadata.onkoApuraha" true (get-in constraints [:lukuvuosimaksu :apuraha]))])))})

(def yhteishaku
  {:id :yhteishaku
   :rajainGroupId :hakutapa
   :make-query #(nested-terms-query "hakutiedot" "yhteishakuOid" %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-terms-aggregation "search_terms.hakutiedot.yhteishakuOid"
                                                (aggregation-filters-for-rajain :yhteishaku constraints rajain-context)
                                                rajain-context))
   :desc "
        - in: query
          name: yhteishaku
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna yhteishakujen oideja
          example: [1.2.246.562.29.00000000000000000800]"})

(def pohjakoulutusvaatimus
  {:id :pohjakoulutusvaatimus
   :make-query #(pohjakoulutusvaatimukset-filter-query %)
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-terms-aggregation "search_terms.hakutiedot.pohjakoulutusvaatimukset"
                                                (aggregation-filters-for-rajain :pohjakoulutusvaatimus constraints rajain-context)
                                                (merge rajain-context {:term-params {:missing "pohjakoulutusvaatimuskonfo_missing"}})))
   :desc "
        - in: query
          name: pohjakoulutusvaatimus
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna \"pohjakoulutusvaatimuskonfo\"-koodiston koodeja
          example: [pohjakoulutusvaatimuskonfo_am, pohjakoulutusvaatimuskonfo_102]"})

(def oppilaitos
  {:id :oppilaitos
   :make-query #(->terms-query "oppilaitosOid.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "oppilaitosOid.keyword")
                                         (aggregation-filters-for-rajain :oppilaitos constraints rajain-context)
                                         (merge rajain-context {:term-params {:size 10000
                                                                              :min_doc_count 1}})))
   :desc "
        - in: query
          name: oppilaitos
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna toteutusten oppilaitosten oideja
          example: [1.2.246.562.10.93483820481, 1.2.246.562.10.29176843356]"})

(def lukiopainotukset
  {:id :lukiopainotukset
   :make-query #(->terms-query "lukiopainotukset.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "lukiopainotukset.keyword")
                                         (aggregation-filters-for-rajain :lukiopainotukset constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: lukiopainotukset
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna \"lukiopainotukset\"-koodiston koodeja
          example: [lukiopainotukset_0111, lukiopainotukset_001]"})

(def lukiolinjaterityinenkoulutustehtava
  {:id :lukiolinjaterityinenkoulutustehtava
   :make-query #(->terms-query "lukiolinjaterityinenkoulutustehtava.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "lukiolinjaterityinenkoulutustehtava.keyword")
                                         (aggregation-filters-for-rajain :lukiolinjaterityinenkoulutustehtava constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: lukiolinjaterityinenkoulutustehtava
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna \"lukiolinjaterityinenkoulutustehtava\"-koodiston koodeja
          example: [lukiolinjaterityinenkoulutustehtava_0100, lukiolinjaterityinenkoulutustehtava_0126]"})

(def osaamisala
  {:id :osaamisala
   :make-query #(->terms-query "osaamisalat.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "osaamisalat.keyword")
                                         (aggregation-filters-for-rajain :osaamisala constraints rajain-context)
                                         rajain-context))
   :desc "
        - in: query
          name: osaamisala
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna ammatillisten koulutusten \"osaamisala\"-koodiston koodeja
          example: [osaamisala_1756, osaamisala_3076]"})

(def hakukaynnissa
  {:id :hakukaynnissa
   :rajainGroupId :hakuaika
   :make-query (fn [value current-time] (when (true? value) (hakukaynnissa-filter-query current-time)))
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-aggregation "search_terms.hakutiedot"
                                          (aggregation-filters-for-rajain :hakukaynnissa constraints rajain-context)
                                          {:filter (hakukaynnissa-filter-query (:current-time rajain-context))}
                                          rajain-context))
   :desc "
        - in: query
          name: hakukaynnissa
          schema:
            type: boolean
            default: false
          required: false
          description: Palautetaan koulutukset, joiden haku on käynissä"})

(def hakualkaapaivissa
  {:id :hakualkaapaivissa
   :agg-id :hakualkaapaivissa_30
   :rajainGroupId :hakuaika
   :make-query (fn [value current-time] (hakualkaapaivissa-filter-query current-time value))
   :make-agg (fn [constraints rajain-context]
               (nested-rajain-aggregation "search_terms.hakutiedot"
                                          (aggregation-filters-for-rajain :hakualkaapaivissa constraints rajain-context)
                                          {:filter (hakualkaapaivissa-filter-query (:current-time rajain-context) 30)}
                                          rajain-context))
   :desc "
        - in: query
          name: hakualkaapaivissa
          schema:
            type: number
            default: 30
          required: false
          description: Palautetaan koulutukset, joiden hakuaika alkaa x vuorokauden sisällä."})

(defn kevat-date? [date]
  (< (time/month date) 8))

(defn get-alkamiskausi-terms-include []
  (let [current-date (time/today)
        current-year (time/year current-date)
        kaudet (take 5 (cycle (if (kevat-date? current-date) ["kevat" "syksy"] ["syksy" "kevat"])))
        vuodet (map (partial + current-year) (if (kevat-date? current-date) [0 0 1 1 2] [0 1 1 2 2]))]
    (->> (map vector vuodet kaudet)
         (map (fn [[vuosi kausi]] (str vuosi "-" kausi)))
         (concat ["henkilokohtainen"]))))

(def alkamiskausi
  {:id :alkamiskausi
   :make-query #(->terms-query "paatellytAlkamiskaudet.keyword" %)
   :make-agg (fn [constraints rajain-context]
               (rajain-terms-aggregation (->field-key "paatellytAlkamiskaudet.keyword")
                                         (aggregation-filters-for-rajain :alkamiskausi constraints rajain-context)
                                         (merge rajain-context {:term-params {:include (get-alkamiskausi-terms-include)}})))
   :desc "
        - in: query
          name: alkamiskausi
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
          description: Pilkulla eroteltuna alkamiskausi-tunnisteita (merkkijono). Validit arvot ovat muotoa \"<vuosi>-kevat/syksy\" (esim. esim. \"2022-kevat\") tai \"henkilokohtainen\"
          example: [henkilokohtainen, 2022-kevat]"})

(def all-rajain-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa
   opetusaika valintatapa hakutapa yhteishaku pohjakoulutusvaatimus alkamiskausi
   koulutuksenkestokuukausina jotpa tyovoimakoulutus taydennyskoulutus
   amm_erityisopetus tuva_erityisopetus maksuton maksullinen
   lukuvuosimaksu hakukaynnissa hakualkaapaivissa lukiopainotukset
   lukiolinjaterityinenkoulutustehtava osaamisala oppilaitos])

(def common-agg-defs
  [maakunta kunta opetuskieli opetustapa opetusaika hakutapa pohjakoulutusvaatimus
   koulutuksenkestokuukausina valintatapa yhteishaku alkamiskausi maksuton maksullinen
   lukuvuosimaksu hakukaynnissa hakualkaapaivissa])

(def all-agg-defs (concat common-agg-defs
                          [koulutusala koulutustyyppi jotpa tyovoimakoulutus taydennyskoulutus
                           lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala oppilaitos]))

(def hakutulos-agg-defs
  (concat common-agg-defs [koulutusala koulutustyyppi jotpa tyovoimakoulutus taydennyskoulutus]))

(def jarjestaja-agg-defs (concat common-agg-defs [koulutustyyppi lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala oppilaitos]))

(def tarjoaja-agg-defs
  (concat common-agg-defs [koulutustyyppi koulutusala]))

(def max-agg-defs (filter #(not (nil? (:make-max-agg %))) all-agg-defs))

(defn ->max-agg-id
  [agg-id]
  (keyword (str/replace-first (str agg-id "-max") ":" "")))

(defn- generate-aggs
  [agg-defs constraints rajain-context]
  (let [max-agg-defs (filter #(not (nil? (:make-max-agg %))) agg-defs)]
    (-> {}
        (into (for [agg agg-defs] {(or (:agg-id agg) (:id agg)) ((:make-agg agg) constraints rajain-context)}))
        (into (for [agg max-agg-defs] {(->max-agg-id (:id agg)) ((:make-max-agg agg) constraints)})))))

(defn generate-hakutulos-aggregations
  [constraints]
  (let [rajain-context {:current-time (current-time-as-kouta-format)}]
    (generate-aggs hakutulos-agg-defs constraints rajain-context)))

(defn generate-jarjestajat-aggregations
  [constraints tuleva?]
  (let [rajain-context {:current-time (current-time-as-kouta-format)
                        :extra-filter (onkoTuleva-query tuleva?)
                        :reverse-nested-path "search_terms"}]
    (generate-aggs jarjestaja-agg-defs constraints rajain-context)))

(defn generate-tarjoajat-aggregations
  [constraints tuleva?]
  (generate-aggs tarjoaja-agg-defs constraints
                 {:current-time (current-time-as-kouta-format)
                  :extra-filter (onkoTuleva-query tuleva?)
                  :reverse-nested-path "search_terms"}))
