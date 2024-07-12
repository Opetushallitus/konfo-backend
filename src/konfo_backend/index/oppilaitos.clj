(ns konfo-backend.index.oppilaitos
  (:refer-clojure :exclude [get])
  (:require [konfo-backend.elastic-tools :refer [clean-unwanted-fields
                                                 get-source get-sources search]]
            [konfo-backend.tools :refer [allowed-to-view]]))

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

(defn- oppilaitos-osa-mapper
  [result]
  (let [oppilaitos (map :_source (some-> result :hits :hits))]
    oppilaitos))

(defn- oppilaitos? [org]
  (some #(= (:koodiUri %) "organisaatiotyyppi_02") (:organisaatiotyyppi org)))

(defn- oppilaitos-osa-entry [osa-oid draft? parents-search-result]
  (let [oppilaitos-org (first (filter oppilaitos? parents-search-result))
        flat-oppilaitos-osat (mapcat :osat parents-search-result)
        osa (first (filter #(= (:oid %) osa-oid) flat-oppilaitos-osat))
        parent-oppilaitos (-> (:oppilaitos oppilaitos-org)
                              (assoc :nimi (:nimi oppilaitos-org))
                              (assoc :oid (:oid oppilaitos-org))
                              (assoc :osat flat-oppilaitos-osat))]
    (some->> (assoc osa :oppilaitos parent-oppilaitos)
             (dissoc-kouta-data-if-not-allowed-to-view draft?)
             (clean-unwanted-fields)
             (not-empty))))
(defn get-osa
  [oid draft?]
  (let [parents-search-result (search index oppilaitos-osa-mapper
                                      :query {:term {:osat.oid.keyword oid}})]
    (oppilaitos-osa-entry oid draft? parents-search-result)))
