(ns konfo-backend.index.valintaperuste
  (:require
    [konfo-backend.tools :refer [allowed-to-view julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "valintaperuste-kouta")

(defn get
  [id draft?]
  (let [valintaperuste (get-source index id)]
    (when (allowed-to-view valintaperuste draft?)
      valintaperuste)))