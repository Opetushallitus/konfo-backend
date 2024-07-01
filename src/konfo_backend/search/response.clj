(ns konfo-backend.search.response
  (:require [konfo-backend.search.rajain-counts :refer [generate-default-rajain-counts
                                                        generate-rajain-counts-for-jarjestajat]]
            [konfo-backend.search.rajain-definitions :refer [all-agg-defs max-agg-defs ->max-agg-id]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [log-pretty
                                         reduce-merge-map rename-key]]))

(defn- buckets-to-map
  [buckets]
  ; filters-aggregaatioilla bucketit palautuu objektina eikä taulukkona!
  (if (map? buckets)
    buckets
    (into {} (map (fn [x] [(keyword (:key x)) x]) buckets))))

(defn hits
  [response]
  (map (fn [x] (-> (:_source x) (assoc :_score (:_score x)))) (get-in response [:hits :hits])))

(defn- autocomplete-hits
  [response]
  (map (fn [x] (let [res (:_source x)]
                 (select-keys res [:oid :nimi :toteutustenTarjoajat])))
       (get-in response [:hits :hits])))

(defn- get-uniform-buckets [agg agg-key]
  ;nested-aggregaatioilla (esim. search_terms.hakutiedot.yhteishakuOid) on yksi ylimääräinen aggregaatiokerros
  (cond
    (nil? agg) {}
    ; single-bucket aggregaatio! esim. hakukaynnissa
    (contains? agg :real_hits) {agg-key agg}
    :else (let [buckets (get agg :buckets)
          ;Jos aggregaatiolla on rajaimia, on lisäksi "rajain"-aggregaatiokerros
                nested-agg (or (get agg agg-key) (get agg :rajain))]
            (if (empty? buckets)
              (get-uniform-buckets nested-agg agg-key)
              (buckets-to-map buckets)))))

(defn- ->doc_count
  [response agg-key]
  (let [hits-buckets (get-uniform-buckets (get-in response [:aggregations :hits_aggregation agg-key]) agg-key)
        mapper (fn [key] {key (get-in hits-buckets (concat [(keyword key)] [:real_hits :doc_count]))})]
    (reduce-merge-map mapper (keys hits-buckets))))

(defn- ->max-number
  [response max-agg-key]
  (let [agg-item (get-in response [:aggregations :hits_aggregation max-agg-key])]
    {max-agg-key (or (get-in agg-item [:value])
                     (get-in agg-item [:max-val :value])
                     0)}))

(defn- numbers-by-filter
  [response]
  (let [doc-counts (reduce-merge-map #(->doc_count response %) (map #(or (:agg-id %) (:id %)) all-agg-defs))
        max-numbers (reduce-merge-map #(->max-number response %) (map #(->max-agg-id (:id %)) max-agg-defs))]
    (merge doc-counts max-numbers)))

(defn- rajain-numbers
  [response]
  (generate-default-rajain-counts (numbers-by-filter response)))

(defn filters-for-jarjestajat
  [response]
  (generate-rajain-counts-for-jarjestajat (numbers-by-filter response)
                                          (let [oppilaitos-aggs (get-in response [:aggregations :hits_aggregation :oppilaitos])]
                                            (if (seq (get-in oppilaitos-aggs [:rajain :buckets]))
                                              (get-uniform-buckets oppilaitos-aggs :oppilaitos)
                                              {}))))

(defn parse
  [response]
  (log-pretty response)
  {:total   (get-in response [:hits :total :value])
   :hits    (hits response)
   :filters (rajain-numbers response)})

(defn parse-for-autocomplete
  [response]
  {:total   (get-in response [:hits :total :value])
   :hits    (autocomplete-hits response)})

(defn- inner-hit->toteutus-hit
  [inner-hit]
  (let [source (:_source inner-hit)]
    {:toteutusOid    (:toteutusOid source)
     :toteutusNimi   (:toteutusNimi source)
     :oppilaitosOid  (:oppilaitosOid source)
     :oppilaitosNimi (:nimi source)
     :kunnat         (get-in source [:metadata :kunnat])}))

(defn- valid-external-inner-hit [hit]
  (let [source (:_source hit)]
    (:toteutusOid source)))

(defn- external-hits
  [response]
  (map
   (fn [hit]
     (-> (:_source hit)
         (rename-key :eperuste :ePerusteId)
         (assoc :toteutukset (vec (map inner-hit->toteutus-hit (filter valid-external-inner-hit (get-in hit [:inner_hits :search_terms :hits :hits])))))))
   (get-in response [:hits :hits])))

(defn parse-external
  [response]
  {:total (get-in response [:hits :total :value])
   :hits  (filter (fn [hit] (not-empty (:toteutukset hit))) (external-hits response))})

(defn parse-inner-hits
  [response filter-generator hits-generator]
  (let [inner-hits (some-> response :hits :hits (first) :inner_hits :search_terms :hits)
        total-inner-hits (:value (:total inner-hits))]
    {:total   (or total-inner-hits 0)
     :hits    (hits-generator inner-hits)
     :filters (filter-generator response)}))


