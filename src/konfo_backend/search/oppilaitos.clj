(ns konfo-backend.search.oppilaitos
  (:require
    [clj-elasticsearch.elastic-connect :refer [search]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer [insert-query-perf index-name]]))

(defn- create-hakutulos [organisaatiohakutulos]
  (let [organisaatio (:_source organisaatiohakutulos)
        score (:_score organisaatiohakutulos)]
    {:score score
     :oid (:oid organisaatio)
     :nimi (get-in organisaatio [:nimi])
     :oppilaitostyyppi (:oppilaitostyyppi (:searchData organisaatio))
     :kayntiosoite (get-in organisaatio [:kayntiosoite :osoite])
     :postitoimipaikka (get-in organisaatio [:kayntiosoite :postitoimipaikka])}))

(defn- create-hakutulokset [hakutulos]
  (let [result (:hits hakutulos)
        count (:total hakutulos)]
    { :count count
     :result (map create-hakutulos result)}))

(defn- oppilaitos-query-with-keyword-and-oids [keyword oids]
  { :bool {
           :must [
                  { :dis_max { :queries [
                         { :constant_score {
                                :filter { :multi_match { :query keyword
                                                        :fields ["searchData.oppilaitostyyppi.nimi.fi"]
                                                        :operator "and" }},
                                :boost 1000 }},
                         { :constant_score {
                                :filter { :multi_match { :query keyword,
                                                        :fields ["nimi.fi"],
                                                        :operator "and" }},
                                :boost 300 }},
                         { :constant_score {
                                :filter { :multi_match { :query keyword,
                                                        :fields ["postiosoite.postitoimipaikka" "kayntiosoite.postitoimipaikka" "yhteystiedot.postitoimipaikka"],
                                                        :operator "and" }},
                                :boost 5 }},
                         { :constant_score {
                                :filter { :multi_match { :query keyword,
                                                        :fields ["postiosoite.osoite" "kayntiosoite.osoite" "yhteystiedot.osoite"],
                                                        :operator "and" }},
                                :boost 4 }},
                         { :constant_score {
                                :filter { :terms { :oid (vec oids) }},
                                :boost 2 }}]}},
                  { :dis_max { :queries [
                         { :constant_score {
                                :filter { :match { :tyypit { :query "Koulutustoimija" }}},
                                :boost 200 }},
                         { :constant_score {
                                :filter { :match { :tyypit { :query "Oppilaitos" }}},
                                :boost 200 }},
                         { :constant_score {
                                :filter { :match { :tyypit { :query "Toimipiste" }}},
                                :boost 100 }}]}}],
           :must_not { :range { :lakkautusPvm { :format "yyyy-MM-dd" :lt "now"}} }}})

(defn text-search
  [keyword oids page size]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          size (if (pos? size) (if (< size 200) size 200) 0)
          from (if (pos? page) (* (- page 1) size) 0)
          res (->> (search
                     (index-name "organisaatio")
                     (index-name "organisaatio")
                     :from from
                     :size size
                     :query (oppilaitos-query-with-keyword-and-oids keyword oids)
                     :sort [:_score, { :nimi.fi.keyword :asc} ]
                     :_source ["oid", "nimi", "kayntiosoite.osoite", "kayntiosoite.postitoimipaikka", "searchData.oppilaitostyyppi"])
                   :hits
                   (create-hakutulokset))]
      (insert-query-perf keyword (- (System/currentTimeMillis) start) start (count res))
      res)))