(ns konfo-backend.search.oppilaitos.query
  (:require
    [konfo-backend.koodisto.koodisto :refer [list]]
    [konfo-backend.tools :refer [not-blank?]]
    [konfo-backend.search.tools :refer [->lng-keyword]]
    [clojure.string :refer [lower-case]]
    [konfo-backend.tools :refer [current-time-as-kouta-format hakuaika-kaynnissa? ->koodi-with-version-wildcard ->lower-case-vec]]))

(defn sijainti?
  [constraints]
  (not (empty? (:sijainti constraints))))

(defn koulutustyyppi?
  [constraints]
  (not (empty? (:koulutustyyppi constraints))))

(defn vain-haku-kaynnissa?
  [constraints]
  (true? (:vainHakuKaynnissa constraints)))

(defn opetuskieli?
  [constraints]
  (not (empty? (:opetuskieli constraints))))

(defn koulutusala?
  [constraints]
  (not (empty? (:koulutusala constraints))))

(defn constraints?
  [constraints]
  (or (sijainti? constraints) (koulutustyyppi? constraints) (vain-haku-kaynnissa? constraints) (opetuskieli? constraints)))

(defn- ->term-filter
  [term]
  {(keyword term) {:term {:hits.sijainti.keyword term}}})

(defn- ->term-filters
  [terms]
  (reduce merge {} (map ->term-filter terms)))

(defn- maakunta-filters
  []
  (->term-filters (list "maakunta")))

(defn- aggs
  []
  {:sijainti {:filters {:filters (maakunta-filters)} :aggs {:oppilaitokset {:reverse_nested {}}}}})

(defn aggregations
  []
  {:hits_aggregation {:nested {:path "hits"}, :aggs (aggs)}})

(defn- ->terms-query
  [key coll]
  (if (= 1 (count coll))
    {:term  {(keyword key) (lower-case (first coll))}}
    {:terms {(keyword key) (->lower-case-vec coll)}}))

(defn- filters
  [constraints]
  (cond-> []
          (koulutustyyppi? constraints)  (conj (->terms-query :hits.koulutustyyppi.keyword (:koulutustyyppi constraints)))
          (opetuskieli? constraints)     (conj (->terms-query :hits.opetuskielet.keyword   (:opetuskieli constraints)))
          (sijainti? constraints)        (conj (->terms-query :hits.sijainti.keyword       (:sijainti constraints)))
          (koulutusala? constraints)     (conj (->terms-query :hits.koulutusalat.keyword   (:koulutusala constraints)))))

(defn- bool                                                 ;
  [keyword lng constraints]
  (cond-> {}
          (not-blank? keyword)       (assoc :must {:match {(->lng-keyword "hits.terms.%s" lng) (lower-case keyword)}})
          (constraints? constraints) (assoc :filter (filters constraints))))

(defn query
  [keyword lng constraints]
  {:nested {:path "hits", :query {:bool (bool keyword lng constraints)}}})