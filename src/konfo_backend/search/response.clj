(ns konfo-backend.search.response
  (:require
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]))

(defn- hits
  [response]
  (map :_source (get-in response [:hits :hits])))

(defn- ->doc_count
  [response filter]
  (let [buckets (get-in response [:aggregations :hits_aggregation filter :buckets])
        mapper  (fn [key] {key (get-in (key buckets) [:real_hits :doc_count])})]
    (reduce merge {} (map mapper (keys buckets)))))

(defn- filters
  [response]
  (reduce merge (map (fn [key] {key (->doc_count response key)}) [:koulutustyyppi :opetuskieli :sijainti :koulutusalataso1])))

(defn parse
  [response]
  (log-pretty response)
  {:hits    (hits response)
   :filters (filters response)})