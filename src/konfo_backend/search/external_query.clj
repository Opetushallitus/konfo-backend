(ns konfo-backend.search.external-query
  (:require [konfo-backend.search.tools :refer :all]))

(defn external-query
  [keyword constraints lng suffixes]
  {:nested {:path       "search_terms",
            :inner_hits {},
            :query      {:bool (fields keyword constraints lng suffixes)}}})

