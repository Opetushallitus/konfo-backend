(ns konfo-backend.index.valintaperuste
  (:require
    [konfo-backend.tools :refer [julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "valintaperuste-kouta")

(defn get
  [id]
  (let [valintaperuste (get-source index id)]
    (when (julkaistu? valintaperuste)
      valintaperuste)))