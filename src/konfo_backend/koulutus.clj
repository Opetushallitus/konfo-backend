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
  `(dissoc (get-by-id "koulutus" "koulutus" ~oid) :searchData))

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
  ;(log/info hakuaika-array)
  (let [hakuajat (:hakuaikas (first hakuaika-array))
        hakuaika-nyt (fn [h] (<= (:alkuPvm h) (System/currentTimeMillis) (if (:loppuPvm h) (:loppuPvm h) (+ (System/currentTimeMillis) 100000))))]
    (not (empty? (filter #(hakuaika-nyt %) hakuajat)))))

(defn- create-hakutulos [koulutushakutulos]
  (let [koulutus (:_source koulutushakutulos)
        score (:_score koulutushakutulos)]
    {:score score
     :oid (:oid koulutus)
     :nimi (get-in koulutus [:koulutuskoodi :nimi])
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

(def boost-values
  ["organisaatio.nimi^50"
   "tutkintonimikes.nimi*^30"
   "koulutusohjelma.nimi*^30"
   "koulutusohjelmanNimiKannassa*^30"
   "koulutuskoodi.nimi^1000"
   "ammattinimikkeet.nimi*^30"
   "aihees.nimi*^10",
   "oppiaineet.oppiaine*^30"])

(defn create-oid-list [hakutulokset]
  ;(log/info hakutulokset)
  (let [get-oid (fn [h] (let [koulutus (:_source h)] (get-in koulutus [:organisaatio :oid])))]
    (map get-oid hakutulokset)))

(defn oid-search
  [keyword]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          res (->> (search
                     (index-name "koulutus")
                     (index-name "koulutus")
                     :from 0
                     :size 10000
                     :query {
                             :bool {
                                    :must [
                                           {
                                            :multi_match {
                                                          :query keyword,
                                                          :fields boost-values
                                                          :operator "and"
                                                          }
                                            }
                                           ],
                                    :must_not {
                                               :range { :searchData.haut.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"}}
                                               }
                                    }
                             }
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
                     :query {
                             :bool {
                                    :must [
                                             {
                                              :multi_match {
                                                            :query keyword,
                                                            :fields boost-values
                                                            :operator "and"
                                                            }
                                              }
                                             ],
                                    :must_not {
                                               :range { :searchData.haut.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"}}
                                               }
                                    }
                             }
                     :sort [
                       { :johtaaTutkintoon :asc },
                       :_score
                     ]
                     :_source ["oid", "koulutuskoodi", "organisaatio", "isAvoimenYliopistonKoulutus", "moduulityyppi", "opintoala",
                               "hakukohteet.", "aihees.nimi", "searchData.hakukohteet.nimi", "searchData.haut.hakuaikas"])
                   :hits
                   (create-hakutulokset))]
      (insert-query-perf keyword (- (System/currentTimeMillis) start) start (count res))
      res)))