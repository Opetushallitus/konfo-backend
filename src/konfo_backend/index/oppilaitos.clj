(ns konfo-backend.index.oppilaitos
  (:refer-clojure :exclude [get])
  (:require
   [konfo-backend.tools :refer [allowed-to-view]]
   [konfo-backend.elastic-tools :refer [get-source get-sources search]]))

(defonce index "oppilaitos-kouta")

(defn- dissoc-if-not-allowed-to-view
  [map key draft?]
  (cond-> map
    (not (allowed-to-view (key map) draft?)) (dissoc key)))

(defn- dissoc-kouta-data-if-not-allowed-to-view
  [draft? oppilaitos]
  (cond-> (dissoc-if-not-allowed-to-view oppilaitos :oppilaitos draft?)
    (seq (:osat oppilaitos))
    (assoc :osat (vec (map #(dissoc-if-not-allowed-to-view % :oppilaitoksenOsa draft?) (:osat oppilaitos))))))

(defn get
  [oid draft?]
  (some->> (get-source index oid)
           (dissoc-kouta-data-if-not-allowed-to-view draft?)))

(defn get-many
  [oids draft?]
  (when-not (empty? oids)
    (some->> (get-sources index oids nil)
             (map #(dissoc-kouta-data-if-not-allowed-to-view draft? %)))))

(defn- select-matching-osat
  [oid oppilaitos]
  (let [matching-osat (->> (:osat oppilaitos)
                           (filter #(= oid (:oid %))))
        parent-oids (->> matching-osat
                         (map :parentToimipisteOid)
                         set)
        parents (->> (:osat oppilaitos)
                     (filter #(contains? parent-oids (:oid %))))]
    (assoc oppilaitos :osat (concat matching-osat parents)))) ; matching osat must be first, see swap-osa-and-parent

(defn- swap-osa-and-parent
  [oppilaitos]
  (assoc (first (:osat oppilaitos)) :oppilaitos oppilaitos))

(defn- oppilaitos-osa-mapper
  [result]
  (let [oppilaitos (map :_source (some-> result :hits :hits))]
    oppilaitos))

(defn get-osa
  [oid draft?]
  (some->> (search index oppilaitos-osa-mapper :query {:term {:osat.oid.keyword oid}})
           (first)
           (select-matching-osat oid)
           (dissoc-kouta-data-if-not-allowed-to-view draft?)
           (swap-osa-and-parent)))
