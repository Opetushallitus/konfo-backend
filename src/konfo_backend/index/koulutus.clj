(ns konfo-backend.index.koulutus
  (:require
    [konfo-backend.tools :refer [esikatselu? julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source get-sources]]))

(defonce index "koulutus-kouta")

(defn get
  [oid draft?]
  (let [koulutus (get-source index oid)]
    (when (or (and draft? (esikatselu? koulutus)) (julkaistu? koulutus))
      (assoc koulutus :toteutukset (-> koulutus (:toteutukset) (julkaistut))))))

(defn get-many
  ([oids excludes]
   (get-sources index oids excludes))
  ([oids]
   (get-many oids [])))