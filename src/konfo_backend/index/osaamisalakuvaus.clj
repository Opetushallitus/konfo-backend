(ns konfo-backend.index.osaamisalakuvaus
  (:refer-clojure :exclude [get])
  (:require
    [konfo-backend.tools :refer [koodi-uri-no-version]]
    [konfo-backend.elastic-tools :refer [get-source search]]))

;TODO tilan pitäisi olla "julkaistu" eikä "valmis"

(defonce index "osaamisalakuvaus")

(def osaamisalakuvaus-search (partial search index))

(defn get
  [id]
  (let [osaamisalakuvaus (get-source index id)]
    (when (some-> osaamisalakuvaus :tila (= "valmis"))
      osaamisalakuvaus)))


(defn- kuvaus-result-mapper
  [result]
  (map (fn [source] {:nimi               (get-in source [:osaamisala :nimi])
                     :osaamisalakoodiUri (get-in source [:osaamisala :uri])
                     :id                 (:id source)
                     :eperuste-id        (:eperuste-oid source)
                     :suoritustapa       (:suoritustapa source)
                     :kuvaus             (:teksti source)}) (map :_source (some-> result :hits :hits))))

(defn get-kuvaukset-by-eperuste-id
  [eperuste-id]
  (osaamisalakuvaus-search kuvaus-result-mapper
                           :_source [:id, :eperuste-oid :suoritustapa :osaamisala.nimi, :osaamisala.uri, :teksti.fi, :teksti.sv, :teksti.en]
                           :query {:bool {:must {:term {:eperuste-oid eperuste-id}},
                                          :filter {:term {:tila "valmis"}}}}))
(defn get-kuvaukset-by-eperuste-ids
  [eperuste-ids]
  (if (= 1 (count eperuste-ids))
    (get-kuvaukset-by-eperuste-id (first eperuste-ids))
    (osaamisalakuvaus-search kuvaus-result-mapper
                             :_source [:id, :eperuste-oid, :suoritustapa :osaamisala.nimi, :osaamisala.uri, :teksti.fi, :teksti.sv, :teksti.en]
                             :query {:bool {:must {:terms {:eperuste-oid (vec eperuste-ids)}},
                                            :filter {:term {:tila "valmis"}}}})))