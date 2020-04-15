(ns konfo-backend.search.koulutus.search
  (:require
    [konfo-backend.tools :refer [not-blank? log-pretty ammatillinen? koodi-uri-no-version]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query aggregations inner-hits-query sorts]]
    [konfo-backend.search.response :refer [parse parse-inner-hits]]
    [konfo-backend.elastic-tools :as e]
    [konfo-backend.index.eperuste :refer [get-kuvaukset-by-koulutuskoodit, get-kuvaukset-by-eperuste-ids]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial e/search-with-pagination index))

(defn- assoc-kuvaus-to-ammatillinen
  [kuvaukset-by-eperuste-id kuvaukset-by-koulutuskoodi hit]
  (if (ammatillinen? hit)
    (if-let [kuvaus (if (some? (:eperuste hit))
                      (get kuvaukset-by-eperuste-id (:eperuste hit))
                      (get kuvaukset-by-koulutuskoodi (koodi-uri-no-version (get-in hit [:koulutus :koodiUri]))))]
      (assoc hit :kuvaus kuvaus)
      hit)
    hit))

(defn- with-kuvaukset
  [result]
  (let [ammatilliset (filter ammatillinen? (:hits result))
        kuvaukset-by-eperuste-id (->> ammatilliset
                                      (filter #(some? (:eperuste %)))
                                      (map :eperuste)
                                      (set)
                                      (get-kuvaukset-by-eperuste-ids))
        kuvaukset-by-koulutuskoodi (->> ammatilliset
                                        (filter #(nil? (:eperuste %)))
                                        (map #(get-in % [:koulutus :koodiUri]))
                                        (set)
                                        (get-kuvaukset-by-koulutuskoodit))
        assoc-kuvaus (partial assoc-kuvaus-to-ammatillinen kuvaukset-by-eperuste-id kuvaukset-by-koulutuskoodi)]
    (assoc result :hits (vec (map assoc-kuvaus (:hits result))))))

(defn search
  [keyword lng page size sort order & {:as constraints}]
  (when (do-search? keyword constraints)
    (let [query (query keyword lng constraints)
          aggs (aggregations)]
      (log-pretty query)
      (log-pretty aggs)
      (koulutus-kouta-search
        page
        size
        #(-> % parse with-kuvaukset)
        :_source ["oid", "nimi", "koulutus", "tutkintonimikkeet", "kielivalinta", "kuvaus", "teemakuva", "eperuste", "opintojenlaajuus", "opintojenlaajuusyksikko", "koulutustyyppi"]
        :sort (sorts sort order lng)
        :query query
        :aggs aggs))))

(defn search-koulutuksen-jarjestajat
  [oid lng page size sort tuleva?]
  (e/search index
            parse-inner-hits
            :_source ["oid", "koulutus", "nimi"]
            :query (inner-hits-query oid lng page size sort tuleva?)))
