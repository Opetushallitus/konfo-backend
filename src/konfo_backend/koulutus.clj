(ns konfo-backend.koulutus
  (:require
    [konfo-backend.search.toteutus :refer [haettavissa]]
    [clj-elasticsearch.elastic-connect :refer [search get-document]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer [insert-query-perf index-name]]))


(defn get-by-id [oid]
  (-> (get-document (index-name "koulutusmoduuli") (index-name "koulutusmoduuli") oid)
      (:_source)))

(defn parse-search-result [res] (map :_source (get-in res [:hits :hits])))

(defn get-koulutukset-by-koulutusmoduuli-oid [koulutusmoduuli-oid]
  (parse-search-result (search
                         (index-name "koulutus")
                         (index-name "koulutus")
                         :query { :bool { :must { :match { :komoOid koulutusmoduuli-oid } }
                                          :must_not { :range { :searchData.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"} } }
                                          :filter [{:match { :tila "JULKAISTU" }}
                                                     {:match { :searchData.haut.tila "JULKAISTU"}}] }}
                         :_source ["searchData.organisaatio.nimi", "oid", "searchData.tyyppi", "koulutusohjelma", "searchData.haut.hakuaikas", "searchData.hakukohteet.hakuaika", "searchData.nimi"])))

(defn toteutus-result [toteutus]
  { :oid (:oid toteutus)
    :haettavissa (haettavissa (:searchData toteutus))
    :osaamisala (:koulutusohjelma toteutus)
    :tarjoaja (get-in toteutus [:searchData :organisaatio])
    :tyyppi (get-in toteutus [:searchData :tyyppi])})

(defn fix-search-data [res toteutukset]
  (if (= 1 (count toteutukset))
    (assoc res :searchData {:nimi (get-in (first toteutukset) [:searchData :nimi])
                            :tyyppi (get-in (first toteutukset) [:searchData :tyyppi])})
    res))

(defn get-koulutus [oid]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          koulutus (get-by-id oid)
          toteutukset (get-koulutukset-by-koulutusmoduuli-oid oid)
          res (-> koulutus
                  (fix-search-data toteutukset)
                  (assoc :toteutukset (map toteutus-result toteutukset)))]
      (insert-query-perf (str "koulutusmoduuli: " oid) (- (System/currentTimeMillis) start) start (count res))
      res)))
