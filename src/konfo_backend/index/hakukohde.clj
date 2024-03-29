(ns konfo-backend.index.hakukohde
  (:refer-clojure :exclude [get])
  (:require
    [konfo-backend.tools :refer [allowed-to-view]]
    [konfo-backend.elastic-tools :refer [get-source get-sources get-sources-with-selected-fields search]]))

(defonce index "hakukohde-kouta")

(defn get
  [oid draft?]
  (let [excludes ["koulutustyypit" "sora"]
        hakukohde (get-source index oid excludes)]
    (when (allowed-to-view hakukohde draft?)
      (if (allowed-to-view (:valintaperuste hakukohde) draft?)
        hakukohde
        (dissoc hakukohde :valintaperuste)))))

(defn get-many
  ([oids excludes]
   (get-sources index oids excludes))
  ([oids]
   (get-many oids [])))

(defn get-many-with-selected-fields
  [oids includes]
  (get-sources-with-selected-fields index oids includes))

(defn get-many-by-terms
  ([k values excludes]
    (search index
            #(->> % :hits :hits (map :_source) (vec))
            :_source {:excludes (vec excludes)}
            :size 1000
            :query {:terms {k (vec values)}}))
  ([k values]
    (search index
            #(->> % :hits :hits (map :_source) (vec))
            :size 1000
            :query {:terms {k (vec values)}})))
