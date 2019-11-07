(ns konfo-backend.index.toteutus
  (:require
    [konfo-backend.tools :refer :all]
    [konfo-backend.elastic-tools :refer [get-source search]]))

(defonce index "toteutus-kouta")

(defn get
  [oid]
  (let [toteutus (get-source index oid)]
    (when (julkaistu? toteutus)
      (assoc toteutus :hakukohteet (-> toteutus :hakukohteet julkaistut)))))