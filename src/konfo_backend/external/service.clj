(ns konfo-backend.external.service
  (:require
    [konfo-backend.koodisto.koodisto :as k]
    [konfo-backend.tools :refer [reduce-merge-map]]
    [konfo-backend.index.koulutus :as koulutus]
    [konfo-backend.index.toteutus :as toteutus]))

(comment defn- toteutukset
  [koulutus toteutukset?]
  (if toteutukset?
    koulutus
    (dissoc koulutus :toteutukset)))

(comment defn- hakukohteet
  [koulutus hakukohteet?]
  (when hakukohteet?
    (assoc koulutus :hakukohteet )))

(defn get-koulutus
  [oid totetukset? hakukohteet? haut?]
  (some-> (koulutus/get oid false)
          (dissoc :muokkaaja :julkinen :esikatselu :toteutukset :organisaatiot)
          ;(toteutukset totetukset?)
          ))

(defn get-toteutus
  [oid]
  (some-> (toteutus/get oid false)
          (dissoc :muokkaaja :esikatselu :organisaatiot :hakutiedot)))