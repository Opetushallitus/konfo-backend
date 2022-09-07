(ns konfo-backend.index.toteutus
  (:refer-clojure :exclude [get])
  (:require [konfo-backend.tools :refer :all]
            [konfo-backend.elastic-tools :refer [get-source search get-sources]]
            [konfo-backend.util.haku-auki :refer [with-is-haku-auki]]))

(defonce index "toteutus-kouta")

(defn- filter-unallowed-hakukohteet
  [hakutieto draft?]
  (update hakutieto
          :hakukohteet
          (fn [hakukohteet] (filter #(allowed-to-view % draft?) hakukohteet))))

(defn- map-allowed-to-view-hakutiedot
  [toteutus draft?]
  (update toteutus
          :hakutiedot
          (fn [hakutiedot] (map #(filter-unallowed-hakukohteet % draft?) hakutiedot))))

(defn- filter-haut-and-hakukohteet
  [toteutus draft?]
  (as-> toteutus t
        (map-allowed-to-view-hakutiedot t draft?)
        (assoc t :hakutiedot (filter #(> (count (:hakukohteet %)) 0) (:hakutiedot t)))))

(defn get
  [oid draft?]
  (let [toteutus (get-source index oid)]
    (when (allowed-to-view toteutus draft?)
      (as-> toteutus t
            (filter-haut-and-hakukohteet t draft?)
            (assoc t :hakuAuki (toteutus-haku-kaynnissa? t))
            (assoc t :hakutiedot (with-is-haku-auki (:hakutiedot t)))))))

(defn get-many ([oids excludes] (get-sources index oids excludes)) ([oids] (get-many oids [])))

(defn- parse-kuvaukset
  [result]
  (let [->kuvaus (fn [s] {(keyword (:oid s)) (get-in s [:metadata :kuvaus])})]
    (apply merge
           (map #(-> %
                     :_source
                     ->kuvaus)
                (some-> result
                        :hits
                        :hits)))))

(defn get-kuvaukset
  [oids]
  (when (seq oids)
    (search index
            parse-kuvaukset
            :_source ["oid" "metadata.kuvaus"]
            :size (count oids)
            :query {:terms {:oid (vec oids)}})))