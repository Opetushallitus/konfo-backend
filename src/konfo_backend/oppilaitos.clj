(ns konfo-backend.oppilaitos
  (:require
    [clj-elasticsearch.elastic-connect :refer [get-document]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer [insert-query-perf index-name]]))

(defn get-by-id [id]
  (-> (get-document (index-name "organisaatio") (index-name "organisaatio") id)
      (:_source)))

(defn get-oppilaitos [oid]
  (let [start (System/currentTimeMillis)
        raw-result (get-by-id oid)
        res { :kayntiosoite (:kayntiosoite raw-result)
              :postiosoite (:postiosoite raw-result)
              :nimi (:nimi raw-result)
              :yleiskuvaus (get-in raw-result [:metadata :data :YLEISKUVAUS])
              :yhteystiedot (:yhteystiedot raw-result)
              :metadata (:metadata raw-result)
              :xxxfordebug raw-result }]
    (insert-query-perf (str "organisaatio: " oid) (- (System/currentTimeMillis) start) start (count res))
    (if (not (nil? raw-result))
      res)))
