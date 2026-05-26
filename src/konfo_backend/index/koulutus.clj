(ns konfo-backend.index.koulutus
  (:refer-clojure :exclude [get])
  (:require
    [konfo-backend.tools :refer [allowed-to-view julkaistut]]
    [konfo-backend.elastic-tools :refer [get-source get-sources]]
    [konfo-backend.eperuste.eperuste :as eperuste]))

(defonce index "koulutus-kouta")

(defn get
  [oid draft?]
  (let [koulutus (get-source index oid)]
    (when (allowed-to-view koulutus draft?)
      (-> koulutus
          (assoc :toteutukset (-> koulutus (:toteutukset) (julkaistut)))
          (update-in [:metadata :paikallisetTutkinnonOsat] eperuste/enrich-paikalliset-tutkinnon-osat)))))

(defn get-many
  ([oids excludes]
   (get-sources index oids excludes))
  ([oids]
   (get-many oids [])))