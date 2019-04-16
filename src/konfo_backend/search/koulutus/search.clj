(ns konfo-backend.search.koulutus.search
  (:require
    [konfo-backend.tools :refer [not-blank?]]
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.koulutus.query :refer [create-query source-fields sort]]
    [konfo-backend.search.koulutus.response :refer [parse-response]]
    [konfo-backend.elastic-tools :refer [search-with-pagination]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial search-with-pagination index))

(defn do-search?
  [keyword constraints]
  (or (not-blank? keyword) (constraints? constraints)))

(defn search
  [keyword lng page size & {:as constraints}]
  (when (do-search? keyword constraints)
    (let [query (create-query keyword lng constraints)]
      (log-pretty query)
      (koulutus-kouta-search
        page
        size
        parse-response
        :_source source-fields,
        :sort (sort lng),
        :query query))))