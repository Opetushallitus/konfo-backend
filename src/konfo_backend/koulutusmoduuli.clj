(ns konfo-backend.koulutusmoduuli
  (:require
    [clj-elasticsearch.elastic-connect :refer [search get-document]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer [insert-query-perf index-name]]))


(defn get-by-id [oid]
  (-> (get-document (index-name "koulutusmoduuli") (index-name "koulutusmoduuli") oid)
      (:_source)))

(defn parse-search-result [res] (map :_source (get-in res [:hits :hits])))

(defn get-koulutukset-by-koulutusmoduuli-oid [koulutusmoduuli-oid]
  (parse-search-result (search (index-name "koulutus") (index-name "koulutus") :query {:match {:komoOid koulutusmoduuli-oid}})))

(defn get-koulutusmoduuli-tulos [oid]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          koulutusmoduuli (#(assoc {} (:oid %) %) (get-by-id oid))
          koulutukset (get-koulutukset-by-koulutusmoduuli-oid oid)
          res {:koulutusmoduuli koulutusmoduuli
               :koulutukset koulutukset}]
      (insert-query-perf (str "koulutusmoduuli: " oid) (- (System/currentTimeMillis) start) start (count res))
      res)))
