(ns konfo-backend.search.koulutus.search
  (:require
    [konfo-backend.tools :refer [not-blank? log-pretty ammatillinen? koodi-uri-no-version]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query aggregations inner-hits-query]]
    [konfo-backend.search.response :refer [parse parse-inner-hits]]
    [konfo-backend.elastic-tools :as e]
    [konfo-backend.index.eperuste :refer [get-kuvaukset-by-koulutuskoodit, get-kuvaukset-by-eperuste-ids]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial e/search-with-pagination index))

(defn- assoc-kuvaus-to-ammatillinen
  [kuvaukset id-kuvaukset hit]
  (if (ammatillinen? hit)
    (let [koulutusKoodiUri (koodi-uri-no-version (get-in hit [:koulutus :koodiUri]))
          eperuste-id (:ePerusteId hit)]
      (if-let [kuvaus (or (first (filter #(= (:id %) eperuste-id) id-kuvaukset))
                          (first (filter #(= (:koulutuskoodiUri %) koulutusKoodiUri) kuvaukset)))]
        (assoc hit :kuvaus (:kuvaus kuvaus))
        hit))
    hit))

(defn- with-kuvaukset
  [result]
  (let [hits         (:hits result)
        kuvaukset    (get-kuvaukset-by-koulutuskoodit (set (map #(get-in % [:koulutus :koodiUri]) (filter ammatillinen? hits))))
        id-kuvaukset (get-kuvaukset-by-eperuste-ids (set (filter some? (map #(:ePerusteId %) (filter ammatillinen? hits)))))]
    (assoc result :hits (vec (map #(assoc-kuvaus-to-ammatillinen kuvaukset id-kuvaukset %) hits)))))

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
        :_source ["oid", "nimi", "koulutus", "tutkintonimikkeet", "kielivalinta", "kuvaus", "teemakuva", "opintojenlaajuus", "opintojenlaajuusyksikko", "koulutustyyppi"]
        :sort [{(->lng-keyword "nimi.%s.keyword" lng) {:order sort}}]
        :query query
        :aggs aggs))))

(defn search-koulutuksen-jarjestajat
  [oid lng page size sort tuleva?]
  (e/search index
            parse-inner-hits
            :_source ["oid", "koulutus", "nimi"]
            :query (inner-hits-query oid lng page size sort tuleva?)))
