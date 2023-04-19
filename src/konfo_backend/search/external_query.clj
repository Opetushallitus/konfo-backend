(ns konfo-backend.search.external-query
  (:require [clojure.string :refer [blank?]]
            [konfo-backend.tools :refer [current-time-as-kouta-format]]
            [konfo-backend.search.tools :refer [make-search-term-query]]
            [konfo-backend.search.rajain.rajain-definitions :refer [common-filters constraints?]]))

(defn external-query
  [keyword constraints user-lng suffixes]
  (let [query-contents (cond-> {}
                               (not (blank? keyword)) (assoc
                                                          :must (make-search-term-query keyword user-lng suffixes))
                               (constraints? constraints) (assoc
                                                            :filter (common-filters
                                                                      constraints (current-time-as-kouta-format))))]
  {:nested {:path       "search_terms",
            :inner_hits {},
            :query      {:bool query-contents}}}))
