(ns konfo-backend.search.rajain-counts
  (:require [konfo-backend.index.haku :refer [get-yhteishaut]]
            [konfo-backend.index.oppilaitos :as oppilaitos]
            [konfo-backend.koodisto.koodisto :as k]
            [konfo-backend.tools :refer [koodi-uri-no-version reduce-merge-map]]))

(defn- koodi->rajain-counts
  [rajain-counts koodi]
  (let [koodiUri (keyword (:koodiUri koodi))
        nimi (get-in koodi [:nimi])
        count (get rajain-counts koodiUri 0)
        alakoodit (when (contains? koodi :alakoodit)
                    (reduce-merge-map #(koodi->rajain-counts rajain-counts %) (:alakoodit koodi)))]
    {koodiUri (cond-> {:nimi nimi}
                count (assoc :count count)
                alakoodit (assoc :alakoodit alakoodit))}))

(defn- koodisto->rajain-counts
  [rajain-counts koodisto]
  (let [rajain-counts-no-koodi-versions (into {} (map (fn [item] [(keyword (koodi-uri-no-version (name (first item))))
                                                                  (second item)]) rajain-counts))
        koodit (:koodit (k/get-koodisto-with-cache koodisto))]
    (reduce-merge-map #(koodi->rajain-counts rajain-counts-no-koodi-versions %)
                      koodit)))

(defn- get-koulutustyyppi-amm-alakoodi-counts [rajain-counts]
  (merge
   (select-keys (koodisto->rajain-counts rajain-counts "koulutustyyppi")
                [:koulutustyyppi_26 :koulutustyyppi_4 :koulutustyyppi_11
                 :koulutustyyppi_12])
   {:muu-amm-tutkinto {:count (get rajain-counts :muu-amm-tutkinto 0)}}))

(defn- koulutustyyppi
  [rajain-counts]
  (let [ammatillinen-count (get rajain-counts :amm 0)
        amm-alakoodi-counts (get-koulutustyyppi-amm-alakoodi-counts rajain-counts)
        lukio-count (get rajain-counts :lk 0)
        amk-count (get rajain-counts :amk 0)
        yo-count (get rajain-counts :yo 0)
        amk-alempi-count (get rajain-counts :amk-alempi 0)
        amk-ylempi-count (get rajain-counts :amk-ylempi 0)
        kandi-count (get rajain-counts :kandi 0)
        kandi-ja-maisteri-count (get rajain-counts :kandi-ja-maisteri 0)
        maisteri-count (get rajain-counts :maisteri 0)
        tohtori-count (get rajain-counts :tohtori 0)]
    {:lk {:count lukio-count}
     :amm (cond-> {:alakoodit amm-alakoodi-counts}
            ammatillinen-count (assoc :count ammatillinen-count))
     :amk (cond-> {:alakoodit {:amk-alempi {:count amk-alempi-count}
                               :amk-ylempi {:count amk-ylempi-count}}}
            amk-count (assoc :count amk-count))
     :yo (cond-> {:alakoodit {:kandi {:count kandi-count}
                              :kandi-ja-maisteri {:count kandi-ja-maisteri-count}
                              :maisteri {:count maisteri-count}
                              :tohtori {:count tohtori-count}}}
           yo-count (assoc :count yo-count))}))

(defn- koulutustyyppi-muu
  [rajain-counts]
  (let [amm-osaamisala-count (get rajain-counts :amm-osaamisala 0)
        amm-tutkinnon-osa-count (get rajain-counts :amm-tutkinnon-osa 0)
        telma-count (get rajain-counts :telma 0)
        amm-muu-count (get rajain-counts :amm-muu 0)
        muut-ammatilliset-count (get rajain-counts :muut-ammatilliset 0)
        amm-ope-erityisope-ja-opo-count (get rajain-counts :amm-ope-erityisope-ja-opo 0)
        total-amk-muu-count (get rajain-counts :amk-muu 0)
        tuva-normal-count (get rajain-counts :tuva-normal 0)
        tuva-erityisopetus-count (get rajain-counts :tuva-erityisopetus 0)
        total-tuva-count (get rajain-counts :tuva 0)
        vapaa-sivistystyo-opistovuosi-count (get rajain-counts :vapaa-sivistystyo-opistovuosi 0)
        vapaa-sivistystyo-muu-count (get rajain-counts :vapaa-sivistystyo-muu 0)
        total-vapaa-sivistystyo-count (get rajain-counts :vapaa-sivistystyo 0)
        aikuisten-perusopetus-count (get rajain-counts :aikuisten-perusopetus 0)
        taiteen-perusopetus-count (get rajain-counts :taiteen-perusopetus 0)
        erikoislaakari-count (get rajain-counts :erikoislaakari 0)
        kk-opintojakso-normal-count (get rajain-counts :kk-opintojakso-normal 0)
        kk-opintojakso-avoin-count (get rajain-counts :kk-opintojakso-avoin 0)
        kk-opintokokonaisuus-normal-count (get rajain-counts :kk-opintokokonaisuus-normal 0)
        kk-opintokokonaisuus-avoin-count (get rajain-counts :kk-opintokokonaisuus-avoin 0)
        ope-pedag-opinnot-count (get rajain-counts :ope-pedag-opinnot 0)
        erikoistumiskoulutus-count (get rajain-counts :erikoistumiskoulutus 0)
        total-kk-muu-count (get rajain-counts :kk-muu 0)
        muu-count (get rajain-counts :muu 0)]
    {:muut-ammatilliset (cond-> {:alakoodit {:amm-tutkinnon-osa {:count amm-tutkinnon-osa-count}
                                             :amm-osaamisala {:count amm-osaamisala-count}
                                             :amm-muu {:count amm-muu-count}
                                             :telma {:count telma-count}}}
                          muut-ammatilliset-count (assoc :count muut-ammatilliset-count))
     :tuva (cond-> {:alakoodit {:tuva-normal {:count tuva-normal-count}
                                :tuva-erityisopetus {:count tuva-erityisopetus-count}}}
             total-tuva-count (assoc :count total-tuva-count))
     :vapaa-sivistystyo
     (cond-> {:alakoodit {:vapaa-sivistystyo-opistovuosi {:count
                                                          vapaa-sivistystyo-opistovuosi-count}
                          :vapaa-sivistystyo-muu {:count vapaa-sivistystyo-muu-count}}}
       total-vapaa-sivistystyo-count (assoc :count total-vapaa-sivistystyo-count))
     :amk-muu
     (cond-> {:alakoodit {:amm-ope-erityisope-ja-opo {:count amm-ope-erityisope-ja-opo-count}}}
       total-amk-muu-count (assoc :count total-amk-muu-count))
     :aikuisten-perusopetus {:count aikuisten-perusopetus-count}
     :taiteen-perusopetus {:count taiteen-perusopetus-count}
     :kk-muu
     (cond-> {:alakoodit {:erikoislaakari {:count erikoislaakari-count}
                          :kk-opintojakso-normal {:count kk-opintojakso-normal-count}
                          :kk-opintojakso-avoin {:count kk-opintojakso-avoin-count}
                          :kk-opintokokonaisuus-normal {:count kk-opintokokonaisuus-normal-count}
                          :kk-opintokokonaisuus-avoin {:count kk-opintokokonaisuus-avoin-count}
                          :ope-pedag-opinnot {:count ope-pedag-opinnot-count}
                          :erikoistumiskoulutus {:count erikoistumiskoulutus-count}}}
       total-kk-muu-count (assoc :count total-kk-muu-count))
     :muu {:count muu-count}}))

(defn- hakukaynnissa [rajain-counts] {:count (get rajain-counts :hakukaynnissa 0)})

(defn- jotpa [rajain-counts] {:count (get rajain-counts :jotpa 0)})

(defn- taydennyskoulutus [rajain-counts] {:count (get rajain-counts :taydennyskoulutus 0)})

(defn- tyovoimakoulutus [rajain-counts] {:count (get rajain-counts :tyovoimakoulutus 0)})

(defn- koulutuksenkesto [rajain-counts] {:count (get rajain-counts :koulutuksenkestokuukausina 0)})

(defn- yhteishaku
  [aggs]
  (let [yhteishaut (get-yhteishaut)]
    (reduce (fn [ret-val yhteishaku]
              (let [yhteishakuKey (keyword (:oid yhteishaku))]
                (assoc ret-val
                       yhteishakuKey
                       (-> yhteishaku
                           (dissoc :oid)
                           (assoc :count (get-in aggs [yhteishakuKey] 0))))))
            {}
            yhteishaut)))


(defn- alkamiskausi-value? [value]
  (or (contains? [:henkilokohtainen :ei-alkamiskausia] value)
      (re-matches #"^\d{4}-(kevat|syksy)$" (name value))))

(defn- alkamiskausi-counts [rajain-counts]
  (reduce (fn [result [rajain-value rajain-count]]
            (if (alkamiskausi-value? rajain-value)
              (assoc result rajain-value {:count (or rajain-count 0)})
              result))
          {}
          rajain-counts))

(defn generate-default-rajain-counts
  ([rajain-counts]
   (let [koodisto-counts (partial koodisto->rajain-counts rajain-counts)]
     {:opetuskieli (koodisto-counts "oppilaitoksenopetuskieli")
      :maakunta (koodisto-counts "maakunta")
      :kunta (koodisto-counts "kunta")
      :koulutustyyppi (koulutustyyppi rajain-counts)
      :koulutustyyppi-muu (koulutustyyppi-muu rajain-counts)
      :koulutusala (koodisto-counts "kansallinenkoulutusluokitus2016koulutusalataso1")
      :opetustapa (koodisto-counts "opetuspaikkakk")
      :opetusaika (koodisto-counts "opetusaikakk")
      :koulutuksenkestokuukausina (koulutuksenkesto rajain-counts)
      :valintatapa (koodisto-counts "valintatapajono")
      :hakukaynnissa (hakukaynnissa rajain-counts)
      :hakutapa (koodisto-counts "hakutapa")
      :jotpa (jotpa rajain-counts)
      :tyovoimakoulutus (tyovoimakoulutus rajain-counts)
      :taydennyskoulutus (taydennyskoulutus rajain-counts)
      :yhteishaku (yhteishaku rajain-counts)
      :pohjakoulutusvaatimus (koodisto-counts "pohjakoulutusvaatimuskonfo")
      :osaamisala (koodisto-counts "osaamisala")
      :lukiolinjaterityinenkoulutustehtava (koodisto-counts "lukiolinjaterityinenkoulutustehtava")
      :lukiopainotukset (koodisto-counts "lukiopainotukset")
      :alkamiskausi (alkamiskausi-counts rajain-counts)}))
  ([] (generate-default-rajain-counts {})))


(defn- create-oppilaitos-counts
  [oppilaitokset]
  (let [oppilaitos-oids (keys oppilaitokset)
        indexed-oppilaitokset (oppilaitos/get-many oppilaitos-oids false)
        oppilaitokset-with-nimet (reduce-kv (fn [target-map oppilaitos-oid oppilaitos]
                                              (let [indexed-oppilaitos (first (filter #(= (keyword (:oid %)) oppilaitos-oid) indexed-oppilaitokset))
                                                    updated-oppilaitos (-> oppilaitos
                                                                           (assoc :nimi (:nimi indexed-oppilaitos))
                                                                           (assoc :count (:doc_count oppilaitos))
                                                                           (dissoc :doc_count :real_hits))]
                                                (assoc target-map oppilaitos-oid updated-oppilaitos))) {} oppilaitokset)]
    oppilaitokset-with-nimet))

(defn generate-rajain-counts-for-jarjestajat
  [rajain-counts oppilaitos-buckets]
  (assoc
   (generate-default-rajain-counts rajain-counts)
   :oppilaitos (create-oppilaitos-counts oppilaitos-buckets)))

(defn- rajain->obj [rajain koodi nimi] {:suodatin rajain :koodi koodi :nimi nimi})

(defn flattened-rajain-counts
  []
  (let [rajain-counts (generate-default-rajain-counts)]
    (reduce-kv (fn [r rajain m] (concat r (into [] (for [[k v] m] (rajain->obj rajain k (:nimi v))))))
               []
               rajain-counts)))
