(ns konfo-backend.search.filters
  (:require
    [konfo-backend.koodisto.koodisto :as k]
    [konfo-backend.tools :refer [reduce-merge-map]]))

(defn- koodi->filter
  [aggs koodi]
  (let [koodiUri  (keyword (:koodiUri koodi))
        nimi      (get-in koodi [:nimi])
        count     (koodiUri aggs)
        alakoodit (when (contains? koodi :alakoodit)
                    (reduce-merge-map #(koodi->filter aggs %) (:alakoodit koodi)))]

    {koodiUri (cond->     {:nimi nimi}
                count     (assoc :count count)
                alakoodit (assoc :alakoodit alakoodit))}))

(defn- koodisto->filters
  [aggs koodisto]
  (reduce-merge-map #(koodi->filter aggs %) (:koodit (k/get-koodisto koodisto))))

(defn- beta-koulutustyyppi
  [filter-counts]
  (let [ammatillinen-count (:amm filter-counts)
        koulutustyyppi-info-and-counts (koodisto->filters filter-counts "koulutustyyppi")]
    {:amm (cond-> {:alakoodit (select-keys koulutustyyppi-info-and-counts [:koulutustyyppi_1 :koulutustyyppi_4 :koulutustyyppi_11 :koulutustyyppi_12])}
            ammatillinen-count (assoc :count ammatillinen-count))}))

(defn- beta-koulutustyyppi-muu
  [filter-counts]
  (let [amm-osaamisala-count (get filter-counts :amm-osaamisala 0)
        amm-tutkinnon-osa-count (get filter-counts :amm-tutkinnon-osa 0)
        total-count (+ amm-osaamisala-count amm-tutkinnon-osa-count)]
    {:amm-muu (cond-> {:count total-count
                       :alakoodit {
                         :amm-tutkinnon-osa {:count amm-tutkinnon-osa-count},
                         :amm-osaamisala {:count amm-osaamisala-count}}})}))

(defn- hakukaynnissa
  [aggs]
  {:count (:hakukaynnissa aggs) })

(defn generate-filter-counts
  ([filter-counts]
   (let [filters (partial koodisto->filters filter-counts)]
     {:opetuskieli           (filters "oppilaitoksenopetuskieli")
      :maakunta              (filters "maakunta")
      :kunta                 (filters "kunta")
      :koulutustyyppi        (beta-koulutustyyppi filter-counts)
      :koulutustyyppi-muu    (beta-koulutustyyppi-muu filter-counts)
      :koulutusala           (filters "kansallinenkoulutusluokitus2016koulutusalataso1")
      :opetustapa            (filters "opetuspaikkakk")
      :valintatapa           (filters "valintatapajono")
      :hakukaynnissa         (hakukaynnissa filter-counts)
      :hakutapa              (filters "hakutapa")
      :pohjakoulutusvaatimus (filters "pohjakoulutusvaatimuskonfo")}))
  ([]
   (generate-filter-counts {})))

(defn generate-filter-counts-for-jarjestajat
  [aggs]
  (let [filters (partial koodisto->filters aggs)]
    {:opetuskieli (filters "oppilaitoksenopetuskieli")
     :maakunta    (filters "maakunta")
     :kunta       (filters "kunta")
     :opetustapa  (filters "opetuspaikkakk")}))


(defn filter->obj [suodatin koodi nimi]
  {:suodatin suodatin
   :koodi koodi
   :nimi nimi})

(defn flattened-filter-counts []
  (if-let [result (generate-filter-counts)]
    (reduce-kv (fn [r suodatin m]
                   (concat r (into []
                                   (for [[k v] m] (filter->obj suodatin k (:nimi v)))))
                   ) [] result)))