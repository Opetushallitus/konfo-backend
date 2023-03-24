(ns konfo-backend.search.filter.query-tools
  (:require
    [konfo-backend.search.tools :refer [->terms-query lukiopainotukset? lukiolinjaterityinenkoulutustehtava? osaamisala?]]
    ))

(defn keyword-terms-query
  [field coll]
  (->terms-query (str field ".keyword") coll))

(defn hakutieto-query
  [nested-field-name field-name constraint]
  {:nested
   {:path "search_terms.hakutiedot"
    :query
    {:bool
     {:filter (->terms-query (str nested-field-name "." field-name) constraint)}}}})


(defn single-tyoelama-boolean-query
  [key]
  {:term {(keyword (str "search_terms." key)) true}})

(defn make-combined-boolean-filter-query
  [constraints sub-filters]
  (let [selected-sub-filters (filter #(true? (get constraints (:id %))) sub-filters)]
    (when (not-empty selected-sub-filters)
      {:bool {:should (mapv #(:make-query %) selected-sub-filters)}})))

(defn- lukio-filters [constraints]
  (cond-> []
          (lukiopainotukset? constraints) (conj (->terms-query "lukiopainotukset" (:lukiopainotukset constraints)))
          (lukiolinjaterityinenkoulutustehtava? constraints) (conj (->terms-query "lukiolinjaterityinenkoulutustehtava" (:lukiolinjaterityinenkoulutustehtava constraints)))))

(defn- osaamisala-filters [constraints]
  [(->terms-query "osaamisalat" (:osaamisala constraints))])

(defn lukiolinjat-and-osaamisala-filters
  [constraints]
  (let [filters
        (concat []
                (when (or (lukiolinjaterityinenkoulutustehtava? constraints) (lukiopainotukset? constraints))
                  (lukio-filters constraints))
                (when (osaamisala? constraints)
                  (osaamisala-filters constraints)))]
    (if (empty? filters)
      {}
      {:should filters})))