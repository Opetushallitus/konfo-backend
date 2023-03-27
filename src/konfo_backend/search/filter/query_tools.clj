(ns konfo-backend.search.filter.query-tools
  (:require [clojure.string :refer [lower-case]]
            [konfo-backend.tools :refer [->lower-case-vec]]))

(defn constraint?
  [constraints key]
  (not (empty? (key constraints))))

(defn lukiopainotukset?
  [constraints]
  (constraint? constraints :lukiopainotukset))

(defn lukiolinjaterityinenkoulutustehtava?
  [constraints]
  (constraint? constraints :lukiolinjaterityinenkoulutustehtava))

(defn osaamisala?
  [constraints]
  (constraint? constraints :osaamisala))

(defn ->terms-query
  [key coll]
  (let [search-field (keyword (str "search_terms." key))]
    (if (= 1 (count coll))
      {:term {search-field (lower-case (first coll))}}
      {:terms {search-field (->lower-case-vec coll)}})))

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

(defn- some-hakuaika-kaynnissa
  [current-time]
  {:should [{:bool {:filter [{:range {:search_terms.toteutusHakuaika.alkaa {:lte current-time}}}
                             {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}},
                                              {:range {:search_terms.toteutusHakuaika.paattyy {:gt current-time}}}]}}]}}
            {:nested {:path  "search_terms.hakutiedot.hakuajat"
                      :query {:bool {:filter [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte current-time}}}
                                              {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                                               {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt current-time}}}]}}]}}}}]})

(defn hakuaika-filter-query
  [current-time]
  {:bool (some-hakuaika-kaynnissa current-time)})