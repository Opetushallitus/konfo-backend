(ns konfo-backend.search.response
  (:require
    [konfo-backend.tools :refer [hakuaika-kaynnissa? log-pretty reduce-merge-map rename-key]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.filters :refer [generate-filter-counts generate-filter-counts-for-jarjestajat]]
    [konfo-backend.index.toteutus :refer [get-kuvaukset]]))

(defn- hits
  [response]
  (map (fn [x] (-> (:_source x) (assoc :_score (:_score x)))) (get-in response [:hits :hits])))

(defn- ->doc_count
  [response agg-key]
  (let [buckets (get-in response [:aggregations :hits_aggregation agg-key :buckets])
        mapper  (fn [key] {key (get-in (key buckets) [:real_hits :doc_count])})]
    (reduce-merge-map mapper (keys buckets))))

(defn- ->doc_count-for-subentity
  [response agg-key]
  (let [buckets (get-in response [:aggregations :hits_aggregation :inner_hits_agg agg-key :buckets])
        mapper  (fn [key] {key (get-in (key buckets) [:doc_count])})]
    (reduce-merge-map mapper (keys buckets))))

(defn- doc_count-by-filter
  [response]
  (let [agg-keys [:koulutustyyppi :koulutustyyppitaso2 :opetuskieli :maakunta :kunta :koulutusala :koulutusalataso2 :opetustapa :valintatapa :hakukaynnissa :hakutapa :yhteishaku :pohjakoulutusvaatimus]]
    (reduce-merge-map #(->doc_count response %) agg-keys)))

(defn- doc_count-by-filter-for-tarjoajat
  [response]
  (let [agg-keys [:koulutustyyppi :koulutustyyppitaso2 :opetuskieli :maakunta :kunta :koulutusala :koulutusalataso2 :opetustapa :valintatapa :hakukaynnissa :hakutapa :yhteishaku :pohjakoulutusvaatimus]]
    (reduce-merge-map #(->doc_count-for-subentity response %) agg-keys)))

(defn- doc_count-by-koodi-uri-for-jarjestajat
  [response]
  (let [agg-keys [:opetuskieli :maakunta :kunta :opetustapa :valintatapa :hakukaynnissa :hakutapa :yhteishaku :pohjakoulutusvaatimus]]
    (reduce-merge-map #(->doc_count-for-subentity response %) agg-keys)))

(defn- filter-counts
  [response]
  (generate-filter-counts (doc_count-by-filter response)))

(defn- filters-for-tarjoajat
  [response]
  (generate-filter-counts (doc_count-by-filter-for-tarjoajat response)))

(defn- filters-for-jarjestajat
  [response]
  (generate-filter-counts-for-jarjestajat (doc_count-by-koodi-uri-for-jarjestajat response)))

(defn parse
  [response]
  (log-pretty response)
  {:total   (get-in response [:hits :total])
   :hits    (hits response)
   :filters (filter-counts response)})

(defn- inner-hit->toteutus-hit
  [inner-hit]
  (let [source (:_source inner-hit)]
    {:toteutusOid         (:toteutusOid source)
     :toteutusNimi        (:toteutusNimi source)
     :oppilaitosOid       (:oppilaitosOid source)
     :oppilaitosNimi      (:nimi source)
     :kunnat              (get-in source [:metadata :kunnat])}))

(defn- external-hits
  [response]
  (map
    (fn [hit]
      (-> (:_source hit)
          (rename-key :eperuste :ePerusteId)
          (assoc :toteutukset (vec (map inner-hit->toteutus-hit (get-in hit [:inner_hits :hits :hits :hits]))))))
    (get-in response [:hits :hits])))

(defn parse-external
  [response]
  (log-pretty response)
  {:total   (get-in response [:hits :total])
   :hits    (external-hits response)})

(defn- inner-hits-with-kuvaukset
  [inner-hits]
  (let [hits      (vec (map :_source (:hits inner-hits)))
        kuvaukset (get-kuvaukset (vec (distinct (remove nil? (map :toteutusOid hits)))))]
    (vec (for [hit hits
               :let [toteutusOid (:toteutusOid hit)]]
           (-> hit
               (select-keys [:koulutusOid :oppilaitosOid :toteutusNimi :opetuskielet :toteutusOid :nimi :koulutustyyppi :kuva])
               (merge (:metadata hit))
               (assoc :hakukaynnissa (some hakuaika-kaynnissa? (flatten (map :hakuajat (:hakutiedot hit)))))
               (assoc :kuvaus (if (not (nil? toteutusOid))
                                (or ((keyword toteutusOid) kuvaukset) {})
                                {})))))))

(defn parse-inner-hits
  ([response]
   (parse-inner-hits response filter-counts))
  ([response filter-generator]
   (let [inner-hits (some-> response :hits :hits (first) :inner_hits :hits :hits)
         total-inner-hits (:total inner-hits)]
     {:total (or total-inner-hits 0)
      :hits (inner-hits-with-kuvaukset inner-hits)
      :filters (filter-generator response)})))

(defn parse-inner-hits-for-jarjestajat
  [response]
  (parse-inner-hits response filters-for-jarjestajat))

(defn parse-inner-hits-for-tarjoajat
  [response]
  (parse-inner-hits response filters-for-tarjoajat))
