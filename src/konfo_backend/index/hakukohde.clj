(ns konfo-backend.index.hakukohde
  (:require
    [konfo-backend.tools :refer [julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source get-sources search]]))

(defonce index "hakukohde-kouta")

(defn get
  [oid draft?]
  (let [hakukohde (get-source index oid)]
    (when (or draft? (julkaistu? hakukohde))
      (if (or draft? (julkaistu? (:valintaperuste hakukohde)))
        hakukohde
        (dissoc hakukohde :valintaperuste)))))

(defn get-many
  ([oids excludes]
   (get-sources index oids excludes))
  ([oids]
   (get-many oids [])))

(defn get-many-by-terms
  ([k values excludes]
    (search index
            #(->> % :hits :hits (map :_source) (vec))
            :_source {:excludes (vec excludes)}
            :query {:terms {k (vec values)}}))
  ([k values]
    (search index
            #(->> % :hits :hits (map :_source) (vec))
            :query {:terms {k (vec values)}})))