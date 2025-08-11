(ns konfo-backend.search.external-query
  (:require
   [clojure.string :refer [blank?]]
   [konfo-backend.search.rajain-definitions :refer [common-filters
                                                    constraints?]]
   [konfo-backend.search.rajain-tools :refer [->terms-query]]
   [konfo-backend.search.tools :refer [make-search-term-query]]
   [konfo-backend.tools :refer [current-time-as-kouta-format]]))

(defn external-query
  [keyword constraints user-lng suffixes]
  (let [luokittelutermit (:luokittelutermi constraints)
        query-contents (cond-> {}
                         (not (blank? keyword)) (assoc
                                                 :must (make-search-term-query keyword user-lng suffixes))
                         (constraints? constraints) (assoc
                                                     :filter (common-filters
                                                              constraints (current-time-as-kouta-format))))
        search_terms_query {:must {:nested {:path       "search_terms",
                                            :inner_hits {:size 500},
                                            :query      {:bool query-contents}}}}]
    {:bool (if (not-empty luokittelutermit)
             (merge search_terms_query
                    {:filter (->terms-query "luokittelutermit.keyword" luokittelutermit false)})
             search_terms_query)}))
