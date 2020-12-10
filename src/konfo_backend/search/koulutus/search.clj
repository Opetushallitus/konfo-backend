(ns konfo-backend.search.koulutus.search
  (:require
    [konfo-backend.tools :refer [not-blank? log-pretty ammatillinen? koodi-uri-no-version]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query match-all-query aggregations inner-hits-query sorts external-query]]
    [konfo-backend.search.response :refer [parse parse-inner-hits parse-external]]
    [konfo-backend.elastic-tools :as e]
    [konfo-backend.index.eperuste :refer [get-kuvaukset-by-eperuste-ids]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial e/search-with-pagination index))

(defn- select-kuvaus
  [eperuste]
  (or (:suorittaneenOsaaminen eperuste) (:tyotehtavatJoissaVoiToimia eperuste) (:kuvaus eperuste)))

(defn- assoc-kuvaus-to-ammatillinen
  [kuvaukset-by-eperuste-id hit]
  (if-let [eperuste-id (:eperuste hit)]
    (->> kuvaukset-by-eperuste-id
         (filter #(= (:id %) eperuste-id))
         (first)
         (select-kuvaus)
         (assoc hit :kuvaus))
    hit))

(defn- with-kuvaukset
  [result]
  (let [ammatilliset (filter ammatillinen? (:hits result))
        kuvaukset-by-eperuste-id (->> ammatilliset
                                      (filter #(some? (:eperuste %)))
                                      (map :eperuste)
                                      (set)
                                      (get-kuvaukset-by-eperuste-ids))
        assoc-kuvaus (partial assoc-kuvaus-to-ammatillinen kuvaukset-by-eperuste-id)]
    (->> (for [hit (:hits result)]
           (if (ammatillinen? hit)
             (assoc-kuvaus hit)
             hit))
         (vec)
         (assoc result :hits))))

(defn search
  [keyword lng page size sort order constraints]
  (let [query (if (match-all? keyword constraints)
                (match-all-query)
                (query keyword lng constraints))
        aggs (aggregations)]
    (log-pretty query)
    (log-pretty aggs)
    (koulutus-kouta-search
      page
      size
      #(-> % parse with-kuvaukset)
      :_source ["oid", "nimi", "koulutus", "tutkintonimikkeet", "kielivalinta", "kuvaus", "teemakuva", "eperuste", "opintojenLaajuus", "opintojenLaajuusyksikko", "opintojenLaajuusNumero", "koulutustyyppi"]
      :sort (sorts sort order lng)
      :query query
      :aggs aggs)))

(defn search-koulutuksen-jarjestajat
  [oid lng page size order tuleva? constraints]
  (e/search index
            parse-inner-hits
            :_source ["oid", "koulutus", "nimi"]
            :query (inner-hits-query oid lng page size order tuleva? constraints)))

(defn external-search
  [keyword lng page size sort order & {:as constraints}]
  (let [query (external-query keyword lng constraints)]
    (log-pretty query)
    (koulutus-kouta-search
      page
      size
      #(-> % parse-external with-kuvaukset)
      :_source ["oid", "nimi", "koulutus", "tutkintonimikkeet", "kielivalinta", "kuvaus", "teemakuva", "eperuste", "opintojenLaajuus", "opintojenLaajuusyksikko", "opintojenLaajuusNumero", "koulutustyyppi"]
      :sort (sorts sort order lng)
      :query query)))
