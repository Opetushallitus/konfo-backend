(ns konfo-backend.search.response
  (:require
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.filters :refer [hierarkia]]
    [konfo-backend.tools :refer [reduce-merge-map]]))

(defn- hits
  [response]
  (map :_source (get-in response [:hits :hits])))

(defn- ->doc_count
  [response agg-key]
  (let [buckets (get-in response [:aggregations :hits_aggregation agg-key :buckets])
        mapper  (fn [key] {key (get-in (key buckets) [:real_hits :doc_count])})]
    (reduce-merge-map mapper (keys buckets))))

(defn- doc_count-by-koodi-uri
  [response]
  (let [agg-keys [:koulutustyyppi :koulutustyyppitaso2 :opetuskieli :maakunta :kunta :koulutusala :koulutusalataso2]]
    (reduce-merge-map #(->doc_count response %) agg-keys)))

(defn- filters
  [response]
  (hierarkia (doc_count-by-koodi-uri response)))

(defn parse
  [response]
  (log-pretty response)
  {:total   (get-in response [:hits :total])
   :hits    (hits response)
   :filters (filters response)})