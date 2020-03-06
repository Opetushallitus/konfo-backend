(ns konfo-backend.search.query
  (:require
    [konfo-backend.koodisto.koodisto :refer [list-koodi-urit]]
    [konfo-backend.tools :refer [not-blank?]]
    [konfo-backend.search.tools :refer :all]
    [clojure.string :refer [lower-case]]
    [konfo-backend.elastic-tools :refer [->size ->from]]
    [konfo-backend.tools :refer [current-time-as-kouta-format hakuaika-kaynnissa? ->koodi-with-version-wildcard ->lower-case-vec]]))

(defn- ->term-filter
  [field term]
  {(keyword term) {:term {field term}}})

(defn- ->term-filters
  [field terms]
  (reduce merge {} (map #(->term-filter field %) terms)))

(defn- ->filters-aggregation
  [field terms]
  {:filters {:filters (->term-filters field terms)} :aggs {:real_hits {:reverse_nested {}}}})

(defn- koodisto-filters
  [field koodisto]
  (->filters-aggregation field (list-koodi-urit koodisto)))

(defn- koulutustyyppi-filters
  [field]
  (->filters-aggregation field '["amm"]))

(defn- aggs
  []
  {:maakunta            (koodisto-filters :hits.sijainti.keyword       "maakunta")
   :kunta               (koodisto-filters :hits.sijainti.keyword       "kunta")
   :opetuskieli         (koodisto-filters :hits.opetuskielet.keyword   "oppilaitoksenopetuskieli")
   :koulutusala         (koodisto-filters :hits.koulutusalat.keyword   "kansallinenkoulutusluokitus2016koulutusalataso1")
   :koulutusalataso2    (koodisto-filters :hits.koulutusalat.keyword   "kansallinenkoulutusluokitus2016koulutusalataso2")
   :koulutustyyppi      (koulutustyyppi-filters :hits.koulutustyypit.keyword)
   :koulutustyyppitaso2 (koodisto-filters :hits.koulutustyypit.keyword "koulutustyyppi")})

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
          (koulutustyyppi? constraints)  (conj (->terms-query :hits.koulutustyypit.keyword (:koulutustyyppi constraints)))
          (opetuskieli? constraints)     (conj (->terms-query :hits.opetuskielet.keyword   (:opetuskieli constraints)))
          (sijainti? constraints)        (conj (->terms-query :hits.sijainti.keyword       (:sijainti constraints)))
          (koulutusala? constraints)     (conj (->terms-query :hits.koulutusalat.keyword   (:koulutusala constraints)))))

(defn- bool
  [keyword lng constraints]
  (cond-> {}
          (not-blank? keyword)       (assoc :must {:match {(->lng-keyword "hits.terms.%s" lng) {:query (lower-case keyword) :operator "and" :fuzziness "AUTO:8,12"}}})
          (constraints? constraints) (assoc :filter (filters constraints))))

(defn query
  [keyword lng constraints]
  {:nested {:path "hits", :query {:bool (bool keyword lng constraints)}}})

(defn inner-hits-query
  [oid lng page size sort tuleva?]
  (let [size (->size size)
        from (->from page size)]
    {:bool {:must [{:term {:oid oid}}
                   {:nested {:inner_hits {:_source ["hits.koulutusOid", "hits.toteutusOid", "hits.oppilaitosOid", "hits.nimi", "hits.metadata"]
                                          :from from
                                          :size size
                                          :sort {(str "hits.nimi." lng ".keyword") {:order sort :unmapped_type "string"}}}
                             :path "hits"
                             :query {:term {"hits.onkoTuleva" tuleva?}}}}]}}))