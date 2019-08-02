(ns konfo-backend.index.osaamisalakuvaus
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
                     :suoritustapa       (:suoritustapa source)
                     :kuvaus             (:teksti source)}) (map :_source (some-> result :hits))))

(defn get-kuvaukset-by-eperuste-id
  [eperuste-id]
  (osaamisalakuvaus-search kuvaus-result-mapper
                           :_source [:id, :suoritustapa :osaamisala.nimi, :osaamisala.uri, :teksti.fi, :teksti.sv, :teksti.en]
                           :query {:bool {:must {:term {:eperuste-oid eperuste-id}},
                                          :filter {:term {:tila "valmis"}}}}))