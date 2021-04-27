(ns konfo-backend.index.haku
  (:refer-clojure :exclude [get])
  (:require
    [konfo-backend.tools :refer [julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source get-sources]]))

(defonce index "haku-kouta")

(defn get
  [oid]
  (let [haku (get-source index oid)]
    (when (julkaistu? haku)
      (assoc haku :hakukohteet (julkaistut (:hakukohteet haku))))))

(defn get-many
  ([oids excludes]
   (get-sources index oids excludes))
  ([oids]
   (get-many oids [])))