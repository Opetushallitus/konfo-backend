(ns konfo-backend.search.hakukohde.search
  (:require [clojure.core.memoize :as memo]
            [konfo-backend.index.haku :as haku]
            [konfo-backend.index.hakukohde :as hakukohde]
            [konfo-backend.index.toteutus :as toteutus]
            [konfo-backend.tools :refer [julkaistu?]]))

(defn- parse-results
  [result]
  (->> (get-in result [:hits :hits])
       (map :_source)
       (map #(select-keys % [:hakukohteet]))))

(defn- ->kohdejoukko-query
  [kohdejoukko]
  {:bool {:must {:term {:kohdejoukko.koodiUri kohdejoukko}}
          :filter {:term {:tila "julkaistu"}}}})

(defn- extract-hakukohteet
  [haku]
  (map #(assoc (select-keys % [:oid :nimi :hakuOid])
         :organisaatio (select-keys (:organisaatio %) [:nimi])
         :jarjestyspaikka (select-keys (:jarjestyspaikka %) [:nimi])
         :toteutus (select-keys (:toteutus %) [:oid]))
       (filter julkaistu? (:hakukohteet haku))))

(defn- search-hakukohteet
  [kohdejoukko]
  (if (seq kohdejoukko)
    (let [haut (haku/haku-search parse-results
                                 :_source [:oid :nimi :tila :hakukohteet]
                                 :query (->kohdejoukko-query kohdejoukko))
          hakukohteet (flatten (map extract-hakukohteet haut))
          indexed-hakukohteet
          (if (seq hakukohteet)
            (group-by :oid
                      (hakukohde/get-many-with-selected-fields (map :oid hakukohteet) ["oid" "koulutustyyppi"]))
            {})
          indexed-toteutukset
          (if (seq hakukohteet)
            (group-by :oid
                      (toteutus/get-many-with-selected-fields (map #(get-in % [:toteutus :oid])
                                                                   hakukohteet)
                                                              ["oid" "metadata.ammatillinenPerustutkintoErityisopetuksena"]))
            {})
          enriched-hakukohteet (map #(assoc %
                                            :koulutustyyppi
                                            (:koulutustyyppi (first (get indexed-hakukohteet (:oid %))))
                                            :ammatillinenPerustutkintoErityisopetuksena
                                            (get-in (first (get indexed-toteutukset (get-in % [:toteutus :oid])))
                                                    [:metadata :ammatillinenPerustutkintoErityisopetuksena]))
                                    hakukohteet)]
      {:total (count enriched-hakukohteet)
       :hits enriched-hakukohteet})
    {:total 0
     :hits []}))

(def search
  (memo/ttl search-hakukohteet {} :ttl/threshold (* 1000 60 15))) ; 15 minuutin cache
