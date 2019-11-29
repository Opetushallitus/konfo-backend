(ns konfo-backend.search.koulutus.search
  (:require
    [konfo-backend.tools :refer [not-blank? log-pretty ammatillinen? koodi-uri-no-version]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query aggregations]]
    [konfo-backend.search.response :refer [parse]]
    [konfo-backend.elastic-tools :refer [search-with-pagination]]
    [konfo-backend.index.eperuste :refer [get-kuvaukset-by-koulutuskoodit]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial search-with-pagination index))

(defn- with-kuvaukset
  [result]
  (let [hits         (:hits result)
        kuvaukset    (get-kuvaukset-by-koulutuskoodit (set (map #(get-in % [:koulutus :koodiUri]) (filter ammatillinen? hits))))]

      (defn- assoc-kuvaus-to-ammatillinen
        [hit]
        (if (ammatillinen? hit)
          (let [koulutusKoodiUri (koodi-uri-no-version (get-in hit [:koulutus :koodiUri]))]
            (if-let [kuvaus (first (filter #(= (:koulutuskoodiUri %) koulutusKoodiUri) kuvaukset))]
              (assoc hit :kuvaus (:kuvaus kuvaus))
              hit))
          hit))

      (assoc result :hits (vec (map #(assoc-kuvaus-to-ammatillinen %) hits)))))

(defn search
  [keyword lng page size sort & {:as constraints}]
  (when (do-search? keyword constraints)
    (let [query (query keyword lng constraints)
          aggs (aggregations)]
      (log-pretty query)
      (log-pretty aggs)
      (koulutus-kouta-search
        page
        size
        #(-> % parse with-kuvaukset)
        :_source ["oid", "nimi", "koulutus", "tutkintonimikkeet", "kielivalinta", "kuvaus", "opintojenlaajuus", "opintojenlaajuusyksikko", "koulutustyyppi"]
        :sort [{(->lng-keyword "nimi.%s.keyword" lng) {:order sort}}]
        :query query
        :aggs aggs))))