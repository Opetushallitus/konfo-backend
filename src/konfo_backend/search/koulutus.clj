(ns konfo-backend.search.koulutus
  (:require
    [clj-elasticsearch.elastic-connect :refer [search]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer [insert-query-perf index-name]]))

(defn- hakuaika-nyt [hakuaika]
  (let [alkuPvm (:alkuPvm hakuaika)
        loppuPvm (if (:loppuPvm hakuaika) (:loppuPvm hakuaika) (+ (System/currentTimeMillis) 100000))]
    (<= alkuPvm (System/currentTimeMillis) loppuPvm)))

(defn haettavissa [searchData]
  (if (some true? (map #(contains? % :hakuaika) (:hakukohteet searchData)))
    (some true? (map #(hakuaika-nyt (:hakuaika %)) (:hakukohteet searchData)))
    (some true? (map #(hakuaika-nyt %) (flatten (map #(:hakuaikas %) (:haut searchData)))))))

(defn- create-hakutulos [koulutushakutulos]
  (let [koulutus (:_source koulutushakutulos)
        score (:_score koulutushakutulos)]
    {:score score
     :oid (:oid koulutus)
     :nimi (get-in koulutus [:searchData :nimi])
     :tarjoaja (get-in koulutus [:organisaatio :nimi])
     :avoin (:isAvoimenYliopistonKoulutus koulutus)
     :koulutustyyppi (:uri (:koulutustyyppi koulutus))
     :johtaaTutkintoon (:johtaaTutkintoon koulutus)
     :aiheet (:aihees koulutus)
     :haettavissa (haettavissa (:searchData koulutus))}))

(defn- create-hakutulokset [hakutulos]
  (let [result (:hits hakutulos)
        count (:total hakutulos)]
    { :count count
     :result (map create-hakutulos result)}))

(defn create-oid-list [hakutulokset]
  (let [get-oid (fn [h] (let [koulutus (:_source h)] (get-in koulutus [:organisaatio :oid])))]
    (map get-oid hakutulokset)))

(defn- koulutus-query-with-keyword [keyword]
  { :bool {
           :must { :dis_max { :queries [
                    { :constant_score {
                             :filter { :multi_match {:query keyword,
                                                     :fields ["searchData.nimi.kieli_fi"],
                                                     :operator "and" }},
                             :boost 10 }},
                    { :constant_score {
                             :filter { :multi_match {:query keyword,
                                                     :fields ["tutkintonimikes.nimi.kieli_fi^2" "koulutusala.nimi.kieli_fi^2" "tutkinto.nimi.kieli_fi^2"],
                                                     :operator "and" }},
                             :boost 5 }},
                    { :constant_score {
                             :filter { :multi_match {:query keyword,
                                                     :fields ["aihees.nimi.kieli_fi" "searchData.oppiaineet.kieli_fi" "ammattinimikkeet.nimi.kieli_fi"],
                                                     :operator "and" }},
                             :boost 4 }},
                    { :constant_score {
                             :filter { :multi_match { :query keyword,
                                                     :fields ["searchData.organisaatio.nimi.kieli_fi^5"],
                                                     :operator "and" }},
                             :boost 2 }}]}},
           :filter [
                    {:match { :tila "JULKAISTU" }},
                    {:match { :searchData.haut.tila "JULKAISTU"}}],
           :must_not { :range { :searchData.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"}}}
           }})

(defn oid-search
  [keyword]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          res (->> (search
                     (index-name "koulutus")
                     (index-name "koulutus")
                     :from 0
                     :size 10000
                     :query (koulutus-query-with-keyword keyword)
                     :_source ["organisaatio.oid"])
                   :hits
                   :hits
                   (create-oid-list))]
      (insert-query-perf keyword (- (System/currentTimeMillis) start) start (count res))
      res)))

(defn text-search
  [keyword page size]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          size (if (pos? size) (if (< size 200) size 200) 0)
          from (if (pos? page) (* (- page 1) size) 0)
          res (->> (search
                     (index-name "koulutus")
                     (index-name "koulutus")
                     :from from
                     :size size
                     :query (koulutus-query-with-keyword keyword)
                     :sort [
                            { :johtaaTutkintoon :asc },
                            :_score,
                            { :searchData.nimi.kieli_fi.keyword :asc},
                            { :searchData.organisaatio.nimi.kieli_fi.keyword :asc}
                            ]
                     :_source ["oid", "koulutustyyppi", "organisaatio", "isAvoimenYliopistonKoulutus",
                               "johtaaTutkintoon", "aihees.nimi", "searchData.nimi", "searchData.haut.hakuaikas"])
                   :hits
                   (create-hakutulokset))]
      (insert-query-perf keyword (- (System/currentTimeMillis) start) start (count res))
      res)))