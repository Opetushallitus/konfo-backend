(ns konfo-backend.search.koulutus
  (:require
    [konfo-backend.elastic-tools :refer :all]
    [konfo-backend.search.toteutus :refer [haettavissa]]
    [clojure.tools.logging :as log]))

(def koulutusmoduulit (partial search "koulutusmoduuli"))

(defn- match-oids [oids]
  (constant_score_query_terms :oid (vec oids) 10))

(defn- complete-koulutus [toteutukset koulutus]
  (if (= 1 (count toteutukset))
    (let [toteutus (first toteutukset)]
      (-> koulutus
          (assoc :haettavissa (haettavissa (:searchData toteutus)))
          (assoc :nimi (get-in toteutus [:searchData :nimi]))
          (assoc :tyyppi (get-in toteutus [:searchData :tyyppi]))
          (assoc :tarjoaja (get-in toteutus [:organisaatio :nimi]))
          (assoc :toteutusOid (:oid toteutus))
          ;(assoc :aiheet (:aihees koulutus))
          ))
    (let [koulutus-haettavissa (some true? (map #(haettavissa (:searchData %)) toteutukset))]
      (-> koulutus
          (assoc :haettavissa koulutus-haettavissa)
          (assoc :toteutusOid (:oid (first toteutukset))))))) ;TODO: Mitä oikeasti halutaan vertailuun?

(defn- create-hakutulos [toteutukset-by-oid komohakutulos]
  (let [komo (:_source komohakutulos)
        score (:_score komohakutulos)
        oid (:oid komo)]
    (complete-koulutus (get toteutukset-by-oid oid)
                       {:score      score
                        :oid        oid
                        :nimi       (get-in komo [:searchData :nimi])
                        :tila        (:tila komo)
                        :komotyyppi (:koulutusmoduuliTyyppi komo)
                        :tyyppi     (get-in komo [:searchData :tyyppi])})))

(defn- create-hakutulokset [toteutukset-by-oid hakutulos]
  (let [create-result (partial create-hakutulos toteutukset-by-oid)
        result (:hits hakutulos)
        count (:total hakutulos)]
    {:count count
     :result (map create-result result)}))

(defn koulutus-search
  [keyword lng page size toteutukset constraints]
  (let [toteutukset-by-oid (group-by :komoOid toteutukset)
        oids (keys toteutukset-by-oid)]
    (if (empty? oids)
      {:count 0 :result []}
      (koulutusmoduulit (query-perf-string "koulutusmoduuli" keyword constraints)
                        page
                        size
                        (partial create-hakutulokset toteutukset-by-oid)
                        :query { :bool { :must (match-oids oids) }}
                        :_source ["searchData.nimi", "oid", "koulutusmoduuliTyyppi", "searchData.tyyppi", "tila"]
                        :sort [:_score,
                               { (clojure.core/keyword (str "searchData.nimi.kieli_" lng ".keyword")) :asc}]))))