(ns konfo-backend.toteutus
  (:require
    [clj-elasticsearch.elastic-connect :refer [search get-document]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer [insert-query-perf index-name]]
    [clojure.tools.logging :as log]))

(defn get-by-id
  [index type id]
  (-> (get-document (index-name index) (index-name type) id)
      (:_source)))

(defmacro get-koulutus [oid]
  `(update-in (get-by-id "koulutus" "koulutus" ~oid) [:searchData] dissoc :haut :organisaatio :hakukohteet))

(defn parse-search-result [res] (map :_source (get-in res [:hits :hits])))

(defn oid-query [oids] {:constant_score {:filter {:terms {:oid (map str oids)}}}})

(defn get-hakukohteet-by-koulutus [koulutus-oid]
  (parse-search-result (search (index-name "hakukohde") (index-name "hakukohde") :query {:match {:koulutukset koulutus-oid}})))

(defn get-haut-by-oids [oids]
  (parse-search-result (search (index-name "haku") (index-name "haku") :query (oid-query oids))))

(defn get-organisaatios-by-oids [oids]
  (parse-search-result (search (index-name "organisaatio") (index-name "organisaatio") :query (oid-query oids))))

(defn add-haun-nimi-and-oid-to-hakuaikas [haku]
    (map #(assoc % :hakuNimi (:nimi haku) :hakuOid (:oid haku)) (:hakuaikas haku)))

(defn parse-hakuajat [haut]
  (let [now (System/currentTimeMillis)
        unknown-time 4102437600000 ;Year 2100, will use this for comparisons when hakuaika has no loppuPvm
        hakuajat (flatten (map #(add-haun-nimi-and-oid-to-hakuaikas %) haut))
        aktiiviset (filter #(and (> (get % :loppuPvm unknown-time) now) (< (:alkuPvm %) now )) hakuajat)
        paattyneet (filter #(< (get % :loppuPvm unknown-time) now) hakuajat)
        tulevat (filter #(> (:alkuPvm %) now) hakuajat)]
    {:aktiiviset aktiiviset
     :tulevat tulevat
     :paattyneet paattyneet
     :kaikki hakuajat}))

(defn get-toteutus [oid]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          koulutus-raw (get-koulutus oid)
          hakukohteet-list (get-hakukohteet-by-koulutus oid)
          hakukohteet (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec hakukohteet-list))
          haut-list (get-haut-by-oids (map :hakuOid (vals hakukohteet)))
          haut (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec haut-list))
          organisaatiot-list (get-organisaatios-by-oids [(get-in koulutus-raw [:organisaatio :oid])])
          organisaatiot (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec organisaatiot-list))
          koulutus (#(assoc {} (:oid %) %) (assoc koulutus-raw :hakuajatFromBackend (parse-hakuajat haut-list)))
          res {:koulutus koulutus
               :haut haut
               :hakukohteet hakukohteet
               :organisaatiot organisaatiot}]
      (insert-query-perf (str "koulutus: " oid) (- (System/currentTimeMillis) start) start (count res))
      res)))