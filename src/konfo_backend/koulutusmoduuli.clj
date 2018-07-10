(ns konfo-backend.koulutusmoduuli
  (:require
    [clj-elasticsearch.elastic-connect :refer [search get-document]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer [insert-query-perf index-name]]))


(defn get-by-id [oid]
  (-> (get-document (index-name "koulutusmoduuli") (index-name "koulutusmoduuli") oid)
      (:_source)))

(defn get-koulutusmoduuli-tulos [oid]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          koulutusmoduuli (#(assoc {} (:oid %) %) (get-by-id oid))
          res {:koulutusmoduuli koulutusmoduuli}]
      (insert-query-perf (str "koulutusmoduuli: " oid) (- (System/currentTimeMillis) start) start (count res))
      res)))
