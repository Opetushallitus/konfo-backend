(ns konfo-backend.index.haku
  (:refer-clojure :exclude [get])
  (:require
    [konfo-backend.tools :refer [julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source get-sources search]]))

(defonce index "haku-kouta")
(defonce yhteishaku-koodi-uri "hakutapa_01")

(def haku-search (partial search index))

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

(defn- ->hakutyyppi-query
  [hakutyypit]
  (let [terms (if (= 1 (count hakutyypit))
                {:term  {:hakutapa.koodiUri (first hakutyypit)}}
                {:terms {:hakutapa.koodiUri (vec hakutyypit)}})]
    {:bool {:must terms, :filter {:term {:tila "julkaistu"}}}}))

(defn- parse-results
  [result]
  (->> (get-in result [:hits :hits])
       (map :_source)
       (map #(select-keys % [:oid :nimi]))))

; TODO Tämä palauttaa atm. kaikki yhteishaut, pitäisi tehdä rajausta että palautetaan vain ne joilla on jokin hakuaika käynnissä / päättynyt 6kk sisään - hakuajat on tallennettuna hauille ja hakukohteille
(defn get-yhteishaut
  []
  (haku-search parse-results
               :_source [:oid :nimi]
               :query (->hakutyyppi-query [yhteishaku-koodi-uri])))

(defn list-yhteishaut
  []
  (vec (map :oid (get-yhteishaut))))
