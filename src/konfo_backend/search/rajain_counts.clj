(ns konfo-backend.search.rajain-counts
  (:require [konfo-backend.index.haku :refer [get-yhteishaut]]
            [konfo-backend.index.oppilaitos :as oppilaitos]
            [konfo-backend.koodisto.koodisto :as k]
            [konfo-backend.tools :refer [koodi-uri-no-version reduce-merge-map]]
            [konfo-backend.index.lokalisointi :as lokalisointi]))

(defn- koodi->rajain-counts
  [rajain-counts koodi]
  (let [koodiUri (keyword (:koodiUri koodi))
        nimi (get-in koodi [:nimi])
        only-one-alakoodit (and (contains? koodi :alakoodit)
                                (= 1 (count (:alakoodit koodi))))
        count (get rajain-counts koodiUri 0)
        alakoodit (when (and (contains? koodi :alakoodit) (not only-one-alakoodit))
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

(defn- get-vaativa-tuki-counts [rajain-counts]
  (let [koodisto-counts (koodisto->rajain-counts rajain-counts "koulutustyyppi")]
    (array-map
     :koulutustyyppi_4 (:koulutustyyppi_4 koodisto-counts)
     :tuva-erityisopetus {:count (get rajain-counts :tuva-erityisopetus 0)})))

(defn- get-ammatilliset-without-erityisopetus-counts [rajain-counts]
  (let [koodisto-counts (koodisto->rajain-counts rajain-counts "koulutustyyppi")]
    (array-map
     :koulutustyyppi_26 (:koulutustyyppi_26 koodisto-counts)
     :koulutustyyppi_11 (:koulutustyyppi_11 koodisto-counts)
     :koulutustyyppi_12 (:koulutustyyppi_12 koodisto-counts)
     :muu-amm-tutkinto {:count (get rajain-counts :muu-amm-tutkinto 0)}
     :amm-osaamisala {:count (get rajain-counts :amm-osaamisala 0)}
     :amm-tutkinnon-osa {:count (get rajain-counts :amm-tutkinnon-osa 0)}
     :amm-muu {:count (get rajain-counts :amm-muu 0)})))

(defn- total-count
  [count-object]
  (let [level-count (get count-object :count 0)
        sub-levels (filter map? (vals count-object))]
    (reduce + level-count (map total-count sub-levels))))

(defn- koulutustyyppi
  [rajain-counts]
  (let [rcount (fn [rajain-key] (get rajain-counts rajain-key 0))
        vaativa-tuki-counts (get-vaativa-tuki-counts rajain-counts)
        ammatilliset-wo-erityisoepetus-counts (get-ammatilliset-without-erityisopetus-counts rajain-counts)
        total-ammatilliset-wo-erityisoepetus-count (total-count ammatilliset-wo-erityisoepetus-counts)
        total-vaativan-tuen-koulutus-count (total-count vaativa-tuki-counts)
        total-valmentavat-koulutukset-count (+ (rcount :tuva-normal) (rcount :telma) (rcount :vapaa-sivistystyo-opistovuosi))
        total-amk-count (+ (rcount :amk-alempi) (rcount :amk-ylempi) (rcount :amm-ope-erityisope-ja-opo)
                           (rcount :amk-opintojakso) (rcount :amk-opintojakso-avoin) (rcount :amk-opintokokonaisuus)
                           (rcount :amk-opintokokonaisuus-avoin) (rcount :amk-erikoistumiskoulutus))
        total-yo-count (+ (rcount :kandi) (rcount :kandi-ja-maisteri) (rcount :maisteri) (rcount :tohtori)
                          (rcount :yo-opintojakso) (rcount :yo-opintojakso-avoin) (rcount :yo-opintokokonaisuus)
                          (rcount :yo-opintokokonaisuus-avoin) (rcount :ope-pedag-opinnot) (rcount :erikoislaakari)
                          (rcount :yo-erikoistumiskoulutus))]
    (array-map :aikuisten-perusopetus {:count (rcount :aikuisten-perusopetus)}
               :taiteen-perusopetus {:count (rcount :taiteen-perusopetus)}
               :vaativan-tuen-koulutukset (cond-> {:alakoodit vaativa-tuki-counts}
                                            total-vaativan-tuen-koulutus-count (assoc :count total-vaativan-tuen-koulutus-count))
               :valmentavat-koulutukset (cond-> {:alakoodit (array-map
                                                             :tuva-normal {:count (rcount :tuva-normal)}
                                                             :telma {:count (rcount :telma)}
                                                             :vapaa-sivistystyo-opistovuosi {:count (rcount :vapaa-sivistystyo-opistovuosi)})}
                                          total-valmentavat-koulutukset-count (assoc :count total-valmentavat-koulutukset-count))
               :amm (cond-> {:alakoodit ammatilliset-wo-erityisoepetus-counts}
                      total-ammatilliset-wo-erityisoepetus-count (assoc :count total-ammatilliset-wo-erityisoepetus-count))
               :lk {:count (rcount :lk)}
               :amk (cond-> {:alakoodit (array-map
                                         :amk-alempi {:count (rcount :amk-alempi)}
                                         :amk-ylempi {:count (rcount :amk-ylempi)}
                                         :amm-ope-erityisope-ja-opo {:count (rcount :amm-ope-erityisope-ja-opo)}
                                         :amk-opintojakso-avoin {:count (rcount :amk-opintojakso-avoin)}
                                         :amk-opintojakso {:count (rcount :amk-opintojakso)}
                                         :amk-opintokokonaisuus-avoin {:count (rcount :amk-opintokokonaisuus-avoin)}
                                         :amk-opintokokonaisuus {:count (rcount :amk-opintokokonaisuus)}
                                         :amk-erikoistumiskoulutus {:count (rcount :amk-erikoistumiskoulutus)})}
                      total-amk-count (assoc :count total-amk-count))
               :yo (cond-> {:alakoodit (array-map
                                        :kandi {:count (rcount :kandi)}
                                        :kandi-ja-maisteri {:count (rcount :kandi-ja-maisteri)}
                                        :maisteri {:count (rcount :maisteri)}
                                        :tohtori {:count (rcount :tohtori)}
                                        :yo-opintojakso-avoin {:count (rcount :yo-opintojakso-avoin)}
                                        :yo-opintojakso {:count (rcount :yo-opintojakso)}
                                        :yo-opintokokonaisuus-avoin {:count (rcount :yo-opintokokonaisuus-avoin)}
                                        :yo-opintokokonaisuus {:count (rcount :yo-opintokokonaisuus)}
                                        :ope-pedag-opinnot {:count (rcount :ope-pedag-opinnot)}
                                        :erikoislaakari {:count (rcount :erikoislaakari)}
                                        :yo-erikoistumiskoulutus {:count (rcount :yo-erikoistumiskoulutus)})}
                     total-yo-count (assoc :count total-yo-count))
               :vapaa-sivistystyo-muu {:count (rcount :vapaa-sivistystyo-muu)}
               :muu {:count (rcount :muu)})))

(defn- hakukaynnissa [rajain-counts] {:count (get rajain-counts :hakukaynnissa 0)})

(defn- jotpa [rajain-counts] {:count (get rajain-counts :jotpa 0)})
(defn- taydennyskoulutus [rajain-counts] {:count (get rajain-counts :taydennyskoulutus 0)})
(defn- tyovoimakoulutus [rajain-counts] {:count (get rajain-counts :tyovoimakoulutus 0)})

(defn- koulutuksenkesto [rajain-counts] {:count (get rajain-counts :koulutuksenkestokuukausina 0)
                                         :max (get rajain-counts :koulutuksenkestokuukausina-max)})

(defn- maksullisuus [rajain-counts]
  {:maksullisuustyyppi
   {:maksuton {:count (get rajain-counts :maksuton 0)}
    :maksullinen {:count (get rajain-counts :maksullinen 0)}
    :lukuvuosimaksu {:count (get rajain-counts :lukuvuosimaksu 0)}}
   :maksunmaara {:count (get rajain-counts :maksullinen 0)
                 :max (get rajain-counts :maksullinen-max 0)}
   :lukuvuosimaksunmaara {:count (get rajain-counts :lukuvuosimaksu 0)
                          :max (get rajain-counts :lukuvuosimaksu-max 0)}
   :apuraha {:count (get rajain-counts :lukuvuosimaksu 0)}})

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


(defn- kausi-ja-vuosi-value? [value] (re-matches #"^\d{4}-(kevat|syksy)$" (name value)))

(defn- get-alkamiskausi-parts [value] (re-find #"^(\d{4})-(kevat|syksy)$" (name value)))

(defn- alkamiskausi-value? [value]
  (or (= value :henkilokohtainen)
      (kausi-ja-vuosi-value? value)))

(defn map-values [mapper m] (into {} (for [[k v] m] [k (mapper v)])))

(defn- alkamiskausi-counts [rajain-counts]
  (let [lokalisoinnit-fi (lokalisointi/get "fi")
        lokalisoinnit-sv (lokalisointi/get "sv")
        lokalisoinnit-en (lokalisointi/get "en")
        nimi-kevat {:fi (get-in lokalisoinnit-fi [:haku :kevat])
                    :sv (get-in lokalisoinnit-sv [:haku :kevat])
                    :en (get-in lokalisoinnit-en [:haku :kevat])}
        nimi-syksy {:fi (get-in lokalisoinnit-fi [:haku :syksy])
                    :sv (get-in lokalisoinnit-sv [:haku :syksy])
                    :en (get-in lokalisoinnit-en [:haku :syksy])}
        nimi-henkilokohtainen {:fi (get-in lokalisoinnit-fi [:haku :henkilokohtainen])
                               :sv (get-in lokalisoinnit-sv [:haku :henkilokohtainen])
                               :en (get-in lokalisoinnit-en [:haku :henkilokohtainen])}]
    (reduce (fn [result [rajain-value rajain-count]]
              (if (alkamiskausi-value? rajain-value)
                (assoc result rajain-value {:count (or rajain-count 0)
                                            :nimi (let [[_ vuosi kausi] (get-alkamiskausi-parts rajain-value)]
                                                    (cond
                                                      (= rajain-value :henkilokohtainen) nimi-henkilokohtainen
                                                      (= kausi "kevat") (map-values #(str % " " vuosi) nimi-kevat)
                                                      (= kausi "syksy") (map-values #(str % " " vuosi) nimi-syksy)
                                                      :else {}))})
                result))
            {}
            rajain-counts)))

(defn- pohjakoulutusvaatimus-counts
  [rajain-counts]
  ; All pohjakoulutusvaatimus results should include values (and hence counts) from
  ; documents that are missing the code set value - interpreted as "ei pohjakoulutusvaatimusta"
  (let [missing-count (get rajain-counts :pohjakoulutusvaatimuskonfo_missing 0)
        koodisto-counts (koodisto->rajain-counts rajain-counts "pohjakoulutusvaatimuskonfo")]
    (reduce (fn [final-counts [code data]]
              (assoc final-counts code
                     (update data :count #(+ missing-count %))))
            {}
            koodisto-counts)))

(defn generate-default-rajain-counts
  ([rajain-counts]
   (let [koodisto-counts (partial koodisto->rajain-counts rajain-counts)]
     {:opetuskieli (koodisto-counts "oppilaitoksenopetuskieli")
      :maakunta (koodisto-counts "maakunta")
      :kunta (koodisto-counts "kunta")
      :koulutustyyppi (koulutustyyppi rajain-counts)
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
      :pohjakoulutusvaatimus (pohjakoulutusvaatimus-counts rajain-counts)
      :osaamisala (koodisto-counts "osaamisala")
      :lukiolinjaterityinenkoulutustehtava (koodisto-counts "lukiolinjaterityinenkoulutustehtava")
      :lukiopainotukset (koodisto-counts "lukiopainotukset")
      :alkamiskausi (alkamiskausi-counts rajain-counts)
      :maksullisuus (maksullisuus rajain-counts)
      :hakualkaapaivissa {:30 {:count (get rajain-counts :hakualkaapaivissa_30 0)}}}))
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
