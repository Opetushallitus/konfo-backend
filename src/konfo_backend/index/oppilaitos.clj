(ns konfo-backend.index.oppilaitos
  (:require
    [konfo-backend.tools :refer [julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "oppilaitos-kouta")

(defn- dissoc-if-not-julkaistu
  [map key]
  (cond-> map (not (julkaistu? (key map))) (dissoc key)))

(defn- dissoc-kouta-data-if-not-julkaistu
  [oppilaitos]
  (cond-> (dissoc-if-not-julkaistu oppilaitos :oppilaitos)
          (seq (:osat oppilaitos))
          (assoc :osat (vec (map #(dissoc-if-not-julkaistu % :oppilaitoksenOsa) (:osat oppilaitos))))))

(defn get
  [oid]
  (let [oppilaitos (get-source index oid)]
    (dissoc-kouta-data-if-not-julkaistu oppilaitos)))


