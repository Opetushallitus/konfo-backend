(ns konfo-backend.index.toteutus
  (:require
    [konfo-backend.tools :refer :all]
    [konfo-backend.elastic-tools :refer [get-source search get-sources]]))

(defonce index "toteutus-kouta")

(defn get
  [oid draft?]
  (let [toteutus (get-source index oid)]
    (when (allowed-to-view toteutus draft?)
      toteutus)))

(defn get-many
  ([oids excludes]
   (get-sources index oids excludes))
  ([oids]
   (get-many oids [])))

(defn- parse-kuvaukset
  [result]
  (let [->kuvaus (fn [s] {(keyword (:oid s)) (get-in s [:metadata :kuvaus])})]
    (apply merge (map #(-> % :_source ->kuvaus) (some-> result :hits :hits)))))

(defn get-kuvaukset
  [oids]
  (when (seq oids)
    (search index
            parse-kuvaukset
            :_source ["oid", "metadata.kuvaus"]
            :size (count oids)
            :query {:terms {:oid (vec oids)}})))