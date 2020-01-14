(ns konfo-backend.search.response
  (:require
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.filters :refer [hierarkia]]
    [konfo-backend.index.toteutus :refer [get-kuvaukset]]
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

(defn- inner-hits-with-kuvaukset
  [inner-hits]
  (let [hits      (vec (map :_source (:hits inner-hits)))
        kuvaukset (get-kuvaukset (vec (distinct (remove nil? (map :toteutusOid hits)))))]
    (vec (for [hit hits
               :let [toteutusOid (:toteutusOid hit)]]
           (do (println hit)
               (-> hit
                   (select-keys [:koulutusOid :oppilaitosOid :toteutusOid :nimi :koulutustyyppi])
                   (merge (:metadata hit))
                   (assoc :kuvaus (if (not (nil? toteutusOid))
                                    (or ((keyword toteutusOid) kuvaukset) {})
                                    {}))))))))

(defn parse-inner-hits
  [response]
  (log-pretty response)
  (when-let [inner-hits (some-> response :hits :hits (first) :inner_hits :hits :hits)]
    {:total (:total inner-hits)
     :hits  (inner-hits-with-kuvaukset inner-hits)}))