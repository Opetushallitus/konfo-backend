(ns konfo-backend.organisaatio
  (:require
    [clj-elasticsearch.elastic-connect :refer [search get-document]]

    [clj-log.error-log :refer [with-error-logging]]
    [cheshire.core :as json]
    [konfo-backend.performance :refer [insert-query-perf]]
    [clojure.tools.logging :as log]))

(defn index-name [name] name)


(defn- create-hakutulos [organisaatiohakutulos]
  (let [organisaatio (:_source organisaatiohakutulos)
        score (:_score organisaatiohakutulos)]
    {:score score
     :oid (:oid organisaatio)
     :nimi (get-in organisaatio [:nimi :fi])
     :kayntiosoite (get-in organisaatio [:kayntiosoite :osoite])
     :postitoimipaikka (get-in organisaatio [:kayntiosoite :postitoimipaikka])}))

(defn- create-hakutulokset [hakutulos]
  (let [result (:hits hakutulos)
        count (:total hakutulos)]
    { :count count
     :result (map create-hakutulos result)}))

(def boost-values
  ["nimi*^30"
   "kayntiosoite.postitoimipaikka^50"
   "kayntiosoite.osoite^50"
   "postiosoite.postitoimipaikka^30"
   "postiosoite.osoite^10"
   "yhteystiedot.postitoimipaikka*^10"
   "yhteystiedot.osoite^10"
   ])

(defn text-search
  [keyword oids page size]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          res (->> (search
                     (index-name "organisaatio")
                     (index-name "organisaatio")
                     :from (if (pos? page) (- page 1) 0)
                     :size (if (pos? size) (if (< size 200) size 200) 0)
                     :query {
                              :bool {
                                      :should [
                                                {
                                                 :multi_match {
                                                                :query keyword,
                                                                :fields boost-values
                                                               }
                                                 },
                                                {
                                                 :terms {
                                                          :oid (vec oids)
                                                          }
                                                 }
                                       ],
                                       :must_not {
                                                  :range { :lakkautusPvm { :format "yyyy-MM-dd" :lt "now"}}
                                                  }
                                      }
                              }
                     :_source ["oid", "nimi", "kayntiosoite.osoite", "kayntiosoite.postitoimipaikka"])
                   :hits
                   (create-hakutulokset))]
      (insert-query-perf keyword (- (System/currentTimeMillis) start) start (count res))
      res)))