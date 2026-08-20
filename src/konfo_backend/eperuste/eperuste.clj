(ns konfo-backend.eperuste.eperuste
  (:require [konfo-backend.index.eperuste :as eperuste-index]
            [konfo-backend.index.tutkinnonosa :as tutkinnonosa-index]
            [konfo-backend.index.osaamisalakuvaus :as osaamisalakuvaus-index]
            [konfo-backend.index.toteutussuunnitelma :as toteutussuunnitelma-index]))

(defn get-eperuste-by-id
  [id]
  (eperuste-index/get id))

(defn get-kuvaus-by-eperuste-id
  [id with-osaamisalakuvaukset?]
  (when-let [eperuste (some-> id eperuste-index/get)]
    (cond-> (select-keys eperuste [:id :kuvaus :tyotehtavatJoissaVoiToimia :suorittaneenOsaaminen])
      with-osaamisalakuvaukset? (assoc :osaamisalat (osaamisalakuvaus-index/get-kuvaukset-by-eperuste-id id)))))

(defn get-tutkinnonosa-by-id
  [id]
  (tutkinnonosa-index/get id))

(defn in?
  [value coll]
  (some #(= value %) coll))

(defn get-tutkinnonosa-kuvaukset
  [eperuste-id koodi-urit]
  (cond->> (some->> (eperuste-index/get-tutkinnon-osa-kuvaukset-by-eperuste-ids [eperuste-id])
                    (first)
                    :tutkinnonOsat
                    (filter #(= (:tila %) "valmis")))
    (seq koodi-urit) (filter #(in? (:koodiUri %) koodi-urit))))

(defn get-osaamisala-kuvaukset
  [eperuste-id koodi-urit]
  (cond->> (osaamisalakuvaus-index/get-kuvaukset-by-eperuste-id eperuste-id)
    (seq koodi-urit) (filter #(in? (:osaamisalakoodiUri %) koodi-urit))))

(defn- extract-ammattitaito-fields [osa-data]
  (let [omat (get-in osa-data [:tosa :omatutkinnonosa])]
    {:ammattitaidonosoittamistavat (:ammattitaidonosoittamistavat omat)
     :ammattitaitovaatimukset      (or (:ammattitaitovaatimukset omat)
                                       (:ammattitaitovaatimuksetlista omat))
     :laajuus                      (:laajuus omat)}))

(defn enrich-paikalliset-tutkinnon-osat
  [paikalliset]
  (when (seq paikalliset)
    (let [ids              (distinct (map :opetussuunnitelmaId paikalliset))
          suunnitelmat-map (->> (toteutussuunnitelma-index/get-many ids)
                                (into {} (map (juxt :oid identity))))]
      (mapv (fn [osa]
              (let [suunnitelma (suunnitelmat-map (:opetussuunnitelmaId osa))
                    osa-data    (first (filter #(= (str (:id %)) (str (:tutkinnonosaId osa)))
                                              (:paikallisetTutkinnonOsat suunnitelma)))]
                (merge osa (extract-ammattitaito-fields osa-data))))
            paikalliset))))