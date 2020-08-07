(ns konfo-backend.index.koulutus
  (:require
    [konfo-backend.tools :refer [julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "koulutus-kouta")

(defn get
  [oid draft?]
  (let [koulutus (get-source index oid)]
    (when (or draft? (julkaistu? koulutus))
      (assoc koulutus :toteutukset (-> koulutus (:toteutukset) (julkaistut))))))