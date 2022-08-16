(ns konfo-backend.search.filters
  (:require [konfo-backend.koodisto.koodisto :as k]
            [konfo-backend.index.haku :refer [get-yhteishaut]]
            [konfo-backend.tools :refer [reduce-merge-map]]))

(defn- koodi->filter
  [filter-counts koodi]
  (let [koodiUri (keyword (:koodiUri koodi))
        nimi (get-in koodi [:nimi])
        count (koodiUri filter-counts)
        alakoodit (when (contains? koodi :alakoodit)
                    (reduce-merge-map #(koodi->filter filter-counts %) (:alakoodit koodi)))]
    {koodiUri (cond-> {:nimi nimi}
                count (assoc :count count)
                alakoodit (assoc :alakoodit alakoodit))}))

(defn- koodisto->filters
  [filter-counts koodisto]
  (reduce-merge-map #(koodi->filter filter-counts %)
                    (:koodit (k/get-koodisto-with-cache koodisto))))

(defn- beta-koulutustyyppi
  [filter-counts]
  (let [ammatillinen-count (get filter-counts :amm 0)
        koulutustyyppi-info-and-counts (koodisto->filters filter-counts "koulutustyyppi")
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
     :amm (cond-> {:alakoodit (select-keys koulutustyyppi-info-and-counts
                                           [:koulutustyyppi_26 :koulutustyyppi_4 :koulutustyyppi_11
                                            :koulutustyyppi_12])}
            ammatillinen-count (assoc :count ammatillinen-count))
     :amk (cond-> {:alakoodit {:amk-alempi {:count amk-alempi-count}
                               :amk-ylempi {:count amk-ylempi-count}}}
            amk-count (assoc :count amk-count))
     :yo (cond-> {:alakoodit {:kandi {:count kandi-count}
                              :kandi-ja-maisteri {:count kandi-ja-maisteri-count}
                              :maisteri {:count maisteri-count}
                              :tohtori {:count tohtori-count}}}
           yo-count (assoc :count yo-count))}))

(defn- beta-koulutustyyppi-muu
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
        erikoislaakari-count (get filter-counts :erikoislaakari 0)
        kk-opintojakso-count (get filter-counts :kk-opintojakso 0)
        total-kk-muu-count (+ kk-opintojakso-count erikoislaakari-count)]
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
     :kk-muu
     (cond-> {:alakoodit {:kk-opintojakso {:count kk-opintojakso-count}
                          :erikoislaakari {:count erikoislaakari-count}}}
        total-kk-muu-count (assoc :count total-kk-muu-count))}))

(defn- hakukaynnissa [aggs] {:count (:hakukaynnissa aggs)})

(defn- jotpa [aggs] {:count (:jotpa aggs)})

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

(defn generate-filter-counts-external
  ([filter-counts]
   (let [filters (partial koodisto->filters filter-counts)]
     {:opetuskieli (filters "oppilaitoksenopetuskieli")
      :maakunta (filters "maakunta")
      :kunta (filters "kunta")
      :koulutustyyppi (beta-koulutustyyppi filter-counts)
      :koulutustyyppi-muu (beta-koulutustyyppi-muu filter-counts)
      :koulutusala (filters "kansallinenkoulutusluokitus2016koulutusalataso1")
      :opetustapa (filters "opetuspaikkakk")
      :valintatapa (filters "valintatapajono")
      :hakukaynnissa (hakukaynnissa filter-counts)
      :hakutapa (filters "hakutapa")
      :jotpa (jotpa filter-counts)
      :yhteishaku (yhteishaku filter-counts)
      :pohjakoulutusvaatimus (filters "pohjakoulutusvaatimuskonfo")}))
  ([] (generate-filter-counts-external {})))

(defn generate-filter-counts
  ([filter-counts]
   (let [filters (partial koodisto->filters filter-counts)]
     {:opetuskieli (filters "oppilaitoksenopetuskieli")
      :maakunta (filters "maakunta")
      :kunta (filters "kunta")
      :koulutustyyppi (beta-koulutustyyppi filter-counts)
      :koulutustyyppi-muu (beta-koulutustyyppi-muu filter-counts)
      :koulutusala (filters "kansallinenkoulutusluokitus2016koulutusalataso1")
      :opetustapa (filters "opetuspaikkakk")
      :valintatapa (filters "valintatapajono")
      :hakukaynnissa (hakukaynnissa filter-counts)
      :hakutapa (filters "hakutapa")
      :jotpa (jotpa filter-counts)
      :yhteishaku (yhteishaku filter-counts)
      :pohjakoulutusvaatimus (filters "pohjakoulutusvaatimuskonfo")
      :osaamisala (filters "osaamisala")
      :lukiolinjaterityinenkoulutustehtava (filters "lukiolinjaterityinenkoulutustehtava")
      :lukiopainotukset (filters "lukiopainotukset")}))
  ([] (generate-filter-counts {})))

(defn generate-filter-counts-for-jarjestajat
  [filter-counts]
  (let [filters (partial koodisto->filters filter-counts)]
    {:opetuskieli           (filters "oppilaitoksenopetuskieli")
     :maakunta              (filters "maakunta")
     :kunta                 (filters "kunta")
     :opetustapa            (filters "opetuspaikkakk")
     :valintatapa           (filters "valintatapajono")
     :hakukaynnissa         (hakukaynnissa filter-counts)
     :hakutapa              (filters "hakutapa")
     :yhteishaku            (yhteishaku filter-counts)
     :pohjakoulutusvaatimus (filters "pohjakoulutusvaatimuskonfo")
     :lukiopainotukset      (filters "lukiopainotukset")
     :lukiolinjaterityinenkoulutustehtava (filters "lukiolinjaterityinenkoulutustehtava")
     :osaamisala            (filters "osaamisala")})
    )

(defn- filter->obj [suodatin koodi nimi] {:suodatin suodatin :koodi koodi :nimi nimi})

(defn flattened-filter-counts
  [for-external?]
  (if-let [result (if for-external? (generate-filter-counts-external) (generate-filter-counts))]
    (reduce-kv (fn [r suodatin m]
                 (concat r (into [] (for [[k v] m] (filter->obj suodatin k (:nimi v))))))
               []
               result)))
