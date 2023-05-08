(ns konfo-backend.search.filters
  (:require [konfo-backend.index.haku :refer [get-yhteishaut]]
            [konfo-backend.index.oppilaitos :as oppilaitos]
            [konfo-backend.koodisto.koodisto :as k]
            [konfo-backend.tools :refer [debug-pretty reduce-merge-map]]))

(defn buckets-to-map
  [buckets]
  (into {} (map (fn [x] [(keyword (:key x)) x]) buckets)))

(defn- koodi->filter
  [filter-counts koodi]
  (let [koodiUri (keyword (:koodiUri koodi))
        nimi (get-in koodi [:nimi])
        count (get filter-counts koodiUri 0)
        alakoodit (when (contains? koodi :alakoodit)
                    (reduce-merge-map #(koodi->filter filter-counts %) (:alakoodit koodi)))]
    {koodiUri (cond-> {:nimi nimi}
                count (assoc :count count)
                alakoodit (assoc :alakoodit alakoodit))}))

(defn- koodisto->filters
  [filter-counts koodisto]
  (reduce-merge-map #(koodi->filter filter-counts %)
                    (:koodit (k/get-koodisto-with-cache koodisto))))

(defn- get-koulutustyyppi-amm-alakoodi-counts [filter-counts]
  (merge
   (select-keys (koodisto->filters filter-counts "koulutustyyppi")
                [:koulutustyyppi_26 :koulutustyyppi_4 :koulutustyyppi_11
                 :koulutustyyppi_12])
   {:muu-amm-tutkinto {:count (get filter-counts :muu-amm-tutkinto 0)}}))

(defn- koulutustyyppi
  [filter-counts]
  (let [ammatillinen-count (get filter-counts :amm 0)
        amm-alakoodi-counts (get-koulutustyyppi-amm-alakoodi-counts filter-counts)
        lukio-count (get filter-counts :lk 0)
        amk-count (get filter-counts :amk 0)
        yo-count (get filter-counts :yo 0)
        amk-alempi-count (get filter-counts :amk-alempi 0)
        amk-ylempi-count (get filter-counts :amk-ylempi 0)
        kandi-count (get filter-counts :kandi 0)
        kandi-ja-maisteri-count (get filter-counts :kandi-ja-maisteri 0)
        maisteri-count (get filter-counts :maisteri 0)
        tohtori-count (get filter-counts :tohtori 0)]
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
  [filter-counts]
  (let [amm-osaamisala-count (get filter-counts :amm-osaamisala 0)
        amm-tutkinnon-osa-count (get filter-counts :amm-tutkinnon-osa 0)
        telma-count (get filter-counts :telma 0)
        amm-muu-count (get filter-counts :amm-muu 0)
        muut-ammatilliset-count (+ amm-osaamisala-count amm-tutkinnon-osa-count telma-count amm-muu-count)
        amm-ope-erityisope-ja-opo-count (get filter-counts :amm-ope-erityisope-ja-opo 0)
        total-amk-muu-count amm-ope-erityisope-ja-opo-count
        tuva-normal-count (get filter-counts :tuva-normal 0)
        tuva-erityisopetus-count (get filter-counts :tuva-erityisopetus 0)
        total-tuva-count (get filter-counts :tuva 0)
        vapaa-sivistystyo-opistovuosi-count (get filter-counts :vapaa-sivistystyo-opistovuosi 0)
        vapaa-sivistystyo-muu-count (get filter-counts :vapaa-sivistystyo-muu 0)
        total-vapaa-sivistystyo-count (+ vapaa-sivistystyo-opistovuosi-count
                                         vapaa-sivistystyo-muu-count)
        aikuisten-perusopetus-count (get filter-counts :aikuisten-perusopetus 0)
        taiteen-perusopetus-count (get filter-counts :taiteen-perusopetus 0)
        erikoislaakari-count (get filter-counts :erikoislaakari 0)
        kk-opintojakso-normal-count (get filter-counts :kk-opintojakso-normal 0)
        kk-opintojakso-avoin-count (get filter-counts :kk-opintojakso-avoin 0)
        kk-opintokokonaisuus-normal-count (get filter-counts :kk-opintokokonaisuus-normal 0)
        kk-opintokokonaisuus-avoin-count (get filter-counts :kk-opintokokonaisuus-avoin 0)
        ope-pedag-opinnot-count (get filter-counts :ope-pedag-opinnot 0)
        erikoistumiskoulutus-count (get filter-counts :erikoistumiskoulutus 0)
        total-kk-muu-count (+ erikoislaakari-count kk-opintojakso-normal-count kk-opintojakso-avoin-count kk-opintokokonaisuus-normal-count kk-opintokokonaisuus-avoin-count ope-pedag-opinnot-count erikoistumiskoulutus-count)
        muu-count (get filter-counts :muu 0)]
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

(defn- hakukaynnissa [filter-counts] {:count (get filter-counts :hakukaynnissa 0)})

(defn- jotpa [filter-counts] {:count (get filter-counts :jotpa 0)})

(defn- taydennyskoulutus [filter-counts] {:count (get filter-counts :taydennyskoulutus 0)})

(defn- tyovoimakoulutus [filter-counts] {:count (get filter-counts :tyovoimakoulutus 0)})

(defn- yhteishaku
  [aggs]
  (let [yhteishaut (get-yhteishaut)]
    (reduce (fn [ret-val yhteishaku]
              (let [yhteishakuKey (keyword (:oid yhteishaku))]
                (assoc ret-val
                       yhteishakuKey
                       (-> yhteishaku
                           (dissoc :oid)
                           (assoc :count (yhteishakuKey aggs))))))
            {}
            yhteishaut)))

(defn generate-default-filter-counts
  ([filter-counts]
   (let [filters (partial koodisto->filters filter-counts)]
     {:opetuskieli (filters "oppilaitoksenopetuskieli")
      :maakunta (filters "maakunta")
      :kunta (filters "kunta")
      :koulutustyyppi (koulutustyyppi filter-counts)
      :koulutustyyppi-muu (koulutustyyppi-muu filter-counts)
      :koulutusala (filters "kansallinenkoulutusluokitus2016koulutusalataso1")
      :opetustapa (filters "opetuspaikkakk")
      :valintatapa (filters "valintatapajono")
      :hakukaynnissa (hakukaynnissa filter-counts)
      :hakutapa (filters "hakutapa")
      :jotpa (jotpa filter-counts)
      :tyovoimakoulutus (tyovoimakoulutus filter-counts)
      :taydennyskoulutus (taydennyskoulutus filter-counts)
      :yhteishaku (yhteishaku filter-counts)
      :pohjakoulutusvaatimus (filters "pohjakoulutusvaatimuskonfo")
      :osaamisala (filters "osaamisala")
      :lukiolinjaterityinenkoulutustehtava (filters "lukiolinjaterityinenkoulutustehtava")
      :lukiopainotukset (filters "lukiopainotukset")}))
  ([] (generate-default-filter-counts {})))

(defn- add-oppilaitos-nimet
  [oppilaitokset]
  (let [oppilaitos-oids (keys oppilaitokset)
        indexed-oppilaitokset (oppilaitos/get-many oppilaitos-oids false)
        oppilaitokset-with-nimet (reduce-kv (fn [target-map oppilaitos-oid oppilaitos]
                                              (let [indexed-oppilaitos (first (filter #(= (keyword (:oid %)) oppilaitos-oid) indexed-oppilaitokset))
                                                    updated-oppilaitos (-> oppilaitos
                                                                           (assoc :nimi (:nimi indexed-oppilaitos))
                                                                           (assoc :count (:doc_count oppilaitos))
                                                                           (dissoc :doc_count))]
                                                (assoc target-map oppilaitos-oid updated-oppilaitos))) {} oppilaitokset)]
    oppilaitokset-with-nimet))

(defn- oppilaitos-filters
  [aggs]
  (add-oppilaitos-nimet (buckets-to-map (get-in aggs [:inner_hits_agg :oppilaitos :buckets]))))

(defn generate-filter-counts-for-jarjestajat
  [filter-counts aggs]
  (assoc
    (generate-default-filter-counts filter-counts)
    :oppilaitos (oppilaitos-filters aggs)))

(defn- filter->obj [suodatin koodi nimi] {:suodatin suodatin :koodi koodi :nimi nimi})

(defn flattened-filter-counts
  []
  (let [filter-counts (generate-default-filter-counts)]
    (reduce-kv (fn [r suodatin m] (concat r (into [] (for [[k v] m] (filter->obj suodatin k (:nimi v))))))
               []
               filter-counts)))
