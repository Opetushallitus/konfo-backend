(ns konfo-backend.search.koulutus.search
  (:require
    [konfo-backend.tools :refer [not-blank? log-pretty ammatillinen? koodi-uri-no-version]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.koulutus.query :refer [create-query source-fields sort]]
    [konfo-backend.search.koulutus.response :refer [parse-response]]
    [konfo-backend.elastic-tools :refer [search-with-pagination]]
    [konfo-backend.index.eperuste :refer [get-kuvaukset-by-koulutuskoodit]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial search-with-pagination index))

(defn do-search?
  [keyword constraints]
  (or (not-blank? keyword) (constraints? constraints)))

(defn- with-kuvaukset
  [result]
  (let [koulutukset (:koulutukset result)
        ammatilliset (filter ammatillinen? koulutukset)]
    (let [get-koulutuskoodi-uri           #(get-in % [:koulutus :koodiUri])
          kuvaukset                       (get-kuvaukset-by-koulutuskoodit (set (map get-koulutuskoodi-uri ammatilliset)))
          kuvaus-by-koulutuskoodi-uri     (fn [uri] (first (filter #(= (:koulutuskoodiUri %) (koodi-uri-no-version uri)) kuvaukset)))
          get-kuvaus                      #(if-let [kuvaus (kuvaus-by-koulutuskoodi-uri %)] (:kuvaus kuvaus) {})]

      (assoc result :koulutukset (vec (map (fn [x] (-> x (assoc :ammatillisenKoulutuksenKuvaus (if (ammatillinen? x)
                                                                                                 (get-kuvaus (get-koulutuskoodi-uri x))
                                                                                                 {})))) koulutukset))))))

(defn search
  [keyword lng page size & {:as constraints}]
  (when (do-search? keyword constraints)
    (let [query (create-query keyword lng constraints)]
      (log-pretty query)
      (koulutus-kouta-search
        page
        size
        #(-> % parse-response with-kuvaukset)
        :_source source-fields,
        :sort (sort lng),
        :query query))))