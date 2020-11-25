(ns konfo-backend.eperuste.eperuste
  (:require [konfo-backend.index.eperuste :as eperuste-index]
            [konfo-backend.index.tutkinnonosa :as tutkinnonosa-index]
            [konfo-backend.index.osaamisalakuvaus :as osaamisalakuvaus-index]))

(defn get-eperuste-by-id
  [id]
  (when-let [eperuste (eperuste-index/get id)]
    (when (= "valmis" (:tila eperuste))
      eperuste)))

(defn get-osaamisalakuvaus-by-id
  [id]
  (osaamisalakuvaus-index/get id))

(defn get-kuvaus-by-eperuste-id
  [id with-osaamisalakuvaukset?]
  (when-let [eperuste (some-> id eperuste-index/get)]
    (cond-> (select-keys eperuste [:id :kuvaus :tyotehtavatJoissaVoiToimia :suorittaneenOsaaminen])
            with-osaamisalakuvaukset? (assoc :osaamisalat (osaamisalakuvaus-index/get-kuvaukset-by-eperuste-id id)))))

(defn get-tutkinnonosa-by-id
  [id]
  (when-let [tutkinnonosa (some-> id tutkinnonosa-index/get)]
    tutkinnonosa))

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