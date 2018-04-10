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

(defn- create-hakutulos [koulutushakutulos]
  (let [koulutus (:_source koulutushakutulos)
        score (:_score koulutushakutulos)]
    {:score score
     :oid (:oid koulutus)
     :nimi (get-in koulutus [:koulutuskoodi :nimi])
     :tarjoaja (get-in koulutus [:organisaatio :nimi])}))

(def boost-values
  ["*fi"
   "*sv"
   "*en"
   "organisaatio.nimi^30"
   "tutkintonimikes.nimi*^30"
   "koulutusohjelma.nimi*^30"
   "koulutusohjelmanNimiKannassa*^30"
   "koulutuskoodi.nimi^30"
   "ammattinimikkeet.nimi*^30"
   "aihees.nimi*^30"])

(defn text-search
  [query]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          res (->> (search
                     (index-name "koulutus")
                     (index-name "koulutus")
                     :query {:multi_match {:query query :fields boost-values}})
                   :hits
                   :hits
                   (map create-hakutulos))]
      (insert-query-perf query (- (System/currentTimeMillis) start) start (count res))
      res)))

