(ns konfo-backend.search.hakukohde.search
  (:require [clojure.core.memoize :as memo]
            [konfo-backend.index.haku :as haku]
            [konfo-backend.tools :refer [julkaistu?]]))

(defn- parse-results
  [result]
  (->> (get-in result [:hits :hits])
       (map :_source)
       (map #(select-keys % [:hakukohteet]))))

(defn- ->kohdejoukko-query
  [kohdejoukko]
  (let [terms {:term {:kohdejoukko.koodiUri kohdejoukko}}]
    {:bool {:must terms, :filter {:term {:tila "julkaistu"}}}}))

(defn- extract-hakukohteet
  [haku]
  (map #(assoc (select-keys % [:oid :nimi :hakuOid])
         :organisaatio (select-keys (:organisaatio %) [:nimi])
         :toteutus (select-keys (:toteutus %) [:oid]))
       (filter julkaistu? (:hakukohteet haku))))

(defn- search-hakukohteet
  [kohdejoukko]
  (if (seq kohdejoukko)
    (let [haut (haku/haku-search parse-results
                                 :_source [:oid :nimi :tila :hakukohteet]
                                 :query (->kohdejoukko-query kohdejoukko))
          hakukohteet (flatten (map extract-hakukohteet haut))]
      {:total (count hakukohteet)
       :hits hakukohteet})
    {:total 0
     :hits []}))

(def search
  (memo/ttl search-hakukohteet {} :ttl/threshold (* 1000 60 5))) ; viiden minuutin cache
