(ns konfo-backend.search.search
  (:require
    [konfo-backend.search.koulutus :as koulutus]
    [konfo-backend.search.oppilaitos :as oppilaitos]
    [konfo-backend.search.koulutusmoduuli :as koulutusmoduuli]
    [clojure.tools.logging :as log]))

(defn- not-blank [s] (not (clojure.string/blank? s)))

(defn constraints [& { :keys [koulutustyyppi paikkakunta kieli]}]
  (-> {}
      (cond-> (not-blank koulutustyyppi) (assoc :koulutustyyppi koulutustyyppi))
      (cond-> (not-blank paikkakunta) (assoc :paikkakunta paikkakunta))
      (cond-> (not-blank kieli) (assoc :kieli kieli))))

(defn search-koulutus
  [keyword lng page size constraints]
  (let [oids (oppilaitos/filter-organisaatio-oids lng constraints)]
    (koulutus/text-search keyword lng page size oids constraints)))

(defn search-oppilaitos
  [keyword lng page size constraints]
  (let [oids (koulutus/filter-organisaatio-oids keyword lng constraints)]
    (oppilaitos/text-search keyword lng page size oids constraints)))

(defn complete-komo [koulutukset komo]
  (if (= 1 (count koulutukset))
    (let [koulutus (first koulutukset)]
      (-> komo
          (assoc :haettavissa (koulutus/haettavissa (:searchData koulutus)))
          (assoc :nimi (get-in koulutus [:searchData :nimi]))
          (assoc :tyyppi (get-in koulutus [:searchData :tyyppi]))
          (assoc :tarjoaja (get-in koulutus [:organisaatio :nimi]))
          (assoc :koulutusOid (:oid koulutus))
          ;(assoc :aiheet (:aihees koulutus))
          ))
    (let [haettavissa (some true? (map #(koulutus/haettavissa (:searchData %)) koulutukset))]
      (-> komo
          (assoc :haettavissa haettavissa)
          (assoc :koulutusOid (:oid (first koulutukset)))))))

(defn search-koulutusmoduuli
  [keyword lng page size constraints]
  (let [oppilaitokset (oppilaitos/filter-organisaatio-oids lng constraints)
        koulutukset (group-by :komoOid (koulutus/filter-komo-oids keyword lng oppilaitokset constraints))
        result (koulutusmoduuli/oid-search keyword lng page size (keys koulutukset) constraints)]
    {:count  (:count result)
     :result (map #(complete-komo (get koulutukset (:oid %)) %) (:result result))}))
