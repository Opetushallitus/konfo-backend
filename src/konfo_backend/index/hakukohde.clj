(ns konfo-backend.index.hakukohde
  (:require
    [konfo-backend.tools :refer [julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "hakukohde-kouta")

(defn get
  [oid draft?]
  (let [hakukohde (get-source index oid)]
    (when (or draft? (julkaistu? hakukohde))
      (if (or draft? (julkaistu? (:valintaperuste hakukohde)))
        hakukohde
        (dissoc hakukohde :valintaperuste)))))