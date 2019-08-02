(ns konfo-backend.index.toteutus
  (:require
    [konfo-backend.tools :refer :all]
    [konfo-backend.elastic-tools :refer [get-source search]]))

(defonce index "toteutus-kouta")

(defn- get-hakutieto
  [koulutus-oid toteutus-oid]
  (search "koulutus-kouta-search"
          #(some-> % :hits first :inner_hits :toteutukset :hits :hits first :_source)
          :_source [:oid]
          :query {:nested {:path       :toteutukset
                           :inner_hits {:_source [:toteutukset.oid, :toteutukset.haut]}
                           :query      {:bool {:must [{:term {:toteutukset.koulutus.oid koulutus-oid}}
                                                      {:term {:toteutukset.oid toteutus-oid}}]}}}}))

(defn- merge-hakutiedot
  [hakukohde hakutiedot]
  (let [hakutieto-haku      (first (filter #(= (:hakuOid hakukohde) (:hakuOid %)) hakutiedot))
        hakutieto-hakukohde (first (filter #(= (:oid hakukohde) (:hakukohdeOid %)) (:hakukohteet hakutieto-haku)))
        haun-aikataulu?     (true? (:kaytetaanHaunAikataulua hakutieto-hakukohde))
        haun-hakulomake?    (clojure.string/blank? (:hakulomaketyyppi hakutieto-hakukohde))]
    (->> {:aloituspaikat                (:aloituspaikat hakutieto-hakukohde)
          :ensikertalaisenAloituspaikat (:ensikertalaisenAloituspaikat hakutieto-hakukohde)
          :hakulomaketyyppi             (:hakulomaketyyppi (if haun-hakulomake? hakutieto-haku hakutieto-hakukohde))
          :hakulomake                   (:hakulomake (if haun-hakulomake? hakutieto-haku hakutieto-hakukohde))
          :hakuajat                     (:hakuajat (if haun-aikataulu? hakutieto-haku hakutieto-hakukohde))}
         (remove #(-> % val nil?))
         (merge hakukohde))))

(defn get
  [oid]
  (let [toteutus (get-source index oid)]
    (when (julkaistu? toteutus)
      (if-let [julkaistut-hakukohteet (not-empty (-> toteutus :hakukohteet julkaistut))]
        (let [hakutiedot (:haut (get-hakutieto (:koulutusOid toteutus) oid))]
          (assoc toteutus :hakukohteet (vec (map #(merge-hakutiedot % hakutiedot) julkaistut-hakukohteet))))
        (assoc toteutus :hakukohteet [])))))