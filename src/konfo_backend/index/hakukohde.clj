(ns konfo-backend.index.hakukohde
  (:require
    [konfo-backend.tools :refer [julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "hakukohde-kouta")

(defn get
  [oid]
  (let [hakukohde (get-source index oid)]
    (when (julkaistu? hakukohde)
      (if (julkaistu? (:valintaperuste hakukohde))
        hakukohde
        (dissoc hakukohde :valintaperuste)))))