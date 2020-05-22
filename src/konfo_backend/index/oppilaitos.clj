(ns konfo-backend.index.oppilaitos
  (:require
    [konfo-backend.tools :refer [julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source]]
    [konfo-backend.search.oppilaitos.search :refer [index] :rename {index search-index}]))

(defonce index "oppilaitos-kouta")

(defn- dissoc-if-not-julkaistu
  [map key]
  (cond-> map (not (julkaistu? (key map))) (dissoc key)))

(defn- dissoc-kouta-data-if-not-julkaistu
  [oppilaitos]
  (cond-> (dissoc-if-not-julkaistu oppilaitos :oppilaitos)
          (seq (:osat oppilaitos))
          (assoc :osat (vec (map #(dissoc-if-not-julkaistu % :oppilaitoksenOsa) (:osat oppilaitos))))))

(defn- assoc-koulutusohjelmat
  [oid oppilaitos]
  (if-let [koulutusohjelmia (some-> (get-source search-index oid) :koulutusohjelmia)]
    (assoc oppilaitos :koulutusohjelmia koulutusohjelmia)
    oppilaitos))

(defn get
  [oid]
  (some->> (get-source index oid)
           (dissoc-kouta-data-if-not-julkaistu)
           (assoc-koulutusohjelmat oid)))


