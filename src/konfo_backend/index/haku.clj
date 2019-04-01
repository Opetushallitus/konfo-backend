(ns konfo-backend.index.haku
  (:require
    [konfo-backend.tools :refer [julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "haku-kouta")

(defn get
  [oid]
  (let [haku (get-source index oid)]
    (when (julkaistu? haku)
      (assoc haku :hakukohteet (julkaistut (:hakukohteet haku))))))