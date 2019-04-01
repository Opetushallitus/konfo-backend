(ns konfo-backend.index.koulutus
  (:require
    [konfo-backend.tools :refer [julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "koulutus-kouta")

(defn get
  [oid]
  (let [koulutus (get-source index oid)]
    (when (julkaistu? koulutus)
      (assoc koulutus :toteutukset (-> koulutus (:toteutukset) (julkaistut))))))