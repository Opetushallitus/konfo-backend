(ns konfo-backend.suosittelu.service
  (:require
    [konfo-backend.elastic-tools :refer [get-source search]]
    [konfo-backend.suosittelu.algorithm :refer :all]))

(defn- source-mapper
  [x]
  (->> x :hits :hits (map #(get-in % [:_source])) (vec)))

(defn- get-oids-by-indices
  [indices]
  (search "suosittelu"
          source-mapper
          :_source [:oid :jarjestysnumero]
          :query {:terms {:jarjestysnumero indices}}))

(defn- get-koulutukset-by-oids
  [oids]
  (search "koulutus-kouta"
          source-mapper
          :_source [:oid :nimi :teemakuva :koulutustyyppi :metadata.opintojenLaajuus :metadata.opintojenLaajuusyksikko :metadata.tutkintonimike]
          :query {:terms {:oid oids}}))

(defn- get-recommendation-indices
  [oids]
  (-> (for [oid oids]
        (get-source "suosittelu" oid))
      (vec)
      (calculate-top-n-recommendations 3)))

(defn get-recommendations
  [oids]
  (let [indices          (get-recommendation-indices oids)
        recommended-oids (get-oids-by-indices indices)
        koulutukset      (get-koulutukset-by-oids recommended-oids)]
    {:total 3
     :hits (-> (for [i indices]
                 (let [oid (first (filter #(= (:jarjestysnumero %) i) recommended-oids))
                       koulutus (first (filter #(= (:oid %) (:oid oid)) koulutukset))]
                   (merge (select-keys koulutus [:oid :nimi :teemakuva :koulutustyyppi])
                          (:metadata koulutus))))
               (vec))}))