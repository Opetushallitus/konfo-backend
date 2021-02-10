(ns konfo-backend.index.oppilaitos
  (:require
    [konfo-backend.tools :refer [julkaistu? esikatselu?]]
    [konfo-backend.elastic-tools :refer [get-source search]]))

(defonce index "oppilaitos-kouta")

(defn- dissoc-if-not-julkaistu
  [map key draft?]
  (cond-> map
          (and (not (and draft? (esikatselu? (key map)))) (not (julkaistu? (key map)))) (dissoc key)))

(defn- dissoc-kouta-data-if-not-julkaistu
  [draft? oppilaitos]
  (cond-> (dissoc-if-not-julkaistu oppilaitos :oppilaitos draft?)
          (seq (:osat oppilaitos))
          (assoc :osat (vec (map #(dissoc-if-not-julkaistu % :oppilaitoksenOsa draft?) (:osat oppilaitos))))))

(defn get
  [oid draft?]
  (some->> (get-source index oid)
           (dissoc-kouta-data-if-not-julkaistu draft?)))

(defn- select-matching-osat
  [oid oppilaitos]
  (->> (:osat oppilaitos)
       (filter #(= oid (:oid %)))
       (assoc oppilaitos :osat)))

(defn- swap-osa-and-parent
  [oppilaitos]
   (assoc (first (:osat oppilaitos)) :oppilaitos (dissoc oppilaitos :osat)))

(defn- oppilaitos-osa-mapper
  [result]
  (let [oppilaitos (map :_source (some-> result :hits :hits))]
    oppilaitos))

(defn get-by-osa
  [oid draft?]
  (some->> (search index oppilaitos-osa-mapper :query {:term {:osat.oid.keyword oid}})
           (first)
           (select-matching-osat oid)
           (dissoc-kouta-data-if-not-julkaistu draft?)
           (swap-osa-and-parent)))
