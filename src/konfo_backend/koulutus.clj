(ns konfo-backend.koulutus
  (:require
    [clj-elasticsearch.elastic-connect :refer [search get-document]]
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.performance :refer [insert-query-perf]]
    [clojure.tools.logging :as log]))

(defn index-name [name] name)

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

(defn get-koulutus-tulos
  [koulutus-oid]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          koulutus (#(assoc {} (:oid %) %) (get-koulutus koulutus-oid))
          hakukohteet-list (get-hakukohteet-by-koulutus koulutus-oid)
          hakukohteet (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec hakukohteet-list))
          haut-list (get-haut-by-oids (map :hakuOid (vals hakukohteet)))
          haut (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec haut-list))
          organisaatiot-list (#(assoc {} (:oid %) %) (get-organisaatios-by-oids [(get-in koulutus [:organisaatio :oid])]))
          organisaatiot (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec organisaatiot-list))
          res {:koulutus koulutus
               :haut haut
               :hakukohteet hakukohteet
               :organisaatiot organisaatiot}]
      (insert-query-perf (str "koulutus: " koulutus-oid) (- (System/currentTimeMillis) start) start (count res))
      res)))

(defn- haettavissa [hakuaika-array]
  (let [hakuajat (:hakuaikas (first hakuaika-array))
        hakuaika-nyt (fn [h] (<= (:alkuPvm h) (System/currentTimeMillis) (if (:loppuPvm h) (:loppuPvm h) (+ (System/currentTimeMillis) 100000))))]
    (not (empty? (filter #(hakuaika-nyt %) hakuajat)))))

(defn- create-hakutulos [koulutushakutulos]
  (let [koulutus (:_source koulutushakutulos)
        score (:_score koulutushakutulos)]
    {:score score
     :oid (:oid koulutus)
     :nimi (get-in koulutus [:searchData :nimi])
     :tarjoaja (get-in koulutus [:organisaatio :nimi])
     :avoin (:isAvoimenYliopistonKoulutus koulutus)
     :tyyppi (:moduulityyppi koulutus)
     :opintoala (get-in koulutus [:opintoala :nimi])
     :hakukohteet (get-in koulutus [:searchData :hakukohteet])
     :aiheet (:aihees koulutus)
     :haettavissa (haettavissa (get-in koulutus [:searchData :haut]))}))

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
           :must [ { :dis_max { :queries [
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
                  {:match { :tila "JULKAISTU" }},
                   {:match { :searchData.haut.tila "JULKAISTU"}}
           ],
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
                      :_source ["oid", "koulutuskoodi", "organisaatio", "isAvoimenYliopistonKoulutus", "moduulityyppi", "opintoala",
                                "hakukohteet.", "aihees.nimi", "searchData.nimi", "searchData.haut.hakuaikas"])
                    :hits
                    (create-hakutulokset))]
       (insert-query-perf keyword (- (System/currentTimeMillis) start) start (count res))
       res)))