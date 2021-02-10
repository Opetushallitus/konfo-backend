(ns konfo-backend.index.valintaperuste
  (:require
    [konfo-backend.tools :refer [draft-view-allowed julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "valintaperuste-kouta")

(defn get
  [id draft?]
  (let [valintaperuste (get-source index id)]
    (when (or (draft-view-allowed valintaperuste draft?)
              (julkaistu? valintaperuste))
      valintaperuste)))