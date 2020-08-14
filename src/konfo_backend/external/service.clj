(ns konfo-backend.external.service
  (:require
    [konfo-backend.koodisto.koodisto :as k]
    [konfo-backend.tools :refer [reduce-merge-map]]
    [konfo-backend.index.koulutus :as koulutus]
    [konfo-backend.index.toteutus :as toteutus]
    [konfo-backend.index.hakukohde :as hakukohde]
    [konfo-backend.index.valintaperuste :as valintaperuste]))

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

(defn get-hakukohde
  [oid]
  (let [hakukohde (some-> (hakukohde/get oid false)
                          (dissoc :muokkaaja :toteutus :hakulomakeAtaruId :yhdenPaikanSaanto))
        valintaperusteId (get-in hakukohde [:valintaperuste :id])
        valintaperuste (valintaperuste/get valintaperusteId false)]

    (-> hakukohde
        (assoc  :valintaperustekuvaus (dissoc valintaperuste :sorakuvausId :muokkaaja :julkinen :sorakuvaus))
        (dissoc :valintaperuste))))